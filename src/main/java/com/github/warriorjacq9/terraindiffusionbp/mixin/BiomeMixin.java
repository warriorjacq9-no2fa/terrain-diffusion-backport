package com.github.warriorjacq9.terraindiffusionbp.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeMixin {

	@Shadow
	public abstract float getTemperature();

	@Shadow
	public abstract boolean hasPrecipitation();

	@Inject(method = "getPrecipitation", at = @At("HEAD"), cancellable = true)
	private void preventHighAltitudeSnow(CallbackInfoReturnable<Biome.RainType> cir) {
		if(!this.hasPrecipitation()) {
			cir.setReturnValue(Biome.RainType.NONE);
			return;
		}

		if(this.getTemperature() >= 0.15F) {
			cir.setReturnValue(Biome.RainType.RAIN);
		}
	}
}