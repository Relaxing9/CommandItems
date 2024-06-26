package me.relaxing9.commanditems.parser;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import me.relaxing9.commanditems.CommandItems;
import me.relaxing9.commanditems.data.CommandItemsConfig;
import me.relaxing9.commanditems.data.ItemDefinition;
import me.relaxing9.commanditems.data.action.Action;

/**
 * Created by Yamakaja on 26.05.18.
 */
public class ConfigManager {

    private CommandItems plugin;
    private CommandItemsConfig config;
    private YAMLMapper mapper;

    public ConfigManager(CommandItems plugin) {
        this.plugin = plugin;

        this.mapper = new YAMLMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ItemStack.class, new ItemStackDeserializer());
        mapper.registerModule(module);
    }

    public void parse() {
        try {
            this.config = mapper.readValue(new File(plugin.getDataFolder(), "config.yml"), CommandItemsConfig.class);
        } catch (IOException e) {
            CommandItems.logger.log(Level.SEVERE, "Failed to read config!", e);
        }

        for (Map.Entry<String, ItemDefinition> entry : this.config.getItems().entrySet()) {
            entry.getValue().setKey(entry.getKey());
            try {
                for (Action action : entry.getValue().getActions())
                    action.init();
            } catch (RuntimeException e) {
                plugin.getLogger().severe(ChatColor.RED + "Failed to initialize command item: " + entry.getKey());
                plugin.getLogger().severe(ChatColor.RED + e.getMessage());
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        }
    }

    public CommandItemsConfig getConfig() {
        return config;
    }

}
