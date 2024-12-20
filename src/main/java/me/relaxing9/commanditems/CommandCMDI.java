package me.relaxing9.commanditems;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Maps;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import me.relaxing9.commanditems.data.ItemDefinition;
import me.relaxing9.commanditems.data.action.ActionMathExpr;

@CommandAlias("cmdi")
public class CommandCMDI extends BaseCommand {

    private CommandItems plugin;

    public CommandCMDI(CommandItems plugin) {
        this.plugin = plugin;
    }

    @Default
    public void onDefault(CommandSender issuer) {
        issuer.sendMessage(ChatColor.AQUA + "Running " + ChatColor.GOLD + "CommandItems v" + this.plugin.getDescription().getVersion()
                + ChatColor.AQUA + " by " + ChatColor.GOLD + "Yamakaja"+ ChatColor.AQUA + " & " + ChatColor.GOLD + "Relaxing9" + ChatColor.AQUA + "!");
        issuer.sendMessage(ChatColor.AQUA + "See " + ChatColor.GOLD + "/cmdi help" + ChatColor.AQUA + " for more information!");
    }

    @CommandPermission("cmdi.help")
    @Syntax("[page]")
    @HelpCommand
    public void onHelp(@Default("1") Integer page, CommandHelp help) {
        help.setPage(page);
        help.showHelp();
    }

    @Subcommand("give")
    @CommandPermission("cmdi.give")
    @Syntax("<player> <item> [amount] [KEY=VAL]...")
    @CommandCompletion("@players @itemdefs @nothing @itemparams")
    public void onGive(CommandSender issuer, OnlinePlayer player, ItemDefinition definition, @Default("1") Integer amount, String... params) {
        Map<String, String> paramMap = Maps.newHashMap();

        for (String param : params) {
            String[] split = param.split("=");

            if (split.length != 2) {
                issuer.sendMessage(ChatColor.RED + "Parameter need to be of the form KEY=VAL");
                return;
            }

            paramMap.put(split[0], split[1]);
        }

        ItemStack item = definition.getItem(plugin, paramMap);

        item.setAmount(amount);
        Map<Integer, ItemStack> leftovers = player.player.getInventory().addItem(item);

        for (ItemStack itemStack : leftovers.values())
            player.getPlayer().getWorld().dropItem(player.getPlayer().getLocation(), itemStack);

            issuer.sendMessage(ChatColor.GREEN + "Successfully gave " + player.player.getName() + " " + amount + "x " + item.getItemMeta().getDisplayName() + ChatColor.GREEN + "!");
    }

    @Subcommand("reload")
    @CommandPermission("cmdi.reload")
    public void onReload(CommandSender sender) {
        try {
            this.plugin.getConfigManager().parse();
            sender.sendMessage(ChatColor.GREEN + "Successfully reloaded config!");
        } catch (RuntimeException e) {
            sender.sendMessage(ChatColor.RED + "Failed to read the configuration:");
            sender.sendMessage(ChatColor.RED + e.getCause().getMessage());
        }
    }

    @Subcommand("inspect")
    @CommandPermission("cmdi.inspect")
    public void onInspect(Player player) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        if (itemInMainHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Please hold a command item in your main hand that you want to inspect!");
            return;
        }

        String command = plugin.getCommandItemManager().getCommandName(itemInMainHand);
        if (command == null) {
            player.sendMessage(ChatColor.RED + "This is not a command item!");
            return;
        }

        Map<String, String> params = plugin.getCommandItemManager().getParams(itemInMainHand);

        player.sendMessage(ChatColor.AQUA + "===========================");
        player.sendMessage(ChatColor.AQUA + "  Command: " + ChatColor.GOLD + command);
        player.sendMessage(ChatColor.AQUA + "  Parameters:");

        for (Map.Entry<String, String> entry : params.entrySet())
            player.sendMessage(ChatColor.AQUA + "  - " + ChatColor.GOLD + entry.getKey() + ChatColor.AQUA
                    + " = " + ChatColor.GOLD + entry.getValue());

        player.sendMessage(ChatColor.AQUA + "  Execution Trace:");

        ItemDefinition itemDefinition = this.plugin.getConfigManager().getConfig().getItems().get(command);

        if (itemDefinition == null)
            player.sendMessage(ChatColor.AQUA + "  - " + ChatColor.RED + "This item has been disabled!");
        else {
            List<ItemDefinition.ExecutionTrace> trace = itemDefinition.getExecutionTrace();

            for (ItemDefinition.ExecutionTrace item : trace)
                player.sendMessage(ChatColor.AQUA + getDepthPrefix(item.depth)
                                    + ChatColor.GOLD + item.label);
        }

        player.sendMessage(ChatColor.AQUA + "===========================");
    }

    private static String getDepthPrefix(int depth) {
        StringBuilder builder = new StringBuilder();
        builder.append("  ");

        for (int i = 0; i < depth; i++)
            builder.append("| ");

        builder.append("|-");
        return builder.toString();
    }

    @Subcommand("calc")
    @Syntax("<expression> [<VAR>=<VAL>]...")
    @CommandPermission("cmdi.math")
    public void onCalc(CommandSender sender, String expression, String... args) {
        ActionMathExpr.Expression ast;
        try {
            ast = ActionMathExpr.parse(expression);
        } catch (RuntimeException e) {
            sender.sendMessage(ChatColor.RED + "Invalid expression: " + e.getMessage());
            return;
        }
        Map<String, Double> params = new HashMap<>();

        for (String arg : args) {
            String[] split = arg.split("=");

            if (split.length != 2) {
                sender.sendMessage(ChatColor.RED + "Invalid parameter description, should be <VAR>=<VAL>");
                return;
            }

            double x;
            try {
                x = Double.parseDouble(split[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid parameter description, <VAL> should be a number");
                return;
            }

            params.put(split[0], x);
        }

        try {
            sender.sendMessage(ChatColor.GREEN + expression + ChatColor.GRAY + " -> " + ChatColor.GREEN + ast.eval(params));
        } catch (RuntimeException e) {
            sender.sendMessage(ChatColor.RED + "Evaluation failed: " + e.getMessage());
        }

    }

}
