/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018 TechReborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package techreborn.world;

import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Category;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.decorator.CountExtraChanceDecoratorConfig;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.decorator.RangeDecoratorConfig;
import net.minecraft.world.gen.feature.BranchedTreeFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig.Target;
import net.minecraft.world.gen.foliage.BlobFoliagePlacer;
import net.minecraft.world.gen.stateprovider.SimpleStateProvider;
import net.minecraft.world.gen.stateprovider.WeightedStateProvider;
import reborncore.common.world.CustomOreFeature;
import reborncore.common.world.CustomOreFeatureConfig;
import techreborn.blocks.misc.BlockRubberLog;
import techreborn.init.TRContent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author drcrazy
 *
 */
public class WorldGenerator {

	public static Feature<BranchedTreeFeatureConfig> RUBBER_TREE;

	public static BranchedTreeFeatureConfig RUBBER_TREE_CONFIG;

	private static List<Biome> checkedBiomes = new ArrayList<>();

	public static void initBiomeFeatures() {
		setupTrees();

		for (Biome biome : Registry.BIOME) {
			addToBiome(biome);
		}

		//Handles modded biomes
		RegistryEntryAddedCallback.event(Registry.BIOME).register((i, identifier, biome) -> addToBiome(biome));
	}

	private static void setupTrees() {
		RUBBER_TREE = Registry.register(Registry.FEATURE, new Identifier("techreborn:rubber_tree"), new RubberTreeFeature(BranchedTreeFeatureConfig::deserialize2));

		WeightedStateProvider logProvider = new WeightedStateProvider();
		logProvider.addState(TRContent.RUBBER_LOG.getDefaultState(), 10);

		Arrays.stream(Direction.values())
				.filter(direction -> direction.getAxis().isHorizontal())
				.map(direction -> TRContent.RUBBER_LOG.getDefaultState()
						.with(BlockRubberLog.HAS_SAP, true)
						.with(BlockRubberLog.SAP_SIDE, direction)
				)
				.forEach(state -> logProvider.addState(state, 1));

		RUBBER_TREE_CONFIG = new BranchedTreeFeatureConfig.Builder(
				logProvider,
				new SimpleStateProvider(TRContent.RUBBER_LEAVES.getDefaultState()),
				new BlobFoliagePlacer(2, 0))
				.baseHeight(6)
				.heightRandA(2)
				.foliageHeight(3)
				.noVines()
				.build();

	}

	private static void addToBiome(Biome biome){
		if(checkedBiomes.contains(biome)){
			//Just to be sure we dont add the stuff twice to the same biome
			return;
		}
		checkedBiomes.add(biome);

		if (biome.getCategory() == Category.NETHER) {
			addOre(biome, OreFeatureConfig.Target.NETHERRACK, TRContent.Ores.CINNABAR);
			addOre(biome, OreFeatureConfig.Target.NETHERRACK, TRContent.Ores.PYRITE);
			addOre(biome, OreFeatureConfig.Target.NETHERRACK, TRContent.Ores.SPHALERITE);

		} else if (biome.getCategory() == Category.THEEND) {
			addEndOre(biome, TRContent.Ores.PERIDOT);
			addEndOre(biome, TRContent.Ores.SHELDONITE);
			addEndOre(biome, TRContent.Ores.SODALITE);
			addEndOre(biome, TRContent.Ores.TUNGSTEN);
		} else {
			addOre(biome, OreFeatureConfig.Target.NATURAL_STONE, TRContent.Ores.BAUXITE);
			addOre(biome, OreFeatureConfig.Target.NATURAL_STONE, TRContent.Ores.COPPER);
			addOre(biome, OreFeatureConfig.Target.NATURAL_STONE, TRContent.Ores.GALENA);
			addOre(biome, OreFeatureConfig.Target.NATURAL_STONE, TRContent.Ores.IRIDIUM);
			addOre(biome, OreFeatureConfig.Target.NATURAL_STONE, TRContent.Ores.LEAD);
			addOre(biome, OreFeatureConfig.Target.NATURAL_STONE, TRContent.Ores.RUBY);
			addOre(biome, OreFeatureConfig.Target.NATURAL_STONE, TRContent.Ores.SAPPHIRE);
			addOre(biome, OreFeatureConfig.Target.NATURAL_STONE, TRContent.Ores.SILVER);
			addOre(biome, OreFeatureConfig.Target.NATURAL_STONE, TRContent.Ores.TIN);

			if (biome.getCategory() == Category.FOREST || biome.getCategory() == Category.TAIGA || biome.getCategory() == Category.SWAMP) {
				biome.addFeature(GenerationStep.Feature.VEGETAL_DECORATION,
						RUBBER_TREE.configure(RUBBER_TREE_CONFIG)
								.createDecoratedFeature(Decorator.COUNT_EXTRA_HEIGHTMAP
										.configure(new CountExtraChanceDecoratorConfig(biome.getCategory() == Category.SWAMP ? 2 : 1, 0.1F, 1))
								)
				);
			}
		}
	}

	private static void addOre(Biome biome, Target canReplaceIn, TRContent.Ores ore) {
		biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES, Feature.ORE.configure(
				new OreFeatureConfig(canReplaceIn, ore.block.getDefaultState(), ore.veinSize)).createDecoratedFeature(Decorator.COUNT_RANGE.configure(
				new RangeDecoratorConfig(ore.veinsPerChunk, ore.minY, ore.minY, ore.maxY))));
	}

	private static void addEndOre(Biome biome, TRContent.Ores ore) {
		biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES, CustomOreFeature.CUSTOM_ORE_FEATURE.configure(
				new CustomOreFeatureConfig(blockState -> blockState.getBlock() == Blocks.END_STONE, ore.block.getDefaultState(), ore.veinSize)).createDecoratedFeature(Decorator.COUNT_RANGE.configure(
				new RangeDecoratorConfig(ore.veinsPerChunk, ore.minY, ore.minY, ore.maxY))));
	}
}