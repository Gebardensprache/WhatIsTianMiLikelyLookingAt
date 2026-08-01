package snownee.jade.util;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.DynamicOps;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

public class ComponentHolders {
	public static String serialize(ResourceLocation holder, @Nullable NBTTagCompound components) {
		StringBuilder stringBuilder = new StringBuilder(holder.toString());
		String string = serializeComponents(components);
		if (!string.isEmpty()) {
			stringBuilder.append('[');
			stringBuilder.append(string);
			stringBuilder.append(']');
		}

		return stringBuilder.toString();
	}

	private static String serializeComponents(@Nullable NBTTagCompound components) {
		if (components == null || components.isEmpty()) {
			return "";
		}
		return components.getKeySet().stream().flatMap((key) -> {
			NBTBase tag = components.getTag(key);
			if (tag instanceof NBTTagString) {
				return Stream.of(key + "=" + ((NBTTagString) tag).getString());
			} else {
				return Stream.of(key + "=" + tag);
			}
		}).collect(Collectors.joining(String.valueOf(',')));
	}
}
