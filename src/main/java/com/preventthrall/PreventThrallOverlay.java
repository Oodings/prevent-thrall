package com.preventthrall;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws a box around the active thrall's spell in the spellbook.
 */
class PreventThrallOverlay extends Overlay
{
	private final Client client;
	private final PreventThrallPlugin plugin;
	private final PreventThrallConfig config;

	@Inject
	PreventThrallOverlay(Client client, PreventThrallPlugin plugin, PreventThrallConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightActive() || !plugin.isThrallActive())
		{
			return null;
		}

		graphics.setColor(plugin.isLeftBehind() ? config.leftBehindColor() : config.highlightColor());
		graphics.setStroke(new BasicStroke(2));

		ThrallSpell active = plugin.getActiveSpell();
		for (ThrallSpell spell : ThrallSpell.values())
		{
			// Box the spell of the thrall we have out, or all of them if we don't know which it is.
			if (active != null && spell != active)
			{
				continue;
			}

			Widget widget = client.getWidget(spell.getWidgetId());
			if (widget == null || widget.isHidden())
			{
				continue;
			}
			Rectangle bounds = widget.getBounds();
			graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
		}
		return null;
	}
}
