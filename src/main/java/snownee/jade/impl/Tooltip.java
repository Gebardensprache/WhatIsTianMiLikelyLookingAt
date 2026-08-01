package snownee.jade.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.ScreenDirection;
import snownee.jade.api.ui.TextElement;
import snownee.jade.impl.ui.JadeUIInternal;

public class Tooltip implements ITooltip {
	private static @Nullable ResourceLocation getTag(Element element) {
		return element.getTag();
	}

	public final List<Line> lines = new ArrayList<>();
	public boolean sneakyDetails;
	public @Nullable Element icon;
	public boolean isDirty;

	@Override
	public void clear() {
		lines.clear();
		isDirty = true;
	}

	@Override
	public int size() {
		return lines.size();
	}

	@Override
	public void append(int index, Element element) {
		if (element.getTag() == null) {
			element.tag(JadeUIInternal.contextUid());
		}
		if (isEmpty() || index == size()) {
			add(element);
		} else {
			Line line = lines.get(index);
			line.elements.add(element);
		}
		isDirty = true;
	}

	@Override
	public void add(int index, Element element) {
		lines.add(index, new Line());
		append(index, element);
	}

	@Override
	public List<Element> get(ResourceLocation tag) {
		List<Element> elements = Lists.newArrayList();
		for (Line line : lines) {
			line.elements().stream().filter(e -> Objects.equal(tag, getTag(e))).forEach(elements::add);
		}
		return elements;
	}

	@Override
	public boolean remove(ResourceLocation tag) {
		return removeInternal(tag, true, null);
	}

	private boolean removeInternal(ResourceLocation tag, boolean removeFirstLineIfEmpty, @Nullable List<List<Element>> collector) {
		boolean removed = false;
		List<Element> collected = collector == null ? null : Lists.newArrayList();
		for (Iterator<Line> iterator = lines.iterator(); iterator.hasNext(); ) {
			Line line = iterator.next();
			if (line.elements.removeIf(e -> {
				if (Objects.equal(tag, getTag(e))) {
					if (collector != null) {
						collected.add(e);
					}
					return true;
				}
				return false;
			})) {
				if (line.elements.isEmpty() && (removed || removeFirstLineIfEmpty)) {
					iterator.remove();
				}
				removed = true;
				if (collector != null && !collected.isEmpty()) {
					collector.add(Lists.newArrayList(collected));
					collected.clear();
				}
			}
		}
		return isDirty = removed;
	}

	@Override
	public boolean replace(ResourceLocation tag, ITextComponent component) {
		return isDirty = replace(tag, $ -> List.of(List.of(JadeUI.text(component))));
	}

	@Override
	public boolean replace(ResourceLocation tag, UnaryOperator<List<List<Element>>> operator) {
		int firstX = -1, firstY = -1;
		for (int y = 0; y < lines.size(); y++) {
			Line line = lines.get(y);
			for (int x = 0; x < line.elements().size(); x++) {
				Element element = line.elements().get(x);
				if (Objects.equal(tag, getTag(element))) {
					if (firstX == -1) {
						firstX = x;
						firstY = y;
					}
				}
			}
		}
		if (firstX != -1) {
			List<List<Element>> elements = Lists.newArrayList();
			removeInternal(tag, false, elements);
			elements = operator.apply(elements);
			for (List<Element> elementList : elements) {
				for (Element element : elementList) {
					if (element.getTag() == null) {
						element.tag(tag);
					}
				}
			}
			for (int i = 0; i < elements.size(); i++) {
				List<Element> list = elements.get(i);
				if (i == 0) {
					Line line = lines.get(firstY);
					line.elements().addAll(firstX, list);
				} else {
					add(firstY + i, list);
				}
			}
		}
		return firstX != -1;
	}

	@Override
	public void setLineMargin(int index, ScreenDirection side, int margin) {
		if (index < 0) {
			index += lines.size();
		}
		Line line = lines.get(index);
		switch (side) {
			case UP -> line.marginTop = margin;
			case DOWN -> line.marginBottom = margin;
			default -> throw new IllegalArgumentException("Only UP and DOWN are allowed.");
		}
		isDirty = true;
	}

	@Override
	public void setLineSettings(int index, UnaryOperator<Object> settings) {
		if (index < 0) {
			index += lines.size();
		}
		Line line = lines.get(index);
		line.settings = settings;
		isDirty = true;
	}

	@Override
	public String getNarration() {
		return getNarration(line -> true);
	}

	public String getNarration(Predicate<Line> predicate) {
		// 1.12.2 has no narration system; return plain text representation
		StringBuilder sb = new StringBuilder();
		for (Line line : lines) {
			if (!predicate.test(line)) {
				continue;
			}
			for (Element element : line.elements()) {
				if (element instanceof TextElement textElement) {
					sb.append(textElement.getString());
				}
			}
			if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n') {
				sb.append('\n');
			}
		}
		if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	@Override
	public String getString(ResourceLocation tag) {
		return get(tag).stream().filter($ -> $ instanceof TextElement).map($ -> ((TextElement) $).getString()).findFirst().orElse("");
	}

	@Override
	public @Nullable Element getIcon() {
		return icon;
	}

	public void setIcon(@Nullable Element icon) {
		this.icon = icon;
		isDirty = true;
	}

	public static class Line {
		private final List<Element> elements = Lists.newArrayList();
		public int marginTop = 0;
		public int marginBottom = 2;
		public @Nullable UnaryOperator<Object> settings;

		public List<Element> elements() {
			return elements;
		}
	}

}
