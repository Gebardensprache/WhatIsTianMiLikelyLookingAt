package snownee.jade.compat.top;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import mcjty.theoneprobe.api.IProbeInfo;

class CaptureProbeInfoTest {

	@Test
	void textThenHorizontalProducesNestedDtos() {
		CaptureProbeInfo info = new CaptureProbeInfo();
		info.text("a");
		IProbeInfo horiz = info.horizontal();
		horiz.text("b");

		assertThat(info.getElements()).hasSize(2);
		assertThat(info.getElements().get(0).type).isEqualTo(ElementDto.TEXT);
		assertThat(info.getElements().get(0).text).isEqualTo("a");
		assertThat(info.getElements().get(1).type).isEqualTo(ElementDto.HORIZONTAL);
		assertThat(info.getElements().get(1).children).hasSize(1);
		assertThat(info.getElements().get(1).children.get(0).text).isEqualTo("b");
	}

	@Test
	void progressCapturesValues() {
		CaptureProbeInfo info = new CaptureProbeInfo();
		info.progress(50, 100);
		assertThat(info.getElements()).hasSize(1);
		ElementDto dto = info.getElements().get(0);
		assertThat(dto.type).isEqualTo(ElementDto.PROGRESS);
		assertThat(dto.current).isEqualTo(50);
		assertThat(dto.max).isEqualTo(100);
	}
}
