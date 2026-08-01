package snownee.jade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.potion.PotionEffect;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import snownee.jade.util.JadeMobEffectInstance;

@Mixin(PotionEffect.class)
public abstract class MobEffectInstanceMixin implements JadeMobEffectInstance {
	@Unique
	private long jade$updateTime;
	@Unique
	private long jade$addTime;

	@Override
	public long jade$updateTime() {
		return jade$updateTime;
	}

	@Override
	public void jade$setUpdateTime(long time) {
		this.jade$updateTime = time;
	}

	@Override
	public long jade$addTime() {
		return jade$addTime;
	}

	@Override
	public void jade$setAddTime(long time) {
		this.jade$addTime = time;
	}

	// 1.12.2: PotionEffect.combine() is void (modern MobEffectInstance.update() returns boolean).
	// Instead of reading a boolean return, we unconditionally adopt the newer timestamp.
	@Inject(method = "combine", at = @At("TAIL"))
	private void jade$combine(PotionEffect other, CallbackInfo ci) {
		long thatTime = ((JadeMobEffectInstance) other).jade$updateTime();
		if (thatTime > jade$updateTime) {
			jade$updateTime = thatTime;
		}
	}
}
