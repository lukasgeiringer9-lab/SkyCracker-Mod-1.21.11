package de.skycracker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Befehle (für alle Spieler, keine Cheats nötig):
 *   /skycracker          holt einen SkyCracker zu dir
 *   /skycracker remove   entfernt alle SkyCracker in deiner Dimension
 *   /skycracker ruhe     schaltet das Anschreien an oder aus
 *   /skycracker schrei   lässt die SkyCracker in deiner Nähe sofort schreien
 */
public final class SkyCrackerCommands {
    private static final int MAX_SKYCRACKERS = 8;

    private SkyCrackerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skycracker")
                .executes(context -> spawn(context.getSource()))
                .then(Commands.literal("remove")
                        .executes(context -> remove(context.getSource())))
                .then(Commands.literal("ruhe")
                        .executes(context -> toggleMute(context.getSource())))
                .then(Commands.literal("schrei")
                        .executes(context -> shout(context.getSource()))));
    }

    private static int spawn(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();

        if (SkyCrackerEntity.countIn(level) >= MAX_SKYCRACKERS) {
            source.sendFailure(Component.literal("Es gibt schon genug SkyCracker! Nutze /skycracker remove."));
            return 0;
        }

        SkyCrackerEntity entity = SkyCrackerMod.SKYCRACKER.spawn(
                level, player.blockPosition(), EntitySpawnReason.COMMAND);
        if (entity == null) {
            source.sendFailure(Component.literal("Der SkyCracker konnte nicht gespawnt werden."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Ein SkyCracker ist jetzt bei dir!"), false);
        return 1;
    }

    private static int remove(CommandSourceStack source) {
        int removed = SkyCrackerEntity.removeAll(source.getLevel());
        source.sendSuccess(() -> Component.literal(removed + " SkyCracker entfernt."), false);
        return removed;
    }

    private static int toggleMute(CommandSourceStack source) {
        SkyCrackerEntity.muted = !SkyCrackerEntity.muted;
        String text = SkyCrackerEntity.muted
                ? "Die SkyCracker sind jetzt still."
                : "Die SkyCracker dürfen wieder schreien!";
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int shout(CommandSourceStack source) {
        int count = SkyCrackerEntity.shoutAll(source.getLevel());
        if (count == 0) {
            source.sendFailure(Component.literal("Kein SkyCracker in deiner Nähe."));
            return 0;
        }
        return count;
    }
}
