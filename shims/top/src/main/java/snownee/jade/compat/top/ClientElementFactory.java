package snownee.jade.compat.top;

import java.util.HashSet;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mcjty.theoneprobe.api.IElement;
import mcjty.theoneprobe.api.IElementFactory;
import mcjty.theoneprobe.api.NumberFormat;
import mcjty.theoneprobe.apiimpl.client.ElementTextRender;
import mcjty.theoneprobe.apiimpl.elements.ElementProgress;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;
import snownee.jade.shim.top.TopShim;

/**
 * Converts {@link ElementDto} trees back into Jade {@link Element}s on the client.
 * <p>
 * 1.12.2 backport: uses the shim's own logger instead of {@code snownee.jade.Jade.LOGGER}.
 */
public final class ClientElementFactory {

	private static final Logger LOGGER = LogManager.getLogger(TopShim.MODID);

	private static final Set<Integer> loggedUnknown = new HashSet<>();

	private ClientElementFactory() {
	}

	public static Element fromDto(ElementDto dto) {
		switch (dto.type) {
			case ElementDto.TEXT:
				// TOP wraps client-side localization keys in {*key*} (STARTLOC/ENDLOC) and text
				// formatting in {=code=} (TextStyleClass). Resolve both like TOP's own
				// ElementTextRender does, so keys become translated text and formatting markers
				// become legacy color codes instead of leaking verbatim into the tooltip.
				String text = ElementTextRender.stylifyString(dto.text);
				return JadeUI.text((net.minecraft.util.text.ITextComponent) new TextComponentString(text));
			case ElementDto.ITEM:
				return JadeUI.item(dto.stack, 1.0F);
			case ElementDto.PROGRESS:
				return progress(dto);
			case ElementDto.ICON:
				logOnce(dto.type, "ICON elements are not rendered by Jade");
				return null;
			case ElementDto.CUSTOM:
				return custom(dto);
			case ElementDto.HORIZONTAL:
			case ElementDto.VERTICAL:
				return layout(dto);
			default:
				logOnce(dto.type, "Unknown TOP element type " + dto.type);
				return null;
		}
	}

	/**
	 * Builds a Jade progress element for a TOP progress bar.
	 * <p>
	 * The TOP {@code IProgressStyle} is forwarded so that custom colors (e.g. GTCEu's
	 * {@code filledColor(0xFFEEE600)} EU bar, {@code 0xFF4CBB17} workable bar) render through
	 * Jade's progress sprite. A default TOP style (no explicit color overrides) renders with
	 * Jade's default white progress sprite, matching Jade's own fluid/energy storage bars.
	 */
	private static Element progress(ElementDto dto) {
		float fraction = dto.max > 0 ? Math.max(0F, Math.min(1F, (float) dto.current / dto.max)) : 0F;
		int color = -1;
		// A TOP style with explicit filledColor colors the whole bar; only honor it when the
		// caller actually chose a color (the TOP default is 0xFFAAAAAA gray, indistinguishable
		// from "no choice" by itself, so require it to differ from the TOP default).
		int filled = dto.progressFilledColor;
		if (filled != -1 && filled != 0xFFAAAAAA) {
			color = filled;
		}
		ProgressView.Part part;
		if (color != -1) {
			part = ProgressView.Part.of(fraction, color);
		} else {
			part = ProgressView.Part.of(fraction);
		}
		ITextComponent text = null;
		if (dto.progressShowText) {
			String label = dto.progressPrefix + ElementProgress.format(
					dto.current,
					NumberFormat.values()[dto.progressNumberFormat],
					dto.progressSuffix);
			if (!label.isEmpty()) {
				text = new TextComponentString(ElementTextRender.stylifyString(label));
			}
		}
		return JadeUI.progress(new ProgressView(part, text, JadeUI.progressStyle(), BoxStyle.nestedBox()));
	}

	/**
	 * Rebuilds a third-party custom element from the registered factory and wraps it in a Jade
	 * {@link Element} that forwards rendering to the TOP element.
	 */
	private static Element custom(ElementDto dto) {
		IElementFactory factory = TheOneProbeImpl.INSTANCE.getStore().getElementFactory(dto.factoryId);
		if (factory == null) {
			logOnce(-dto.factoryId, "No TOP element factory registered for id " + dto.factoryId);
			return null;
		}
		ByteBuf buf = Unpooled.wrappedBuffer(dto.data);
		IElement element;
		try {
			element = factory.createElement(buf);
		} catch (Exception e) {
			LOGGER.error("TOP element factory {} threw creating element", dto.factoryId, e);
			return null;
		} finally {
			buf.release();
		}
		if (element == null) {
			return null;
		}
		return new TopElementElement(element);
	}

	private static Element layout(ElementDto dto) {
		ITooltip tooltip = JadeUI.tooltip();
		boolean first = true;
		for (ElementDto childDto : dto.children) {
			Element child = fromDto(childDto);
			if (child == null) {
				continue;
			}
			if (dto.type == ElementDto.HORIZONTAL && !first) {
				tooltip.append(child);
			} else {
				tooltip.add(child);
			}
			first = false;
		}
		return JadeUI.box(tooltip, BoxStyle.nestedBox());
	}

	private static void logOnce(int type, String message) {
		if (loggedUnknown.add(type)) {
			LOGGER.warn("TOP compat: {}", message);
		}
	}
}
