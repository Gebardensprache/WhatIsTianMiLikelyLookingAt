package snownee.jade.compat.top;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IElement;
import mcjty.theoneprobe.api.IEntityStyle;
import mcjty.theoneprobe.api.IIconStyle;
import mcjty.theoneprobe.api.IItemStyle;
import mcjty.theoneprobe.api.ILayoutStyle;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProgressStyle;
import mcjty.theoneprobe.api.ITextStyle;
import mcjty.theoneprobe.api.NumberFormat;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Server-side {@link IProbeInfo} implementation that captures every mutating
 * call as an {@link ElementDto} tree for later serialization.
 */
public class CaptureProbeInfo implements IProbeInfo {

	private List<ElementDto> elements = new ArrayList<>();

	public List<ElementDto> getElements() {
		return elements;
	}

	@Override
	public IProbeInfo text(String text, ITextStyle style) {
		return text(text);
	}

	@Override
	public IProbeInfo text(String text) {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.TEXT;
		dto.text = text == null ? "" : text;
		elements.add(dto);
		return this;
	}

	@Override
	public IProbeInfo item(ItemStack stack, IItemStyle style) {
		return item(stack);
	}

	@Override
	public IProbeInfo item(ItemStack stack) {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.ITEM;
		dto.stack = stack == null ? ItemStack.EMPTY : stack.copy();
		elements.add(dto);
		return this;
	}

	@Override
	public IProbeInfo itemLabel(ItemStack stack, ITextStyle style) {
		return itemLabel(stack);
	}

	@Override
	public IProbeInfo itemLabel(ItemStack stack) {
		if (stack != null && !stack.isEmpty()) {
			item(stack);
			text(stack.getDisplayName());
		}
		return this;
	}

	@Override
	public IProbeInfo progress(int current, int max, IProgressStyle style) {
		return progress((long) current, (long) max, style);
	}

	@Override
	public IProbeInfo progress(long current, long max, IProgressStyle style) {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.PROGRESS;
		dto.current = current;
		dto.max = max;
		if (style != null) {
			dto.progressFilledColor = style.getFilledColor();
			dto.progressAlternateColor = style.getAlternatefilledColor();
			dto.progressBorderColor = style.getBorderColor();
			dto.progressBackgroundColor = style.getBackgroundColor();
			dto.progressShowText = style.isShowText();
			dto.progressNumberFormat = style.getNumberFormat().ordinal();
			dto.progressPrefix = style.getPrefix();
			dto.progressSuffix = style.getSuffix();
		}
		elements.add(dto);
		return this;
	}

	@Override
	public IProbeInfo progress(int current, int max) {
		return progress((long) current, (long) max);
	}

	@Override
	public IProbeInfo progress(long current, long max) {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.PROGRESS;
		dto.current = current;
		dto.max = max;
		elements.add(dto);
		return this;
	}

	@Override
	public IProbeInfo horizontal(ILayoutStyle style) {
		return horizontal();
	}

	@Override
	public IProbeInfo horizontal() {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.HORIZONTAL;
		elements.add(dto);
		CaptureProbeInfo child = new CaptureProbeInfo();
		child.elements = dto.children;
		return child;
	}

	@Override
	public IProbeInfo vertical(ILayoutStyle style) {
		return vertical();
	}

	@Override
	public IProbeInfo vertical() {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.VERTICAL;
		elements.add(dto);
		CaptureProbeInfo child = new CaptureProbeInfo();
		child.elements = dto.children;
		return child;
	}

	@Override
	public IProbeInfo icon(ResourceLocation icon, int u, int v, int w, int h, IIconStyle style) {
		return icon(icon, u, v, w, h);
	}

	@Override
	public IProbeInfo icon(ResourceLocation icon, int u, int v, int w, int h) {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.ICON;
		dto.text = icon == null ? "" : icon.toString();
		elements.add(dto);
		return this;
	}

	@Override
	public IProbeInfo entity(String entityName, IEntityStyle style) {
		return entity(entityName);
	}

	@Override
	public IProbeInfo entity(String entityName) {
		return text(entityName);
	}

	@Override
	public IProbeInfo entity(Entity entity, IEntityStyle style) {
		return entity(entity);
	}

	@Override
	public IProbeInfo entity(Entity entity) {
		return text(entity == null ? "" : entity.getName());
	}

	@Override
	public IProbeInfo element(IElement element) {
		if (element != null) {
			ElementDto dto = new ElementDto();
			dto.type = ElementDto.CUSTOM;
			dto.factoryId = element.getID();
			dto.data = writeElement(element);
			elements.add(dto);
		}
		return this;
	}

	/**
	 * Serializes a custom element through its {@link IElement#toBytes(ByteBuf)} protocol so the
	 * client can rebuild it with the registered {@code IElementFactory}.
	 */
	static byte[] writeElement(IElement element) {
		ByteBuf buf = Unpooled.buffer();
		try {
			element.toBytes(buf);
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			return data;
		} finally {
			buf.release();
		}
	}

	// Style factories — return defaults (we don't render TOP styles).

	@Override
	public ILayoutStyle defaultLayoutStyle() {
		return new LayoutStyleImpl();
	}

	@Override
	public IProgressStyle defaultProgressStyle() {
		return new ProgressStyleImpl();
	}

	@Override
	public ITextStyle defaultTextStyle() {
		return new TextStyleImpl();
	}

	@Override
	public IItemStyle defaultItemStyle() {
		return new ItemStyleImpl();
	}

	@Override
	public IEntityStyle defaultEntityStyle() {
		return new EntityStyleImpl();
	}

	@Override
	public IIconStyle defaultIconStyle() {
		return new IconStyleImpl();
	}

	// Minimal style implementations matching the TOP API exactly.

	private static class LayoutStyleImpl implements ILayoutStyle {
		@Override public ILayoutStyle borderColor(Integer c) { return this; }
		@Override public ILayoutStyle spacing(int f) { return this; }
		@Override public ILayoutStyle alignment(ElementAlignment alignment) { return this; }
		@Override public Integer getBorderColor() { return null; }
		@Override public int getSpacing() { return 0; }
		@Override public ElementAlignment getAlignment() { return ElementAlignment.ALIGN_TOPLEFT; }
	}

	private static class ProgressStyleImpl implements IProgressStyle {
		@Override public IProgressStyle borderColor(int c) { return this; }
		@Override public IProgressStyle backgroundColor(int c) { return this; }
		@Override public IProgressStyle filledColor(int c) { return this; }
		@Override public IProgressStyle alternateFilledColor(int c) { return this; }
		@Override public IProgressStyle showText(boolean b) { return this; }
		@Override public IProgressStyle numberFormat(NumberFormat f) { return this; }
		@Override public IProgressStyle prefix(String prefix) { return this; }
		@Override public IProgressStyle suffix(String suffix) { return this; }
		@Override public IProgressStyle width(int w) { return this; }
		@Override public IProgressStyle height(int h) { return this; }
		@Override public IProgressStyle lifeBar(boolean b) { return this; }
		@Override public IProgressStyle armorBar(boolean b) { return this; }
		@Override public int getBorderColor() { return 0; }
		@Override public int getBackgroundColor() { return 0xFF000000; }
		@Override public int getFilledColor() { return 0xFF55FF55; }
		@Override public int getAlternatefilledColor() { return 0xFF55FF55; }
		@Override public boolean isShowText() { return true; }
		@Override public NumberFormat getNumberFormat() { return NumberFormat.FULL; }
		@Override public String getPrefix() { return ""; }
		@Override public String getSuffix() { return ""; }
		@Override public int getWidth() { return 100; }
		@Override public int getHeight() { return 8; }
		@Override public boolean isLifeBar() { return false; }
		@Override public boolean isArmorBar() { return false; }
	}

	private static class TextStyleImpl implements ITextStyle {
	}

	private static class ItemStyleImpl implements IItemStyle {
		@Override public IItemStyle width(int w) { return this; }
		@Override public IItemStyle height(int h) { return this; }
		@Override public int getWidth() { return 16; }
		@Override public int getHeight() { return 16; }
	}

	private static class EntityStyleImpl implements IEntityStyle {
		@Override public IEntityStyle width(int w) { return this; }
		@Override public IEntityStyle height(int h) { return this; }
		@Override public IEntityStyle scale(float scale) { return this; }
		@Override public int getWidth() { return 25; }
		@Override public int getHeight() { return 25; }
		@Override public float getScale() { return 1.0f; }
	}

	private static class IconStyleImpl implements IIconStyle {
		@Override public IIconStyle width(int w) { return this; }
		@Override public IIconStyle height(int h) { return this; }
		@Override public int getWidth() { return 16; }
		@Override public int getHeight() { return 16; }
		@Override public IIconStyle textureWidth(int w) { return this; }
		@Override public IIconStyle textureHeight(int h) { return this; }
		@Override public int getTextureWidth() { return 256; }
		@Override public int getTextureHeight() { return 256; }
	}
}
