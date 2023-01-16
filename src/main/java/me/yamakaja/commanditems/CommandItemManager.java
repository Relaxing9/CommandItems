package me.yamakaja.commanditems;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import me.yamakaja.commanditems.data.ItemDefinition;
import me.yamakaja.commanditems.util.CommandItemsI18N.MsgKey;
import me.yamakaja.commanditems.util.NMSUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class CommandItemManager implements Listener {

    private CommandItems plugin;
    private int iterator = 0;

    private Table<UUID, String, Long> lastUse = HashBasedTable.create();

    public CommandItemManager(CommandItems plugin) {
        this.plugin = plugin;

        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    private static String getTimeString(long d) {
        long duration = d;
        int seconds = (int) (duration % 60);
        duration /= 60;
        int minutes = (int) (duration % 60);
        duration /= 60;
        int hours = (int) (duration % 60);
        duration /= 24;
        int days = (int) duration;

        StringBuilder builder = new StringBuilder();

        if (days != 0) {
            builder.append(days);
            builder.append('d');
        }

        if (hours != 0) {
            if (builder.length() > 0)
                builder.append(' ');

            builder.append(hours);
            builder.append('h');
        }

        if (minutes != 0) {
            if (builder.length() > 0)
                builder.append(' ');

            builder.append(minutes);
            builder.append('m');
        }

        if (seconds != 0) {
            if (builder.length() > 0)
                builder.append(' ');

            builder.append(seconds);
            builder.append('s');
        }

        return builder.toString();
    }

    private boolean checkCooldown(Player player, String command, long duration) {
        long lastUse = 0;
        if (this.lastUse.contains(player.getUniqueId(), command))
            lastUse = this.lastUse.get(player.getUniqueId(), command);

        if (System.currentTimeMillis() < lastUse + duration * 1000)
            return false;

        this.lastUse.put(player.getUniqueId(), command, System.currentTimeMillis());
        return true;
    }

    private long getSecondsUntilNextUse(Player player, String command, long duration) {
        long lastUse = 0;
        if (this.lastUse.contains(player.getUniqueId(), command))
            lastUse = this.lastUse.get(player.getUniqueId(), command);

        long difference = lastUse + duration * 1000 - System.currentTimeMillis();
        if (difference < 0)
            return 0;

        return (long) Math.ceil(difference / 1000.0);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!isValidInteraction(event)) {
            return;
        }

        ItemMeta itemMeta = event.getItem().getItemMeta();
        String command = NMSUtil.getNBTString(itemMeta, "command");
        if (event.getItem() == null || itemMeta == null || command == null) {
            return;
        }
    
        ItemDefinition itemDefinition = this.plugin.getConfigManager().getConfig().getItems().get(command);
        event.setCancelled(true);
    
        if (!isValidPlayer(event.getPlayer(), itemDefinition, command)) {
            return;
        }
    
        Map<String, String> params = NMSUtil.getNBTStringMap(event.getItem().getItemMeta(), "params");
    
        if (itemDefinition.isConsumed()) {
            ItemStack contents = runConsume(event);
            event.getPlayer().getInventory().setItem(getIter(), contents);
        }
    
        try {
            this.plugin.getExecutor().processInteraction(event.getPlayer(), itemDefinition, params);
        } catch (RuntimeException e) {
            CommandItems.logger.log(Level.SEVERE, "Failed to process command item: " + command);
            event.getPlayer().sendMessage(MsgKey.ITEM_ERROR.get());
            e.printStackTrace();
        }
    }
    
    private boolean isValidInteraction(PlayerInteractEvent event) {
        return event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }
    
    private boolean isValidPlayer(Player player, ItemDefinition itemDefinition, String command) {
        if (itemDefinition == null) {
            player.sendMessage(MsgKey.ITEM_DISABLED.get());
            return false;
        }
    
        if (itemDefinition.isSneaking() && !player.isSneaking()) {
            return false;
        }
    
        if (!player.hasPermission("cmdi.item." + command)) {
            player.sendMessage(MsgKey.ITEM_NOPERMISSION.get());
            return false;
        }
    
        if (!checkCooldown(player, command, itemDefinition.getCooldown())) {
            Map<String, String> params = Maps.newHashMap();
            params.put("TIME_PERIOD", getTimeString(itemDefinition.getCooldown()));
            params.put("TIME_REMAINING", getTimeString(getSecondsUntilNextUse(player, command, itemDefinition.getCooldown())));
            player.sendMessage(MsgKey.ITEM_COOLDOWN.get(params));
            return false;
        }
        return true;
    }

    public ItemStack runConsume(PlayerInteractEvent event) {
        ItemStack[] contents = event.getPlayer().getInventory().getContents();
        int i;
        for (i = 0; i < contents.length; i++)
            if (contents[i] != null && contents[i].isSimilar(event.getItem())) {
                int amount = contents[i].getAmount();
                if (amount == 1)
                    contents[i] = null;
                else
                    contents[i].setAmount(amount - 1);
                break;
            }
        setIter(i);
        return contents[i];
    }

    public void setIter(int i) { this.iterator = i; }
    public int getIter() { return this.iterator; }

}
