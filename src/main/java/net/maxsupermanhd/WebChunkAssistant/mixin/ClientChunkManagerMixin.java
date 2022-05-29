package net.maxsupermanhd.WebChunkAssistant.mixin;

import me.shedaniel.autoconfig.AutoConfig;
import net.maxsupermanhd.WebChunkAssistant.TerrainReporter;
import net.maxsupermanhd.WebChunkAssistant.ChunkAssistantConfig;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ClientChunkManager.class)
public class ClientChunkManagerMixin {
    @Inject(at = @At("TAIL"), method = "Lnet/minecraft/client/world/ClientChunkManager;loadChunkFromPacket(IILnet/minecraft/network/PacketByteBuf;Lnet/minecraft/nbt/NbtCompound;Ljava/util/function/Consumer;)Lnet/minecraft/world/chunk/WorldChunk;")
    private void loadChunkFromPacket(int x, int z, PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfoReturnable<WorldChunk> cir) throws InterruptedException {
        ChunkAssistantConfig config = AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig();
        if(config.enabled) {
            TerrainReporter.submitChunkTrigger(x, z);
        }
    }
}