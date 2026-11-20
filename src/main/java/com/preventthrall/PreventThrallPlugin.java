package com.preventthrall;

import com.google.inject.Provides;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Prevent Thrall",
	description = "Greys out thrall spells and ignores left-clicks on them while your thrall is active (right-click still casts)",
	tags = {"thrall", "arceuus", "misclick", "spellbook", "resurrect", "necromancy"}
)
public class PreventThrallPlugin extends Plugin
{
	/**
	 * Ticks after a deliberate (right-click) recast during which an expiry message is assumed to
	 * belong to the old thrall being replaced, so the block stays on for the new one.
	 */
	private static final int RECAST_GRACE_TICKS = 5;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PreventThrallConfig config;

	private boolean thrallActive;

	/** True while the right-click menu is open, so menu selections can be told apart from plain left-clicks. */
	private boolean menuOpen;

	/** Game tick of the last thrall cast we let through, or -1. */
	private int lastCastTick = -1;

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
		clientThread.invokeLater(this::syncState);
	}

	@Override
	protected void shutDown()
	{
		thrallActive = false;
		menuOpen = false;
		lastCastTick = -1;
		clientThread.invokeLater(this::restoreAll);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			syncState();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			thrallActive = false;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// This varbit only tracks the local player's own thrall. It starts the block; the block ends on
		// the thrall's expiry chat message, because thrall uptime varies (magic level, boosts, etc.).
		if (event.getVarbitId() == VarbitID.ARCEUUS_RESURRECTION_ACTIVE && event.getValue() != 0)
		{
			thrallActive = true;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (!thrallActive || (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM))
		{
			return;
		}

		String expiry = config.expiryMessage().trim().toLowerCase();
		String message = Text.removeTags(event.getMessage()).toLowerCase();
		if (expiry.isEmpty() || !message.contains(expiry))
		{
			return;
		}

		boolean justRecast = lastCastTick >= 0 && client.getTickCount() - lastCastTick <= RECAST_GRACE_TICKS;
		if (!justRecast)
		{
			thrallActive = false;
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		menuOpen = true;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		boolean fromRightClickMenu = menuOpen;
		menuOpen = false;

		MenuEntry entry = event.getMenuEntry();
		if (!ThrallSpell.isThrallSpell(entry.getParam1()) || !isCastAction(entry.getType()))
		{
			return;
		}

		if (thrallActive && config.blockLeftClick() && !fromRightClickMenu)
		{
			// A plain left-click on a thrall spell: swallow it. Choosing "Cast" from the right-click menu still works.
			event.consume();
			return;
		}

		lastCastTick = client.getTickCount();
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!client.isMenuOpen())
		{
			menuOpen = false;
		}

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

			if (thrallActive)
			{
				widget.setOpacity(config.dimAmount());
				dimmed.add(spell.getWidgetId());
			}
			else if (dimmed.remove(spell.getWidgetId()))
			{
				widget.setOpacity(0);
			}
		}
	}

	private static boolean isCastAction(MenuAction action)
	{
		return action == MenuAction.CC_OP || action == MenuAction.CC_OP_LOW_PRIORITY;
	}

	/** Re-reads the thrall state from the game, e.g. after login or enabling the plugin. */
	private void syncState()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			thrallActive = client.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_ACTIVE) != 0;
		}
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
