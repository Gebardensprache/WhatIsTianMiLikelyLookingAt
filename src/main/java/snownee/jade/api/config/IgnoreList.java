package snownee.jade.api.config;

import java.util.Arrays;
import java.util.List;

/**
 * Serialized list of ignored entries used by Jade's config screens.
 */
public class IgnoreList {
	/**
	 * Ignored entry identifiers or names.
	 */
	public List<String> values = Arrays.asList();
	/**
	 * Serialized data version.
	 */
	public int version = 1;
}
