package fun.modoru.survival.mechanics.recipe;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class RecipesBootstrap {

    static final Recipes RECIPES = new Recipes("modoru")
            .shapeless("amethyst", new ItemStack(Material.AMETHYST_SHARD, 4), Material.AMETHYST_BLOCK)
            .shaped("gilded_blackstone", new ItemStack(Material.GILDED_BLACKSTONE), shaped -> {
                shaped.shape("AB", "BA");
                shaped.setIngredient('A', Material.GOLD_NUGGET);
                shaped.setIngredient('B', Material.BLACKSTONE);
            })
            .blasting("blackstone", new ItemStack(Material.BLACKSTONE), new RecipeChoice.MaterialChoice(List.of(Material.COBBLESTONE)), 0, 200)
            .stonecutter("sand", new ItemStack(Material.SAND), new RecipeChoice.MaterialChoice(List.of(Material.GRAVEL)))
            .stonecutter("gravel", new ItemStack(Material.GRAVEL), new RecipeChoice.MaterialChoice(List.of(Material.COBBLESTONE)))
            .shaped("calcite", new ItemStack(Material.CALCITE, 2), shaped -> {
                shaped.shape("AB", "BA");
                shaped.setIngredient('A', Material.COBBLESTONE);
                shaped.setIngredient('B', Material.BONE_MEAL);
            });

    private RecipesBootstrap() {}

    public static void bootstrap(Consumer<Recipe> recipeRegistrar) {
        RECIPES.recipes.forEach(recipeRegistrar);
    }

    public static final class Recipes {

        final String namespace;

        final Set<NamespacedKey> recipesKeys;
        final Set<Recipe> recipes;

        public Recipes(String namespace) {
            this.namespace = namespace;
            this.recipesKeys = new HashSet<>();
            this.recipes = new HashSet<>();
        }

        private <R extends Keyed & Recipe> Recipes add(R recipe) {
            recipesKeys.add(recipe.getKey());
            recipes.add(recipe);
            return this;
        }

        public Recipes shaped(String key, ItemStack result, Consumer<ShapedRecipe> configurator) {
            ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(namespace, key), result);
            configurator.accept(shapedRecipe);
            return add(shapedRecipe);
        }

        public Recipes stonecutter(String key, ItemStack result, RecipeChoice choice) {
            return add(new StonecuttingRecipe(new NamespacedKey(namespace, key), result, choice));
        }

        public Recipes shapeless(String key, ItemStack result, Material... ingredients) {
            ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(namespace, key), result);
            for (Material ingredient : ingredients) {
                recipe.addIngredient(ingredient);
            }
            return add(recipe);
        }

        public Recipes shapeless(String key, ItemStack result, ItemStack... ingredients) {
            ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(namespace, key), result);
            for (ItemStack ingredient : ingredients) {
                recipe.addIngredient(ingredient);
            }
            return add(recipe);
        }

        public Recipes blasting(String key, ItemStack result, RecipeChoice input, float experience, int cookingTime) {
            return add(new BlastingRecipe(new NamespacedKey(namespace, key), result, input, experience, cookingTime));
        }

        public Recipes furnace(String key, ItemStack result, RecipeChoice input, float experience, int cookingTime) {
            return add(new FurnaceRecipe(new NamespacedKey(namespace, key), result, input, experience, cookingTime));
        }


    }

}
