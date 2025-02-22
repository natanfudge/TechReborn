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

package techreborn.blockentity.generator.basic;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import reborncore.api.IToolDrop;
import reborncore.api.blockentity.InventoryProvider;
import reborncore.client.containerBuilder.IContainerProvider;
import reborncore.client.containerBuilder.builder.BuiltContainer;
import reborncore.client.containerBuilder.builder.ContainerBuilder;
import reborncore.common.blocks.BlockMachineBase;
import reborncore.common.powerSystem.PowerAcceptorBlockEntity;
import reborncore.common.util.RebornInventory;
import techreborn.config.TechRebornConfig;
import techreborn.init.TRBlockEntities;
import techreborn.init.TRContent;

import javax.annotation.Nonnull;
import java.util.Map;

public class SolidFuelGeneratorBlockEntity extends PowerAcceptorBlockEntity implements IToolDrop, InventoryProvider, IContainerProvider {

	public RebornInventory<SolidFuelGeneratorBlockEntity> inventory = new RebornInventory<>(2, "SolidFuelGeneratorBlockEntity", 64, this);
	public int fuelSlot = 0;
	public int burnTime;
	public int totalBurnTime = 0;
	// sould properly use the conversion
	// ratio here.
	public boolean isBurning;
	public boolean lastTickBurning;
	ItemStack burnItem;

	public SolidFuelGeneratorBlockEntity() {
		super(TRBlockEntities.SOLID_FUEL_GENEREATOR);
	}

	public static int getItemBurnTime(@Nonnull ItemStack stack) {
		Map<Item, Integer> burnMap = AbstractFurnaceBlockEntity.createFuelTimeMap();
		if(burnMap.containsKey(stack.getItem())){
			return burnMap.get(stack.getItem()) / 4;
		}
		return 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (world.isClient) {
			return;
		}
		discharge(1);
		if (getEnergy() < getMaxPower()) {
			if (burnTime > 0) {
				burnTime--;
				addEnergy(TechRebornConfig.solidFuelGeneratorOutputAmount);
				isBurning = true;
			}
		} else {
			isBurning = false;
			updateState();
		}

		if (burnTime == 0) {
			updateState();
			burnTime = totalBurnTime = SolidFuelGeneratorBlockEntity.getItemBurnTime(inventory.getInvStack(fuelSlot));
			if (burnTime > 0) {
				updateState();
				burnItem = inventory.getInvStack(fuelSlot);
				if (inventory.getInvStack(fuelSlot).getCount() == 1) {
					if (inventory.getInvStack(fuelSlot).getItem() == Items.LAVA_BUCKET || inventory.getInvStack(fuelSlot).getItem() instanceof BucketItem) {
						inventory.setInvStack(fuelSlot, new ItemStack(Items.BUCKET));
					} else {
						inventory.setInvStack(fuelSlot, ItemStack.EMPTY);
					}

				} else {
					inventory.shrinkSlot(fuelSlot, 1);
				}
			}
		}

		lastTickBurning = isBurning;
	}

	public void updateState() {
		final BlockState BlockStateContainer = world.getBlockState(pos);
		if (BlockStateContainer.getBlock() instanceof BlockMachineBase) {
			final BlockMachineBase blockMachineBase = (BlockMachineBase) BlockStateContainer.getBlock();
			boolean active = burnTime > 0 && getEnergy() < getMaxPower();
			if (BlockStateContainer.get(BlockMachineBase.ACTIVE) != active) {
				blockMachineBase.setActive(active, world, pos);
			}
		}
	}

	@Override
	public double getBaseMaxPower() {
		return TechRebornConfig.solidFuelGeneratorMaxEnergy;
	}

	@Override
	public boolean canAcceptEnergy(Direction direction) {
		return false;
	}

	@Override
	public boolean canProvideEnergy(Direction direction) {
		return true;
	}

	@Override
	public double getBaseMaxOutput() {
		return TechRebornConfig.solidFuelGeneratorMaxOutput;
	}

	@Override
	public double getBaseMaxInput() {
		return 0;
	}

	@Override
	public ItemStack getToolDrop(PlayerEntity playerIn) {
		return TRContent.Machine.SOLID_FUEL_GENERATOR.getStack();
	}

	@Override
	public RebornInventory<SolidFuelGeneratorBlockEntity> getInventory() {
		return inventory;
	}

	public int getBurnTime() {
		return burnTime;
	}

	public void setBurnTime(final int burnTime) {
		this.burnTime = burnTime;
	}

	public int getTotalBurnTime() {
		return totalBurnTime;
	}

	public void setTotalBurnTime(final int totalBurnTime) {
		this.totalBurnTime = totalBurnTime;
	}

	public int getScaledBurnTime(final int i) {
		return (int) ((float) burnTime / (float) totalBurnTime * i);
	}

	@Override
	public BuiltContainer createContainer(int syncID, final PlayerEntity player) {
		return new ContainerBuilder("generator").player(player.inventory).inventory().hotbar().addInventory()
			.blockEntity(this).fuelSlot(0, 80, 54).energySlot(1, 8, 72).syncEnergyValue()
			.sync(this::getBurnTime, this::setBurnTime)
			.sync(this::getTotalBurnTime, this::setTotalBurnTime).addInventory().create(this, syncID);
	}

	@Override
	public boolean canBeUpgraded() {
		return false;
	}
}
