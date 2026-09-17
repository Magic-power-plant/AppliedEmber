package com.pingsu.appliedember.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.pingsu.appliedember.AppliedEmber;
import com.rekindled.embers.recipe.IAlchemyRecipe;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Server commands for querying AppliedEmber content. Currently exposes the alchemy recipe
 * aspect lookup: given a recipe id, it prints what each pedestal input corresponds to which
 * aspectus (象征) for the current world seed.
 */
@Mod.EventBusSubscriber(modid = AppliedEmber.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AppliedEmberCommands {

    private AppliedEmberCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("appliedember")
                .then(Commands.literal("alchemy")
                        .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                .executes(ctx -> queryAlchemy(ctx.getSource(),
                                        ResourceLocationArgument.getId(ctx, "recipe"))))));
    }

    private static int queryAlchemy(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        RecipeManager recipeManager = source.getServer().getRecipeManager();
        var holder = recipeManager.byKey(id);
        if (holder.isEmpty() || !(holder.get() instanceof IAlchemyRecipe alchemy)) {
            throw new SimpleCommandExceptionType(
                    Component.literal(id + " 不是一个已加载的炼金配方 (embers:alchemy)")).create();
        }

        source.sendSystemMessage(Component.literal("炼金配方 " + id));
        source.sendSystemMessage(Component.literal("  核心 (tablet): ").append(describe(alchemy.getCenterInput())));
        source.sendSystemMessage(Component.literal("  产物: ").append(alchemy.getResultItem().getHoverName()));

        // Embers decides the pedestal->aspectus assignment from a seed; the tablet uses the world
        // seed, so querying with the same seed reproduces what players would actually get.
        long seed = source.getLevel().getSeed();
        List<Ingredient> code = alchemy.getCode(seed);
        List<Ingredient> inputs = alchemy.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Ingredient aspect = code.get(i % code.size());
            source.sendSystemMessage(Component.literal("  基座 " + (i + 1) + ": ")
                    .append(describe(inputs.get(i)))
                    .append(" -> 象征: ")
                    .append(describe(aspect)));
        }
        return inputs.size();
    }

    private static Component describe(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) {
            return Component.literal("∅");
        }
        MutableComponent text = Component.empty();
        for (int i = 0; i < stacks.length; i++) {
            if (i > 0) {
                text.append(Component.literal(", "));
            }
            text.append(stacks[i].getHoverName());
        }
        return text;
    }
}
