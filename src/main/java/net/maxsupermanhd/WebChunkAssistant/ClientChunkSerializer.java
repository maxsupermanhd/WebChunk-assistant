package net.maxsupermanhd.WebChunkAssistant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.chunk.BlendingData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ClientChunkSerializer extends ChunkSerializer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Codec<PalettedContainer<BlockState>> CODEC;

    public static NbtCompound serializeClientChunk(ClientWorld world, int x, int z) {
        Chunk chunk = world.getChunk(x, z);
        NbtCompound nbtCompound3;
        UpgradeData upgradeData;
        BelowZeroRetrogen belowZeroRetrogen;
        ChunkPos chunkPos = chunk.getPos();
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putInt("DataVersion", SharedConstants.getGameVersion().getSaveVersion().getId());
        nbtCompound.putInt("xPos", chunkPos.x);
        nbtCompound.putInt("yPos", chunk.getBottomSectionCoord());
        nbtCompound.putInt("zPos", chunkPos.z);
        nbtCompound.putLong("LastUpdate", world.getTime());
        nbtCompound.putLong("InhabitedTime", chunk.getInhabitedTime());
        nbtCompound.putString("Status", "ripped");
        BlendingData blendingData = chunk.getBlendingData();
        if (blendingData != null) {
            BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, blendingData).resultOrPartial(LOGGER::error).ifPresent(nbtElement -> nbtCompound.put("blending_data", (NbtElement)nbtElement));
        }
        if ((belowZeroRetrogen = chunk.getBelowZeroRetrogen()) != null) {
            BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowZeroRetrogen).resultOrPartial(LOGGER::error).ifPresent(nbtElement -> nbtCompound.put("below_zero_retrogen", (NbtElement)nbtElement));
        }
        if (!(upgradeData = chunk.getUpgradeData()).isDone()) {
            nbtCompound.put("UpgradeData", upgradeData.toNbt());
        }
        ChunkSection[] chunkSections = chunk.getSectionArray();
        NbtList nbtList = new NbtList();
        LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
        Registry<Biome> registry = world.getRegistryManager().get(Registry.BIOME_KEY);
        Codec<ReadableContainer<RegistryEntry<Biome>>> codec = createCodec(registry);
        boolean bl = chunk.isLightOn();
        for (int i = lightingProvider.getBottomY(); i < lightingProvider.getTopY(); ++i) {
            int j = chunk.sectionCoordToIndex(i);
            boolean bl2 = j >= 0 && j < chunkSections.length;
            ChunkNibbleArray chunkNibbleArray = lightingProvider.get(LightType.BLOCK).getLightSection(ChunkSectionPos.from(chunkPos, i));
            ChunkNibbleArray chunkNibbleArray2 = lightingProvider.get(LightType.SKY).getLightSection(ChunkSectionPos.from(chunkPos, i));
            if (!bl2 && chunkNibbleArray == null && chunkNibbleArray2 == null) continue;
            NbtCompound nbtCompound2 = new NbtCompound();
            if (bl2) {
                ChunkSection chunkSection = chunkSections[j];
                nbtCompound2.put("block_states", CODEC.encodeStart(NbtOps.INSTANCE, chunkSection.getBlockStateContainer()).getOrThrow(false, LOGGER::error));
                DataResult<NbtElement> var10002 = codec.encodeStart(NbtOps.INSTANCE, chunkSection.getBiomeContainer());
                nbtCompound2.put("biomes", var10002.getOrThrow(false, LOGGER::error));
            }
            if (chunkNibbleArray != null && !chunkNibbleArray.isUninitialized()) {
                nbtCompound2.putByteArray("BlockLight", chunkNibbleArray.asByteArray());
            }
            if (chunkNibbleArray2 != null && !chunkNibbleArray2.isUninitialized()) {
                nbtCompound2.putByteArray("SkyLight", chunkNibbleArray2.asByteArray());
            }
            if (nbtCompound2.isEmpty()) continue;
            nbtCompound2.putByte("Y", (byte)i);
            nbtList.add(nbtCompound2);
        }
        nbtCompound.put("sections", nbtList);
        if (bl) {
            nbtCompound.putBoolean("isLightOn", true);
        }
        NbtList nbtList2 = new NbtList();
        for (BlockPos blockPos : chunk.getBlockEntityPositions()) {
            nbtCompound3 = chunk.getPackedBlockEntityNbt(blockPos);
            if (nbtCompound3 == null) continue;
            nbtList2.add(nbtCompound3);
        }
        nbtCompound.put("block_entities", nbtList2);
        if (chunk.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            NbtList nbtList3 = new NbtList();
            nbtList3.addAll(protoChunk.getEntities());
            nbtCompound.put("entities", nbtList3);
            nbtCompound.put("Lights", ChunkSerializer.toNbt(protoChunk.getLightSourcesBySection()));
            nbtCompound3 = new NbtCompound();
            for (GenerationStep.Carver carver : GenerationStep.Carver.values()) {
                CarvingMask carvingMask = protoChunk.getCarvingMask(carver);
                if (carvingMask == null) continue;
                nbtCompound3.putLongArray(carver.toString(), carvingMask.getMask());
            }
            nbtCompound.put("CarvingMasks", nbtCompound3);
        }
//        ChunkSerializer.serializeTicks(world, nbtCompound, chunk.getTickSchedulers());
        nbtCompound.put("PostProcessing", ChunkSerializer.toNbt(chunk.getPostProcessingLists()));
        NbtCompound nbtCompound4 = new NbtCompound();
        for (Map.Entry<Heightmap.Type, Heightmap> entry : chunk.getHeightmaps()) {
            if (!chunk.getStatus().getHeightmapTypes().contains(entry.getKey())) continue;
            nbtCompound4.put(entry.getKey().getName(), new NbtLongArray(entry.getValue().asLongArray()));
        }
        nbtCompound.put("Heightmaps", nbtCompound4);
//        nbtCompound.put("structures", ChunkSerializer.writeStructures(StructureContext.from(world), chunkPos, chunk.getStructureStarts(), chunk.getStructureReferences()));
        return nbtCompound;
    }

    private static Codec<ReadableContainer<RegistryEntry<Biome>>> createCodec(Registry<Biome> biomeRegistry) {
        return PalettedContainer.createReadableContainerCodec(biomeRegistry.getIndexedEntries(), biomeRegistry.createEntryCodec(), PalettedContainer.PaletteProvider.BIOME, biomeRegistry.entryOf(BiomeKeys.PLAINS));
    }

    static {
        CODEC = PalettedContainer.createPalettedContainerCodec(Block.STATE_IDS, BlockState.CODEC, PalettedContainer.PaletteProvider.BLOCK_STATE, Blocks.AIR.getDefaultState());
    }
}
