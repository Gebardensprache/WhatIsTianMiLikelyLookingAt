package snownee.jade.mixin.gregtech;

import gregtech.common.metatileentities.multi.multiblockpart.appeng.MetaTileEntityAEHostablePart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MetaTileEntityAEHostablePart.class, remap = false)
public interface AccessorMTEAEHostablePart {

	@Accessor("isOnline")
	boolean isOnline();

}
