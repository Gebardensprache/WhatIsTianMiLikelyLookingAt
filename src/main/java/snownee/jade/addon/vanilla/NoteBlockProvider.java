package snownee.jade.addon.vanilla;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.JadeUI;

public class NoteBlockProvider implements IBlockComponentProvider {
	public static final NoteBlockProvider INSTANCE = new NoteBlockProvider();

	private static final String[] PITCH = {"F♯/G♭", "G", "G♯/A♭", "A", "A♯/B♭", "B", "C", "C♯/D♭", "D", "D♯/E♭", "E", "F"};
	private static final TextFormatting[] OCTAVE = {TextFormatting.WHITE, TextFormatting.YELLOW, TextFormatting.GOLD};
	private static final TextFormatting[] OCTAVE_LIGHT = {TextFormatting.DARK_PURPLE, TextFormatting.DARK_BLUE, TextFormatting.BLUE};

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		// 1.12.2: notes are stored on TileEntityNote rather than a BlockNote state property.
		if (!(accessor.getBlockEntity() instanceof TileEntityNote)) {
			return;
		}
		int note = MathHelper.clamp(((TileEntityNote) accessor.getBlockEntity()).note, 0, 24);
		String instrument = getInstrument(accessor);
		String key = "jade.instrument." + instrument;
		String name = JadeUI.hasTranslation(key) ? I18n.format(key) : capitalize(instrument);
		String pitch = PITCH[note % PITCH.length];
		TextFormatting octave = (IThemeHelper.get().isLightColorScheme() ? OCTAVE_LIGHT : OCTAVE)[note / PITCH.length];
		tooltip.add(new TextComponentString(name + " " + octave + pitch));
	}

	private static String getInstrument(BlockAccessor accessor) {
		IBlockState state = accessor.getLevel().getBlockState(accessor.getPosition().down());
		Block block = state.getBlock();
		if (block == Blocks.CLAY) {
			return "flute";
		}
		if (block == Blocks.GOLD_BLOCK) {
			return "bell";
		}
		if (block == Blocks.WOOL) {
			return "guitar";
		}
		if (block == Blocks.PACKED_ICE) {
			return "chime";
		}
		if (block == Blocks.BONE_BLOCK) {
			return "xylophone";
		}

		Material material = state.getMaterial();
		if (material == Material.WOOD) {
			return "bass";
		}
		if (material == Material.ROCK) {
			return "basedrum";
		}
		if (material == Material.GLASS) {
			return "hat";
		}
		if (material == Material.SAND) {
			return "snare";
		}
		return "harp";
	}

	private static String capitalize(String value) {
		String[] words = value.replace('_', ' ').split(" ");
		StringBuilder builder = new StringBuilder(value.length());
		for (String word : words) {
			if (builder.length() > 0) {
				builder.append(' ');
			}
			builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
		}
		return builder.toString();
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_NOTE_BLOCK;
	}

}
