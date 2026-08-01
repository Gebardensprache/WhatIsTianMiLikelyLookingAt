package snownee.jade.impl.ui;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.HoverEvent;
import snownee.jade.api.ui.Element;
import snownee.jade.overlay.DisplayHelper;

public class ItemStackElement extends Element {

	private final ItemStack item;
	private final float scale;
	private final @Nullable String text;

	private ItemStackElement(ItemStack item, float scale, @Nullable String text) {
		this.item = item;
		this.scale = scale == 0 ? 1 : scale;
		this.text = text;
		width = height = MathHelper.floor(18 * scale);
	}

	public static ItemStackElement of(ItemStack stack) {
		return of(stack, 1);
	}

	public static ItemStackElement of(ItemStack stack, float scale) {
		return of(stack, scale, null);
	}

	public static ItemStackElement of(ItemStack stack, float scale, @Nullable String text) {
		return new ItemStackElement(stack, scale, text);
	}

	@Override
	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		if (item.isEmpty()) {
			return;
		}
		if (mouseX != -1 && getRectangle().containsPoint(mouseX, mouseY)) {
			setHoverEffect(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new TextComponentString(item.writeToNBT(new NBTTagCompound()).toString())));
		}
		DisplayHelper.INSTANCE.drawItem(getX() + 1, getY() + 1, item, scale, text);
	}

	@Override
	public @Nullable ITextComponent getNarration() {
		if (item.isEmpty()) {
			return null;
		}
		return new TextComponentString("%s %s".formatted(item.getCount(), item.getDisplayName()));
	}

	public ItemStack getItem() {
		return item;
	}

	/**
	 * 1.12.2: upstream serializes the item's full component data via {@code ComponentHolders.serialize(...)}
	 * against the connection's registry access, neither of which exist here. Falls back to copying the
	 * item's NBT representation as a plain string.
	 */
	@Override
	public boolean copyToClipboard() {
		if (item.isEmpty()) {
			return false;
		}
		GuiScreen.setClipboardString(item.writeToNBT(new NBTTagCompound()).toString());
		return true;
	}
}
