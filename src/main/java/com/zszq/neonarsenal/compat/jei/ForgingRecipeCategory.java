package com.zszq.neonarsenal.compat.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** JEI category that documents every anvil forging operation of the mod. */
public final class ForgingRecipeCategory implements IRecipeCategory<ForgingRecipe> {

    private final IGuiHelper guiHelper;

    public ForgingRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
    }

    @Override
    public RecipeType<ForgingRecipe> getRecipeType() {
        return NeonArsenalJeiPlugin.FORGING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.bettercombat_neonarsenal.forging");
    }

    @Override
    public IDrawable getIcon() {
        return this.guiHelper.createDrawableItemStack(new ItemStack(Items.ANVIL));
    }

    @Override
    public int getWidth() {
        return 86;
    }

    @Override
    public int getHeight() {
        return 26;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ForgingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 4).setStandardSlotBackground().addItemStack(recipe.weapon());
        builder.addInputSlot(19, 4).setStandardSlotBackground().addItemStack(recipe.material());
        builder.addOutputSlot(65, 4).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(ForgingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        this.guiHelper.getRecipeArrow().draw(guiGraphics, 40, 5);
    }

    @Override
    public List<Component> getTooltipStrings(ForgingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (mouseX >= 40 && mouseX <= 60 && mouseY >= 5 && mouseY <= 21) {
            return List.of(Component.translatable("jei.bettercombat_neonarsenal.forging.xp", recipe.xp()));
        }
        return List.of();
    }
}
