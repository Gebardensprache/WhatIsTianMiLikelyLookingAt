package snownee.jade.gui.config;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import snownee.jade.gui.BaseOptionsScreen;
import snownee.jade.gui.config.OptionsList.Title;

/**
 * 1.12.2: the modern {@code ObjectSelectionList}-based navbar is replaced by a plain rendered column of
 * category titles. The modern navbar background sprites do not exist in 1.12.2 assets, so the panel is drawn
 * with a plain rectangle. Scrolling (wheel) is supported when the categories do not fit.
 */
public class OptionsNav {

	private static final int COLOR_BACKGROUND = 0x99101010;
	private static final int COLOR_INDICATOR = 0xFFFFFFFF;
	private final OptionsList options;
	private final int width;
	private final int height;
	private final int top;
	private final int itemHeight;
	private final List<OptionsList.Title> entries = Lists.newArrayList();
	private int current;
	private int scroll;
	private @Nullable Title hovered;
	private @Nullable FixedTooltipPositioner hoveredTooltip;

	public OptionsNav(OptionsList options, int width, int height, int top, int itemHeight) {
		this.options = options;
		this.width = width;
		this.height = height;
		this.top = top;
		this.itemHeight = itemHeight;
	}

	public void addEntry(OptionsList.Title entry) {
		entries.add(entry);
	}

	public void refresh() {
		entries.clear();
		if (options.children().size() <= 1) {
			return; // only the "no results" entry
		}
		for (OptionsList.Entry child : options.children()) {
			if (child instanceof OptionsList.Title titleEntry) {
				addEntry(titleEntry);
			}
		}
		scroll = Math.max(0, Math.min(scroll, getMaxScroll()));
	}

	private int getMaxScroll() {
		return Math.max(0, entries.size() * itemHeight - height);
	}

	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		Gui.drawRect(0, top, width, top + height, COLOR_BACKGROUND);
		hovered = null;
		for (int i = 0; i < entries.size(); i++) {
			OptionsList.Title entry = entries.get(i);
			int y = top + i * itemHeight - scroll;
			if (y + itemHeight < top || y > top + height) {
				continue;
			}
			int textX = 10;
			if (mouseY >= y && mouseY < y + itemHeight) {
				hovered = entry;
			}
			if (current == i) {
				Gui.drawRect(2, y + 2, 4, y + itemHeight - 2, COLOR_INDICATOR);
			}
			Minecraft mc = Minecraft.getMinecraft();
			String text = entry.title().getFormattedText();
			int color = current == i ? 0xFFFFFFFF : 0xFFA0A0A0;
			mc.fontRenderer.drawString(text, textX, y + (itemHeight - mc.fontRenderer.FONT_HEIGHT) / 2, color);
		}
		if (hovered != null) {
			if (10 + hovered.getTextWidth() > width) {
				if (hoveredTooltip == null) {
					hoveredTooltip = new FixedTooltipPositioner(10, top + (itemHeight / 2) - (Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT / 2));
				}
				BaseOptionsScreen owner = options.owner();
				List<String> lines = owner.splitLines(hovered.title().getFormattedText());
				owner.drawTooltip(lines, hoveredTooltip.positionTooltip(owner.width, owner.height, mouseX, mouseY, 0, 0)[0],
						hoveredTooltip.positionTooltip(owner.width, owner.height, mouseX, mouseY, 0, 0)[1]);
			}
			hoveredTooltip = null;
		}
	}

	public boolean isMouseOver(int mouseX, int mouseY) {
		return mouseX >= 0 && mouseX < width && mouseY >= top && mouseY < top + height;
	}

	@Nullable
	public Title getEntryAt(int mouseX, int mouseY) {
		if (!isMouseOver(mouseX, mouseY)) {
			return null;
		}
		int index = (mouseY - top + scroll) / itemHeight;
		if (index >= 0 && index < entries.size()) {
			return entries.get(index);
		}
		return null;
	}

	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		if (mouseButton != 0) {
			return;
		}
		OptionsList.Title title = getEntryAt(mouseX, mouseY);
		if (title != null) {
			Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
					SoundEvents.UI_BUTTON_CLICK, 1.0F));
			options.showOnTop(title);
			setCurrent(title);
		}
	}

	public void mouseScrolled(int amount) {
		scroll = Math.max(0, Math.min(scroll + amount * itemHeight, getMaxScroll()));
	}

	public int getCurrent() {
		return current;
	}

	@Nullable
	public Title getCurrentEntry() {
		if (current >= 0 && current < entries.size()) {
			return entries.get(current);
		}
		return null;
	}

	public void setCurrent(OptionsList.Title title) {
		int index = entries.indexOf(title);
		if (index >= 0) {
			current = index;
		}
	}
}
