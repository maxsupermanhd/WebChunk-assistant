package net.maxsupermanhd.TerrainReporter;

import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.SimpleTickScheduler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.*;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.GenerationStep;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;

public class ClientChunkSerializer extends ChunkSerializer {
    public static NbtCompound serializeClientChunk(ClientWorld world, int x, int z) {
        Chunk chunk = world.getChunk(x, z);
        ChunkPos chunkPos = new ChunkPos(x, z);
        NbtCompound nbtCompound = new NbtCompound();
        NbtCompound nbtCompound2 = new NbtCompound();
        nbtCompound.putInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
        nbtCompound.put("Level", nbtCompound2);
        nbtCompound2.putInt("xPos", chunkPos.x);
        nbtCompound2.putInt("zPos", chunkPos.z);
        nbtCompound2.putLong("LastUpdate", world.getTime());
        nbtCompound2.putLong("InhabitedTime", chunk.getInhabitedTime());
        nbtCompound2.putString("Status", chunk.getStatus().getId());
        UpgradeData upgradeData = chunk.getUpgradeData();
        if (!upgradeData.isDone()) {
            nbtCompound2.put("UpgradeData", upgradeData.toNbt());
        }

        ChunkSection[] chunkSections = chunk.getSectionArray();
        NbtList nbtList = new NbtList();
        LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
        boolean bl = chunk.isLightOn();

        for(int i = lightingProvider.getBottomY(); i < lightingProvider.getTopY(); ++i) {
            int finalI = i;
            ChunkSection chunkSection = (ChunkSection) Arrays.stream(chunkSections).filter((chunkSectionx) -> {
                return chunkSectionx != null && ChunkSectionPos.getSectionCoord(chunkSectionx.getYOffset()) == finalI;
            }).findFirst().orElse(WorldChunk.EMPTY_SECTION);
            ChunkNibbleArray chunkNibbleArray = lightingProvider.get(LightType.BLOCK).getLightSection(ChunkSectionPos.from(chunkPos, i));
            ChunkNibbleArray chunkNibbleArray2 = lightingProvider.get(LightType.SKY).getLightSection(ChunkSectionPos.from(chunkPos, i));
            if (chunkSection != WorldChunk.EMPTY_SECTION || chunkNibbleArray != null || chunkNibbleArray2 != null) {
                NbtCompound nbtCompound3 = new NbtCompound();
                nbtCompound3.putByte("Y", (byte)(i & 255));
                if (chunkSection != WorldChunk.EMPTY_SECTION) {
                    chunkSection.getContainer().write(nbtCompound3, "Palette", "BlockStates");
                }

                if (chunkNibbleArray != null && !chunkNibbleArray.isUninitialized()) {
                    nbtCompound3.putByteArray("BlockLight", chunkNibbleArray.asByteArray());
                }

                if (chunkNibbleArray2 != null && !chunkNibbleArray2.isUninitialized()) {
                    nbtCompound3.putByteArray("SkyLight", chunkNibbleArray2.asByteArray());
                }

                nbtList.add(nbtCompound3);
            }
        }

        nbtCompound2.put("Sections", nbtList);
        if (bl) {
            nbtCompound2.putBoolean("isLightOn", true);
        }

        BiomeArray i = chunk.getBiomeArray();
        if (i != null) {
            nbtCompound2.putIntArray("Biomes", i.toIntArray());
        }

        NbtList j = new NbtList();
        Iterator var21 = chunk.getBlockEntityPositions().iterator();

        NbtCompound chunkNibbleArray2;
        while(var21.hasNext()) {
            BlockPos chunkNibbleArray = (BlockPos)var21.next();
            chunkNibbleArray2 = chunk.getPackedBlockEntityNbt(chunkNibbleArray);
            if (chunkNibbleArray2 != null) {
                j.add(chunkNibbleArray2);
            }
        }

        nbtCompound2.put("TileEntities", j);
        if (chunk.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
            ProtoChunk chunkSection = (ProtoChunk)chunk;
            NbtList chunkNibbleArray = new NbtList();
            chunkNibbleArray.addAll(chunkSection.getEntities());
            nbtCompound2.put("Entities", chunkNibbleArray);
            nbtCompound2.put("Lights", toNbt(chunkSection.getLightSourcesBySection()));
            chunkNibbleArray2 = new NbtCompound();
            GenerationStep.Carver[] var28 = GenerationStep.Carver.values();
            int var16 = var28.length;

            for(int var17 = 0; var17 < var16; ++var17) {
                GenerationStep.Carver carver = var28[var17];
                BitSet bitSet = chunkSection.getCarvingMask(carver);
                if (bitSet != null) {
                    chunkNibbleArray2.putByteArray(carver.toString(), bitSet.toByteArray());
                }
            }

            nbtCompound2.put("CarvingMasks", chunkNibbleArray2);
        }

        TickScheduler<Block> chunkSection = chunk.getBlockTickScheduler();
        if (chunkSection instanceof ChunkTickScheduler) {
            nbtCompound2.put("ToBeTicked", ((ChunkTickScheduler)chunkSection).toNbt());
        } else if (chunkSection instanceof SimpleTickScheduler) {
            nbtCompound2.put("TileTicks", ((SimpleTickScheduler)chunkSection).toNbt());
        } else {
//            nbtCompound2.put("TileTicks", world.getBlockTickScheduler().toNbt(chunkPos));
            nbtCompound2.put("TileTicks", new NbtList());
        }

        TickScheduler<Fluid> chunkNibbleArray = chunk.getFluidTickScheduler();
        if (chunkNibbleArray instanceof ChunkTickScheduler) {
            nbtCompound2.put("LiquidsToBeTicked", ((ChunkTickScheduler)chunkNibbleArray).toNbt());
        } else if (chunkNibbleArray instanceof SimpleTickScheduler) {
            nbtCompound2.put("LiquidTicks", ((SimpleTickScheduler)chunkNibbleArray).toNbt());
        } else {
//            nbtCompound2.put("LiquidTicks", world.getFluidTickScheduler().toNbt(chunkPos));
            nbtCompound2.put("LiquidTicks", new NbtList());
        }

        nbtCompound2.put("PostProcessing", toNbt(chunk.getPostProcessingLists()));
        chunkNibbleArray2 = new NbtCompound();
        Iterator var29 = chunk.getHeightmaps().iterator();

        while(var29.hasNext()) {
            Map.Entry<Heightmap.Type, Heightmap> entry = (Map.Entry)var29.next();
            if (chunk.getStatus().getHeightmapTypes().contains(entry.getKey())) {
                chunkNibbleArray2.put(((Heightmap.Type)entry.getKey()).getName(), new NbtLongArray(((Heightmap)entry.getValue()).asLongArray()));
            }
        }

        nbtCompound2.put("Heightmaps", chunkNibbleArray2);
//        nbtCompound2.put("Structures", writeStructures(world, chunkPos, chunk.getStructureStarts(), chunk.getStructureReferences()));
        return nbtCompound;
    }
}
