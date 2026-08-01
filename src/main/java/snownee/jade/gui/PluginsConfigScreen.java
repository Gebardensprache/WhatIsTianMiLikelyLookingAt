package snownee.jade.gui;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.gui.config.OptionsList;
import snownee.jade.gui.config.value.OptionValue;
import snownee.jade.impl.WailaClientRegistration;

public class PluginsConfigScreen extends PreviewOptionsScreen {

	@Nullable
	private Function<OptionsList, OptionsList.@Nullable Entry> jumpTo;

	public PluginsConfigScreen(@Nullable GuiScreen parent) {
		super(parent, new TextComponentTranslation("gui.jade.plugin_settings"));
		saver = IWailaConfig.get()::save;
		canceller = IWailaConfig.get()::invalidate;
	}

	public static GuiScreen createPluginConfigScreen(
			@Nullable GuiScreen parent,
			@Nullable Function<OptionsList, OptionsList.Entry> jumpTo,
			boolean dontSave) {
		PluginsConfigScreen screen = new PluginsConfigScreen(parent);
		screen.jumpTo = jumpTo;
		return screen;
	}

	@Override
	public OptionsList createOptions(OptionsList options) {
		boolean noteServerFeature = Objects.requireNonNull(mc).world == null || IWailaConfig.get().general().isDebug() ||
				!WailaClientRegistration.instance().isServerConnected();
		BiConsumer<ResourceLocation, Object> setter = (key, value) -> {
			IWailaConfig.get().plugin().set(key, value);
			options.updateOptionValue(key);
		};
		WailaClientRegistration.instance().getConfigListView(IWailaConfig.get().accessibility().getEnableAccessibilityPlugin()).forEach(
				category -> {
					options.add(new OptionsList.Title(category.title()));
					MutableObject<OptionValue<?>> lastPrimary = new MutableObject<>();
					category.entries().forEach(entry -> {
						OptionValue<?> option = entry.createUI(
								options,
								"plugin_" + entry.id().getNamespace() + "." + entry.id().getPath(),
								IWailaConfig.get().plugin(),
								setter);
						option.setId(entry.id());
						if (entry.isSynced()) {
							option.setDisabled(true);
							option.appendDescription(new TextComponentTranslation("gui.jade.forced_plugin_config")
									.setStyle(new Style().setColor(TextFormatting.DARK_RED)));
						} else if (noteServerFeature && !WailaClientRegistration.instance().isClientFeature(entry.id())) {
							option.setServerFeature();
						}
						if (!IPluginConfig.isPrimaryKey(entry.id())) {
							OptionValue<?> last = lastPrimary.getValue();
							if (last != null) {
								option.parent(last);
							}
						} else {
							lastPrimary.setValue(option);
						}
					});
				});
		return options;
	}

	@Override
	public void initGui() {
		super.initGui();
		if (jumpTo != null) {
			OptionsList.Entry entry = jumpTo.apply(options());
			if (entry != null) {
				options().showOnTop(entry);
			}
			jumpTo = null;
		}
	}
}
