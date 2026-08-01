package snownee.jade.api.ui;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import com.google.common.base.Preconditions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.ResourceLocation;
import snownee.jade.JadeInternals;
import snownee.jade.gui.JadeLinearLayout;
import snownee.jade.impl.ui.JadeUIInternal;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.overlay.OverlayRenderer;

/**
 * Base class for renderable Jade UI elements.
 * <p>
 * 1.12.2: stands in for the modern class implementing {@code Renderable}, {@code LayoutElement},
 * {@code NarrationSupplier}, {@code GuiEventListener} and {@code CopyBehavior}. Only {@code Renderable}/
 * {@code LayoutElement} (Jade's own stand-ins) and {@code CopyBehavior} are kept: 1.12.2 has no
 * {@code AbstractWidget}/{@code GuiEventListener} hierarchy to satisfy narration or focus/mouse-over plumbing.
 */
public abstract class Element implements Renderable, LayoutElement, CopyBehavior {

	protected @Nullable ResourceLocation tag;
	protected int width;
	protected int height;
	private int x;
	private int y;
	private static final ITextComponent EMPTY_NARRATION = new TextComponentString("");
	private @Nullable ITextComponent narration = EMPTY_NARRATION;
	private @Nullable UnaryOperator<Object> settings;
	private JadeLinearLayout.@Nullable Align alignSelf;

	@Contract("_, _ -> new")
	public ResizeableElement offset(int x, int y) {
		return JadeUIInternal.offset(this, x, y);
	}

	@Contract("_, _ -> new")
	public ResizeableElement size(int width, int height) {
		return JadeUIInternal.size(this, width, height);
	}

	@Contract("_ -> new")
	public ResizeableElement onClick(Predicate<Element> onClick) {
		return JadeUIInternal.onClick(this, onClick);
	}

	@Contract("_ -> this")
	public Element settings(UnaryOperator<Object> settings) {
		this.settings = settings;
		return this;
	}

	public @Nullable UnaryOperator<Object> getSettings() {
		return settings;
	}

	@Contract("-> this")
	public Element alignSelfStart() {
		alignSelf = JadeLinearLayout.Align.START;
		return this;
	}

	@Contract("-> this")
	public Element alignSelfCenter() {
		alignSelf = JadeLinearLayout.Align.CENTER;
		return this;
	}

	@Contract("-> this")
	public Element alignSelfEnd() {
		alignSelf = JadeLinearLayout.Align.END;
		return this;
	}

	@Contract("-> this")
	public Element alignSelfStretch() {
		alignSelf = JadeLinearLayout.Align.STRETCH;
		return this;
	}

	public JadeLinearLayout.@Nullable Align getAlignSelf() {
		return alignSelf;
	}

	@Contract("_ -> this")
	public Element tag(@Nullable ResourceLocation tag) {
		this.tag = tag;
		return this;
	}

	public @Nullable ResourceLocation getTag() {
		return tag;
	}

	public @Nullable ITextComponent cachedNarration() {
		if (narration == EMPTY_NARRATION) {
			narration = getNarration();
		}
		return narration;
	}

	public abstract @Nullable ITextComponent getNarration();

	@Contract("-> this")
	public Element refreshNarration() {
		narration = EMPTY_NARRATION;
		return this;
	}

	@Contract("_ -> this")
	public Element narration(String narration) {
		Preconditions.checkNotNull(narration, "narration must not be null");
		this.narration = narration.isEmpty() ? null : new TextComponentString(narration);
		return this;
	}

	@Contract("_ -> this")
	public Element narration(ITextComponent narration) {
		Preconditions.checkNotNull(narration, "narration must not be null");
		this.narration = narration;
		return this;
	}

	@Override
	public abstract void extractRenderState(int mouseX, int mouseY, float partialTicks);

	@Override
	public void setX(int x) {
		this.x = x;
	}

	@Override
	public void setY(int y) {
		this.y = y;
	}

	@Override
	public final int getX() {
		return x;
	}

	@Override
	public final int getY() {
		return y;
	}

	@Override
	public final int getWidth() {
		return width;
	}

	@Override
	public final int getHeight() {
		return height;
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight();
	}

	@Override
	public boolean copyToClipboard() {
		ITextComponent component = cachedNarration();
		if (component != null) {
			GuiScreen.setClipboardString(component.getUnformattedText());
			return true;
		}
		return false;
	}

	/**
	 * 1.12.2: {@code GuiGraphicsExtractor} parameter dropped. Pose transforms use {@code GlStateManager}
	 * instead of the deleted {@code Matrix3x2fStack}; the font renderer comes from {@link DisplayHelper#font()}.
	 */
	public void renderDebug(int mouseX, int mouseY, float partialTicks, RenderDebugContext context) {
		JadeInternals.getDisplayHelper().drawBorder(getRectangle(), 1, 0x88FF0000, true);
		if (JadeUI.hasAltDown() && getTag() != null) {
			int centerX = context.root.getX() + context.root.getWidth() / 2;
			int x = getX();
			int y = getY();
			String s = getTag().toString();
			int textWidth = DisplayHelper.font().width(s);
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, 0.0F);
			GlStateManager.scale(0.5F, 0.5F, 1.0F);
			if (x > centerX) {
				GlStateManager.translate(getWidth() + getWidth(), 0, 0.0F);
			} else {
				GlStateManager.translate(-textWidth - 4, 0, 0.0F);
			}
			Gui.drawRect(0, 0, textWidth + 4, DisplayHelper.font().lineHeight() + 4, 0x88000000);
			Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(s, 2, 2, 0xFFFFFFFF);
			GlStateManager.popMatrix();
		}
	}

	/**
	 * 1.12.2: {@code GuiGraphicsExtractor} parameter dropped. Writes directly to
	 * {@link OverlayRenderer#hoveredTextStyle} instead of a graphics-context field.
	 */
	public static void setHoverEffect(ITextComponent component) {
		HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, component);
		OverlayRenderer.hoveredTextStyle = new Style().setHoverEvent(event);
	}

	public static void setHoverEffect(HoverEvent event) {
		OverlayRenderer.hoveredTextStyle = new Style().setHoverEvent(event);
	}

	public static class RenderDebugContext {
		public final LayoutElement root;
		public final Rect2f rootRect;
		public final boolean renderChildren;

		public RenderDebugContext(LayoutElement root, Rect2f rootRect, boolean renderChildren) {
			this.root = root;
			this.rootRect = rootRect;
			this.renderChildren = renderChildren;
		}
	}
}