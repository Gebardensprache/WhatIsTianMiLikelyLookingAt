package snownee.jade.gui.config.value;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.gui.config.OptionsList;

public abstract class OptionValue<T> extends OptionsList.Entry {

	private static final ITextComponent SERVER_FEATURE = new TextComponentString("* ")
			.setStyle(new Style().setColor(TextFormatting.GRAY)
					.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("gui.jade.server_feature"))));
	protected final Supplier<T> getter;
	protected final Consumer<T> setter;
	protected @Nullable ResourceLocation id;
	protected T value;
	protected int indent;
	private ITextComponent rawTitle;

	public OptionValue(String optionName, Supplier<T> getter, Consumer<T> setter) {
		super(makeTitle(optionName));
		this.getter = getter;
		this.setter = setter;
		rawTitle = title();
		addMessageKey(optionName);
		String key = makeKey(optionName + "_desc");
		if (JadeUI.hasTranslation(key)) {
			appendDescription(new TextComponentTranslation(key));
		}
	}

	@Override
	public void setDisabled(boolean disabled) {
		super.setDisabled(disabled);
		if (disabled) {
			setTitle(rawTitle.createCopy().setStyle(new Style().setColor(TextFormatting.GRAY)));
		} else {
			setTitle(rawTitle);
		}
	}

	public void save() {
		setter.accept(value);
	}

	public void appendDescription(ITextComponent description) {
		if (this.description.isEmpty()) {
			this.description = Lists.newArrayList(description);
		} else {
			this.description.add(description);
		}
		addMessage(description.getFormattedText());
	}

	@Override
	public int getTextX() {
		return indent + 10;
	}

	public boolean isValidValue() {
		return true;
	}

	@Override
	public OptionsList.Entry parent(OptionsList.Entry parent) {
		super.parent(parent);
		if (parent instanceof OptionValue) {
			indent = ((OptionValue<?>) parent).indent + 12;
		}
		return this;
	}

	public abstract void setValue(T value);

	public abstract void updateValue();

	public void setId(ResourceLocation id) {
		this.id = id;
	}

	public @Nullable ResourceLocation getId() {
		return id;
	}

	@Override
	public List<ITextComponent> getDescriptionOnShift() {
		if (id == null) {
			return List.of();
		}
		return List.of(new TextComponentString(id.toString()).setStyle(new Style().setColor(TextFormatting.GRAY)));
	}

	public void setServerFeature() {
		setTitle(title().createCopy().appendSibling(SERVER_FEATURE));
	}
}
