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

package techreborn.client.gui;

import net.minecraft.entity.player.PlayerEntity;
import reborncore.client.containerBuilder.builder.BuiltContainer;
import reborncore.client.gui.builder.GuiBase;
import techreborn.blockentity.machine.tier3.QuantumChestBlockEntity;

public class GuiQuantumChest extends GuiBase<BuiltContainer> {

	QuantumChestBlockEntity quantumChest;

	public GuiQuantumChest(int syncID, final PlayerEntity player, final QuantumChestBlockEntity quantumChest) {
		super(player, quantumChest, quantumChest.createContainer(syncID, player));
		this.quantumChest = quantumChest;
	}

	@Override
	protected void drawBackground(final float f, final int mouseX, final int mouseY) {
		super.drawBackground(f, mouseX, mouseY);
		final Layer layer = Layer.BACKGROUND;

		drawSlot(80, 24, layer);
		drawSlot(80, 64, layer);
	}

	@Override
	protected void drawForeground(final int mouseX, final int mouseY) {
		super.drawForeground(mouseX, mouseY);
		final Layer layer = Layer.FOREGROUND;

		if (!this.quantumChest.storedItem.isEmpty() && !this.quantumChest.inventory.getInvStack(1).isEmpty()) {
			this.builder.drawBigBlueBar(this, 31, 43, this.quantumChest.storedItem.getCount() + this.quantumChest.inventory.getInvStack(1).getCount(), this.quantumChest.maxCapacity, mouseX - this.x, mouseY - this.y, "Stored", layer);
		}
		if (this.quantumChest.storedItem.isEmpty() && !this.quantumChest.inventory.getInvStack(1).isEmpty()) {
			this.builder.drawBigBlueBar(this, 31, 43, this.quantumChest.inventory.getInvStack(1).getCount(), this.quantumChest.maxCapacity, mouseX - this.x, mouseY - this.y, "Stored", layer);
		}
	}
}
