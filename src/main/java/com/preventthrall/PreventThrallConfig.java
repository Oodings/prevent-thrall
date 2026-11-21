package com.preventthrall;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("preventthrall")
public interface PreventThrallConfig extends Config
{
	enum BlockMode
	{
		ALL_THRALLS("All thralls"),
		SAME_TYPE("Same type only");

		private final String label;

		BlockMode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigSection(
		name = "Clicks & menu",
		description = "What happens when you click the thrall spells",
		position = 0
	)
	String clicksSection = "clicks";

	@ConfigSection(
		name = "Appearance",
		description = "How blocked and active thrall spells look",
		position = 1
	)
	String lookSection = "look";

	@ConfigSection(
		name = "Advanced",
		description = "Expiry detection",
		position = 2,
		closedByDefault = true
	)
	String advancedSection = "advanced";

	@ConfigItem(
		keyName = "blockLeftClick",
		name = "Block left-click",
		description = "Ignore left-clicks on blocked thrall spells.<br>Right-click > Cast always still works.",
		section = clicksSection,
		position = 0
	)
	default boolean blockLeftClick()
	{
		return true;
	}

	@ConfigItem(
		keyName = "blockMode",
		name = "Block which thralls",
		description = "While your thrall is out, block:<br>"
			+ "All thralls: every thrall spell.<br>"
			+ "Same type only: just the type you have out, so you can switch type<br>"
			+ "(e.g. mage thrall on Olm's mage hand, then melee thrall on the melee hand).<br>"
			+ "A new thrall always replaces your current one.",
		section = clicksSection,
		position = 1
	)
	default BlockMode blockMode()
	{
		return BlockMode.ALL_THRALLS;
	}

	@ConfigItem(
		keyName = "unblockLeftBehind",
		name = "Unblock if left behind",
		description = "Thralls can't follow you between rooms.<br>"
			+ "Allow a new one once yours has been left behind, e.g. in the previous raid room.",
		section = clicksSection,
		position = 2
	)
	default boolean unblockLeftBehind()
	{
		return true;
	}

	@ConfigItem(
		keyName = "blockLowerTiers",
		name = "Block lesser/superior",
		description = "Always ignore left-clicks on lesser and superior thralls,<br>"
			+ "so you can't misclick them instead of greater. Right-click still casts them.",
		section = clicksSection,
		position = 3
	)
	default boolean blockLowerTiers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideCosmetics",
		name = "Hide Cosmetics option",
		description = "Remove Cosmetics from the thrall spells' right-click menu",
		section = clicksSection,
		position = 4
	)
	default boolean hideCosmetics()
	{
		return false;
	}

	@ConfigItem(
		keyName = "greyOut",
		name = "Grey out blocked spells",
		description = "Fade thrall spells while they are blocked",
		section = lookSection,
		position = 0
	)
	default boolean greyOut()
	{
		return true;
	}

	@Range(max = 90)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "fadePercent",
		name = "Fade amount",
		description = "How faded blocked spells look (needs Grey out blocked spells)",
		section = lookSection,
		position = 1
	)
	default int fadePercent()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "highlightActive",
		name = "Highlight active thrall",
		description = "Draw a box around the spell of the thrall you have out.<br>"
			+ "To only warn instead of blocking, turn this on and turn off<br>"
			+ "Block left-click and Grey out blocked spells.",
		section = lookSection,
		position = 2
	)
	default boolean highlightActive()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight colour",
		description = "Box colour while your thrall is with you (needs Highlight active thrall)",
		section = lookSection,
		position = 3
	)
	default Color highlightColor()
	{
		return Color.RED;
	}

	@Alpha
	@ConfigItem(
		keyName = "leftBehindColor",
		name = "Left behind colour",
		description = "Box colour once your thrall has been left behind in another room (needs Highlight active thrall)",
		section = lookSection,
		position = 4
	)
	default Color leftBehindColor()
	{
		return Color.ORANGE;
	}

	@ConfigItem(
		keyName = "expiryMessage",
		name = "Thrall expiry message",
		description = "A game message containing this text also ends the block.<br>"
			+ "Only change this if Jagex changes the wording.",
		section = advancedSection,
		position = 0
	)
	default String expiryMessage()
	{
		return "returns to the grave";
	}
}
