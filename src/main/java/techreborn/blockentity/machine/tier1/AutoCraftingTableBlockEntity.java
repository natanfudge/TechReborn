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

package techreborn.blockentity.machine.tier1;

import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.tuple.Pair;
import reborncore.api.IToolDrop;
import reborncore.api.blockentity.InventoryProvider;
import reborncore.client.containerBuilder.IContainerProvider;
import reborncore.client.containerBuilder.builder.BuiltContainer;
import reborncore.client.containerBuilder.builder.ContainerBuilder;
import reborncore.common.powerSystem.PowerAcceptorBlockEntity;
import reborncore.common.util.IInventoryAccess;
import reborncore.common.util.ItemUtils;
import reborncore.common.util.RebornInventory;
import techreborn.config.TechRebornConfig;
import techreborn.init.ModSounds;
import techreborn.init.TRBlockEntities;
import techreborn.init.TRContent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by modmuss50 on 20/06/2017.
 */
public class AutoCraftingTableBlockEntity extends PowerAcceptorBlockEntity
		implements IToolDrop, InventoryProvider, IContainerProvider {

	public RebornInventory<AutoCraftingTableBlockEntity> inventory = new RebornInventory<>(11, "AutoCraftingTableBlockEntity", 64, this);
	public int progress;
	public int maxProgress = 120;
	public int euTick = 10;
	public int balanceSlot = 0;

	CraftingInventory inventoryCrafting = null;
	CraftingRecipe lastCustomRecipe = null;
	CraftingRecipe lastRecipe = null;

	public boolean locked = true;

	public AutoCraftingTableBlockEntity() {
		super(TRBlockEntities.AUTO_CRAFTING_TABLE);
	}

	@Nullable
	public CraftingRecipe getCurrentRecipe() {
		CraftingInventory crafting = getCraftingInventory();
		if (!crafting.isInvEmpty()) {
			if (lastRecipe != null) {
				if (lastRecipe.matches(crafting, world)) {
					return lastRecipe;
				}
			}
			Optional<CraftingRecipe> testRecipe = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, crafting, world);
			if (testRecipe.isPresent()) {
				lastRecipe = testRecipe.get();
				return lastRecipe;
			}
		}
		return null;
	}

	public CraftingInventory getCraftingInventory() {
		if (inventoryCrafting == null) {
			inventoryCrafting = new CraftingInventory(new Container(null, -1) {
				@Override
				public boolean canUse(PlayerEntity playerIn) {
					return false;
				}
			}, 3, 3);
		}
		for (int i = 0; i < 9; i++) {
			inventoryCrafting.setInvStack(i, inventory.getInvStack(i));
		}
		return inventoryCrafting;
	}

	public boolean canMake(CraftingRecipe recipe) {
		if (recipe != null) {
			boolean missingOutput = false;
			int[] stacksInSlots = new int[9];
			for (int i = 0; i < 9; i++) {
				stacksInSlots[i] = inventory.getInvStack(i).getCount();
			}

			DefaultedList<Ingredient> ingredients = recipe.getPreviewInputs();
			List<Integer> checkedSlots = new ArrayList<>();
			for (Ingredient ingredient : ingredients) {
				if (ingredient != Ingredient.EMPTY) {
					boolean foundIngredient = false;
					for (int i = 0; i < 9; i++) {
						if(checkedSlots.contains(i)) {
							continue;
						}
						ItemStack stack = inventory.getInvStack(i);
						int requiredSize = locked ? 1 : 0;
						if (stack.getMaxCount() == 1) {
							requiredSize = 0;
						}
						if (stacksInSlots[i] > requiredSize) {
							if (ingredient.test(stack)) {
								if (stack.getItem().getRecipeRemainder() != null) {
									if (!hasRoomForExtraItem(new ItemStack(stack.getItem().getRecipeRemainder()))) {
										continue;
									}
								}
								foundIngredient = true;
								checkedSlots.add(i);
								stacksInSlots[i]--;
								break;
							}
						}
					}
					if (!foundIngredient) {
						missingOutput = true;
					}
				}
			}
			if (!missingOutput) {
				if (hasOutputSpace(recipe.getOutput(), 9)) {
					return true;
				}
			}
			return false;
		}
		return false;
	}

	boolean hasRoomForExtraItem(ItemStack stack) {
		ItemStack extraOutputSlot = inventory.getInvStack(10);
		if (extraOutputSlot.isEmpty()) {
			return true;
		}
		return hasOutputSpace(stack, 10);
	}

	public boolean hasOutputSpace(ItemStack output, int slot) {
		ItemStack stack = inventory.getInvStack(slot);
		if (stack.isEmpty()) {
			return true;
		}
		if (ItemUtils.isItemEqual(stack, output, true, true)) {
			if (stack.getMaxCount() > stack.getCount() + output.getCount()) {
				return true;
			}
		}
		return false;
	}

	public boolean make(CraftingRecipe recipe) {
		if (recipe == null || !canMake(recipe)) {
			return false;
		}
		for (int i = 0; i < recipe.getPreviewInputs().size(); i++) {
			DefaultedList<Ingredient> ingredients = recipe.getPreviewInputs();
			Ingredient ingredient = ingredients.get(i);
			// Looks for the best slot to take it from
			ItemStack bestSlot = inventory.getInvStack(i);
			if (ingredient.test(bestSlot)) {
				handleContainerItem(bestSlot);
				bestSlot.decrement(1);
			} else {
				for (int j = 0; j < 9; j++) {
					ItemStack stack = inventory.getInvStack(j);
					if (ingredient.test(stack)) {
						handleContainerItem(stack);
						stack.decrement(1);
						break;
					}
				}
			}
		}
		ItemStack output = inventory.getInvStack(9);
		ItemStack outputStack = recipe.craft(getCraftingInventory());
		if (output.isEmpty()) {
			inventory.setInvStack(9, outputStack.copy());
		} else {
			output.increment(recipe.getOutput().getCount());
		}
		return true;
	}

	private void handleContainerItem(ItemStack stack) {
		if (stack.getItem().hasRecipeRemainder()) {
			ItemStack containerItem = new ItemStack(stack.getItem().getRecipeRemainder());
			ItemStack extraOutputSlot = inventory.getInvStack(10);
			if (hasOutputSpace(containerItem, 10)) {
				if (extraOutputSlot.isEmpty()) {
					inventory.setInvStack(10, containerItem.copy());
				} else if (ItemUtils.isItemEqual(extraOutputSlot, containerItem, true, true)
						&& extraOutputSlot.getMaxCount() < extraOutputSlot.getCount() + containerItem.getCount()) {
					extraOutputSlot.increment(1);
				}
			}
		}
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public int getMaxProgress() {
		if (maxProgress == 0) {
			maxProgress = 1;
		}
		return maxProgress;
	}

	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
	}

	// TilePowerAcceptor
	@Override
	public void tick() {
		super.tick();
		if (world.isClient) {
			return;
		}
		CraftingRecipe recipe = getCurrentRecipe();
		if (recipe != null) {
			Optional<CraftingInventory> balanceResult = balanceRecipe(getCraftingInventory());
			balanceResult.ifPresent(craftingInventory -> inventoryCrafting = craftingInventory);

			if (progress >= maxProgress) {
				if (make(recipe)) {
					progress = 0;
				}
			} else {
				if (canMake(recipe)) {
					if (canUseEnergy(euTick)) {
						progress++;
						if (progress == 1) {
							world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.AUTO_CRAFTING,
									SoundCategory.BLOCKS, 0.3F, 0.8F);
						}
						useEnergy(euTick);
					}
				} else {
					progress = 0;
				}
			}
		}
		if (recipe == null) {
			progress = 0;
		}
	}

	public Optional<CraftingInventory> balanceRecipe(CraftingInventory craftCache) {
		CraftingRecipe currentRecipe = getCurrentRecipe();
		if (currentRecipe == null) {
			return Optional.empty();
		}
		if (world.isClient) {
			return Optional.empty();
		}
		if (craftCache.isInvEmpty()) {
			return Optional.empty();
		}
		balanceSlot++;
		if (balanceSlot > craftCache.getInvSize()) {
			balanceSlot = 0;
		}
		//Find the best slot for each item in a recipe, and move it if needed
		ItemStack sourceStack = inventory.getInvStack(balanceSlot);
		if (sourceStack.isEmpty()) {
			return Optional.empty();
		}
		List<Integer> possibleSlots = new ArrayList<>();
		for (int s = 0; s < currentRecipe.getPreviewInputs().size(); s++) {
			for (int i = 0; i < 9; i++) {
				if(possibleSlots.contains(i)) {
					continue;
				}
				ItemStack stackInSlot = inventory.getInvStack(i);
				Ingredient ingredient = currentRecipe.getPreviewInputs().get(s);
				if (ingredient != Ingredient.EMPTY && ingredient.test(sourceStack)) {
					if (stackInSlot.getItem() == sourceStack.getItem()) {
						possibleSlots.add(i);
						break;
					}
				}
			}

		}

		if(!possibleSlots.isEmpty()){
			int totalItems =  possibleSlots.stream()
					.mapToInt(value -> inventory.getInvStack(value).getCount()).sum();
			int slots = possibleSlots.size();

			//This makes an array of ints with the best possible slot EnvTyperibution
			int[] split = new int[slots];
			int remainder = totalItems % slots;
			Arrays.fill(split, totalItems / slots);
			while (remainder > 0){
				for (int i = 0; i < split.length; i++) {
					if(remainder > 0){
						split[i] +=1;
						remainder --;
					}
				}
			}

			List<Integer> slotEnvTyperubution = possibleSlots.stream()
					.mapToInt(value -> inventory.getInvStack(value).getCount())
					.boxed().collect(Collectors.toList());

			boolean needsBalance = false;
			for (int i = 0; i < split.length; i++) {
				int required = split[i];
				if(slotEnvTyperubution.contains(required)){
					//We need to remove the int, not at the int, this seems to work around that
					slotEnvTyperubution.remove(new Integer(required));
				} else {
					needsBalance = true;
				}
			}
			if (!needsBalance) {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}

		//Slot, count
		Pair<Integer, Integer> bestSlot = null;
		for (Integer slot : possibleSlots) {
			ItemStack slotStack = inventory.getInvStack(slot);
			if (slotStack.isEmpty()) {
				bestSlot = Pair.of(slot, 0);
			}
			if (bestSlot == null) {
				bestSlot = Pair.of(slot, slotStack.getCount());
			} else if (bestSlot.getRight() >= slotStack.getCount()) {
				bestSlot = Pair.of(slot, slotStack.getCount());
			}
		}
		if (bestSlot == null
				|| bestSlot.getLeft() == balanceSlot
				|| bestSlot.getRight() == sourceStack.getCount()
				|| inventory.getInvStack(bestSlot.getLeft()).isEmpty()
				|| !ItemUtils.isItemEqual(sourceStack, inventory.getInvStack(bestSlot.getLeft()), true, true)) {
			return Optional.empty();
		}
		sourceStack.decrement(1);
		inventory.getInvStack(bestSlot.getLeft()).increment(1);
		inventory.setChanged();

		return Optional.of(getCraftingInventory());
	}

	// Easyest way to sync back to the client
	public int getLockedInt() {
		return locked ? 1 : 0;
	}

	public void setLockedInt(int lockedInt) {
		locked = lockedInt == 1;
	}

	@Override
	public double getBaseMaxPower() {
		return TechRebornConfig.autoCraftingTableMaxEnergy;
	}

	@Override
	public double getBaseMaxOutput() {
		return 0;
	}

	@Override
	public double getBaseMaxInput() {
		return TechRebornConfig.autoCraftingTableMaxInput;
	}

	@Override
	public boolean canAcceptEnergy(Direction enumFacing) {
		return true;
	}

	@Override
	public boolean canProvideEnergy(Direction enumFacing) {
		return false;
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		tag.putBoolean("locked", locked);
		return super.toTag(tag);
	}

	@Override
	public void fromTag(CompoundTag tag) {
		if (tag.contains("locked")) {
			locked = tag.getBoolean("locked");
		}
		super.fromTag(tag);
	}

	// TileMachineBase
	@Override
	public boolean canBeUpgraded() {
		return false;
	}

	// This machine doesnt have a facing
	@Override
	public Direction getFacingEnum() {
		return Direction.NORTH;
	}

	// IToolDrop
	@Override
	public ItemStack getToolDrop(PlayerEntity playerIn) {
		return TRContent.Machine.AUTO_CRAFTING_TABLE.getStack();
	}

	// ItemHandlerProvider
	@Override
	public RebornInventory<AutoCraftingTableBlockEntity> getInventory() {
		return inventory;
	}

	// IContainerProvider
	@Override
	public BuiltContainer createContainer(int syncID, PlayerEntity player) {
		return new ContainerBuilder("autocraftingtable").player(player.inventory).inventory().hotbar().addInventory()
				.blockEntity(this).slot(0, 28, 25).slot(1, 46, 25).slot(2, 64, 25).slot(3, 28, 43).slot(4, 46, 43)
				.slot(5, 64, 43).slot(6, 28, 61).slot(7, 46, 61).slot(8, 64, 61).outputSlot(9, 145, 42)
				.outputSlot(10, 145, 70).syncEnergyValue().sync(this::getProgress, this::setProgress)
				.sync(this::getMaxProgress, this::setMaxProgress)
				.sync(this::getLockedInt, this::setLockedInt).addInventory().create(this, syncID);
	}

	@Override
	public boolean hasSlotConfig() {
		return true;
	}
}
