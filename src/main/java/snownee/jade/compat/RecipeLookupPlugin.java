package snownee.jade.compat;

import org.jspecify.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public interface RecipeLookupPlugin {
	RecipeLookupResult lookup(ItemStack itemStack, @Nullable ResourceLocation specialId, boolean uses);
}
