package snownee.jade.gui;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import snownee.jade.api.JadeKeys;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.gui.config.BelowOrAboveListEntryTooltipPositioner;
import snownee.jade.gui.config.NotUglyEditBox;
import snownee.jade.gui.config.OptionsList;
import snownee.jade.gui.config.OptionsNav;
import snownee.jade.gui.config.value.OptionValue;

public abstract class BaseOptionsScreen extends GuiScreen {

	protected final @Nullable GuiScreen parent;
	public @Nullable GuiButton saveButton;
	public @Nullable ITextComponent saveButtonTooltip;
	protected @Nullable Runnable saver;
	protected @Nullable Runnable canceller;
	protected @Nullable OptionsList options;
	protected @Nullable OptionsNav optionsNav;
	private @Nullable NotUglyEditBox searchBox;
	private @Nullable ITextComponent pendingTooltip;
	private int tooltipX;
	private int tooltipY;

	public BaseOptionsScreen(@Nullable GuiScreen parent) {
		this(parent, null);
	}

	public BaseOptionsScreen(@Nullable GuiScreen parent, @Nullable ITextComponent title) {
		this.parent = parent;
	}

	@Override
	public void initGui() {
		Objects.requireNonNull(saver);
		double scroll = options == null ? 0 : options.getAmountScrolled();
		super.initGui();
		if (options != null) {
			options.removed();
		}
		options = createOptions(new OptionsList(this, mc, 120, 0, width - 120, height - 32, 26, IWailaConfig.get()::save));
		optionsNav = new OptionsNav(options, 120, height - 32 - 18, 18, 18);
		searchBox = new NotUglyEditBox(mc.fontRenderer, 0, 0, 120, 18, searchBox, new TextComponentTranslation("gui.jade.search"));
		searchBox.setResponder(s -> {
			options.updateSearch(s);
			optionsNav.refresh();
		});
		searchBox.fixedTextX = 12;
		searchBox.fixedTextY = 6;
		searchBox.fixedInnerWidth = searchBox.getWidth() - 12 - 18;
		searchBox.alwaysRenderCross = true;

		options.updateSearch(searchBox.getValue());
		optionsNav.refresh();
		options.forceSetScrollAmount(scroll);

		saveButton = new GuiButton(0, width - 100, height - 25, 90, 20, I18n.format("gui.jade.save_and_quit")) {
			@Override
			public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
				if (super.mousePressed(mc, mouseX, mouseY)) {
					OptionValue<?> invalidEntry = options.invalidEntry;
					if (invalidEntry == null) {
						options.save();
						Objects.requireNonNull(saver).run();
						mc.displayGuiScreen(parent);
					} else {
						options.scrollToEntry(invalidEntry);
					}
					return true;
				}
				return false;
			}
		};
		saveButton.packedFGColour = 0xFFB9F6CA;
		buttonList.add(saveButton);
		if (canceller != null) {
			buttonList.add(new GuiButton(1, saveButton.x - 95, height - 25, 90, 20, I18n.format("gui.cancel")) {
				@Override
				public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
					if (super.mousePressed(mc, mouseX, mouseY)) {
						onClose();
						return true;
					}
					return false;
				}
			});
		}

		options.updateSaveState();
	}

	public OptionsList options() {
		return Objects.requireNonNull(options);
	}

	public OptionsNav optionsNav() {
		return Objects.requireNonNull(optionsNav);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		pendingTooltip = null;
		drawDefaultBackground();
		optionsNav.extractRenderState(mouseX, mouseY, partialTicks);
		if (searchBox != null) {
			searchBox.x = 0;
			searchBox.y = 0;
			searchBox.drawTextBox();
		}
		options.extractRenderState(mouseX, mouseY, partialTicks);

		OptionsList.Entry entry = options.isMouseOver(mouseX, mouseY) ? options.getEntryAt(mouseX, mouseY) : null;
		if (entry != null) {
			int valueX = entry.getContentX() + entry.getTextX();
			if (mouseX >= valueX && mouseX < valueX + entry.getTextWidth()) {
				List<ITextComponent> descs = Lists.newArrayListWithExpectedSize(3);
				descs.addAll(entry.getDescription());
				if (JadeUI.hasShiftDown()) {
					descs.addAll(entry.getDescriptionOnShift());
				}
				if (!descs.isEmpty()) {
					descs.replaceAll(BaseOptionsScreen::processBuiltInVariables);
					setTooltipForNextFrame(descs, mouseX, mouseY, entry);
				}
			}
		}
		if (pendingTooltip != null) {
			drawHoveringText(splitLines(pendingTooltip.getFormattedText()), tooltipX, tooltipY);
			pendingTooltip = null;
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
		if (saveButtonTooltip != null && saveButton != null && saveButton.isMouseOver()) {
			drawHoveringText(splitLines(saveButtonTooltip.getFormattedText()), saveButton.x + 20, saveButton.y + 10);
		}
	}

	public List<String> splitLines(String text) {
		return mc.fontRenderer.listFormattedStringToWidth(text, 255);
	}

	public void drawTooltip(List<String> lines, int x, int y) {
		drawHoveringText(lines, x, y);
	}

	public void setTooltipForNextFrame(List<ITextComponent> descs, int mouseX, int mouseY, OptionsList.Entry entry) {
		List<String> lines = Lists.newArrayList();
		for (ITextComponent desc : descs) {
			lines.addAll(splitLines(desc.getFormattedText()));
		}
		FontRenderer font = mc.fontRenderer;
		int m = 0;
		for (String line : lines) {
			m = Math.max(m, font.getStringWidth(line));
		}
		int n = lines.size() * font.FONT_HEIGHT + 8;
		int[] pos = new BelowOrAboveListEntryTooltipPositioner(options(), entry).positionTooltip(width, height, mouseX, mouseY, m + 8, n);
		tooltipX = pos[0];
		tooltipY = pos[1];
		pendingTooltip = new TextComponentString(String.join("\n", lines));
	}

	/**
	 * 1.12.2: the modern per-character text walk is replaced by a plain string search across the flattened
	 * formatted text, keeping the substitution semantics (key name + colored binding name).
	 */
	public static ITextComponent processBuiltInVariables(ITextComponent component) {
		String text = component.getFormattedText();
		ITextComponent result = component;
		if (text.contains("${SHOW_DETAILS}")) {
			List<ITextComponent> objects = Lists.newArrayListWithExpectedSize(3);
			objects.add(new TextComponentTranslation("key.jade.show_details"));
			KeyBinding key = JadeKeys.showDetails();
			if (key.getKeyCode() != 0) {
				objects.add(new TextComponentString(key.getDisplayName()).setStyle(new Style().setColor(TextFormatting.AQUA)));
			}
			ITextComponent keyName = new TextComponentTranslation("config.jade.key_name_n_bind_" + (objects.size() - 1), objects.toArray());
			result = replaceVariables(result, "${SHOW_DETAILS}", keyName);
		}
		if (result.getFormattedText().contains("${SHOW_OVERLAY}")) {
			List<ITextComponent> objects = Lists.newArrayListWithExpectedSize(3);
			KeyBinding key = JadeKeys.showOverlay();
			objects.add(new TextComponentTranslation(key.getKeyDescription() == null ? "key.jade.show_overlay" : key.getKeyDescription()));
			if (key.getKeyCode() != 0) {
				objects.add(new TextComponentString(key.getDisplayName()).setStyle(new Style().setColor(TextFormatting.AQUA)));
			}
			ITextComponent keyName = new TextComponentTranslation("config.jade.key_name_n_bind_" + (objects.size() - 1), objects.toArray());
			result = replaceVariables(result, "${SHOW_OVERLAY}", keyName);
		}
		return result;
	}

	/**
	 * Modern Jade walks the component tree and replaces the variable inside every sibling, including text that
	 * lives inside translation arguments. 1.12.2's {@code TextComponentTranslation} keeps arguments in a private
	 * field (not exposed as siblings), so a plain string replacement over the flattened formatted text is used
	 * instead; the surrounding text of the translation is preserved by splitting the original formatted string.
	 */
	private static ITextComponent replaceVariables(ITextComponent component, String source, ITextComponent replacement) {
		String text = component.getFormattedText();
		if (!text.contains(source)) {
			return component;
		}
		// 1.12.2: Style parent chains are MUTABLE -- setStyle re-points every sibling's
		// parentStyle to the new style, and appendSibling points the appended style at
		// the parent's style. Sharing one Style object across the wrapper and its split
		// pieces makes appendSibling set style.parentStyle = style (a self-cycle), and
		// Style.getColor() then recurses forever (config-GUI StackOverflowError). Give
		// every piece its own deep copy so no two components ever share a Style.
		Style style = component.getStyle().createDeepCopy();
		ITextComponent newComponent = new TextComponentString("").setStyle(style.createDeepCopy());
		for (String s : StringUtils.splitByWholeSeparatorPreserveAllTokens(text, source)) {
			if (!s.isEmpty()) {
				newComponent.appendSibling(new TextComponentString(s).setStyle(style.createDeepCopy()));
			}
			newComponent.appendSibling(replacement);
		}
		return newComponent;
	}

	public abstract OptionsList createOptions(OptionsList optionsList);

	@Override
	public void handleMouseInput() throws IOException {
		OptionsNav nav = optionsNav;
		int wheel = Mouse.getEventDWheel();
		if (nav != null && nav.isMouseOver(Mouse.getEventX() * width / mc.displayWidth,
				height - Mouse.getEventY() * height / mc.displayHeight - 1)) {
			if (wheel != 0) {
				nav.mouseScrolled(wheel > 0 ? -1 : 1);
			}
		} else if (options != null) {
			if (wheel != 0) {
				options.mouseScrolled(wheel > 0 ? -1 : 1);
			}
		}
		super.handleMouseInput();
	}

	/**
	 * 1.12.2: onGuiClosed fires on every screen switch, so it must only drop the option list state (modern
	 * {@code removed()}); running the canceller here would cancel a successful save (the save path calls
	 * {@code displayGuiScreen(parent)}). The canceller runs only in {@link #onClose()} (cancel button / ESC).
	 */
	@Override
	public void onGuiClosed() {
		if (options != null) {
			options.removed();
		}
	}

	/** 1.12.2: explicit close path (cancel button / ESC) -- modern {@code onClose}. */
	public void onClose() {
		if (canceller != null) {
			canceller.run();
		}
		if (options != null) {
			options.removed();
		}
		if (parent != null) {
			mc.displayGuiScreen(parent);
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (options != null && options.isMouseOver(mouseX, mouseY)) {
			options.mouseClickedInternal(mouseX, mouseY, mouseButton);
		}
		if (optionsNav != null && optionsNav.isMouseOver(mouseX, mouseY)) {
			optionsNav.mouseClicked(mouseX, mouseY, mouseButton);
		}
		if (searchBox != null && mouseX >= 0 && mouseX < 120 && mouseY >= 0 && mouseY < 18) {
			searchBox.mouseClicked(mouseX, mouseY, mouseButton);
		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if (options != null && options.isDragging()) {
			options.mouseDragged(mouseX, mouseY);
		}
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int state) {
		if (options != null) {
			options.mouseReleased(mouseX, mouseY, state);
		}
		super.mouseReleased(mouseX, mouseY, state);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (options != null && options.keyPressed(typedChar, keyCode)) {
			return;
		}
		if (searchBox != null && searchBox.isFocused()) {
			searchBox.textboxKeyTyped(typedChar, keyCode);
			return;
		}
		boolean selected = options != null && options.selectedKey != null;
		if (keyCode == 1) { // ESC
			if (!selected) {
				onClose();
				return;
			}
		}
		if (searchBox != null && !searchBox.isFocused() && !selected) {
			// search autofocus on alphanumeric keys
			boolean printable = typedChar >= ' ' && typedChar != 127;
			if (printable) {
				searchBox.setFocused(true);
				searchBox.textboxKeyTyped(typedChar, keyCode);
				return;
			}
			// Ctrl+F focuses search
			if (keyCode == 33 && hasControlDown()) { // KEY_F
				searchBox.setFocused(true);
				return;
			}
		}
		super.keyTyped(typedChar, keyCode);
	}

	private static boolean hasControlDown() {
		return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
