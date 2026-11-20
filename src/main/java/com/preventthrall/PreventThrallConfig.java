package com.preventthrall;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("preventthrall")
public interface PreventThrallConfig extends Config
{
	@ConfigSection(
		name = "Appearance and behaviour",
		description = "How blocked spells look and act",
		position = 0
	)
	String lookSection = "look";

	@ConfigSection(
		name = "Advanced",
		description = "Expiry detection",
		position = 1
	)
	String advancedSection = "advanced";

	@ConfigItem(
		keyName = "greyOut",
		name = "Grey out blocked spells",
		description = "Dim the thrall spell icons while a thrall is active",
		section = lookSection,
		position = 0
	)
	default boolean greyOut()
	{
		return true;
	}

	@Range(min = 0, max = 254)
	@ConfigItem(
		keyName = "dimAmount",
		name = "Dim amount",
		description = "How faded blocked spells look (0 = no fade, 254 = almost invisible)",
		section = lookSection,
		position = 1
	)
	default int dimAmount()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "blockLeftClick",
		name = "Block left-click",
		description = "Ignore left-clicks on thrall spells while a thrall is active. They can still be cast from the right-click menu.",
		section = lookSection,
		position = 2
	)
	default boolean blockLeftClick()
	{
		return true;
	}

	@ConfigItem(
		keyName = "expiryMessage",
		name = "Thrall expiry message",
		description = "The block ends when a game message containing this text appears (case-insensitive)",
		section = advancedSection,
		position = 0
	)
	default String expiryMessage()
	{
		return "returns to the grave";
	}
}
