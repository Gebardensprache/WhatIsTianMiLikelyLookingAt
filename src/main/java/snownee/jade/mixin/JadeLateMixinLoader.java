package snownee.jade.mixin;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.List;

public class JadeLateMixinLoader implements ILateMixinLoader {
	@Override
	public List<String> getMixinConfigs() {
		var list = new ArrayList<String>();
		list.add("mixins.jade.gregtech.json");
		return list;
	}

	@Override
	public boolean shouldMixinConfigQueue(String mixinConfig) {
		String id = mixinConfig.split("\\.")[2];
		return Loader.isModLoaded(id);
	}
}
