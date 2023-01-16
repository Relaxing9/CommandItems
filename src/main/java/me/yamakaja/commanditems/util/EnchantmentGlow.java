package me.yamakaja.commanditems.util;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.logging.Level;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;

import me.yamakaja.commanditems.CommandItems;

public class EnchantmentGlow extends EnchantmentWrapper {

    private static Enchantment glow;

    public EnchantmentGlow() {
        super("enchantment_glow");
    }
    
    @SuppressWarnings({"deprecation", "setAccessible"})
    public static Enchantment getGlow() {
        if (glow != null)
            return glow;
        else if ((glow = Enchantment.getByName("Glow")) != null)
            return glow;

        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
        } catch (InaccessibleObjectException e) {
            CommandItems.logger.log(Level.SEVERE, "Could not access the Glow enchantment field", e);
        } catch (Exception e) {
            e.printStackTrace();
        }

        glow = new EnchantmentGlow();
        Enchantment.registerEnchantment(glow);
        return glow;
    }

    public static void addGlow(ItemStack item) {
        item.addEnchantment(getGlow(), 1);
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        return true;
    }

    @Override
    public boolean conflictsWith(Enchantment other) {
        return false;
    }

    @Override
    public EnchantmentTarget getItemTarget() {
        return null;
    }

    @Override
    public int getMaxLevel() {
        return 10;
    }

    @Override
    public String getName() {
        return "Glow";
    }

    @Override
    public int getStartLevel() {
        return 1;
    }
}