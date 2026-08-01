package snownee.jade.compat;

import org.jspecify.annotations.Nullable;

import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IRecipesGui;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.JadeIds;

/**
 * 1.12.2 port of modern {@code JEICompat}: exposes Jade's recipe lookup through
 * JEI 4.x. Modern's JEI API ({@code IRecipeRegistration}, {@code IJeiHelpers},
 * {@code IFocusFactory}, {@code RecipeIngredientRole}) does not exist here; the
 * equivalent is {@code IJeiRuntime.getRecipeRegistry().createFocus(IFocus.Mode, V)}
 * plus {@code IRecipesGui.show(IFocus)}, registered with the legacy
 * {@code @JEIPlugin} annotation (no {@code getPluginUid()}).
 */
@JEIPlugin
public class JeiCompat implements IModPlugin, RecipeLookupPlugin {

	public static final ResourceLocation ID = JadeIds.JADE("main");
	private static @Nullable IJeiRuntime runtime;

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		JeiCompat.runtime = jeiRuntime;
	}

	@Override
	public RecipeLookupResult lookup(ItemStack itemStack, @Nullable ResourceLocation specialId, boolean uses) {
		return new RecipeLookupResult(
				"jei", 0.9f, (_, _) -> {
					IJeiRuntime r = runtime;
					if (r == null) {
						return;
					}
					IRecipesGui gui = r.getRecipesGui();
					gui.show(r.getRecipeRegistry().createFocus(
							uses ? IFocus.Mode.INPUT : IFocus.Mode.OUTPUT,
							itemStack));
				});
	}
}
