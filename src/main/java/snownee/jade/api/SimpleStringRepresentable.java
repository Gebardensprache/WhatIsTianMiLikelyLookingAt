package snownee.jade.api;

/**
 * Convenience base for enums whose serialized name is their {@link #toString()} value.
 */
public interface SimpleStringRepresentable extends StringRepresentable {
	@Override
	default String getSerializedName() {
		return toString();
	}
}
