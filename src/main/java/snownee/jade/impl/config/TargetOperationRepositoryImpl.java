package snownee.jade.impl.config;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.util.ResourceLocation;
import snownee.jade.Jade;
import snownee.jade.api.config.IgnoreList;
import snownee.jade.api.config.TargetOperationRepository;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.JadeCodecs;
import snownee.jade.util.JsonConfig;

public class TargetOperationRepositoryImpl<T, U> implements TargetOperationRepository<T, U> {
	private final Function<U, ResourceLocation> mapper;
	private final String fileName;
	private final Supplier<List<String>> defaultValues;
	private final Map<ResourceLocation, TargetOperation> builtIn = Maps.newIdentityHashMap();
	private final Map<ResourceLocation, TargetOperation> operations = Maps.newIdentityHashMap();

	public TargetOperationRepositoryImpl(
			Function<U, ResourceLocation> mapper,
			String fileName,
			Supplier<List<String>> defaultValues) {
		this.mapper = mapper;
		this.fileName = fileName;
		this.defaultValues = defaultValues;
	}

	@Override
	public void reload() {
		operations.clear();
		operations.putAll(builtIn);

		IgnoreList list = new JsonConfig<>(
				Jade.ID + "/" + fileName,
				JadeCodecs.ignoreList(),
				null,
				() -> {
					IgnoreList l = new IgnoreList();
					l.values = defaultValues.get();
					return l;
				}).get();
		List<Pattern> patterns = Lists.newArrayList();
		// Collect entries that are patterns vs. concrete resource locations
		for (String value : list.values) {
			try {
				if (value.startsWith("/") && value.endsWith("/") && value.length() > 1) {
					patterns.add(Pattern.compile(value.substring(1, value.length() - 1)));
				} else {
					ResourceLocation key = new ResourceLocation(value);
					operations.put(key, TargetOperation.HIDE);
				}
			} catch (Exception e) {
				Jade.LOGGER.error("Failed to parse ignore list entry: %s".formatted(value), e);
			}
		}
		// Pattern matching: apply regex against the plain-text values list
		if (!patterns.isEmpty()) {
			for (String value : list.values) {
				if (value.startsWith("/") && value.endsWith("/") && value.length() > 1) {
					continue;
				}
				try {
					ResourceLocation key = new ResourceLocation(value);
					for (Pattern pattern : patterns) {
						if (pattern.matcher(value).find()) {
							operations.put(key, TargetOperation.HIDE);
							break;
						}
					}
				} catch (Exception ignored) {
				}
			}
		}
	}

	@Override
	public boolean shouldHide(ResourceLocation key) {
		return operations.get(key) == TargetOperation.HIDE;
	}

	@Override
	public boolean shouldPick(ResourceLocation key) {
		return operations.get(key) == TargetOperation.PICK;
	}

	@Override
	public void hide(ResourceLocation key) {
		builtIn.put(Objects.requireNonNull(key), TargetOperation.HIDE);
	}

	@Override
	public void pick(ResourceLocation key) {
		if (!CommonProxy.isPhysicallyClient()) {
			return;
		}
		builtIn.put(Objects.requireNonNull(key), TargetOperation.PICK);
	}

	@Override
	public ResourceLocation map(U obj) {
		return mapper.apply(obj);
	}
}
