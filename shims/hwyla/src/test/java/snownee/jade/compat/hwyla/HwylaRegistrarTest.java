package snownee.jade.compat.hwyla;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.ScreenDirection;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.WailaCommonRegistration;

class HwylaRegistrarTest {

	@BeforeAll
	static void bootstrap() {
		Bootstrap.register();
		seedClientSide();
	}

	/**
	 * 1.12.2: {@code CommonProxy.isPhysicallyClient()} calls {@code FMLLaunchHandler.side()},
	 * which reads a static field only populated by the FML launcher. Under a bare JUnit worker it
	 * is null, so seed it to {@code Side.CLIENT} (the same side the Jade root's run tasks use).
	 */
	private static void seedClientSide() {
		try {
			java.lang.reflect.Field side = net.minecraftforge.fml.relauncher.FMLLaunchHandler.class
					.getDeclaredField("side");
			side.setAccessible(true);
			side.set(null, net.minecraftforge.fml.relauncher.Side.CLIENT);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to seed FMLLaunchHandler.side for tests", e);
		}
	}

	@BeforeEach
	void resetRegistrations() {
		WailaClientRegistration.reset();
		WailaCommonRegistration.reset();
		// 1.12.2 shim: the config handler reads through the public client registration, which is
		// handed to it by HwylaCompat.registerClient; wire it to the fresh instance for the test.
		HwylaConfigHandler.INSTANCE.setClientRegistration(WailaClientRegistration.instance());
	}

	@Test
	void repeatedProviderRegistrationsHaveUniqueUidsAndPreserveRoles() {
		HwylaRegistrar registrar = registrar();
		AtomicInteger headCalls = new AtomicInteger();
		AtomicInteger bodyCalls = new AtomicInteger();
		AtomicInteger tailCalls = new AtomicInteger();
		AtomicInteger stackCalls = new AtomicInteger();
		mcp.mobius.waila.api.IWailaDataProvider provider = new mcp.mobius.waila.api.IWailaDataProvider() {
			@Override
			public ItemStack getWailaStack(mcp.mobius.waila.api.IWailaDataAccessor accessor,
					mcp.mobius.waila.api.IWailaConfigHandler config) {
				stackCalls.incrementAndGet();
				return ItemStack.EMPTY;
			}

			@Override
			public List<String> getWailaHead(ItemStack stack, List<String> tooltip,
					mcp.mobius.waila.api.IWailaDataAccessor accessor,
					mcp.mobius.waila.api.IWailaConfigHandler config) {
				headCalls.incrementAndGet();
				return tooltip;
			}

			@Override
			public List<String> getWailaBody(ItemStack stack, List<String> tooltip,
					mcp.mobius.waila.api.IWailaDataAccessor accessor,
					mcp.mobius.waila.api.IWailaConfigHandler config) {
				bodyCalls.incrementAndGet();
				return tooltip;
			}

			@Override
			public List<String> getWailaTail(ItemStack stack, List<String> tooltip,
					mcp.mobius.waila.api.IWailaDataAccessor accessor,
					mcp.mobius.waila.api.IWailaConfigHandler config) {
				tailCalls.incrementAndGet();
				return tooltip;
			}
		};

		registrar.registerHeadProvider(provider, BlockChest.class);
		registrar.registerBodyProvider(provider, BlockChest.class);
		registrar.registerTailProvider(provider, BlockChest.class);
		registrar.registerStackProvider(provider, BlockChest.class);
		registrar.registerNBTProvider(provider, BlockChest.class);

		List<IComponentProvider<BlockAccessor>> providers = WailaClientRegistration.instance()
				.getBlockProviders(net.minecraft.init.Blocks.CHEST, provider1 -> provider1 instanceof HwylaBlockProviderBridge);
		assertThat(providers).hasSize(4);
		assertThat(providers).extracting(IComponentProvider::getUid).doesNotHaveDuplicates();
		assertThat(providers).extracting(provider1 -> ((HwylaBlockProviderBridge) provider1).getRole())
				.containsExactlyInAnyOrder(
						HwylaBlockProviderBridge.Role.HEAD,
						HwylaBlockProviderBridge.Role.BODY,
						HwylaBlockProviderBridge.Role.TAIL,
						HwylaBlockProviderBridge.Role.STACK);
		WailaClientRegistration.instance().loadComplete();
		List<snownee.jade.api.IServerDataProvider<BlockAccessor>> dataProviders = WailaCommonRegistration.instance()
				.blockDataProvidersOf(net.minecraft.init.Blocks.CHEST.getDefaultState(), null, false);
		assertThat(dataProviders).hasSize(1);
		assertThat(dataProviders.get(0).getUid()).isNotIn(providers.stream()
				.map(IComponentProvider::getUid).collect(java.util.stream.Collectors.toList()));
		WailaCommonRegistration.instance().loadComplete();

		for (IComponentProvider<BlockAccessor> component : providers) {
			component.appendTooltip(new RecordingTooltip(), blockAccessorProxy(), null);
		}

		assertThat(headCalls).hasValue(1);
		assertThat(bodyCalls).hasValue(1);
		assertThat(tailCalls).hasValue(1);
		assertThat(stackCalls).hasValue(1);
	}

	@Test
	void bodyOnlyRegistrationDoesNotInvokeOtherCallbacks() {
		HwylaRegistrar registrar = registrar();
		List<String> calls = new ArrayList<>();
		mcp.mobius.waila.api.IWailaDataProvider provider = new mcp.mobius.waila.api.IWailaDataProvider() {
			@Override
			public List<String> getWailaHead(ItemStack stack, List<String> tooltip,
					mcp.mobius.waila.api.IWailaDataAccessor accessor,
					mcp.mobius.waila.api.IWailaConfigHandler config) {
				calls.add("head");
				return tooltip;
			}

			@Override
			public List<String> getWailaBody(ItemStack stack, List<String> tooltip,
					mcp.mobius.waila.api.IWailaDataAccessor accessor,
					mcp.mobius.waila.api.IWailaConfigHandler config) {
				calls.add("body");
				return tooltip;
			}

			@Override
			public List<String> getWailaTail(ItemStack stack, List<String> tooltip,
					mcp.mobius.waila.api.IWailaDataAccessor accessor,
					mcp.mobius.waila.api.IWailaConfigHandler config) {
				calls.add("tail");
				return tooltip;
			}
		};
		registrar.registerBodyProvider(provider, BlockChest.class);
		HwylaBlockProviderBridge bridge = (HwylaBlockProviderBridge) WailaClientRegistration.instance()
				.getBlockProviders(net.minecraft.init.Blocks.CHEST, provider1 -> provider1 instanceof HwylaBlockProviderBridge).get(0);
		assertThat(bridge.getRole()).isEqualTo(HwylaBlockProviderBridge.Role.BODY);
		bridge.appendTooltip(new RecordingTooltip(), blockAccessorProxy(), null);
		assertThat(calls).containsExactly("body");
	}

	@Test
	void configPreservesModuleAndKeyNames() {
		registrar().addConfig("examplemod", "example.key", "Example", false);
		assertThat(HwylaConfigHandler.INSTANCE.getModuleNames()).contains("examplemod");
		assertThat(HwylaConfigHandler.INSTANCE.getConfigKeys("examplemod")).containsEntry("example.key", "Example");
		assertThat(HwylaConfigHandler.INSTANCE.getConfig("example.key")).isFalse();
	}

	@Test
	void entityOverrideRegistrationWarnsAndDoesNotRegisterBodyBridge() {
		mcp.mobius.waila.api.IWailaEntityProvider provider = new mcp.mobius.waila.api.IWailaEntityProvider() {
			@Override
			public Entity getWailaOverride(mcp.mobius.waila.api.IWailaEntityAccessor accessor,
					mcp.mobius.waila.api.IWailaConfigHandler config) {
				return accessor.getEntity();
			}
		};
		registrar().registerOverrideEntityProvider(provider, EntityLivingBase.class);
		assertThat(WailaClientRegistration.instance().entityComponentProviders.isEmpty()).isTrue();
	}

	@Test
	void bridgeRegistrationRetainsProvider() {
		mcp.mobius.waila.api.IWailaDataProvider provider = new mcp.mobius.waila.api.IWailaDataProvider() { };
		registrar().registerBodyProvider(provider, BlockChest.class);
		List<IComponentProvider<BlockAccessor>> providers = WailaClientRegistration.instance()
				.getBlockProviders(net.minecraft.init.Blocks.CHEST, provider1 -> true);
		assertThat(providers).hasSize(1);
		assertThat(((HwylaBlockProviderBridge) providers.get(0)).getHwylaProvider()).isSameAs(provider);
	}

	@Test
	void unsupportedOpsDoNotThrow() {
		HwylaRegistrar registrar = registrar();
		registrar.registerDecorator((mcp.mobius.waila.api.IWailaBlockDecorator) (stack, accessor, config) -> {}, Block.class);
		registrar.registerTooltipRenderer("test", new mcp.mobius.waila.api.IWailaTooltipRenderer() {
			@Override
			public java.awt.Dimension getSize(String[] params, mcp.mobius.waila.api.IWailaCommonAccessor accessor) {
				return new java.awt.Dimension(0, 0);
			}

			@Override
			public void draw(String[] params, mcp.mobius.waila.api.IWailaCommonAccessor accessor) {
			}
		});
	}

	private static HwylaRegistrar registrar() {
		return new HwylaRegistrar(HwylaConfigHandler.INSTANCE,
				WailaClientRegistration.instance(), WailaCommonRegistration.instance());
	}

	private static BlockAccessor blockAccessorProxy() {
		return (BlockAccessor) Proxy.newProxyInstance(HwylaRegistrarTest.class.getClassLoader(),
				new Class<?>[] {BlockAccessor.class}, (proxy, method, args) -> {
					if (method.getName().equals("getPickedResult")) {
						return ItemStack.EMPTY;
					}
					if (method.getReturnType() == boolean.class) return false;
					if (method.getReturnType() == float.class) return 0F;
					if (method.getReturnType() == int.class) return 0;
					return null;
				});
	}

	private static final class RecordingTooltip implements ITooltip {
		@Override public void clear() { }
		@Override public int size() { return 0; }
		@Override public void add(int index, Element element) { }
		@Override public void append(int index, Element element) { }
		@Override public boolean remove(ResourceLocation tag) { return false; }
		@Override public boolean replace(ResourceLocation tag, java.util.function.UnaryOperator<List<List<Element>>> elements) { return false; }
		@Override public boolean replace(ResourceLocation tag, net.minecraft.util.text.ITextComponent component) { return false; }
		@Override public List<Element> get(ResourceLocation tag) { return List.of(); }
		@Override public void setLineMargin(int index, ScreenDirection side, int margin) { }
		@Override public void setLineSettings(int index, java.util.function.UnaryOperator<Object> settings) { }
		@Override public String getNarration() { return ""; }
		@Override public String getString(ResourceLocation tag) { return ""; }
		@Override public Element getIcon() { return null; }
	}
}
