package com.preventthrall;

import net.runelite.api.gameval.InterfaceID;

/**
 * The nine Arceuus resurrection (thrall) spells: spellbook widget, type and tier.
 */
enum ThrallSpell
{
	LESSER_GHOST(InterfaceID.MagicSpellbook.RESURRECT_LESSER_GHOST, ThrallStyle.GHOST, false),
	LESSER_SKELETON(InterfaceID.MagicSpellbook.RESURRECT_LESSER_SKELETON, ThrallStyle.SKELETON, false),
	LESSER_ZOMBIE(InterfaceID.MagicSpellbook.RESURRECT_LESSER_ZOMBIE, ThrallStyle.ZOMBIE, false),
	SUPERIOR_GHOST(InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_GHOST, ThrallStyle.GHOST, false),
	SUPERIOR_SKELETON(InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_SKELETON, ThrallStyle.SKELETON, false),
	SUPERIOR_ZOMBIE(InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_ZOMBIE, ThrallStyle.ZOMBIE, false),
	GREATER_GHOST(InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST, ThrallStyle.GHOST, true),
	GREATER_SKELETON(InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON, ThrallStyle.SKELETON, true),
	GREATER_ZOMBIE(InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE, ThrallStyle.ZOMBIE, true);

	private final int widgetId;
	private final ThrallStyle style;
	private final boolean greater;

	ThrallSpell(int widgetId, ThrallStyle style, boolean greater)
	{
		this.widgetId = widgetId;
		this.style = style;
		this.greater = greater;
	}

	int getWidgetId()
	{
		return widgetId;
	}

	ThrallStyle getStyle()
	{
		return style;
	}

	boolean isGreater()
	{
		return greater;
	}

	static ThrallSpell fromWidgetId(int widgetId)
	{
		for (ThrallSpell spell : values())
		{
			if (spell.widgetId == widgetId)
			{
				return spell;
			}
		}
		return null;
	}
}
