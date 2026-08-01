package snownee.jade.addon.universal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityMinecartContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.ScreenDirection;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.impl.ui.HorizontalLineElement;
import snownee.jade.util.ClientProxy;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.WailaExceptionHandler;

public class ItemStorageProvider<T extends Accessor<?>> implements IServerDataProvider<T> {

	public static final Cache<Object, ItemCollector<?>> targetCache = CacheBuilder.newBuilder().weakKeys().expireAfterAccess(60, TimeUnit.SECONDS).build();
	public static final Cache<Object, ItemCollector<?>> containerCache = CacheBuilder.newBuilder().weakKeys().expireAfterAccess(120, TimeUnit.SECONDS).build();
	private static final DataCodec<ItemStack> ITEM_STACK_CODEC = new DataCodec<>() {
		@Override
		public ItemStack decode(PacketBuffer buf) {
			return DataCodec.readStack(buf);
		}

		@Override
		public void encode(PacketBuffer buf, ItemStack value) {
			buf.writeItemStack(value);
		}
	};
	private static final DataCodec<Map.Entry<ResourceLocation, List<ViewGroup<ItemStack>>>> STREAM_CODEC = ViewGroup.listCodec(ITEM_STACK_CODEC);

	public static final ItemStorageProvider<BlockAccessor> BLOCK = new ItemStorageProvider<>();
	public static final ItemStorageProvider<EntityAccessor> ENTITY = new ItemStorageProvider<>();

	public static class Client<T extends Accessor<?>> extends ItemStorageProvider<T> implements IComponentProvider<T> {
		public static final Client<BlockAccessor> BLOCK = new Client<>();
		public static final Client<EntityAccessor> ENTITY = new Client<>();

		@Override
		public void appendTooltip(ITooltip tooltip, T accessor, IPluginConfig config) {
			if (accessor.getTarget() instanceof TileEntityFurnace) {
				return;
			}
			append(tooltip, accessor, config);
		}

		public static void append(ITooltip tooltip, Accessor<?> accessor, IPluginConfig config) {
			if (!accessor.getServerData().hasKey(JadeIds.UNIVERSAL_ITEM_STORAGE.toString())) {
				if (accessor.getServerData().getBoolean("Loot")) {
					tooltip.add(new TextComponentTranslation("jade.loot_not_generated"));
				} else if (accessor.getServerData().getBoolean("Locked")) {
					tooltip.add(new TextComponentTranslation("jade.locked"));
				}
				return;
			}

			List<ClientViewGroup<ItemView>> groups = ClientProxy.mapToClientGroups(
					accessor,
					JadeIds.UNIVERSAL_ITEM_STORAGE,
					ItemStorageProvider.STREAM_CODEC,
					WailaClientRegistration.instance().itemStorageProviders::get,
					tooltip);
			if (groups == null || groups.isEmpty()) {
				return;
			}

			MutableBoolean showName = new MutableBoolean(true);
			MutableInt amountWidth = new MutableInt();
			int totalSize = 0;
			for (ClientViewGroup<ItemView> group : groups) {
				for (ItemView view : group.views) {
					if (view.amountText != null) {
						showName.setFalse();
					}
					if (!view.item.isEmpty()) {
						totalSize++;
						if (totalSize == config.getInt(JadeIds.UNIVERSAL_ITEM_STORAGE_SHOW_NAME_AMOUNT)) {
							showName.setFalse();
						}
					}
					if (showName.isTrue()) {
						String s = IDisplayHelper.get().humanReadableNumber(view.item.getCount(), "", false, null);
						amountWidth.setValue(Math.max(amountWidth.intValue(), Minecraft.getMinecraft().fontRenderer.getStringWidth(s)));
					}
				}
			}

			boolean renderGroup = groups.size() > 1 || groups.get(0).shouldRenderGroup();
			ClientViewGroup.tooltip(tooltip, groups, renderGroup, (theTooltip, group) -> {
				if (renderGroup) {
					theTooltip.add(new HorizontalLineElement());
					if (group.title != null) {
						theTooltip.append(JadeUI.text(group.title).scale(0.5F));
						theTooltip.append(new HorizontalLineElement().flexGrow(1));
					}
				}
				if (group.views.isEmpty() && group.extraData != null) {
					float progress = group.extraData.hasKey("Collecting") ? group.extraData.getFloat("Collecting") : -1F;
					if (progress >= 0 && progress < 1) {
						ITextComponent component = new TextComponentTranslation("jade.collectingItems");
						if (progress != 0) {
							component.appendText(" " + (int) (progress * 100) + "%");
						}
						theTooltip.add(component);
					}
				}
				int drawnCount = 0;
				int realSize = config.getInt(accessor.showDetails() ? JadeIds.UNIVERSAL_ITEM_STORAGE_DETAILED_AMOUNT :
						JadeIds.UNIVERSAL_ITEM_STORAGE_NORMAL_AMOUNT);
				realSize = Math.min(group.views.size(), realSize);
				List<Element> elements = Lists.newArrayList();
				for (int i = 0; i < realSize; i++) {
					ItemView itemView = group.views.get(i);
					ItemStack stack = itemView.item;
					if (stack.isEmpty()) {
						continue;
					}
					if (i > 0 && (showName.isTrue() || drawnCount >= config.getInt(JadeIds.UNIVERSAL_ITEM_STORAGE_ITEMS_PER_LINE))) {
						theTooltip.add(elements);
						theTooltip.setLineMargin(-1, ScreenDirection.DOWN, 0);
						elements.clear();
						drawnCount = 0;
					}

					if (showName.isTrue()) {
						if (itemView.description != null) {
							elements.add(JadeUI.smallItem(stack));
							elements.addAll(itemView.description);
						} else {
							elements.add(JadeUI.smallItem(stack).refreshNarration());
							String s = IDisplayHelper.get().humanReadableNumber(stack.getCount(), "", false, null);
							int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(s);
							if (width < amountWidth.intValue()) {
								elements.add(JadeUI.spacer(amountWidth.intValue() - width, 0));
							}
							elements.add(JadeUI.text(new TextComponentString(s).appendText("× ")
										.appendSibling(IDisplayHelper.get().stripColor(new TextComponentString(stack.getDisplayName())))).narration(""));
						}
					} else if (itemView.amountText != null) {
						elements.add(JadeUI.item(stack, 1, itemView.amountText));
					} else {
						elements.add(JadeUI.item(stack));
					}
					drawnCount++;
				}

				if (!elements.isEmpty()) {
					theTooltip.add(elements);
				}
			});
		}
	}

	public static void putData(Accessor<?> accessor) {
		NBTTagCompound tag = accessor.getServerData();
		Object target = accessor.getTarget();
		EntityPlayer player = accessor.getPlayer();
		Map.Entry<ResourceLocation, List<ViewGroup<ItemStack>>> entry = CommonProxy.getServerExtensionData(
				accessor, WailaCommonRegistration.instance().itemStorageProviders);
		if (entry != null) {
			for (ViewGroup<ItemStack> group : entry.getValue()) {
				if (group.views.size() > ItemCollector.MAX_SIZE) {
					group.views = group.views.subList(0, ItemCollector.MAX_SIZE);
				}
			}
			tag.setTag(JadeIds.UNIVERSAL_ITEM_STORAGE.toString(), accessor.encodeAsNbt(STREAM_CODEC, entry));
			return;
		}
		if (hasLootTable(target)) {
			tag.setBoolean("Loot", true);
		} else if (!player.isCreative() && !player.isSpectator() && target instanceof TileEntityLockable te && te.isLocked()) {
			tag.setBoolean("Locked", true);
		}
	}

	private static boolean hasLootTable(Object target) {
		return target instanceof TileEntityLockableLoot container && container.getLootTable() != null ||
				target instanceof EntityMinecartContainer minecart && minecart.getLootTable() != null;
	}

	@Override
	public void appendServerData(NBTTagCompound tag, T accessor) {
		if (accessor.getTarget() instanceof TileEntityFurnace) {
			return;
		}
		putData(accessor);
	}

	@Override
	public boolean shouldRequestData(T accessor) {
		if (accessor.getTarget() instanceof TileEntityFurnace) {
			return false;
		}
		int amount = accessor.showDetails() ? IWailaConfig.get().plugin().getInt(JadeIds.UNIVERSAL_ITEM_STORAGE_DETAILED_AMOUNT) :
				IWailaConfig.get().plugin().getInt(JadeIds.UNIVERSAL_ITEM_STORAGE_NORMAL_AMOUNT);
		if (amount == 0) {
			return false;
		}
		if (IWailaConfig.get().plugin().get(JadeIds.UNIVERSAL_ITEM_STORAGE_SORT)) {
			accessor.getServerData().setBoolean("SortItems", true);
		}
		return WailaCommonRegistration.instance().itemStorageProviders.hitsAny(accessor, IServerExtensionProvider::shouldRequestData);
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.UNIVERSAL_ITEM_STORAGE;
	}

	@Override
	public int getDefaultPriority() {
		return TooltipPosition.BODY + 1000;
	}

	public static class Extension implements IServerExtensionProvider<ItemStack>, IClientExtensionProvider<ItemStack, ItemView> {
		public static final Extension INSTANCE = new Extension();

		@Override
		public ResourceLocation getUid() {
			return JadeIds.UNIVERSAL_ITEM_STORAGE_DEFAULT;
		}

		@Nullable
		@Override
		public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
			Object target = accessor.getTarget();
			if (target == null) {
				return CommonProxy.createItemCollector(accessor, containerCache).update(accessor);
			}
			if (hasLootTable(target)) {
				return null;
			}
			if (target instanceof EntityArmorStand) {
				return null;
			}
			EntityPlayer player = accessor.getPlayer();
			if (!player.isCreative() && !player.isSpectator() && target instanceof TileEntityLockable te && te.isLocked()) {
				return null;
			}
			if (target instanceof TileEntityEnderChest) {
				InventoryEnderChest inventory = player.getInventoryEnderChest();
				return new ItemCollector<>(new ItemIterator.ContainerItemIterator($ -> inventory, 0)).update(accessor);
			}
			ItemCollector<?> itemCollector;
			try {
				itemCollector = targetCache.get(target, () -> CommonProxy.createItemCollector(accessor, containerCache));
			} catch (ExecutionException e) {
				WailaExceptionHandler.handleErr(e, this, null);
				return null;
			}
			return itemCollector.update(accessor);
		}

		@Override
		public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
			return ClientViewGroup.map(groups, ItemView::new, null);
		}

		@Override
		public boolean shouldRequestData(Accessor<?> accessor) {
			Object target = accessor.getTarget();
			if (target instanceof TileEntityEnderChest || target instanceof IInventory) {
				return true;
			}
			return CommonProxy.hasDefaultItemStorage(accessor);
		}

		@Override
		public int getDefaultPriority() {
			return 9999;
		}
	}
}
