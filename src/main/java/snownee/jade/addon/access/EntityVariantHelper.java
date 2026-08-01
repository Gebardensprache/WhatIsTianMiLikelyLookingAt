package snownee.jade.addon.access;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.entity.Entity;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;

public final class EntityVariantHelper {
	/**
	 * 1.12.2: DataComponents do not exist. Variant types are opaque Object markers instead
	 * of DataComponentTypes: the "color" marker classifies a variant as a color (mapped to
	 * an entity's {@code getColor()} getter) and the "variant" marker maps to
	 * {@code getVariant()} / {@code getVariantName()}. The modern ends-with-"/color" id
	 * heuristic is preserved for addon markers that mimic the modern component-id naming.
	 */
	private static final String COLOR = "color";
	private static final String VARIANT = "variant";

	private static final Map<Object, Boolean> isVariantType = Maps.newHashMap();
	private static final Set<Object> isColorType = Sets.newHashSet();
	private static final Map<Class<? extends Entity>, @Nullable Object> variantTypeByEntity = Maps.newHashMap();
	private static final Map<Class<?>, @Nullable Object> variantTypeByClass = Maps.newHashMap();

	public static synchronized void addVariantMapping(Class<? extends Entity> entityType, @Nullable Object variantType) {
		variantTypeByEntity.put(entityType, variantType);
		if (variantType != null && !isVariantType.containsKey(variantType)) {
			addVariantType(variantType, true);
		}
	}

	public static synchronized void addVariantType(Object type, boolean isVariant) {
		String name = nameOf(type);
		if (name == null) {
			isVariant = false;
		}
		isVariantType.put(type, isVariant);
		boolean isColor = name != null && (name.endsWith("/color") || COLOR.equals(name));
		if (isColor) {
			isColorType.add(type);
		}
	}

	@Nullable
	public static synchronized Object getVariantType(Entity entity) {
		Class<? extends Entity> entityClass = entity.getClass();
		if (variantTypeByEntity.containsKey(entityClass)) {
			return variantTypeByEntity.get(entityClass);
		}
		// Modern scans the entity's implicit data components for a registered variant type;
		// 1.12.2 has no components, so entities without an explicit mapping are matched
		// against a color/variant getter instead (e.g. EntityShulker#getColor).
		Object type = findVariantType(entityClass);
		variantTypeByEntity.put(entityClass, type);
		return type;
	}

	@Nullable
	public static synchronized Object getVariant(Entity entity, boolean isColor) {
		Object type = getVariantType(entity);
		if (type == null || isColorType.contains(type) != isColor) {
			return null;
		}
		return getVariantValue(entity, type);
	}

	@Nullable
	public static synchronized String getVariantName(Entity entity, boolean isColor) {
		Object variant = getVariant(entity, isColor);
		if (variant == null) {
			return null;
		}
		// 1.12.2: the modern Either<String, Component> result collapses to a plain name
		// String -- the Component (right) side only ever came from the neoforge
		// TranslatableEnum translation, which has no 1.12.2 counterpart.
		return variantName(variant);
	}

	@Nullable
	private static Object getVariantValue(Entity entity, Object type) {
		String name = nameOf(type);
		if (name == null) {
			return null;
		}
		if (COLOR.equals(name) || name.endsWith("/color")) {
			return invokeNoArg(entity, "getColor");
		}
		Object value = invokeNoArg(entity, "getVariant");
		return value != null ? value : invokeNoArg(entity, "getVariantName");
	}

	@Nullable
	private static Object invokeNoArg(Entity entity, String methodName) {
		try {
			Method method = entity.getClass().getMethod(methodName);
			return method.invoke(entity);
		} catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
			return null;
		}
	}

	@Nullable
	private static String variantName(Object variant) {
		if (variant instanceof EnumDyeColor) {
			// 1.12.2 EnumDyeColor.SILVER corresponds to modern DyeColor.LIGHT_GRAY; the
			// language keys use the modern names ("jade.access.entity.light_gray").
			return variant == EnumDyeColor.SILVER ? "light_gray" : ((EnumDyeColor) variant).getName();
		}
		if (variant instanceof IStringSerializable) {
			return ((IStringSerializable) variant).getName();
		}
		if (variant instanceof Enum) {
			return ((Enum<?>) variant).name().toLowerCase(Locale.ENGLISH);
		}
		if (variant instanceof String) {
			return ((String) variant).toLowerCase(Locale.ENGLISH);
		}
		return null;
	}

	@Nullable
	private static String nameOf(Object type) {
		if (type instanceof String) {
			return (String) type;
		}
		if (type instanceof ResourceLocation) {
			return ((ResourceLocation) type).getPath();
		}
		if (type instanceof IStringSerializable) {
			return ((IStringSerializable) type).getName();
		}
		if (type instanceof Enum) {
			return ((Enum<?>) type).name().toLowerCase(Locale.ENGLISH);
		}
		return null;
	}

	@Nullable
	private static Object findVariantType(Class<?> entityClass) {
		if (variantTypeByClass.containsKey(entityClass)) {
			return variantTypeByClass.get(entityClass);
		}
		Object type = null;
		for (String methodName : new String[] {"getColor", "getVariant", "getVariantName"}) {
			try {
				Method method = entityClass.getMethod(methodName);
				Class<?> returnType = method.getReturnType();
				if (returnType.isEnum() || returnType == String.class || IStringSerializable.class.isAssignableFrom(returnType)) {
					type = "getColor".equals(methodName) ? COLOR : VARIANT;
					break;
				}
			} catch (NoSuchMethodException | SecurityException ignored) {
			}
		}
		variantTypeByClass.put(entityClass, type);
		return type;
	}
}
