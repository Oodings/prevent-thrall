package com.preventthrall;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Prevent Thrall",
	description = "Stops accidental thrall recasts while your thrall is out (right-click still casts)",
	tags = {"thrall", "arceuus", "misclick", "spellbook", "resurrect", "necromancy"}
)
public class PreventThrallPlugin extends Plugin
{
	/**
	 * Ticks after a cast during which a matching thrall appearing next to us is taken to be ours, and during
	 * which the old thrall's expiry signals are ignored because a recast replaces it.
	 */
	static final int CAST_WINDOW_TICKS = 5;

	/** A newly summoned thrall must appear within this many tiles of us to be ours. */
	static final int CLAIM_DISTANCE = 3;

	/** Ticks after an area loads during which our thrall reappearing right next to us is picked up again. */
	static final int RECLAIM_WINDOW_TICKS = 3;

	/** Our thrall counts as left behind when idle and this far away... */
	static final int LEFT_BEHIND_DISTANCE = 10;

	/** ...for this many ticks in a row, so a thrall walking back to us after a kill doesn't count. */
	static final int LEFT_BEHIND_TICKS = 3;

	private static final String CAST_OPTION = "cast";
	private static final String COSMETICS_OPTION = "cosmetics";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PreventThrallConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PreventThrallOverlay overlay;

	private boolean thrallActive;

	/** The spell of our current thrall, or null if not known. */
	private ThrallSpell activeSpell;

	/** Our current thrall NPC, while we can see it. */
	private NPC ownThrall;

	/** Our thrall is out but no longer in the area we're in (teleported away, or a different room or floor). */
	private boolean lostSight;

	/** Tick we last recognised our thrall. */
	private int ownClaimTick = -1;

	/** Since our last cast: whether the game confirmed it (flag flipped or new thrall seen)... */
	private boolean recastConfirmed;

	/** ...and whether the previous thrall disappeared meanwhile, to re-check once the cast window ends. */
	private boolean oldLostDuringCast;

	/** Consecutive ticks our thrall has been idle and far away. */
	private int farIdleTicks;

	/** Safety net: the block clears itself after this tick even if every expiry signal is missed. */
	private int safetyExpiryTick = -1;

	/** The last thrall spell we let through, and when. Used to recognise the thrall that appears next. */
	private ThrallSpell pendingSpell;
	private int lastCastTick = -1;

	/** Re-read the game's thrall flag on the next tick (after login or enabling the plugin). */
	private boolean syncPending;

	/** Until this tick, our thrall reappearing right next to us after an area load is picked up again. */
	private int reclaimUntilTick = -1;

	/** True while the right-click menu is open, so menu selections can be told apart from plain left-clicks. */
	private boolean menuOpen;

	/** Spell widgets we have dimmed, so we only ever undo our own changes. */
	private final Set<Integer> dimmed = new HashSet<>();

	@Provides
	PreventThrallConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PreventThrallConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		syncPending = true;
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clearThrall();
		pendingSpell = null;
		lastCastTick = -1;
		menuOpen = false;
		syncPending = false;
		clientThread.invokeLater(this::restoreAll);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			// Also fires after every area load. Keep what we know; just re-check on the next tick.
			syncPending = true;
			reclaimUntilTick = client.getTickCount() + RECLAIM_WINDOW_TICKS;
		}
		else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			clearThrall();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// The game's "thrall out" flag for the local player only. On a recast it turns off and on again
		// in the same tick, so "off" right after our own cast is ignored.
		if (event.getVarbitId() != VarbitID.ARCEUUS_RESURRECTION_ACTIVE)
		{
			return;
		}

		if (event.getValue() != 0)
		{
			activateFromFlag();
			if (justCast() && pendingSpell != null)
			{
				// A recast that went through: the new thrall is the one we cast, even if we haven't seen it yet.
				activeSpell = pendingSpell;
				recastConfirmed = true;
				lostSight = false;
				farIdleTicks = 0;
				resetSafetyExpiry();
			}
		}
		else if (thrallActive && !justCast())
		{
			clearThrall();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		considerThrall(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		if (npc == ownThrall && ThrallNpcs.spellFor(npc.getId()) == null)
		{
			ownThrall = null;
			return;
		}
		considerThrall(npc);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc() == ownThrall)
		{
			loseSightOfThrall();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (!thrallActive || justCast() || (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM))
		{
			return;
		}

		String expiry = config.expiryMessage().trim().toLowerCase();
		String message = Text.removeTags(event.getMessage()).toLowerCase();
		if (!expiry.isEmpty() && message.contains(expiry))
		{
			clearThrall();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		boolean flagOn = client.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_ACTIVE) != 0;

		if (syncPending)
		{
			syncPending = false;
			reclaimUntilTick = client.getTickCount() + RECLAIM_WINDOW_TICKS;
			if (flagOn)
			{
				activateFromFlag();
			}
		}

		if (thrallActive && !justCast())
		{
			// Catch anything the events missed, e.g. a failed recast during the cooldown while the old
			// thrall expired, whose signals were ignored as part of the recast.
			if (!flagOn || (safetyExpiryTick >= 0 && client.getTickCount() > safetyExpiryTick))
			{
				clearThrall();
				return;
			}
		}

		if (!thrallActive)
		{
			return;
		}

		if (oldLostDuringCast && !justCast())
		{
			// Our previous thrall vanished during a cast. If that cast never went through (e.g. the
			// cooldown), the thrall we still have out was simply left behind.
			oldLostDuringCast = false;
			if (!recastConfirmed && ownThrall == null)
			{
				lostSight = true;
			}
		}

		if (ownThrall != null && !isInScene(ownThrall))
		{
			// Despawn events are not guaranteed on every area change.
			loseSightOfThrall();
		}

		if (ownThrall != null)
		{
			boolean farIdle = ownThrall.getInteracting() == null && distanceToPlayer(ownThrall) > LEFT_BEHIND_DISTANCE;
			farIdleTicks = farIdle ? farIdleTicks + 1 : 0;
		}
		else if (activeSpell == null && !lostSight && client.getTickCount() <= reclaimUntilTick)
		{
			// Out when we logged in: pick up the thrall standing right next to us.
			findThrallNextToUs();
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		menuOpen = true;

		if (!config.hideCosmetics())
		{
			return;
		}

		MenuEntry[] entries = client.getMenu().getMenuEntries();
		List<MenuEntry> kept = new ArrayList<>(entries.length);
		for (MenuEntry entry : entries)
		{
			boolean cosmetics = COSMETICS_OPTION.equalsIgnoreCase(Text.removeTags(entry.getOption()).trim());
			if (!(cosmetics && ThrallSpell.fromWidgetId(entry.getParam1()) != null))
			{
				kept.add(entry);
			}
		}
		if (kept.size() != entries.length)
		{
			client.getMenu().setMenuEntries(kept.toArray(new MenuEntry[0]));
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		boolean fromRightClickMenu = menuOpen;
		menuOpen = false;

		MenuEntry entry = event.getMenuEntry();
		ThrallSpell spell = ThrallSpell.fromWidgetId(entry.getParam1());
		if (spell == null || !isCastAction(entry.getType())
			|| !CAST_OPTION.equalsIgnoreCase(Text.removeTags(entry.getOption()).trim()))
		{
			return;
		}

		if (config.blockLeftClick() && !fromRightClickMenu && isBlocked(spell))
		{
			// A plain left-click on a blocked thrall spell: swallow it. "Cast" from the right-click menu still works.
			event.consume();
			return;
		}

		pendingSpell = spell;
		lastCastTick = client.getTickCount();
		recastConfirmed = false;
		oldLostDuringCast = false;
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!client.isMenuOpen())
		{
			menuOpen = false;
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		// Applied right before each frame is drawn, after the game's own spellbook scripts have run,
		// so the dimming cannot be briefly undone between frames (which caused flickering).
		if (!config.greyOut())
		{
			restoreAll();
			return;
		}

		for (ThrallSpell spell : ThrallSpell.values())
		{
			Widget widget = client.getWidget(spell.getWidgetId());
			if (widget == null)
			{
				continue;
			}

			if (isBlocked(spell))
			{
				widget.setOpacity(fadeOpacity());
				dimmed.add(spell.getWidgetId());
			}
			else if (dimmed.remove(spell.getWidgetId()))
			{
				widget.setOpacity(0);
			}
		}
	}

	/** Widget opacity (0 = solid, 255 = invisible) for the configured fade percentage. */
	private int fadeOpacity()
	{
		int percent = Math.max(0, Math.min(90, config.fadePercent()));
		return Math.round(percent * 255 / 100f);
	}

	/** Whether a plain left-click on this spell should be blocked right now. */
	boolean isBlocked(ThrallSpell spell)
	{
		if (config.blockLowerTiers() && !spell.isGreater())
		{
			return true;
		}
		if (!thrallActive || (config.unblockLeftBehind() && isLeftBehind()))
		{
			return false;
		}
		if (config.blockMode() == PreventThrallConfig.BlockMode.SAME_TYPE && activeSpell != null)
		{
			return activeSpell.getStyle() == spell.getStyle();
		}
		// All thralls mode, or same-type mode before we know the type: block everything to be safe.
		return true;
	}

	boolean isThrallActive()
	{
		return thrallActive;
	}

	/** The spell of the thrall we have out, or null if unknown. */
	ThrallSpell getActiveSpell()
	{
		return activeSpell;
	}

	/** Thralls can't follow between rooms: true once ours has been left somewhere else. */
	boolean isLeftBehind()
	{
		return thrallActive && (lostSight || farIdleTicks >= LEFT_BEHIND_TICKS);
	}

	/** A thrall NPC has appeared (or changed into one): decide whether it is ours. */
	private void considerThrall(NPC npc)
	{
		ThrallSpell spell = ThrallNpcs.spellFor(npc.getId());
		if (spell == null || npc == ownThrall)
		{
			return;
		}

		int distance = distanceToPlayer(npc);
		if (justCast() && spell == pendingSpell && distance <= CLAIM_DISTANCE)
		{
			// The thrall we just summoned: always anchor to the most recent one.
			claim(npc, spell);
		}
		else if (thrallActive && ownThrall == null && distance <= 1
			&& (activeSpell == null || spell == activeSpell)
			&& (activeSpell == null || client.getTickCount() <= reclaimUntilTick))
		{
			// Our thrall reappearing next to us after an area load, or the one we had out when we logged in.
			// Outside those moments, an identical thrall walking past is assumed to be a teammate's.
			claim(npc, spell);
		}
	}

	private void claim(NPC npc, ThrallSpell spell)
	{
		ownThrall = npc;
		activeSpell = spell;
		ownClaimTick = client.getTickCount();
		lostSight = false;
		farIdleTicks = 0;
		if (justCast())
		{
			recastConfirmed = true;
		}
		if (!thrallActive)
		{
			thrallActive = true;
		}
		resetSafetyExpiry();
	}

	private void activateFromFlag()
	{
		if (thrallActive)
		{
			return;
		}
		thrallActive = true;
		lostSight = false;
		farIdleTicks = 0;
		if (activeSpell == null && justCast())
		{
			activeSpell = pendingSpell;
		}
		resetSafetyExpiry();
	}

	private void loseSightOfThrall()
	{
		ownThrall = null;
		farIdleTicks = 0;
		if (justCast() && ownClaimTick < lastCastTick)
		{
			// The thrall from before our latest cast: normally replaced by it, so decide once the cast window ends.
			oldLostDuringCast = true;
		}
		else
		{
			// Still out (the flag would have ended the block otherwise), just not here: left behind.
			lostSight = true;
		}
	}

	private void findThrallNextToUs()
	{
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			ThrallSpell spell = ThrallNpcs.spellFor(npc.getId());
			if (spell != null && distanceToPlayer(npc) <= 1)
			{
				claim(npc, spell);
				return;
			}
		}
	}

	private void clearThrall()
	{
		thrallActive = false;
		activeSpell = null;
		ownThrall = null;
		lostSight = false;
		farIdleTicks = 0;
		ownClaimTick = -1;
		oldLostDuringCast = false;
		safetyExpiryTick = -1;
	}

	/**
	 * Thrall duration is one tick per (boosted) Magic level, doubled with the Master combat achievement reward.
	 * Allow for the longest case plus a margin, so this only ever catches a missed expiry.
	 */
	private void resetSafetyExpiry()
	{
		int magic = Math.max(1, client.getBoostedSkillLevel(Skill.MAGIC));
		safetyExpiryTick = client.getTickCount() + magic * 2 + 20;
	}

	private boolean justCast()
	{
		return lastCastTick >= 0 && client.getTickCount() - lastCastTick <= CAST_WINDOW_TICKS;
	}

	private boolean isInScene(NPC npc)
	{
		for (NPC other : client.getTopLevelWorldView().npcs())
		{
			if (other == npc)
			{
				return true;
			}
		}
		return false;
	}

	private int distanceToPlayer(NPC npc)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return Integer.MAX_VALUE;
		}
		WorldPoint here = player.getWorldLocation();
		WorldPoint there = npc.getWorldLocation();
		// distanceTo returns Integer.MAX_VALUE across planes, so a different floor counts as far away.
		return here == null || there == null ? Integer.MAX_VALUE : here.distanceTo(there);
	}

	private static boolean isCastAction(MenuAction action)
	{
		return action == MenuAction.CC_OP || action == MenuAction.CC_OP_LOW_PRIORITY;
	}

	private void restoreAll()
	{
		if (dimmed.isEmpty())
		{
			return;
		}

		for (Integer widgetId : dimmed)
		{
			Widget widget = client.getWidget(widgetId);
			if (widget != null)
			{
				widget.setOpacity(0);
			}
		}
		dimmed.clear();
	}
}
