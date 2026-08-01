package snownee.jade.addon.vanilla;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.DataCodec;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.util.JadeMobEffectInstance;

public class StatusEffectsProvider implements StreamServerDataProvider<EntityAccessor, List<StatusEffectsProvider.Effect>> {
	public static final StatusEffectsProvider INSTANCE = new StatusEffectsProvider();

	private static final DataCodec<List<Effect>> STREAM_CODEC = new DataCodec<>() {
		@Override
		public List<Effect> decode(PacketBuffer buf) {
			NBTTagCompound tag = DataCodec.readTag(buf);
			int count = tag.getInteger("Count");
			List<Effect> effects = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(tag.getCompoundTag("Effect" + i));
				long updateTime = tag.getLong("UpdateTime" + i);
				long addTime = tag.getLong("AddTime" + i);
				effects.add(new Effect(effect, updateTime, addTime));
			}
			return effects;
		}

		@Override
		public void encode(PacketBuffer buf, List<Effect> value) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("Count", value.size());
			for (int i = 0; i < value.size(); i++) {
				Effect data = value.get(i);
				tag.setTag("Effect" + i, data.effect().writeCustomPotionEffectToNBT(new NBTTagCompound()));
				tag.setLong("UpdateTime" + i, data.updateTime());
				tag.setLong("AddTime" + i, data.addTime());
			}
			buf.writeCompoundTag(tag);
		}
	};

	@Override
	public boolean shouldRequestData(EntityAccessor accessor) {
		return accessor.getEntity() instanceof EntityLivingBase;
	}

	@Override
	public @Nullable List<Effect> streamData(EntityAccessor accessor) {
		List<Effect> effects = new ArrayList<>();
		for (PotionEffect effect : ((EntityLivingBase) accessor.getEntity()).getActivePotionEffects()) {
			if (effect.doesShowParticles() && !WailaCommonRegistration.instance().mobEffectOperations().shouldHide(effect)) {
				effects.add(new Effect(effect));
			}
		}
		return effects.isEmpty() ? null : effects;
	}

	@Override
	public DataCodec<List<Effect>> streamCodec() {
		return STREAM_CODEC;
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_POTION_EFFECTS;
	}

	public static class Client implements IEntityComponentProvider {
		public static final Client INSTANCE = new Client();
		// 1.12.2: vanilla has no effect.duration.infinite translation key.
		public static final ITextComponent INFINITE = new TextComponentTranslation("jade.potion.infinite");

		public static ITextComponent getEffectName(PotionEffect effect) {
			TextComponentTranslation name = new TextComponentTranslation(effect.getEffectName());
			if (effect.getAmplifier() >= 1) {
				String levelKey = "enchantment.level." + (effect.getAmplifier() + 1);
				ITextComponent level = I18n.hasKey(levelKey) ?
						new TextComponentTranslation(levelKey) :
						new TextComponentString(Integer.toString(effect.getAmplifier() + 1));
				name.appendSibling(new TextComponentString(" "));
				name.appendSibling(level);
			}
			return name;
		}

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			List<Effect> effects = StatusEffectsProvider.INSTANCE.decodeFromData(accessor).orElse(new ArrayList<>());
			if (effects.isEmpty()) {
				return;
			}
			ITooltip box = JadeUI.tooltip();
			IThemeHelper t = IThemeHelper.get();
			long current = System.currentTimeMillis();
			effects.removeIf(data -> WailaCommonRegistration.instance().mobEffectOperations().shouldHide(data.effect()) ||
					current - data.addTime() - 20 <= 0);
			effects.sort(null);
			int limit = Math.min(effects.size(), config.getInt(JadeIds.MC_POTION_EFFECTS_LIMIT));
			effects = effects.subList(0, limit);
			float scale = effects.size() > 2 ? 0.75F : 1F;
			boolean animation = IWailaConfig.get().overlay().getAnimation();
			for (Effect data : effects) {
				long ms = current - data.addTime() - 20;
				float alpha = 1F;
				if (animation && ms < 480) {
					alpha = ms / 480F;
				}
				PotionEffect effect = data.effect();
				ITextComponent name = getEffectName(effect);
				// 1.12.2: durations at 32767+ ticks are treated as infinite.
				String duration = effect.getDuration() >= 32767 ?
						INFINITE.getFormattedText() : Potion.getPotionDurationString(effect, 1.0F);
				ITextComponent text = new TextComponentTranslation("jade.potion", name, duration);
				// 1.12.2: only bad/beneficial potion classification exists.
				text = effect.getPotion().isBadEffect() ? t.danger(text) : t.success(text);
				box.add(JadeUI.text(text).scale(scale).alpha(alpha));
			}
			tooltip.add(JadeUI.box(box, BoxStyle.nestedBox()).flexGrow(1));
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_POTION_EFFECTS;
		}
	}

	public record Effect(PotionEffect effect, long updateTime, long addTime) implements Comparable<Effect> {
		public Effect(PotionEffect effect) {
			// 1.12.2: timestamp state is supplied by the retargeted PotionEffect mixin.
			this(effect, ((JadeMobEffectInstance) effect).jade$updateTime(), ((JadeMobEffectInstance) effect).jade$addTime());
		}

		@Override
		public int compareTo(Effect other) {
			int compared = Long.compare(updateTime(), other.updateTime());
			if (compared != 0) {
				return -compared;
			}
			return effect().compareTo(other.effect());
		}
	}
}
