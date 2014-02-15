package me.libraryaddict.Limit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class InteractionListener implements Listener {
    private JavaPlugin plugin;
    private String creativeMessage;
    private List<String> disallowedWorlds;
    private HashMap<Block, String> markedBlocks = new HashMap<Block, String>();

    public void saveBlocks() {
        File file = new File(plugin.getDataFolder(), "blocks.yml");
        if (file.exists())
            file.delete();
        try {
            file.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (Block block : markedBlocks.keySet()) {
                config.set(block.getWorld().getName() + "." + block.getX() + "." + block.getY() + "." + block.getZ(),
                        markedBlocks.get(block));
            }
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        if (this.isCreativeItem(event.getSource())
                || isCreativeItem(((Furnace) event.getBlock().getState()).getInventory().getFuel())) {
            event.setCancelled(true);
        }
    }

    public void loadBlocks() {
        File file = new File(plugin.getDataFolder(), "blocks.yml");
        if (file.exists()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String worldName : config.getKeys(false)) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        for (String x : config.getConfigurationSection(worldName).getKeys(false)) {
                            for (String y : config.getConfigurationSection(worldName + "." + x).getKeys(false)) {
                                for (String z : config.getConfigurationSection(worldName + "." + x + "." + y).getKeys(false)) {
                                    Block block = world.getBlockAt(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z));
                                    markedBlocks.put(block, config.getString(worldName + "." + x + "." + y + "." + z));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public InteractionListener(JavaPlugin plugin) {
        this.plugin = plugin;
        creativeMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("ItemMessage"));
        disallowedWorlds = getConfig().getStringList("WorldsDisabled");
    }

    private boolean checkEntity(Entity entity) {
        if (getConfig().getBoolean("PreventUsage") && entity != null && entity instanceof Player
                && ((Player) entity).getGameMode() != GameMode.CREATIVE && isCreativeItem(((Player) entity).getItemInHand()))
            return true;
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled() || disallowedWorlds.contains(event.getBlock().getWorld().getName()))
            return;
        if (getConfig().getBoolean("MarkBlocks")
                && (isCreativeItem(event.getItemInHand()) || event.getPlayer().getGameMode() == GameMode.CREATIVE)) {
            markedBlocks.put(event.getBlockPlaced(), getCreativeString(event.getItemInHand()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPush(BlockPistonExtendEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName()))
            return;
        for (Block block : event.getBlocks()) {
            if (markedBlocks.containsKey(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRetract(BlockPistonRetractEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName()))
            return;
        if (event.isSticky()) {
            Block block = event.getBlock().getRelative(event.getDirection()).getRelative(event.getDirection());
            if (markedBlocks.containsKey(block)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExplode(EntityExplodeEvent event) {
        if (disallowedWorlds.contains(event.getLocation().getWorld().getName()))
            return;
        for (Block block : event.blockList()) {
            if (markedBlocks.containsKey(block)) {
                String message = markedBlocks.remove(block);
                for (ItemStack item : block.getDrops()) {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = new ArrayList<String>();
                    if (meta.hasLore())
                        lore = meta.getLore();
                    lore.add(0, message);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    block.getWorld().dropItemNaturally(block.getLocation().clone().add(0.5, 0, 0.5), item);
                }
                block.setType(Material.AIR);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled() || disallowedWorlds.contains(event.getBlock().getWorld().getName()))
            return;
        if (markedBlocks.containsKey(event.getBlock())) {
            String message = markedBlocks.remove(event.getBlock());
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setExpToDrop(0);
                Collection<ItemStack> drops = event.getBlock().getDrops(event.getPlayer().getItemInHand());
                for (ItemStack item : drops) {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = new ArrayList<String>();
                    if (meta.hasLore())
                        lore = meta.getLore();
                    lore.add(0, message);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().clone().add(0.5, 0, 0.5), item);
                }
                event.getBlock().setType(Material.AIR);
            }
        }
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    private boolean isCreativeItem(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasLore()) {
                for (String s : meta.getLore()) {
                    if (s.startsWith(creativeMessage.replace("%Name%", "")))
                        return true;
                }
            }
        }
        return false;
    }

    private String getCreativeString(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasLore()) {
                for (String s : meta.getLore()) {
                    if (s.startsWith(creativeMessage.replace("%Name%", "")))
                        return s;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (disallowedWorlds.contains(event.getEntity().getWorld().getName()))
            return;
        if (checkEntity(event.getDamager()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (disallowedWorlds.contains(event.getEntity().getWorld().getName()))
            return;
        if (getConfig().getBoolean("PreventArmor")) {
            if (event.getEntity() instanceof Player && ((Player) event.getEntity()).getGameMode() != GameMode.CREATIVE) {
                ItemStack[] items = ((Player) event.getEntity()).getInventory().getArmorContents();
                for (int i = 0; i < 4; i++) {
                    ItemStack item = items[i];
                    if (isCreativeItem(item)) {
                        items[i] = new ItemStack(0);
                        HashMap<Integer, ItemStack> leftovers = ((Player) event.getEntity()).getInventory().addItem(item);
                        for (ItemStack leftoverItem : leftovers.values()) {
                            ((Player) event.getEntity()).getWorld().dropItem(((Player) event.getEntity()).getEyeLocation(),
                                    leftoverItem);
                        }
                    }
                }
                ((HumanEntity) event.getEntity()).getInventory().setArmorContents(items);
            }
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getViewers().isEmpty() || disallowedWorlds.contains(event.getViewers().get(0).getWorld().getName()))
            return;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (isCreativeItem(item)) {
                if (event.getViewers().get(0).getGameMode() != GameMode.CREATIVE && getConfig().getBoolean("PreventCrafting")) {
                    event.getInventory().setItem(0, new ItemStack(0, 0));
                } else if (getConfig().getBoolean("RenameCrafting"))
                    setCreativeItem(event.getViewers().get(0).getName(), event.getInventory().getItem(0));
                break;
            }
        }
    }

    @EventHandler
    public void onCreativeClick(InventoryCreativeEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName()))
            return;
        event.setCursor(setCreativeItem(event.getWhoClicked().getName(), event.getCursor()));
    }

    @EventHandler
    public void onGameModeSwitch(PlayerGameModeChangeEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName()))
            return;
        if (getConfig().getBoolean("PreventArmor") && event.getPlayer().getGameMode() == GameMode.CREATIVE
                && event.getNewGameMode() != GameMode.CREATIVE) {
            ItemStack[] items = event.getPlayer().getInventory().getArmorContents();
            for (int i = 0; i < 4; i++) {
                ItemStack item = items[i];
                if (isCreativeItem(item)) {
                    items[i] = new ItemStack(0);
                    HashMap<Integer, ItemStack> leftovers = event.getPlayer().getInventory().addItem(item);
                    for (ItemStack leftoverItem : leftovers.values()) {
                        event.getPlayer().getWorld().dropItem(event.getPlayer().getEyeLocation(), leftoverItem);
                    }
                }
            }
            event.getPlayer().getInventory().setArmorContents(items);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName()))
            return;
        if (checkEntity(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName()))
            return;
        if (checkEntity(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName()))
            return;
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE && event.getInventory().getType() == InventoryType.ANVIL
                && isCreativeItem(event.getCurrentItem())) {
            if (getConfig().getBoolean("PreventAnvil"))
                event.setCancelled(true);
        }
    }

    private ItemStack setCreativeItem(String who, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.getType() != Material.BOOK_AND_QUILL) {
            if (!isCreativeItem(item)) {
                ItemMeta meta = item.getItemMeta();
                List<String> lore = new ArrayList<String>();
                if (meta.hasLore())
                    lore = meta.getLore();
                lore.add(0, creativeMessage.replace("%Name%", who));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

}