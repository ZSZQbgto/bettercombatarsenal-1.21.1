package com.zszq.neonarsenal.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.zszq.neonarsenal.NeonArsenal;
import com.zszq.neonarsenal.item.Forge;
import com.zszq.neonarsenal.registry.ModDataComponents;
import com.zszq.neonarsenal.registry.ModItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.recipe.RecipeType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** JEI plugin: shows every anvil forging recipe (effects, switches and dyes) of the arsenal weapons. */
@JeiPlugin
public final class NeonArsenalJeiPlugin implements IModPlugin {

    public static final RecipeType<ForgingRecipe> FORGING =
            RecipeType.create(NeonArsenal.MODID, "forging", ForgingRecipe.class);

    public NeonArsenalJeiPlugin() {
        NeonArsenal.LOGGER.info("Arsenal JEI plugin loaded");
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(NeonArsenal.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            return;
        }

        registration.addRecipeCategories(new ForgingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            return;
        }

        java.util.List<ForgingRecipe> recipes = buildRecipes();
        registration.addRecipes(FORGING, recipes);
        NeonArsenal.LOGGER.info("JEI: registered {} forging recipes", recipes.size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            return;
        }

        registration.addRecipeCatalysts(FORGING, Items.ANVIL);
    }

    private static List<ForgingRecipe> buildRecipes() {
        List<ForgingRecipe> recipes = new ArrayList<>();
        for (var holder : ModItems.all()) {
            Item item = holder.get();
            ItemStack base = new ItemStack(item);

            // Effect layers.
            addLayer(recipes, base, Items.CHORUS_FRUIT, Forge.CHORUS, 8);
            addLayer(recipes, base, Items.CREEPER_HEAD, Forge.CREEPER, 8);
            addLayer(recipes, base, Items.RABBIT_FOOT, Forge.RABBIT, 8);
            addLayer(recipes, base, Items.ECHO_SHARD, Forge.ECHO, 8);

            // Basic switches.
            ItemStack glow = base.copy();
            glow.set(ModDataComponents.GLOW_FORGE.get(), 1);
            recipes.add(new ForgingRecipe(base, new ItemStack(Items.GLOW_INK_SAC), glow, 1));
            ItemStack particles = base.copy();
            particles.set(ModDataComponents.PARTICLE_FORGE.get(), 1);
            recipes.add(new ForgingRecipe(base, new ItemStack(Items.BLAZE_POWDER), particles, 1));

            // Dyes (unlimited, blended in the anvil).
            for (DyeColor color : DyeColor.values()) {
                ItemStack dyed = base.copy();
                dyed.set(ModDataComponents.DYE.get(), color.getTextureDiffuseColor() & 0xFFFFFF);
                recipes.add(new ForgingRecipe(base, new ItemStack(DyeItem.byColor(color)), dyed, 1));
            }
        }
        return recipes;
    }

    private static void addLayer(List<ForgingRecipe> recipes, ItemStack base, Item material, String layer, int xp) {
        ItemStack once = base.copy();
        Forge.add(once, layer);
        recipes.add(new ForgingRecipe(base, new ItemStack(material), once, xp));
        // The same material can be forged a second time: that copy unlocks the material's hit effect.
        ItemStack twice = once.copy();
        Forge.add(twice, layer);
        recipes.add(new ForgingRecipe(once, new ItemStack(material), twice, xp));
    }

}
