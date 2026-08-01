package snownee.jade.compat;

import java.util.List;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiScreen;

public record RecipeLookupResult(String source, float score, BiConsumer<@Nullable GuiScreen, List<RecipeLookupResult>> action) {
	public static final RecipeLookupResult FAIL = new RecipeLookupResult("n/a", 0, (s, results) -> {});

	public boolean isFail() {
		return score <= 0;
	}
}
