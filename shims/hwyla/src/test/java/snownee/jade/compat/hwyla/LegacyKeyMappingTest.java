package snownee.jade.compat.hwyla;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;

class LegacyKeyMappingTest {

	@BeforeAll
	static void bootstrap() {
		Bootstrap.register();
	}

	@Test
	void mapsInventoryKeyToUniversalItemStorage() {
		ResourceLocation uid = LegacyKeyMapping.map("capability.inventoryinfo");
		assertThat(uid).isEqualTo(new ResourceLocation("item_storage"));
	}

	@Test
	void unknownKeyReturnsNull() {
		assertThat(LegacyKeyMapping.map("nonexistent.key")).isNull();
	}

	@Test
	void mapsFurnaceKey() {
		ResourceLocation uid = LegacyKeyMapping.map("capability.furnace");
		assertThat(uid).isEqualTo(new ResourceLocation("furnace"));
	}

	@Test
	void mapsBrewingKey() {
		ResourceLocation uid = LegacyKeyMapping.map("capability.brewing");
		assertThat(uid).isEqualTo(new ResourceLocation("brewing_stand"));
	}
}
