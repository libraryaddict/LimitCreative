package me.libraryaddict.Limit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Creative extends JavaPlugin implements Listener {
   private InteractionListener listener;

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
      if (cmd.getName().equalsIgnoreCase("clearlore")) {
         if (sender.getName().equals("CONSOLE")) {
            sender.sendMessage(ChatColor.RED + "Shove off console");
            return true;
         }
         if (sender.hasPermission("limitcreative.clearlore")) {
            final String creativeMessage = ChatColor.translateAlternateColorCodes('&',
                  getConfig().getString("ItemMessage")).replace("%Name%", "");
            final ItemStack item = ((Player) sender).getItemInHand();
            if (item != null && item.getType() != Material.AIR) {
               boolean removed = false;
               if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                  final ItemMeta meta = item.getItemMeta();
                  final Iterator<String> itel = meta.getLore().iterator();
                  final List<String> lore = new ArrayList<String>();
                  while (itel.hasNext()) {
                     final String s = itel.next();
                     if (s.startsWith(creativeMessage)) {
                        removed = true;
                     } else {
                        lore.add(s);
                     }
                  }
                  meta.setLore(lore);
                  item.setItemMeta(meta);
               }
               if (!removed) {
                  sender.sendMessage(ChatColor.RED + "Didn't find the creative message on the item!");
               } else {
                  sender.sendMessage(ChatColor.RED + "Removed the creative lore from the item!");
               }
            } else {
               sender.sendMessage(ChatColor.RED + "You are not holding a item!");
            }
         } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command");
         }
      } else if (cmd.getName().equalsIgnoreCase("limitcreativeconvert")) {
         if (sender.hasPermission("limitcreative.convert")) {
            sender.sendMessage(ChatColor.RED + "Now converting flatfile to mysql.. You may see lag");
            StorageApi.loadBlocksFromFlatfile();
            StorageApi.saveBlocksToMysql();
            sender.sendMessage(ChatColor.RED + "Converted!");
         }
      }
      return true;
   }

   @Override
   public void onEnable() {
      saveDefaultConfig();
      listener = new InteractionListener(this);
      Bukkit.getPluginManager().registerEvents(listener, this);
      StorageApi.setMainPlugin(this);
      if (getConfig().getBoolean("SaveBlocks")) {
         Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
               if (getConfig().getBoolean("UseMysql")) {
                  StorageApi.setMysqlDetails(getConfig().getString("MysqlUsername"),
                        getConfig().getString("MysqlPassword"), getConfig().getString("MysqlHost"), getConfig()
                              .getString("MysqlDatabase"));
                  StorageApi.loadBlocksFromMysql();
               } else {
                  StorageApi.loadBlocksFromFlatfile();
               }
            }
         }, 10);
      }
   }

}
