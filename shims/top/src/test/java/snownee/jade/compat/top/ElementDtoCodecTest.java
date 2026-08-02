package snownee.jade.compat.top;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

class ElementDtoCodecTest {

	@BeforeAll
	static void bootstrap() {
		// 1.12.2: the test JVM never runs the game, so Items.<clinit> would throw
		// "Accessed Items before Bootstrap!" without an explicit bootstrap.
		Bootstrap.register();
	}

	@Test
	void roundTripText() {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.TEXT;
		dto.text = "hello";
		NBTTagCompound tag = dto.toNbt();
		ElementDto decoded = ElementDto.fromNbt(tag);
		assertThat(decoded.type).isEqualTo(ElementDto.TEXT);
		assertThat(decoded.text).isEqualTo("hello");
	}

	@Test
	void roundTripProgress() {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.PROGRESS;
		dto.current = 42;
		dto.max = 100;
		NBTTagCompound tag = dto.toNbt();
		ElementDto decoded = ElementDto.fromNbt(tag);
		assertThat(decoded.current).isEqualTo(42);
		assertThat(decoded.max).isEqualTo(100);
	}

	@Test
	void roundTripItemStack() {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.ITEM;
		dto.stack = new ItemStack(Items.DIAMOND, 3);
		NBTTagCompound tag = dto.toNbt();
		ElementDto decoded = ElementDto.fromNbt(tag);
		assertThat(decoded.stack.getItem()).isEqualTo(Items.DIAMOND);
		assertThat(decoded.stack.getCount()).isEqualTo(3);
	}

	@Test
	void roundTripItemStackLargeCount() {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.ITEM;
		dto.stack = new ItemStack(Items.DIAMOND, 192);
		NBTTagCompound tag = dto.toNbt();
		ElementDto decoded = ElementDto.fromNbt(tag);
		assertThat(decoded.stack.getItem()).isEqualTo(Items.DIAMOND);
		assertThat(decoded.stack.getCount()).isEqualTo(192);
		assertThat(decoded.stack.isEmpty()).isFalse();
	}

	@Test
	void roundTripNestedChildren() {
		ElementDto parent = new ElementDto();
		parent.type = ElementDto.VERTICAL;
		ElementDto child = new ElementDto();
		child.type = ElementDto.TEXT;
		child.text = "nested";
		parent.children.add(child);

		NBTTagCompound tag = parent.toNbt();
		ElementDto decoded = ElementDto.fromNbt(tag);
		assertThat(decoded.type).isEqualTo(ElementDto.VERTICAL);
		assertThat(decoded.children).hasSize(1);
		assertThat(decoded.children.get(0).text).isEqualTo("nested");
	}

	@Test
	void roundTripProgressWithStyle() {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.PROGRESS;
		dto.current = 42;
		dto.max = 100;
		dto.progressFilledColor = 0xFFEEE600;
		dto.progressAlternateColor = 0xFFEEE600;
		dto.progressBorderColor = 0xFF555555;
		dto.progressShowText = false;
		dto.progressPrefix = "x";
		dto.progressSuffix = " EU";

		NBTTagCompound tag = dto.toNbt();
		ElementDto decoded = ElementDto.fromNbt(tag);
		assertThat(decoded.current).isEqualTo(42);
		assertThat(decoded.max).isEqualTo(100);
		assertThat(decoded.progressFilledColor).isEqualTo(0xFFEEE600);
		assertThat(decoded.progressAlternateColor).isEqualTo(0xFFEEE600);
		assertThat(decoded.progressBorderColor).isEqualTo(0xFF555555);
		assertThat(decoded.progressShowText).isFalse();
		assertThat(decoded.progressPrefix).isEqualTo("x");
		assertThat(decoded.progressSuffix).isEqualTo(" EU");
	}

	@Test
	void roundTripProgressDefaultStyle() {
		ElementDto dto = new ElementDto();
		dto.type = ElementDto.PROGRESS;
		dto.current = 1;
		dto.max = 2;

		NBTTagCompound tag = dto.toNbt();
		ElementDto decoded = ElementDto.fromNbt(tag);
		assertThat(decoded.progressFilledColor).isEqualTo(-1);
		assertThat(decoded.progressShowText).isTrue();
		assertThat(decoded.progressPrefix).isEmpty();
		assertThat(decoded.progressSuffix).isEmpty();
	}
}
