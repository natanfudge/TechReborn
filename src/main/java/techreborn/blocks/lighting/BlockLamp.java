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

package techreborn.blocks.lighting;

import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import reborncore.api.ToolManager;
import reborncore.common.BaseBlockEntityProvider;
import reborncore.common.blocks.BlockWrenchEventHandler;
import reborncore.common.util.WrenchUtils;
import techreborn.blockentity.lighting.LampBlockEntity;

import javax.annotation.Nullable;

public class BlockLamp extends BaseBlockEntityProvider {

	public static DirectionProperty FACING = Properties.FACING;
	public static BooleanProperty ACTIVE;
	protected final VoxelShape[] shape;

	private int cost;
	private static int brightness = 15;

	public BlockLamp(int cost, double depth, double width) {
		super(FabricBlockSettings.of(Material.REDSTONE_LAMP).strength(2f, 2f).lightLevel(brightness).build());
		this.shape = genCuboidShapes(depth, width);
		this.cost = cost;
		this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(ACTIVE, false));
		BlockWrenchEventHandler.wrenableBlocks.add(this);
	}
	
	private VoxelShape[] genCuboidShapes(double depth, double width) {
		double culling = (16.0D - width) / 2 ;
		return new VoxelShape[]{
				createCuboidShape(culling, 16.0 - depth, culling, 16.0 - culling, 16.0D, 16.0 - culling),
				createCuboidShape(culling, 0.0D, culling, 16.0D - culling, depth, 16.0 - culling),
				createCuboidShape(culling, culling, 16.0 - depth, 16.0 - culling, 16.0 - culling, 16.0D),
				createCuboidShape(culling, culling, 0.0D, 16.0 - culling, 16.0 - culling, depth),
				createCuboidShape(16.0 - depth, culling, culling, 16.0D, 16.0 - culling, 16.0 - culling),
				createCuboidShape(0.0D, culling, culling, depth, 16.0 - culling, 16.0 - culling)
			};
	}

	@Override
	public VoxelShape getOutlineShape(BlockState blockState, BlockView blockView, BlockPos blockPos, EntityContext entityContext) {
		return shape[blockState.get(FACING).ordinal()];
	}

	public static boolean isActive(BlockState state) {
		return state.get(ACTIVE);
	}

	public static Direction getFacing(BlockState state) {
		return state.get(FACING);
	}

	public static void setFacing(Direction facing, World world, BlockPos pos) {
		world.setBlockState(pos, world.getBlockState(pos).with(FACING, facing));
	}

	public static void setActive(Boolean active, World world, BlockPos pos) {
		Direction facing = (Direction)world.getBlockState(pos).get(FACING);
		BlockState state = world.getBlockState(pos).with(ACTIVE, active).with(FACING, facing);
		world.setBlockState(pos, state, 3);
	}

	public int getCost() {
		return cost;
	}
	
	// BaseTileBlock
	@Override
	public BlockEntity createBlockEntity(BlockView worldIn) {
		return new LampBlockEntity();
	}	

	// Block
	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		ACTIVE = BooleanProperty.of("active");
		builder.add(FACING, ACTIVE);
	}
	
	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		for (Direction enumfacing : context.getPlacementDirections()) {
			BlockState iblockstate = this.getDefaultState().with(FACING, enumfacing.getOpposite());
			if (iblockstate.canPlaceAt(context.getWorld(), context.getBlockPos())) {
				return iblockstate;
			}
		}
		return null;
	}
	
	@Override
	public int getLuminance(BlockState state) {
		return state.get(ACTIVE) ? brightness : 0;
	}
	
	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}
	
	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity playerIn, Hand hand, BlockHitResult hitResult) {
		ItemStack stack = playerIn.getStackInHand(Hand.MAIN_HAND);
		BlockEntity blockEntity = worldIn.getBlockEntity(pos);

		// We extended BaseTileBlock. Thus we should always have blockEntity entity. I hope.
		if (blockEntity == null) {
			return ActionResult.FAIL;
		}

		if (!stack.isEmpty() && ToolManager.INSTANCE.canHandleTool(stack)) {
			if (WrenchUtils.handleWrench(stack, worldIn, pos, playerIn, hitResult.getSide())) {
				return ActionResult.SUCCESS;
			}
		}

		return super.onUse(state, worldIn, pos, playerIn, hand, hitResult);
	}
}
