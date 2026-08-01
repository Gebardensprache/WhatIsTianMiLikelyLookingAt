package snownee.jade.gui.config;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.settings.KeyModifier;
import snownee.jade.Jade;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.gui.BaseOptionsScreen;
import snownee.jade.gui.PreviewOptionsScreen;
import snownee.jade.gui.config.value.CycleOptionValue;
import snownee.jade.gui.config.value.InputOptionValue;
import snownee.jade.gui.config.value.OptionValue;
import snownee.jade.gui.config.value.SliderOptionValue;
import snownee.jade.util.ClientProxy;
import snownee.jade.util.JadeFont;
import snownee.jade.util.SmoothChasingValue;

/**
 * 1.12.2: the modern {@code ContainerObjectSelectionList} is replaced by {@link GuiListExtended}, but the
 * entries carry their own widgets (which vanilla {@code GuiSlot} has no notion of), so scrolling, wheel input,
 * selection and drawing are driven by {@link #extractRenderState}/{@link #mouseScrolled} and friends from the
 * owning screen. Vanilla's wheel handler ({@code slotHeight/2} per notch) is skipped in favor of the modern
 * 3-row rate.
 */
public class OptionsList extends GuiListExtended {

	public static final ITextComponent OPTION_ON = new TextComponentTranslation("options.on")
			.setStyle(new Style().setColor(TextFormatting.GREEN));
	public static final ITextComponent OPTION_OFF = new TextComponentTranslation("options.off")
			.setStyle(new Style().setColor(TextFormatting.RED));
	public final Set<OptionsList.Entry> forcePreview = Sets.newIdentityHashSet();
	protected final List<Entry> entries = Lists.newArrayList();
	private final @Nullable Runnable diskWriter;
	public @Nullable Title currentTitle;
	public @Nullable OptionValue<?> invalidEntry;
	public @Nullable KeyBinding selectedKey;
	private final BaseOptionsScreen owner;
	private final SmoothChasingValue smoothScroll;
	private @Nullable Entry defaultParent;
	private @Nullable Entry hovered;
	private boolean dragging;
	private @Nullable Entry selected;

	public OptionsList(
			BaseOptionsScreen owner,
			Minecraft client,
			int x,
			int y,
			int width,
			int height,
			int entryHeight,
			@Nullable Runnable diskWriter) {
		super(client, width, height, y, y + height, entryHeight);
		left = x;
		right = x + width;
		this.owner = owner;
		this.diskWriter = diskWriter;
		smoothScroll = new SmoothChasingValue().withSpeed(0.6F);
	}

	public OptionsList(BaseOptionsScreen owner, Minecraft client, int x, int y, int width, int height, int entryHeight) {
		this(owner, client, x, y, width, height, entryHeight, null);
	}

	public BaseOptionsScreen owner() {
		return owner;
	}
	@Override
	public int getListWidth() {
		return Math.min(width, 300);
	}

	@Override
	protected int getScrollBarX() {
		return owner.width - 6;
	}

	@Override
	public int getSize() {
		return children().size();
	}

	@Override
	public GuiListExtended.IGuiListEntry getListEntry(int index) {
		return children().get(index);
	}

	public void setScrollAmount(double scroll) {
		if (ClientProxy.metadata.hasSmoothScroll()) {
			amountScrolled = (float) scroll;
			bindAmountScrolled();
		} else {
			smoothScroll.target(MathHelper.clamp((float) scroll, 0, getMaxScroll()));
		}
	}

	public void forceSetScrollAmount(double scroll) {
		smoothScroll.start((float) scroll);
		amountScrolled = (float) scroll;
		bindAmountScrolled();
	}

	/** Modern {@code scrollRate}: 3 rows per wheel notch, 9 when fast-scroll is unavailable and Ctrl is held. */
	protected int scrollRate() {
		return slotHeight * (!ClientProxy.metadata.hasFastScroll() && JadeUI.hasControlDown() ? 9 : 3);
	}
	/** Wheel input (modern {@code mouseScrolled}); vanilla's {@code slotHeight/2} wheel handler is not used. */
	public void mouseScrolled(int amount) {
		if (amount == 0) {
			return;
		}
		int rate = scrollRate();
		if (amount > 0) {
			amountScrolled -= rate;
		} else {
			amountScrolled += rate;
		}
		bindAmountScrolled();
	}

	public void scrollToEntry(Entry entry) {
		int index = children().indexOf(entry);
		if (index < 0) {
			return;
		}
		amountScrolled = Math.max(0, index * slotHeight - 4);
		bindAmountScrolled();
	}

	protected boolean entriesCanBeSelected() {
		return !PreviewOptionsScreen.isAdjustingPosition();
	}

	@Override
	protected void drawBackground() {
		// modern Jade renders no list background; the screen draws the base background
	}

	/**
	 * Renders the list content (entries, selection, scrollbar). Called from the owning screen's drawScreen;
	 * replaces the modern {@code extractWidgetRenderState}. Entry rendering itself is dispatched through the
	 * inherited {@code GuiListExtended.drawSlot} -> {@code Entry.drawEntry} -> {@code extractContent} chain.
	 */
	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		if (!visible) {
			return;
		}
		// modern extractWidgetRenderState: tick the chasing scroll value and commit it to the
		// vanilla scroll when it has settled (smoothScroll is only used on non-smooth clients,
		// where setScrollAmount only targets it and something must drive it each frame)
		smoothScroll.tick(partialTicks);
		if (!ClientProxy.metadata.hasSmoothScroll() && smoothScroll.isMoving()) {
			amountScrolled = Math.round(smoothScroll.value);
		}
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		updateHovered(mouseX, mouseY);
		bindAmountScrolled();
		int insideLeft = left + width / 2 - getListWidth() / 2 + 2;
		int insideTop = top + 4 - (int) amountScrolled;
		drawSelectionBox(insideLeft, insideTop, mouseX, mouseY, partialTicks);
		drawScrollbar();
	}

	private void updateHovered(int mouseX, int mouseY) {
		hovered = null;
		if (!entriesCanBeSelected()) {
			return;
		}
		int index = getSlotIndexFromScreenCoords(mouseX, mouseY);
		if (index >= 0 && index < children().size()) {
			hovered = children().get(index);
		}
		if (hovered instanceof Title) {
			setSelected(null);
			currentTitle = (Title) hovered;
		} else {
			setSelected(hovered);
			if (hovered != null && hovered.root() instanceof Title) {
				currentTitle = (Title) hovered.root();
			}
		}
	}

	private void drawScrollbar() {
		int j1 = getMaxScroll();
		if (j1 > 0) {
			int i = getScrollBarX();
			int j = i + 6;
			int k1 = (bottom - top) * (bottom - top) / getContentHeight();
			k1 = MathHelper.clamp(k1, 32, bottom - top - 8);
			int l1 = (int) amountScrolled * (bottom - top - k1) / j1 + top;
			if (l1 < top) {
				l1 = top;
			}
			Gui.drawRect(i, top, j, bottom, 0xFF000000);
			Gui.drawRect(i, l1, j, l1 + k1, 0xFF808080);
			Gui.drawRect(i, l1, j - 1, l1 + k1 - 1, 0xFFC0C0C0);
		}
	}

	public void save() {
		for (Entry entry : children()) {
			if (entry instanceof OptionValue) {
				((OptionValue<?>) entry).save();
			}
		}
		if (diskWriter != null) {
			diskWriter.run();
		}
	}

	public <T extends Entry> T add(T entry) {
		entries.add(entry);
		if (entry instanceof Title) {
			setDefaultParent(entry);
		} else if (defaultParent != null) {
			entry.parent(defaultParent);
		}
		return entry;
	}

	@Nullable
	public Entry getEntryAt(double x, double y) {
		int index = getSlotIndexFromScreenCoords((int) x, (int) y);
		if (index >= 0 && index < children().size()) {
			return children().get(index);
		}
		return null;
	}

	/** 1.12.2: GuiSlot has no getRowTop -- the list computes row positions itself. */
	public int getRowTop(int index) {
		return top + 4 - (int) amountScrolled + index * slotHeight + headerPadding;
	}

	/** 1.12.2: GuiSlot has no getRowBottom. */
	public int getRowBottom(int index) {
		return getRowTop(index) + slotHeight;
	}

	public int getRowLeft() {
		return left + width / 2 - getListWidth() / 2;
	}

	public int getRowRight() {
		return left + width / 2 + getListWidth() / 2;
	}

	/** 1.12.2: GuiSlot has no getRowWidth; used by the tooltip positioner. */
	public int getRowWidth() {
		return getRowRight() - getRowLeft();
	}

	public void setDefaultParent(Entry defaultParent) {
		this.defaultParent = defaultParent;
	}

	public Title title(String string) {
		return add(new Title(string));
	}

	public OptionValue<Float> slider(String optionName, Supplier<Float> getter, Consumer<Float> setter) {
		return slider(optionName, getter, setter, 0, 1, FloatUnaryOperator.identity());
	}

	public OptionValue<Float> slider(
			String optionName,
			Supplier<Float> getter,
			Consumer<Float> setter,
			float min,
			float max,
			FloatUnaryOperator aligner) {
		return add(new SliderOptionValue(optionName, getter, setter, min, max, aligner));
	}

	public <T> OptionValue<T> input(String optionName, Supplier<T> getter, Consumer<T> setter, Predicate<String> validator) {
		return add(new InputOptionValue<>(this::updateSaveState, optionName, getter, setter, validator));
	}

	public <T> OptionValue<T> input(String optionName, Supplier<T> getter, Consumer<T> setter) {
		return input(optionName, getter, setter, Predicates.alwaysTrue());
	}

	public OptionValue<Boolean> choices(String optionName, Supplier<Boolean> getter, BooleanConsumer setter) {
		return choices(optionName, getter, setter, null);
	}

	public OptionValue<Boolean> choices(
			String optionName,
			Supplier<Boolean> getter,
			BooleanConsumer setter,
			@Nullable Consumer<CycleOptionValue.Builder<Boolean>> builderConsumer) {
		CycleOptionValue.Builder<Boolean> builder = CycleOptionValue.Builder.booleanBuilder(OPTION_ON, OPTION_OFF, getter.get());
		if (builderConsumer != null) {
			builderConsumer.accept(builder);
		}
		return add(new CycleOptionValue<>(optionName, builder, getter, setter));
	}

	public <T extends Enum<T>> OptionValue<T> choices(String optionName, Supplier<T> getter, Consumer<T> setter) {
		return choices(optionName, getter, setter, null);
	}

	public <T extends Enum<T>> OptionValue<T> choices(
			String optionName,
			Supplier<T> getter,
			Consumer<T> setter,
			@Nullable Consumer<CycleOptionValue.Builder<T>> builderConsumer) {
		List<T> values = Arrays.asList(getter.get().getDeclaringClass().getEnumConstants());
		CycleOptionValue.Builder<T> builder = CycleOptionValue.Builder.enumBuilder(getter.get());
		builder.withValues(values);
		builder.withTooltip(v -> {
			String key = Entry.makeKey(optionName + "_" + v.name().toLowerCase(Locale.ENGLISH) + "_desc");
			if (!JadeUI.hasTranslation(key)) {
				return null;
			}
			return new TextComponentTranslation(key);
		});
		if (builderConsumer != null) {
			builderConsumer.accept(builder);
		}
		return add(new CycleOptionValue<>(optionName, builder, getter, setter));
	}

	public <T> OptionValue<T> choices(
			String optionName,
			Supplier<T> getter,
			List<T> values,
			Consumer<T> setter,
			Function<T, ITextComponent> nameProvider) {
		CycleOptionValue.Builder<T> builder = new CycleOptionValue.Builder<>(nameProvider, values, getter.get());
		return add(new CycleOptionValue<>(optionName, builder, getter, setter));
	}

	public void keybind(KeyBinding keybind) {
		add(new KeybindOptionButton(this, keybind));
	}

	public void removed() {
		forcePreview.clear();
		for (Entry entry : entries) {
			entry.parent = null;
			if (!entry.children.isEmpty()) {
				entry.children.clear();
			}
		}
		clearEntries();
	}

	public void updateSearch(String search) {
		clearEntries();
		if (search.trim().isEmpty()) {
			entries.forEach(this::addEntryToChildren);
			return;
		}
		Set<Entry> matches = Sets.newLinkedHashSet();
		String[] keywords = search.toLowerCase(Locale.ENGLISH).split("\\s+");
		for (Entry entry : entries) {
			int bingo = 0;
			List<String> messages = entry.getMessages();
			for (String keyword : keywords) {
				for (String message : messages) {
					if (message.contains(keyword)) {
						bingo++;
						break;
					}
				}
			}
			if (bingo == keywords.length) {
				walkChildren(entry, matches::add);
				while (entry.parent() != null) {
					entry = Objects.requireNonNull(entry.parent());
					matches.add(entry);
				}
			}
		}
		for (Entry entry : entries) {
			if (matches.contains(entry)) {
				addEntryToChildren(entry);
			}
		}
		if (matches.isEmpty()) {
			addEntryToChildren(new Title(new TextComponentTranslation("gui.jade.no_results")
					.setStyle(new Style().setColor(TextFormatting.GRAY))));
		}
	}

	private void addEntryToChildren(Entry entry) {
		children0.add(entry);
	}

	private void clearEntries() {
		children0.clear();
	}

	private static void walkChildren(Entry entry, Consumer<Entry> consumer) {
		consumer.accept(entry);
		for (Entry child : entry.children) {
			walkChildren(child, consumer);
		}
	}

	public void updateSaveState() {
		invalidEntry = null;
		for (Entry entry : entries) {
			if (entry instanceof OptionValue<?> value && !value.isValidValue()) {
				invalidEntry = value;
				break;
			}
		}
		owner.saveButtonTooltip = invalidEntry == null ? null : new TextComponentTranslation("gui.jade.invalid_value_cant_save");
	}

	public void updateOptionValue(@Nullable ResourceLocation key) {
		for (Entry entry : entries) {
			if (entry instanceof OptionValue<?> value && (key == null || key.equals(value.getId()))) {
				value.updateValue();
			}
		}
	}

	public void showOnTop(Entry entry) {
		setScrollAmount(slotHeight * children().indexOf(entry) + 1);
		if (entry instanceof Title title) {
			currentTitle = title;
		}
	}

	public void resetMappingAndUpdateButtons() {
		for (Entry entry : entries) {
			if (entry instanceof KeybindOptionButton button) {
				button.refresh(selectedKey);
			}
		}
	}

	/** Key capture while a keybind is being bound (modern {@code keyPressed}). Returns true if consumed. */
	public boolean keyPressed(char typedChar, int keyCode) {
		if (selectedKey != null) {
			if (keyCode == 1) { // ESC
				selectedKey.setKeyModifierAndCode(KeyModifier.NONE, 0);
			} else {
				selectedKey.setKeyModifierAndCode(
						KeyModifier.getActiveModifier(), keyCode);
			}
			selectedKey = null;
			resetMappingAndUpdateButtons();
			return true;
		}
		return false;
	}

	/** Mouse click routing (modern {@code mouseClicked}). Consumes the click when capturing a keybind. */
	public boolean mouseClickedInternal(int mouseX, int mouseY, int mouseButton) {
		if (selectedKey != null) {
			selectedKey.setKeyModifierAndCode(
					KeyModifier.getActiveModifier(), -100 + mouseButton);
			selectedKey = null;
			resetMappingAndUpdateButtons();
			return true;
		}
		if (!isMouseYWithinSlotBounds(mouseY)) {
			return false;
		}
		int i = getSlotIndexFromScreenCoords(mouseX, mouseY);
		if (i >= 0 && i < children().size()) {
			int j = left + width / 2 - getListWidth() / 2 + 2;
			int k = top + 4 - getAmountScrolled() + i * slotHeight + headerPadding;
			int l = mouseX - j;
			int i1 = mouseY - k;
			Entry entry = children().get(i);
			if (entry.mousePressed(i, mouseX, mouseY, mouseButton, l, i1)) {
				dragging = true;
			}
		}
		return false;
	}

	/** Drag continuation (modern {@code mouseDragged}) driven from the screen's mouseClickMove. */
	public void mouseDragged(int mouseX, int mouseY) {
		if (dragging) {
			for (Entry entry : children()) {
				entry.mouseDragged(mouseX, mouseY);
			}
		}
	}

	@Override
	public boolean mouseReleased(int mouseX, int mouseY, int mouseButton) {
		dragging = false;
		for (Entry entry : children()) {
			entry.mouseReleased(0, mouseX, mouseY, mouseButton, 0, 0);
		}
		return false;
	}

	public boolean isMouseOver(int mouseX, int mouseY) {
		return isMouseYWithinSlotBounds(mouseY) && mouseX >= left && mouseX <= right;
	}

	public boolean isDragging() {
		return dragging;
	}

	public @Nullable Entry getSelected() {
		return selected;
	}

	public void setSelected(@Nullable Entry entry) {
		if (selected == entry) {
			return;
		}
		selected = entry;
	}

	public List<Entry> children() {
		return children0;
	}

	/** The displayed (search-filtered) entries. */
	protected final List<Entry> children0 = Lists.newArrayList();

	/** Toggling button used for the preview-overlay toggle (modern CycleButton). */
	public static class OptionButtonLike extends GuiButton {

		private java.util.function.Consumer<OptionButtonLike> onPress = $ -> {
		};

		public OptionButtonLike(ITextComponent message, ITextComponent title, int x, int y, int width, int height) {
			super(0, x, y, width, height, message.getFormattedText());
		}

		public void setOnPress(java.util.function.Consumer<OptionButtonLike> onPress) {
			this.onPress = onPress;
		}

		/** 1.12.2: GuiButton.displayString is protected, so screens update the label through this setter. */
		public void setMessage(ITextComponent message) {
			displayString = message.getFormattedText();
		}

		@Override
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			if (super.mousePressed(mc, mouseX, mouseY)) {
				onPress.accept(this);
				return true;
			}
			return false;
		}
	}

	public static class EntryWidget {
		public final JadeWidget widget;
		public int offsetX;

		public EntryWidget(JadeWidget widget) {
			this.widget = widget;
		}

		public EntryWidget(JadeWidget widget, int offsetX, int offsetY, boolean floatRight) {
			this(widget);
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.floatRight = floatRight;
		}

		public int offsetY;
		public boolean floatRight;
	}

	public static class Entry implements IGuiListEntry {

		protected final List<String> messages = Lists.newArrayList();
		private final List<JadeWidget> rawWidgets = Lists.newArrayList();
		protected final List<EntryWidget> widgets = Lists.newArrayList();
		protected List<ITextComponent> description = List.of();
		private @Nullable Entry parent;
		private List<Entry> children = List.of();
		protected TitleWidget title;
		protected JadeFont font;
		private @Nullable JadeWidget mainWidget;
		private final List<Consumer<Entry>> resizeListeners = Lists.newArrayList();
		protected int contentX;
		protected int contentY;
		protected int contentWidth;
		protected int contentHeight;
		private boolean hovered;

		public Entry(TitleWidget title) {
			this.title = title;
			font = title.font();
			addWidget(new EntryWidget(title, getTextX(), getTextY(), false));
			addMessage(title.getString());
		}

		public Entry(ITextComponent component) {
			this(new TitleWidget(component));
		}

		public static ITextComponent makeTitle(String key) {
			return new TextComponentTranslation(makeKey(key));
		}

		public static String makeKey(String key) {
			return "config." + Jade.ID + "." + key;
		}

		@Override
		public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
			// no-op: positions are computed at draw time
		}

		@Override
		public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
			contentX = x;
			contentY = y;
			contentWidth = listWidth;
			contentHeight = slotHeight;
			extractContent(mouseX, mouseY, isSelected, partialTicks);
		}

		@Override
		public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
			for (EntryWidget widget : widgets) {
				JadeWidget rawWidget = widget.widget;
				if (rawWidget instanceof TitleWidget) {
					continue;
				}
				int x = widgetX(widget);
				int y = contentY + contentHeight / 2 + widget.offsetY;
				if (mouseX >= x && mouseX < x + rawWidget.getWidth() && mouseY >= y && mouseY < y + rawWidget.getHeight()) {
					if (rawWidget instanceof JadeWidget.Button button) {
						GuiButton guiButton = button.button;
						if (guiButton instanceof CycleOptionValue.CycleButton<?> || guiButton instanceof SliderOptionValue.Slider) {
							// cycling/slider buttons own their press handling (cycle / setValueFromMouse)
							if (guiButton.mousePressed(Minecraft.getMinecraft(), mouseX, mouseY)) {
								guiButton.playPressSound(Minecraft.getMinecraft().getSoundHandler());
								guiButton.mouseReleased(mouseX, mouseY);
							}
						} else if (button.button.mousePressed(Minecraft.getMinecraft(), mouseX, mouseY)) {
							button.button.playPressSound(Minecraft.getMinecraft().getSoundHandler());
							button.button.mouseReleased(mouseX, mouseY);
						}
						return true;
					}
					if (rawWidget instanceof JadeWidget.TextField textField) {
						textField.textField.mouseClicked(mouseX, mouseY, mouseEvent);
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
			// button mouseReleased handled synchronously in mousePressed; nothing left to do
		}

		public void mouseDragged(int mouseX, int mouseY) {
			// slider drag support: re-route to the slider button
			for (EntryWidget widget : widgets) {
				if (widget.widget instanceof JadeWidget.Button button && button.button instanceof SliderOptionValue.Slider slider) {
					slider.mouseDragged(mouseX, mouseY);
				}
			}
		}

		public void extractContent(int mouseX, int mouseY, boolean hovered, float partialTicks) {
			this.hovered = hovered;
			for (EntryWidget widget : widgets) {
				JadeWidget rawWidget = widget.widget;
				int x;
				if (widget.floatRight) {
					x = contentWidth - 110 + widget.offsetX;
				} else {
					x = widget.offsetX;
				}
				rawWidget.setX(contentX + x);
				rawWidget.setY(contentY + contentHeight / 2 + widget.offsetY);
				if (rawWidget instanceof JadeWidget.TextField textField && textField.textField instanceof NotUglyEditBox editBox) {
					editBox.hovered = isMouseOverWidget(rawWidget, mouseX, mouseY);
				}
				rawWidget.extractRenderState(mouseX, mouseY, partialTicks);
			}
		}

		private boolean isMouseOverWidget(JadeWidget widget, int mouseX, int mouseY) {
			return mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
					&& mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight();
		}

		private int widgetX(EntryWidget widget) {
			if (widget.floatRight) {
				return contentX + contentWidth - 110 + widget.offsetX;
			}
			return contentX + widget.offsetX;
		}

		public void setWidth(int width) {
			contentWidth = width;
			notifyResizeListeners();
		}

		public void setHeight(int height) {
			contentHeight = height;
			notifyResizeListeners();
		}

		public void addResizeListener(Consumer<Entry> listener) {
			resizeListeners.add(listener);
		}

		public void notifyResizeListeners() {
			for (EntryWidget widget : widgets) {
				if (widget.widget == title) {
					widget.offsetX = getTextX();
					widget.offsetY = getTextY();
					break;
				}
			}
			for (Consumer<Entry> listener : resizeListeners) {
				listener.accept(this);
			}
		}

		public @Nullable JadeWidget mainWidget() {
			return mainWidget;
		}

		@SuppressWarnings("UnusedReturnValue")
		public EntryWidget addWidget(JadeWidget widget, int offsetX) {
			return addWidget(new EntryWidget(widget, offsetX, -widget.getHeight() / 2, true));
		}

		public EntryWidget addWidget(EntryWidget widget) {
			widgets.add(widget);
			rawWidgets.add(widget.widget);
			if (mainWidget == null && !(widget.widget instanceof TitleWidget)) {
				mainWidget = widget.widget;
			}
			return widget;
		}

		public List<JadeWidget> children() {
			return rawWidgets;
		}

		public void setDisabled(boolean disabled) {
			for (JadeWidget widget : rawWidgets) {
				if (widget instanceof TitleWidget) {
					continue;
				}
				widget.active = !disabled;
				if (widget instanceof JadeWidget.TextField textField && textField.textField instanceof NotUglyEditBox editBox) {
					editBox.setEnabled(!disabled);
				}
			}
		}

		public List<ITextComponent> getDescription() {
			return description;
		}

		public List<ITextComponent> getDescriptionOnShift() {
			return List.of();
		}

		public int getTextX() {
			return 10;
		}

		public int getTextY() {
			return -3;
		}

		public int getTextWidth() {
			return title.getWidth();
		}

		public Entry parent(Entry parent) {
			this.parent = parent;
			if (parent.children.isEmpty()) {
				parent.children = Lists.newArrayList();
			}
			parent.children.add(this);
			return this;
		}

		public @Nullable Entry parent() {
			return parent;
		}

		public Entry root() {
			Entry entry = this;
			while (entry.parent() != null) {
				entry = Objects.requireNonNull(entry.parent());
			}
			return entry;
		}

		public final List<String> getMessages() {
			return messages;
		}

		public void addMessage(String message) {
			messages.add(TextFormatting.getTextWithoutFormattingCodes(message).toLowerCase(Locale.ENGLISH));
		}

		public void addMessageKey(String key) {
			key = makeKey(key + "_extra_msg");
			if (JadeUI.hasTranslation(key)) {
				addMessage(I18n.format(key));
			}
		}

		public ITextComponent title() {
			return title.getMessage();
		}

		public void setTitle(ITextComponent title) {
			this.title.setMessage(title);
		}

		public int getContentX() {
			return contentX;
		}

		public int getContentY() {
			return contentY;
		}

		public int getContentWidth() {
			return contentWidth;
		}

		public int getContentHeight() {
			return contentHeight;
		}

		public int getContentRight() {
			return contentX + contentWidth;
		}

		public int getContentBottom() {
			return contentY + contentHeight;
		}

		public int getContentYMiddle() {
			return contentY + contentHeight / 2;
		}

		public boolean isMouseOver(int mouseX, int mouseY) {
			return mouseX >= contentX && mouseX < contentX + contentWidth
					&& mouseY >= contentY && mouseY < contentY + contentHeight;
		}

		public boolean isHovered() {
			return hovered;
		}

		public void updateHovered(int mouseX, int mouseY) {
			hovered = isMouseOver(mouseX, mouseY);
		}

		public boolean isFocused() {
			return false;
		}
	}

	public static class Title extends Entry {

		public ITextComponent narration;

		public Title(String key) {
			this(makeTitle(key));
			addMessageKey(key);
			key = makeKey(key + "_desc");
			if (JadeUI.hasTranslation(key)) {
				description = List.of(new TextComponentTranslation(key));
				addMessage(description.get(0).getFormattedText());
			}
			narration = new TextComponentTranslation("narration.jade.category", title());
		}

		public Title(ITextComponent title) {
			super(title);
			narration = title;
		}

		@Override
		public int getTextX() {
			return (getContentWidth() - getTextWidth()) / 2 - 10;
		}

		@Override
		public int getTextY() {
			return 0;
		}
	}
}
