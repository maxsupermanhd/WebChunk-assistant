package net.maxsupermanhd.TerrainReporter.mixin;

import net.maxsupermanhd.TerrainReporter.TerrainReporter;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;

@Mixin(TitleScreen.class)
public class TerrainReporterMixin {
	@Inject(at = @At("HEAD"), method = "init()V")
	private void init(CallbackInfo info) {
		TerrainReporter.LOGGER.info("This line is printed by an example mod mixin!");
	}
}


