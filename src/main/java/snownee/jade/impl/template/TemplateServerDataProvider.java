package snownee.jade.impl.template;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.Accessor;
import snownee.jade.api.IServerDataProvider;

/**
 * A template implementation for script languages like KubeJS
 */
public class TemplateServerDataProvider<T extends Accessor<?>> implements IServerDataProvider<T> {
	private final ResourceLocation uid;
	private BiConsumer<NBTTagCompound, T> dataFunction = (data, accessor) -> {};
	private Predicate<T> shouldRequestData = accessor -> true;

	protected TemplateServerDataProvider(ResourceLocation uid) {
		this.uid = uid;
	}

	@Override
	public ResourceLocation getUid() {
		return uid;
	}

	@Override
	public void appendServerData(NBTTagCompound data, T accessor) {
		dataFunction.accept(data, accessor);
	}

	@Override
	public boolean shouldRequestData(T accessor) {
		return shouldRequestData.test(accessor);
	}

	public void setDataFunction(BiConsumer<NBTTagCompound, T> dataFunction) {
		this.dataFunction = dataFunction;
	}

	public void setShouldRequestData(Predicate<T> shouldRequestData) {
		this.shouldRequestData = shouldRequestData;
	}
}
