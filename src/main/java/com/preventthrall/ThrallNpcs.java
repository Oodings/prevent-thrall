package com.preventthrall;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.NpcID;

/**
 * Maps thrall NPC ids (including the impish whistle cosmetic versions) to the spell that summons them.
 */
final class ThrallNpcs
{
	private static final Map<Integer, ThrallSpell> BY_ID = new HashMap<>();

	static
	{
		BY_ID.put(NpcID.ARCEUUS_THRALL_GHOST_LESSER, ThrallSpell.LESSER_GHOST);
		BY_ID.put(NpcID.ARCEUUS_THRALL_GHOST_SUPERIOR, ThrallSpell.SUPERIOR_GHOST);
		BY_ID.put(NpcID.ARCEUUS_THRALL_GHOST_GREATER, ThrallSpell.GREATER_GHOST);
		BY_ID.put(NpcID.ARCEUUS_THRALL_SKELETON_LESSER, ThrallSpell.LESSER_SKELETON);
		BY_ID.put(NpcID.ARCEUUS_THRALL_SKELETON_SUPERIOR, ThrallSpell.SUPERIOR_SKELETON);
		BY_ID.put(NpcID.ARCEUUS_THRALL_SKELETON_GREATER, ThrallSpell.GREATER_SKELETON);
		BY_ID.put(NpcID.ARCEUUS_THRALL_ZOMBIE_LESSER, ThrallSpell.LESSER_ZOMBIE);
		BY_ID.put(NpcID.ARCEUUS_THRALL_ZOMBIE_SUPERIOR, ThrallSpell.SUPERIOR_ZOMBIE);
		BY_ID.put(NpcID.ARCEUUS_THRALL_ZOMBIE_GREATER, ThrallSpell.GREATER_ZOMBIE);
		BY_ID.put(NpcID.THRALL_IMP_MAGIC_LESSER, ThrallSpell.LESSER_GHOST);
		BY_ID.put(NpcID.THRALL_IMP_MAGIC_SUPERIOR, ThrallSpell.SUPERIOR_GHOST);
		BY_ID.put(NpcID.THRALL_IMP_MAGIC_GREATER, ThrallSpell.GREATER_GHOST);
		BY_ID.put(NpcID.THRALL_IMP_RANGED_LESSER, ThrallSpell.LESSER_SKELETON);
		BY_ID.put(NpcID.THRALL_IMP_RANGED_SUPERIOR, ThrallSpell.SUPERIOR_SKELETON);
		BY_ID.put(NpcID.THRALL_IMP_RANGED_GREATER, ThrallSpell.GREATER_SKELETON);
		BY_ID.put(NpcID.THRALL_IMP_MELEE_LESSER, ThrallSpell.LESSER_ZOMBIE);
		BY_ID.put(NpcID.THRALL_IMP_MELEE_SUPERIOR, ThrallSpell.SUPERIOR_ZOMBIE);
		BY_ID.put(NpcID.THRALL_IMP_MELEE_GREATER, ThrallSpell.GREATER_ZOMBIE);
	}

	private ThrallNpcs()
	{
	}

	/** The spell that summons this NPC, or null if it is not a thrall. */
	static ThrallSpell spellFor(int npcId)
	{
		return BY_ID.get(npcId);
	}
}
