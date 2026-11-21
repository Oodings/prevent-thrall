# Prevent Thrall

Stops accidental thrall recasts on the Arceuus spellbook.

While your thrall is out, the Resurrect thrall spells are greyed out and left-clicking them does nothing. You can still cast one on purpose by right-clicking the spell and choosing **Cast**.

The plugin follows your own thrall: it recognises the thrall that appears next to you when you cast (including impish whistle cosmetics), always tracks the most recent one, and ends the block when the game says it has gone, so it follows your real thrall uptime.

## Settings

**Clicks & menu**
- **Block left-click**: ignore left-clicks on blocked thrall spells (on by default).
- **Block which thralls**: *All thralls*, or *Same type only* so you can still switch type (e.g. a mage thrall on Olm's mage hand, then a melee thrall on the melee hand). Summoning a new thrall always replaces your current one.
- **Unblock if left behind**: thralls can't follow you between rooms, so allow a new one once yours has been left behind (on by default).
- **Block lesser/superior**: avoid misclicking the weaker tiers next to greater (off by default).
- **Hide Cosmetics option**: remove Cosmetics from the thrall spells' right-click menu.

**Appearance**
- **Grey out blocked spells** and **Fade amount**.
- **Highlight active thrall**: a box around the spell of the thrall you have out, in one colour while it is with you and another once it has been left behind. Turn off Block left-click and Grey out to only warn instead of blocking.

**Advanced**
- **Thrall expiry message**: game message text that also ends the block. Only change this if Jagex changes the wording.
