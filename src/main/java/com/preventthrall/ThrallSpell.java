package com.preventthrall;

import net.runelite.api.gameval.InterfaceID;

/**
 * The nine Arceuus resurrection (thrall) spells and their spellbook widget ids.
 */
enum ThrallSpell
{
	LESSER_GHOST(InterfaceID.MagicSpellbook.RESURRECT_LESSER_GHOST),
	LESSER_SKELETON(InterfaceID.MagicSpellbook.RESURRECT_LESSER_SKELETON),
	LESSER_ZOMBIE(InterfaceID.MagicSpellbook.RESURRECT_LESSER_ZOMBIE),
	SUPERIOR_GHOST(InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_GHOST),
	SUPERIOR_SKELETON(InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_SKELETON),
	SUPERIOR_ZOMBIE(InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_ZOMBIE),
	GREATER_GHOST(InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST),
	GREATER_SKELETON(InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON),
	GREATER_ZOMBIE(InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE);

	private final int widgetId;

	ThrallSpell(int widgetId)
	{
		this.widgetId = widgetId;
	}

	int getWidgetId()
	{
		return widgetId;
	}

	static boolean isThrallSpell(int widgetId)
	{
		for (ThrallSpell spell : values())
		{
			if (spell.widgetId == widgetId)
			{
				return true;
			}
		}
		return false;
	}
}
