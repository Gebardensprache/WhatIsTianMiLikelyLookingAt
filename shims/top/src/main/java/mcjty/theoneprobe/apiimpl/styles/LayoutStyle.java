package mcjty.theoneprobe.apiimpl.styles;

import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.ILayoutStyle;

/**
 * 1.12.2 backport: verbatim from The One Probe (mcjty/theoneprobe).
 */
public class LayoutStyle implements ILayoutStyle {
    private Integer borderColor = null;
    private ElementAlignment alignment = ElementAlignment.ALIGN_TOPLEFT;
    private int spacing = -1;

    @Override
    public ILayoutStyle alignment(ElementAlignment alignment) {
        this.alignment = alignment;
        return this;
    }

    @Override
    public ElementAlignment getAlignment() {
        return alignment;
    }

    @Override
    public LayoutStyle borderColor(Integer c) {
        borderColor = c;
        return this;
    }

    @Override
    public LayoutStyle spacing(int f) {
        spacing = f;
        return this;
    }

    @Override
    public Integer getBorderColor() {
        return borderColor;
    }

    @Override
    public int getSpacing() {
        return spacing;
    }
}
