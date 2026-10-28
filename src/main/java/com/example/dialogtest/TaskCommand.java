package com.example.dialogtest;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.type.MultiActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public final class TaskCommand implements CommandExecutor {

    private final DialogTestPlugin plugin;
    private final MiniMessage miniMessage;

    public TaskCommand(final @NotNull DialogTestPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by a player in-game.", NamedTextColor.RED));
            return true;
        }

        openTaskDialog(player);
        return true;
    }

    private void openTaskDialog(final @NotNull Player player) {
        final FileConfiguration config = this.plugin.getConfig();

        // 1. Title & Body Lines from config.yml
        final String rawTitle = config.getString("dialog.title", "<gold><bold>Task Selection</bold></gold>");
        final String rawLine1 = config.getString("dialog.body.line1", "Please select an operational task to perform:");
        final String rawLine2 = config.getString("dialog.body.line2", "Clicking any button will execute the selected action.");

        final Component titleComponent = parseComponent(rawTitle);
        final Component line1Component = parseComponent(rawLine1);
        final Component line2Component = parseComponent(rawLine2);

        final Dialog dialog = Dialog.create(factory -> {
            // Configure dialog base: title and exactly two lines of body text
            final DialogBase base = DialogBase.builder(titleComponent)
                    .canCloseWithEscape(true)
                    .pause(false)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .body(List.of(
                            DialogBody.plainMessage(line1Component),
                            DialogBody.plainMessage(line2Component)
                    ))
                    .inputs(List.of())
                    .build();

            final ClickCallback.Options callbackOptions = ClickCallback.Options.builder()
                    .uses(1)
                    .lifetime(Duration.ofMinutes(5))
                    .build();

            // 2. Button 1
            final String b1Label = config.getString("dialog.buttons.button1.label", "<green>Task Alpha</green>");
            final int b1Width = config.getInt("dialog.buttons.button1.width", 150);
            final String b1Message = config.getString("dialog.buttons.button1.message", "<green>[Task] You executed Task Alpha!</green>");
            final String b1Log = config.getString("dialog.buttons.button1.console_log", "clicked: Task Alpha");

            final ActionButton button1 = ActionButton.builder(parseComponent(b1Label))
                    .width(b1Width)
                    .action(DialogAction.customClick((response, audience) -> {
                        audience.sendMessage(parseComponent(b1Message));
                        final String playerName = (audience instanceof Player p) ? p.getName() : player.getName();
                        this.plugin.getLogger().info(playerName + " " + b1Log);
                    }, callbackOptions))
                    .build();

            // 3. Button 2
            final String b2Label = config.getString("dialog.buttons.button2.label", "<aqua>Task Beta</aqua>");
            final int b2Width = config.getInt("dialog.buttons.button2.width", 150);
            final String b2Message = config.getString("dialog.buttons.button2.message", "<aqua>[Task] You executed Task Beta!</aqua>");
            final String b2Log = config.getString("dialog.buttons.button2.console_log", "clicked: Task Beta");

            final ActionButton button2 = ActionButton.builder(parseComponent(b2Label))
                    .width(b2Width)
                    .action(DialogAction.customClick((response, audience) -> {
                        audience.sendMessage(parseComponent(b2Message));
                        final String playerName = (audience instanceof Player p) ? p.getName() : player.getName();
                        this.plugin.getLogger().info(playerName + " " + b2Log);
                    }, callbackOptions))
                    .build();

            // 4. Button 3
            final String b3Label = config.getString("dialog.buttons.button3.label", "<light_purple>Task Gamma</light_purple>");
            final int b3Width = config.getInt("dialog.buttons.button3.width", 150);
            final String b3Message = config.getString("dialog.buttons.button3.message", "<light_purple>[Task] You executed Task Gamma!</light_purple>");
            final String b3Log = config.getString("dialog.buttons.button3.console_log", "clicked: Task Gamma");

            final ActionButton button3 = ActionButton.builder(parseComponent(b3Label))
                    .width(b3Width)
                    .action(DialogAction.customClick((response, audience) -> {
                        audience.sendMessage(parseComponent(b3Message));
                        final String playerName = (audience instanceof Player p) ? p.getName() : player.getName();
                        this.plugin.getLogger().info(playerName + " " + b3Log);
                    }, callbackOptions))
                    .build();

            // Multi-action layout with three buttons
            final MultiActionType type = DialogType.multiAction(List.of(button1, button2, button3))
                    .columns(1)
                    .build();

            factory.empty()
                    .base(base)
                    .type(type);
        });

        // Open the dialog for the player
        player.showDialog(dialog);
    }

    private Component parseComponent(final @NotNull String text) {
        if (text.contains("<") && text.contains(">")) {
            return this.miniMessage.deserialize(text);
        }
        return Component.text(text);
    }
}
