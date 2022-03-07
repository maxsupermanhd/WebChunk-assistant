package net.maxsupermanhd.TerrainReporter;

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
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.chunk.BlendingData;

import java.util.*;

public class ClientChunkSerializer extends ChunkSerializer {
    private static final Codec<PalettedContainer<BlockState>> CODEC;

    public static NbtCompound serializeClientChunk(ClientWorld world, int x, int z) {
        Chunk chunk = world.getChunk(x, z);
        ChunkPos chunkPos = chunk.getPos();
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
        nbtCompound.putInt("xPos", x);
        nbtCompound.putInt("yPos", chunk.getBottomSectionCoord());
        nbtCompound.putInt("zPos", z);
        nbtCompound.putLong("LastUpdate", world.getTime());
        nbtCompound.putLong("InhabitedTime", chunk.getInhabitedTime());
        nbtCompound.putString("Status", chunk.getStatus().getId());
        BlendingData blendingData = chunk.getBlendingData();
//        DataResult var10000;
//        Logger var10001;
//        if (blendingData != null) {
//            var10000 = BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, blendingData);
//            var10001 = LOGGER;
//            Objects.requireNonNull(var10001);
//            var10000.resultOrPartial(var10001::error).ifPresent((nbtElement) -> {
//                nbtCompound.put("blending_data", nbtElement);
//            });
//        }

//        BelowZeroRetrogen belowZeroRetrogen = chunk.getBelowZeroRetrogen();
//        if (belowZeroRetrogen != null) {
//            var10000 = BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowZeroRetrogen);
//            var10001 = LOGGER;
//            Objects.requireNonNull(var10001);
//            var10000.resultOrPartial(var10001::error).ifPresent((nbtElement) -> {
//                nbtCompound.put("below_zero_retrogen", nbtElement);
//            });
//        }

        UpgradeData upgradeData = chunk.getUpgradeData();
        if (!upgradeData.isDone()) {
            nbtCompound.put("UpgradeData", upgradeData.toNbt());
        }

        ChunkSection[] chunkSections = chunk.getSectionArray();
        NbtList nbtList = new NbtList();
        LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
        Registry<Biome> registry = world.getRegistryManager().get(Registry.BIOME_KEY);
        Codec<PalettedContainer<Biome>> codec = createCodec(registry);
        boolean bl = chunk.isLightOn();

        for(int i = lightingProvider.getBottomY(); i < lightingProvider.getTopY(); ++i) {
            int j = chunk.sectionCoordToIndex(i);
            boolean bl2 = j >= 0 && j < chunkSections.length;
            ChunkNibbleArray chunkNibbleArray = lightingProvider.get(LightType.BLOCK).getLightSection(ChunkSectionPos.from(chunkPos, i));
            ChunkNibbleArray chunkNibbleArray2 = lightingProvider.get(LightType.SKY).getLightSection(ChunkSectionPos.from(chunkPos, i));
            if (bl2 || chunkNibbleArray != null || chunkNibbleArray2 != null) {
                NbtCompound nbtCompound2 = new NbtCompound();
                if (bl2) {
                    ChunkSection chunkSection = chunkSections[j];
                    DataResult<NbtElement> var10002 = CODEC.encodeStart(NbtOps.INSTANCE, chunkSection.getBlockStateContainer());
                    nbtCompound2.put("block_states", (NbtElement)var10002.getOrThrow(false, TerrainReporter.LOGGER::error));
                    var10002 = codec.encodeStart(NbtOps.INSTANCE, chunkSection.getBiomeContainer());
                    nbtCompound2.put("biomes", (NbtElement)var10002.getOrThrow(false, TerrainReporter.LOGGER::error));
                }

                if (chunkNibbleArray != null && !chunkNibbleArray.isUninitialized()) {
                    nbtCompound2.putByteArray("BlockLight", chunkNibbleArray.asByteArray());
                }

                if (chunkNibbleArray2 != null && !chunkNibbleArray2.isUninitialized()) {
                    nbtCompound2.putByteArray("SkyLight", chunkNibbleArray2.asByteArray());
                }

                if (!nbtCompound2.isEmpty()) {
                    nbtCompound2.putByte("Y", (byte)i);
                    nbtList.add(nbtCompound2);
                }
            }
        }

        nbtCompound.put("sections", nbtList);
        if (bl) {
            nbtCompound.putBoolean("isLightOn", true);
        }

        NbtList nbtList2 = new NbtList();
        Iterator<BlockPos> var23 = chunk.getBlockEntityPositions().iterator();

        NbtCompound nbtCompound3;
        while(var23.hasNext()) {
            BlockPos blockPos = (BlockPos)var23.next();
            nbtCompound3 = chunk.getPackedBlockEntityNbt(blockPos);
            if (nbtCompound3 != null) {
                nbtList2.add(nbtCompound3);
            }
        }

        nbtCompound.put("block_entities", nbtList2);
        if (chunk.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            NbtList nbtList3 = new NbtList();
            nbtList3.addAll(protoChunk.getEntities());
            nbtCompound.put("entities", nbtList3);
            nbtCompound.put("Lights", toNbt(protoChunk.getLightSourcesBySection()));
            nbtCompound3 = new NbtCompound();
            GenerationStep.Carver[] var31 = GenerationStep.Carver.values();
            int var32 = var31.length;

            for (GenerationStep.Carver carver : var31) {
                CarvingMask carvingMask = protoChunk.getCarvingMask(carver);
                if (carvingMask != null) {
                    nbtCompound3.putLongArray(carver.toString(), carvingMask.getMask());
                }
            }

            nbtCompound.put("CarvingMasks", nbtCompound3);
        }

//        serializeTicks(world, nbtCompound, chunk.getTickSchedulers());
        nbtCompound.put("PostProcessing", toNbt(chunk.getPostProcessingLists()));
        NbtCompound nbtCompound4 = new NbtCompound();

        for (Map.Entry<Heightmap.Type, Heightmap> typeHeightmapEntry : chunk.getHeightmaps()) {
            if (chunk.getStatus().getHeightmapTypes().contains(((Map.Entry<Heightmap.Type, Heightmap>) (Map.Entry) typeHeightmapEntry).getKey())) {
                nbtCompound4.put(((Heightmap.Type) ((Map.Entry<Heightmap.Type, Heightmap>) (Map.Entry) typeHeightmapEntry).getKey()).getName(), new NbtLongArray(((Heightmap) ((Map.Entry<Heightmap.Type, Heightmap>) (Map.Entry) typeHeightmapEntry).getValue()).asLongArray()));
            }
        }

        nbtCompound.put("Heightmaps", nbtCompound4);
//        nbtCompound.put("structures", writeStructures(StructureContext.from(world), chunkPos, chunk.getStructureStarts(), chunk.getStructureReferences()));
        return nbtCompound;
//
//        ChunkPos chunkPos = new ChunkPos(x, z);
//        NbtCompound nbtCompound = new NbtCompound();
//        NbtCompound nbtCompound2 = new NbtCompound();
//        nbtCompound.putInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
//        nbtCompound.put("Level", nbtCompound2);
//        nbtCompound2.putInt("xPos", chunkPos.x);
//        nbtCompound2.putInt("zPos", chunkPos.z);
//        nbtCompound2.putLong("LastUpdate", world.getTime());
//        nbtCompound2.putLong("InhabitedTime", chunk.getInhabitedTime());
//        nbtCompound2.putString("Status", chunk.getStatus().getId());
//        UpgradeData upgradeData = chunk.getUpgradeData();
//        if (!upgradeData.isDone()) {
//            nbtCompound2.put("UpgradeData", upgradeData.toNbt());
//        }
//
//        ChunkSection[] chunkSections = chunk.getSectionArray();
//        NbtList nbtList = new NbtList();
//        LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
//        boolean bl = chunk.isLightOn();
//
//        for(int i = lightingProvider.getBottomY(); i < lightingProvider.getTopY(); ++i) {
//            int finalI = i;
//            ChunkSection chunkSection = (ChunkSection) Arrays.stream(chunkSections).filter((chunkSectionx) -> {
//                return chunkSectionx != null && ChunkSectionPos.getSectionCoord(chunkSectionx.getYOffset()) == finalI;
//            }).findFirst().orElse(WorldChunk.EMPTY_SECTION);
//            ChunkNibbleArray chunkNibbleArray = lightingProvider.get(LightType.BLOCK).getLightSection(ChunkSectionPos.from(chunkPos, i));
//            ChunkNibbleArray chunkNibbleArray2 = lightingProvider.get(LightType.SKY).getLightSection(ChunkSectionPos.from(chunkPos, i));
//            if (chunkSection != WorldChunk.EMPTY_SECTION || chunkNibbleArray != null || chunkNibbleArray2 != null) {
//                NbtCompound nbtCompound3 = new NbtCompound();
//                nbtCompound3.putByte("Y", (byte)(i & 255));
//                if (chunkSection != WorldChunk.EMPTY_SECTION) {
//                    chunkSection.getContainer().write(nbtCompound3, "Palette", "BlockStates");
//                }
//
//                if (chunkNibbleArray != null && !chunkNibbleArray.isUninitialized()) {
//                    nbtCompound3.putByteArray("BlockLight", chunkNibbleArray.asByteArray());
//                }
//
//                if (chunkNibbleArray2 != null && !chunkNibbleArray2.isUninitialized()) {
//                    nbtCompound3.putByteArray("SkyLight", chunkNibbleArray2.asByteArray());
//                }
//
//                nbtList.add(nbtCompound3);
//            }
//        }
//
//        nbtCompound2.put("Sections", nbtList);
//        if (bl) {
//            nbtCompound2.putBoolean("isLightOn", true);
//        }
//
//        BiomeArray i = chunk.getBiomeArray();
//        if (i != null) {
//            nbtCompound2.putIntArray("Biomes", i.toIntArray());
//        }
//
//        NbtList j = new NbtList();
//        Iterator var21 = chunk.getBlockEntityPositions().iterator();
//
//        NbtCompound chunkNibbleArray2;
//        while(var21.hasNext()) {
//            BlockPos chunkNibbleArray = (BlockPos)var21.next();
//            chunkNibbleArray2 = chunk.getPackedBlockEntityNbt(chunkNibbleArray);
//            if (chunkNibbleArray2 != null) {
//                j.add(chunkNibbleArray2);
//            }
//        }
//
//        nbtCompound2.put("TileEntities", j);
//        if (chunk.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
//            ProtoChunk chunkSection = (ProtoChunk)chunk;
//            NbtList chunkNibbleArray = new NbtList();
//            chunkNibbleArray.addAll(chunkSection.getEntities());
//            nbtCompound2.put("Entities", chunkNibbleArray);
//            nbtCompound2.put("Lights", toNbt(chunkSection.getLightSourcesBySection()));
//            chunkNibbleArray2 = new NbtCompound();
//            GenerationStep.Carver[] var28 = GenerationStep.Carver.values();
//            int var16 = var28.length;
//
//            for(int var17 = 0; var17 < var16; ++var17) {
//                GenerationStep.Carver carver = var28[var17];
//                BitSet bitSet = chunkSection.getCarvingMask(carver);
//                if (bitSet != null) {
//                    chunkNibbleArray2.putByteArray(carver.toString(), bitSet.toByteArray());
//                }
//            }
//
//            nbtCompound2.put("CarvingMasks", chunkNibbleArray2);
//        }
//
//        TickScheduler<Block> chunkSection = chunk.getBlockTickScheduler();
//        if (chunkSection instanceof ChunkTickScheduler) {
//            nbtCompound2.put("ToBeTicked", ((ChunkTickScheduler)chunkSection).toNbt());
//        } else if (chunkSection instanceof SimpleTickScheduler) {
//            nbtCompound2.put("TileTicks", ((SimpleTickScheduler)chunkSection).toNbt());
//        } else {
////            nbtCompound2.put("TileTicks", world.getBlockTickScheduler().toNbt(chunkPos));
//            nbtCompound2.put("TileTicks", new NbtList());
//        }
//
//        TickScheduler<Fluid> chunkNibbleArray = chunk.getFluidTickScheduler();
//        if (chunkNibbleArray instanceof ChunkTickScheduler) {
//            nbtCompound2.put("LiquidsToBeTicked", ((ChunkTickScheduler)chunkNibbleArray).toNbt());
//        } else if (chunkNibbleArray instanceof SimpleTickScheduler) {
//            nbtCompound2.put("LiquidTicks", ((SimpleTickScheduler)chunkNibbleArray).toNbt());
//        } else {
////            nbtCompound2.put("LiquidTicks", world.getFluidTickScheduler().toNbt(chunkPos));
//            nbtCompound2.put("LiquidTicks", new NbtList());
//        }
//
//        nbtCompound2.put("PostProcessing", toNbt(chunk.getPostProcessingLists()));
//        chunkNibbleArray2 = new NbtCompound();
//        Iterator var29 = chunk.getHeightmaps().iterator();
//
//        while(var29.hasNext()) {
//            Map.Entry<Heightmap.Type, Heightmap> entry = (Map.Entry)var29.next();
//            if (chunk.getStatus().getHeightmapTypes().contains(entry.getKey())) {
//                chunkNibbleArray2.put(((Heightmap.Type)entry.getKey()).getName(), new NbtLongArray(((Heightmap)entry.getValue()).asLongArray()));
//            }
//        }
//
//        nbtCompound2.put("Heightmaps", chunkNibbleArray2);
////        nbtCompound2.put("Structures", writeStructures(world, chunkPos, chunk.getStructureStarts(), chunk.getStructureReferences()));
//        return nbtCompound;
    }

    private static Codec<PalettedContainer<Biome>> createCodec(Registry<Biome> biomeRegistry) {
        return PalettedContainer.createCodec(biomeRegistry, biomeRegistry.getCodec(), PalettedContainer.PaletteProvider.BIOME, (Biome)biomeRegistry.getOrThrow(BiomeKeys.PLAINS));
    }

    static {
        CODEC = PalettedContainer.createCodec(Block.STATE_IDS, BlockState.CODEC, PalettedContainer.PaletteProvider.BLOCK_STATE, Blocks.AIR.getDefaultState());
    }
}
