// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.engine.world.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.terasology.TerasologyTestingEnvironment;
import org.terasology.engine.math.Diamond3iIterator;
import org.terasology.engine.math.Region3i;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.family.SymmetricFamily;
import org.terasology.engine.world.block.internal.BlockManagerImpl;
import org.terasology.engine.world.block.loader.BlockFamilyDefinition;
import org.terasology.engine.world.block.loader.BlockFamilyDefinitionData;
import org.terasology.engine.world.block.shapes.BlockShape;
import org.terasology.engine.world.block.tiles.NullWorldAtlas;
import org.terasology.engine.world.chunks.Chunk;
import org.terasology.engine.world.chunks.ChunkConstants;
import org.terasology.engine.world.chunks.blockdata.ExtraBlockDataManager;
import org.terasology.engine.world.chunks.internal.ChunkImpl;
import org.terasology.engine.world.propagation.light.InternalLightProcessor;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.management.AssetManager;
import org.terasology.math.geom.Vector3i;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InternalLightGeneratorTest extends TerasologyTestingEnvironment {

    Block airBlock;
    Block solidBlock;
    Block fullLight;

    private BlockManager blockManager;
    private ExtraBlockDataManager extraDataManager;


    @BeforeEach
    public void setup() throws Exception {
        super.setup();
        AssetManager assetManager = CoreRegistry.get(AssetManager.class);
        blockManager = new BlockManagerImpl(new NullWorldAtlas(), assetManager);
        CoreRegistry.put(BlockManager.class, blockManager);
        airBlock = blockManager.getBlock(BlockManager.AIR_ID);


        extraDataManager = new ExtraBlockDataManager();

        BlockFamilyDefinitionData solidData = new BlockFamilyDefinitionData();
        solidData.getBaseSection().setDisplayName("Stone");
        solidData.getBaseSection().setShape(assetManager.getAsset("engine:cube", BlockShape.class).get());
        solidData.getBaseSection().setTranslucent(false);
        solidData.setBlockFamily(SymmetricFamily.class);
        assetManager.loadAsset(new ResourceUrn("engine:stone"), solidData, BlockFamilyDefinition.class);
        solidBlock = blockManager.getBlock(new BlockUri(new ResourceUrn("engine:stone")));

        BlockFamilyDefinitionData fullLightData = new BlockFamilyDefinitionData();
        fullLightData.getBaseSection().setDisplayName("Torch");
        fullLightData.getBaseSection().setShape(assetManager.getAsset("engine:cube", BlockShape.class).get());
        fullLightData.getBaseSection().setLuminance(ChunkConstants.MAX_LIGHT);
        fullLightData.setBlockFamily(SymmetricFamily.class);
        assetManager.loadAsset(new ResourceUrn("engine:torch"), fullLightData, BlockFamilyDefinition.class);
        fullLight = blockManager.getBlock(new BlockUri(new ResourceUrn("engine:torch")));
    }

    @Test
    public void testUnblockedSunlightRegenPropagation() {
        Chunk chunk = new ChunkImpl(0, 0, 0, blockManager, extraDataManager);
        InternalLightProcessor.generateInternalLighting(chunk);

        for (Vector3i pos : Region3i.createFromMinAndSize(Vector3i.zero(), new Vector3i(ChunkConstants.SIZE_X,
                ChunkConstants.SIZE_Y, ChunkConstants.SIZE_Z))) {
            byte expectedRegen = (byte) Math.min(ChunkConstants.SIZE_Y - pos.y - 1, ChunkConstants.MAX_SUNLIGHT_REGEN);
            assertEquals(expectedRegen, chunk.getSunlightRegen(pos));
        }
    }

    @Test
    public void testBlockedSunlightRegenPropagationResets() {
        Chunk chunk = new ChunkImpl(0, 0, 0, blockManager, extraDataManager);
        for (Vector3i pos : Region3i.createFromMinAndSize(new Vector3i(0, 60, 0), new Vector3i(ChunkConstants.SIZE_X,
                1, ChunkConstants.SIZE_Z))) {
            chunk.setBlock(pos, solidBlock);
        }
        InternalLightProcessor.generateInternalLighting(chunk);

        for (Vector3i pos : Region3i.createFromMinAndSize(new Vector3i(0, 61, 0), new Vector3i(ChunkConstants.SIZE_X,
                3, ChunkConstants.SIZE_Z))) {
            byte expectedRegen = (byte) Math.min(ChunkConstants.SIZE_Y - pos.y - 1, ChunkConstants.MAX_SUNLIGHT_REGEN);
            assertEquals(expectedRegen, chunk.getSunlightRegen(pos));
        }
        for (Vector3i pos : Region3i.createFromMinAndSize(new Vector3i(0, 60, 0), new Vector3i(ChunkConstants.SIZE_X,
                1, ChunkConstants.SIZE_Z))) {
            assertEquals(0, chunk.getSunlightRegen(pos));
        }
        for (Vector3i pos : Region3i.createFromMinAndSize(new Vector3i(0, 0, 0), new Vector3i(ChunkConstants.SIZE_X,
                59, ChunkConstants.SIZE_Z))) {
            byte expectedRegen = (byte) Math.min(60 - pos.y - 1, ChunkConstants.MAX_SUNLIGHT_REGEN);
            assertEquals(expectedRegen, chunk.getSunlightRegen(pos));
        }
    }

    @Test
    public void testBlockedAtTopSunlightRegenPropagationResets() {
        Chunk chunk = new ChunkImpl(0, 0, 0, blockManager, extraDataManager);
        for (Vector3i pos : Region3i.createFromMinAndSize(new Vector3i(0, 63, 0), new Vector3i(ChunkConstants.SIZE_X,
                1, ChunkConstants.SIZE_Z))) {
            chunk.setBlock(pos, solidBlock);
        }
        InternalLightProcessor.generateInternalLighting(chunk);

        for (Vector3i pos : Region3i.createFromMinAndSize(Vector3i.zero(), new Vector3i(ChunkConstants.SIZE_X,
                ChunkConstants.SIZE_Y - 1, ChunkConstants.SIZE_Z))) {
            byte expectedRegen = (byte) Math.min(ChunkConstants.SIZE_Y - pos.y - 2, ChunkConstants.MAX_SUNLIGHT_REGEN);
            assertEquals(expectedRegen, chunk.getSunlightRegen(pos));
        }
    }

    @Test
    public void testUnblockedSunlightPropagationAfterHittingMaxRegen() {
        Chunk chunk = new ChunkImpl(0, 0, 0, blockManager, extraDataManager);
        InternalLightProcessor.generateInternalLighting(chunk);

        for (Vector3i pos : Region3i.createFromMinAndSize(new Vector3i(0, 15, 0), new Vector3i(ChunkConstants.SIZE_X,
                ChunkConstants.SIZE_Y - 15,
                ChunkConstants.SIZE_Z))) {
            assertEquals(0, chunk.getSunlight(pos));
        }

        for (Vector3i pos : Region3i.createFromMinAndSize(Vector3i.zero(), new Vector3i(ChunkConstants.SIZE_X,
                ChunkConstants.SIZE_Y - ChunkConstants.MAX_SUNLIGHT_REGEN,
                ChunkConstants.SIZE_Z))) {
            byte expectedSunlight =
                    (byte) Math.min(ChunkConstants.SIZE_Y - ChunkConstants.SUNLIGHT_REGEN_THRESHOLD - pos.y - 1,
                            ChunkConstants.MAX_SUNLIGHT);
            assertEquals(expectedSunlight, chunk.getSunlight(pos), () -> "Incorrect lighting at " + pos);
        }
    }

    @Test
    public void testBlockedSunlightPropagation() {
        Chunk chunk = new ChunkImpl(0, 0, 0, blockManager, extraDataManager);
        for (Vector3i pos : Region3i.createFromMinAndSize(new Vector3i(0, 4, 0), new Vector3i(ChunkConstants.SIZE_X,
                1, ChunkConstants.SIZE_Z))) {
            chunk.setBlock(pos, solidBlock);
        }
        InternalLightProcessor.generateInternalLighting(chunk);

        for (Vector3i pos : Region3i.createFromMinAndSize(new Vector3i(0, 0, 0), new Vector3i(ChunkConstants.SIZE_X, 5,
                ChunkConstants.SIZE_Z))) {
            assertEquals(0, chunk.getSunlight(pos), () -> "Incorrect lighting at " + pos);
        }
    }

    @Test
    public void testUnblockedSunlightPropagation() {
        Chunk chunk = new ChunkImpl(0, 0, 0, blockManager, extraDataManager);
        InternalLightProcessor.generateInternalLighting(chunk);

        for (Vector3i pos : Region3i.createFromMinAndSize(new Vector3i(0, 0, 0), new Vector3i(ChunkConstants.SIZE_X, 15,
                ChunkConstants.SIZE_Z))) {
            assertEquals(15 - pos.y, chunk.getSunlight(pos), () -> "Incorrect lighting at " + pos);
        }
    }

    @Test
    public void testHorizontalSunlightPropagation() {
        Chunk chunk = new ChunkImpl(0, 0, 0, blockManager, extraDataManager);
        for (Vector3i pos : Region3i.createFromMinAndSize(new Vector3i(0, 4, 0), new Vector3i(ChunkConstants.SIZE_X,
                1, ChunkConstants.SIZE_Z))) {
            chunk.setBlock(pos, solidBlock);
        }
        chunk.setBlock(new Vector3i(16, 4, 16), airBlock);
        InternalLightProcessor.generateInternalLighting(chunk);

        assertEquals(12, chunk.getSunlight(16, 3, 16));
        assertEquals(11, chunk.getSunlight(15, 3, 16));
        assertEquals(11, chunk.getSunlight(17, 3, 16));
        assertEquals(11, chunk.getSunlight(16, 3, 15));
        assertEquals(11, chunk.getSunlight(16, 3, 17));

        assertEquals(12, chunk.getSunlight(15, 2, 16));
        assertEquals(12, chunk.getSunlight(17, 2, 16));
        assertEquals(12, chunk.getSunlight(16, 2, 15));
        assertEquals(12, chunk.getSunlight(16, 2, 17));
    }

    @Test
    public void testLightPropagation() {
        Chunk chunk = new ChunkImpl(0, 0, 0, blockManager, extraDataManager);
        chunk.setBlock(16, 32, 16, fullLight);

        InternalLightProcessor.generateInternalLighting(chunk);
        assertEquals(fullLight.getLuminance(), chunk.getLight(16, 32, 16));
        assertEquals(fullLight.getLuminance() - 1, chunk.getLight(new Vector3i(16, 33, 16)));
        for (int i = 1; i < fullLight.getLuminance(); ++i) {
            for (Vector3i pos : Diamond3iIterator.iterateAtDistance(new Vector3i(16, 32, 16), i)) {
                assertEquals(fullLight.getLuminance() - i, chunk.getLight(pos));
            }
        }
    }

}