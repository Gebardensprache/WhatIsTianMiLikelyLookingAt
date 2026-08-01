package snownee.jade.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import org.jspecify.annotations.Nullable;

import zone.rong.mixinbooter.IEarlyMixinLoader;
import zone.rong.mixinbooter.ILateMixinLoader;
import zone.rong.mixinbooter.MixinLoader;

@IFMLLoadingPlugin.Name("SussyPatchesPlugin")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
@IFMLLoadingPlugin.TransformerExclusions("dev.tianmi.sussypatches.core.asm.")
public class JadeMixinLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

	@Override
	public List<String> getMixinConfigs() {
		return Collections.singletonList("mixins.jade.json");
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[0];
	}

	@Override
	public @Nullable String getModContainerClass() {
		return null;
	}

	@Override
	public @Nullable String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		/* Do nothing */
	}

	@Override
	public @Nullable String getAccessTransformerClass() {
		return null;
	}
}
