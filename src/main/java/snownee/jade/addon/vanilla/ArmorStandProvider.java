package snownee.jade.addon.vanilla;

import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.ScreenDirection;

public class ArmorStandProvider implements IEntityComponentProvider {
	public static final ArmorStandProvider INSTANCE = new ArmorStandProvider();

	/**
	 * 1.12.2: upstream iterates {@code EquipmentSlot.VALUES}, which (like 1.12.2's
	 * {@code EntityEquipmentSlot.values()}) is declared feet-first — so upstream lists
	 * boots on top and the helmet at the bottom. User feedback calls that ordering out as
	 * inverted; list the pieces head-to-toe like the vanilla inventory armor columns
	 * (helmet first, boots last), with hand-held items trailing. This is a deliberate,
	 * documented divergence from the modern source tree.
	 */
	private static final EntityEquipmentSlot[] SLOT_ORDER = {
			EntityEquipmentSlot.HEAD,
			EntityEquipmentSlot.CHEST,
			EntityEquipmentSlot.LEGS,
			EntityEquipmentSlot.FEET,
			EntityEquipmentSlot.MAINHAND,
			EntityEquipmentSlot.OFFHAND
	};

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		EntityArmorStand entity = (EntityArmorStand) accessor.getEntity();
		boolean empty = true;
		// 1.12.2: no EquipmentSlot.VALUES constant, use the explicit head-to-toe order
		for (EntityEquipmentSlot slot : SLOT_ORDER) {
			ItemStack stack = entity.getItemStackFromSlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			tooltip.add(JadeUI.smallItem(stack));
			// 1.12.2: ItemStack.getDisplayName() returns a String, wrap it for stripColor
			tooltip.append(IDisplayHelper.get().stripColor(new TextComponentString(stack.getDisplayName())));
			tooltip.setLineMargin(-1, ScreenDirection.DOWN, 0);
			empty = false;
		}
		if (!empty) {
			tooltip.setLineMargin(-1, ScreenDirection.DOWN, 1);
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_ARMOR_STAND;
	}

}
