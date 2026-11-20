package com.preventthrall;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PreventThrallPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PreventThrallPlugin.class);
		RuneLite.main(args);
	}
}
