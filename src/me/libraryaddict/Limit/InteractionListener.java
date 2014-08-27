package me.libraryaddict.Limit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Objects;

public class InteractionListener implements Listener {
    private final String creativeMessage;
    private final String DisabledItemsMessage;
    private final ArrayList<Material> disallowedItems = new ArrayList<Material>();
    private final List<String> disallowedWorlds;
    private final JavaPlugin plugin;
    private final boolean PreventUsage;
    private final boolean MarkBlocks;
    private final boolean PreventCrafting;
    private final boolean RenameCrafting;
    private final boolean PreventAdventureMode;
    private final boolean PreventArmor;
    private final boolean PreventAnvil;
    private final boolean PreventPuttingNonPlayerInv;
    private final boolean PreventTakingNonPlayerInv;
    private final boolean PreventHopper;
    private final boolean PreventSurvivalPickup;
    private final boolean PreventCreativePickup;
    private final boolean PreventSurvivalDrop;
    private boolean PreventCreativeDrop;

    public InteractionListener(JavaPlugin plugin) {
        this.plugin = plugin;
        creativeMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("ItemMessage"));
        DisabledItemsMessage = ChatColor.translateAlternateColorCodes('&', getConfig()
                .getString("DisabledItemsMessage"));
        disallowedWorlds = getConfig().getStringList("WorldsDisabled");
        PreventUsage = getConfig().getBoolean("PreventUsage");
        MarkBlocks = getConfig().getBoolean("MarkBlocks");
        PreventCrafting = getConfig().getBoolean("PreventCrafting");
        RenameCrafting = getConfig().getBoolean("RenameCrafting");
        PreventAdventureMode = getConfig().getBoolean("PreventAdventureMode");
        PreventArmor = getConfig().getBoolean("PreventArmor");
        PreventAnvil = getConfig().getBoolean("PreventAnvil");
        PreventPuttingNonPlayerInv = getConfig().getBoolean("PreventPuttingNonPlayerInv");
        PreventTakingNonPlayerInv = getConfig().getBoolean("PreventTakingNonPlayerInv");
        PreventHopper = getConfig().getBoolean("PreventHopper");
        PreventSurvivalPickup = getConfig().getBoolean("PreventSurvivalPickup");
        PreventCreativePickup = getConfig().getBoolean("PreventCreativePickup");
        PreventSurvivalDrop = getConfig().getBoolean("PreventSurvivalDrop");

        for (final String disallowed : getConfig().getStringList("DisabledItems")) {
            try {
                disallowedItems.add(Material.valueOf(disallowed.toUpperCase()));
            } catch (final Exception ex) {
                try {
                    disallowedItems.add(Material.getMaterial(Integer.parseInt(disallowed)));
                } catch (final Exception e) {
                    System.out.print("[LimitCreative] Cannot parse " + disallowed + " to a valid material");
                }
            }
        }
    }

    private boolean checkEntity(Entity entity) {
        if (PreventUsage && entity != null && entity instanceof Player
                && ((Player) entity).getGameMode() != GameMode.CREATIVE
                && isCreativeItem(((Player) entity).getItemInHand())) {
            return true;
        }
        return false;
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    private String getCreativeString(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            final ItemMeta meta = item.getItemMeta();
            if (meta.hasLore()) {
                for (final String s : meta.getLore()) {
                    if (s.startsWith(creativeMessage.replace("%Name%", ""))) {
                        return s;
                    }
                }
            }
        }
        return null;
    }

    private boolean isCreativeItem(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            final ItemMeta meta = item.getItemMeta();
            if (meta.hasLore()) {
                for (final String s : meta.getLore()) {
                    if (s.startsWith(creativeMessage.replace("%Name%", ""))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (disallowedWorlds.contains(event.getEntity().getWorld().getName())) {
            return;
        }
        if (checkEntity(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled() || disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        if (MarkBlocks
                && (isCreativeItem(event.getItemInHand()) || event.getPlayer().getGameMode() == GameMode.CREATIVE)) {
            StorageApi.markBlock(event.getBlockPlaced(), getCreativeString(event.getItemInHand()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled() || disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }

        if (isCreativeItem(event.getPlayer().getItemInHand()) && event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            event.setCancelled(true);
            event.setExpToDrop(0);
        }

        if (StorageApi.isMarked(event.getBlock())) {
            final String message = StorageApi.unmarkBlock(event.getBlock());
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setExpToDrop(0);
                final Collection<ItemStack> drops = event.getBlock().getDrops(event.getPlayer().getItemInHand());
                for (final ItemStack item : drops) {
                    final ItemMeta meta = item.getItemMeta();
                    List<String> lore = new ArrayList<String>();
                    if (meta.hasLore()) {
                        lore = meta.getLore();
                    }
                    lore.add(0, message);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    event.getBlock().getWorld()
                            .dropItemNaturally(event.getBlock().getLocation().clone().add(0.5, 0, 0.5), item);
                }
                event.getBlock().setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        if (isCreativeItem(event.getContents().getIngredient())) {
            final List<String> lore = event.getContents().getIngredient().getItemMeta().getLore();
            final ItemStack[] items = event.getContents().getContents();
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null && items[i].getItemMeta() != null) {
                    final ItemMeta meta = items[i].getItemMeta();
                    meta.setLore(lore);
                    items[i].setItemMeta(meta);
                }
            }
            event.getContents().setContents(items);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getViewers().isEmpty() || disallowedWorlds.contains(event.getViewers().get(0).getWorld().getName())) {
            return;
        }
        for (final ItemStack item : event.getInventory().getMatrix()) {
            if (isCreativeItem(item)) {
                if (event.getViewers().get(0).getGameMode() != GameMode.CREATIVE && PreventCrafting) {
                    event.getInventory().setItem(0, new ItemStack(0, 0));
                } else if (RenameCrafting) {
                    setCreativeItem(event.getViewers().get(0).getName(), event.getInventory().getItem(0));
                }
                break;
            }
        }
    }

    @EventHandler
    public void onCreativeClick(InventoryCreativeEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }

        event.setCursor(setCreativeItem(event.getWhoClicked().getName(), event.getCursor()));

        if (disallowedItems.contains(event.getCursor().getType())) {
            if (!event.getWhoClicked().hasPermission("limitcreative.useblacklistitems")) {

                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    ((Player) event.getWhoClicked()).sendMessage(DisabledItemsMessage);
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (disallowedWorlds.contains(event.getEntity().getWorld().getName())) {
            return;
        }
        if (PreventArmor) {
            if (event.getEntity() instanceof Player && ((Player) event.getEntity()).getGameMode() != GameMode.CREATIVE) {
                final ItemStack[] items = ((Player) event.getEntity()).getInventory().getArmorContents();
                for (int i = 0; i < 4; i++) {
                    final ItemStack item = items[i];
                    if (isCreativeItem(item)) {
                        items[i] = new ItemStack(Material.AIR);
                        final HashMap<Integer, ItemStack> leftovers = ((Player) event.getEntity()).getInventory()
                                .addItem(item);
                        for (final ItemStack leftoverItem : leftovers.values()) {
                            ((Player) event.getEntity()).getWorld().dropItem(
                                    ((Player) event.getEntity()).getEyeLocation(), leftoverItem);
                        }
                    }
                }
                ((HumanEntity) event.getEntity()).getInventory().setArmorContents(items);
            }
        }
    }

    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent event) {
        if (this.isCreativeItem(event.getItem()) && event.getEnchanter().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExplode(EntityExplodeEvent event) {
        if (disallowedWorlds.contains(event.getLocation().getWorld().getName())) {
            return;
        }
        for (final Block block : event.blockList()) {
            if (StorageApi.isMarked(block)) {
                final String message = StorageApi.unmarkBlock(block);
                for (final ItemStack item : block.getDrops()) {
                    final ItemMeta meta = item.getItemMeta();
                    List<String> lore = new ArrayList<String>();
                    if (meta.hasLore()) {
                        lore = meta.getLore();
                    }
                    lore.add(0, message);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    block.getWorld().dropItemNaturally(block.getLocation().clone().add(0.5, 0, 0.5), item);
                }
                block.setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onGameModeSwitch(PlayerGameModeChangeEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }

        if (PreventAdventureMode && event.getNewGameMode() == GameMode.ADVENTURE) {
            event.setCancelled(true);
        }

        if (PreventArmor && event.getPlayer().getGameMode() == GameMode.CREATIVE
                && event.getNewGameMode() != GameMode.CREATIVE) {
            final ItemStack[] items = event.getPlayer().getInventory().getArmorContents();
            for (int i = 0; i < 4; i++) {
                final ItemStack item = items[i];
                if (isCreativeItem(item)) {
                    items[i] = new ItemStack(0);
                    final HashMap<Integer, ItemStack> leftovers = event.getPlayer().getInventory().addItem(item);
                    for (final ItemStack leftoverItem : leftovers.values()) {
                        event.getPlayer().getWorld().dropItem(event.getPlayer().getEyeLocation(), leftoverItem);
                    }
                }
            }
            event.getPlayer().getInventory().setArmorContents(items);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (checkEntity(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (checkEntity(event.getPlayer())) {
            event.setCancelled(true);
        }
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE && event.getRightClicked() instanceof ItemFrame) {
            final ItemStack item = event.getPlayer().getItemInHand();
            if (item != null && item.getType() != Material.AIR && !isCreativeItem(item)) {
                final ItemFrame frame = (ItemFrame) event.getRightClicked();
                if (frame.getItem() == null || frame.getItem().getType() == Material.AIR) {
                    event.getPlayer().setItemInHand(setCreativeItem(event.getPlayer().getName(), item));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE
                && event.getInventory().getType() == InventoryType.ANVIL && isCreativeItem(event.getCurrentItem())) {
            if (PreventAnvil) {
                event.setCancelled(true);
            }
        }
        if (event.getWhoClicked().getGameMode() == GameMode.CREATIVE
                && event.getAction() == InventoryAction.CLONE_STACK && !isCreativeItem(event.getCurrentItem())) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR) {
                item = setCreativeItem(event.getWhoClicked().getName(), event.getCurrentItem().clone());
                item.setAmount(item.getMaxStackSize());
                event.getWhoClicked().setItemOnCursor(item);
                event.setCancelled(true);
            }
        }

        if (PreventPuttingNonPlayerInv || !(event.getWhoClicked().hasPermission("limitcreative.itemTransfer"))) {
            final Inventory top = event.getView().getTopInventory();
            final Inventory bottom = event.getView().getBottomInventory();

            if (!Objects.equal(top, bottom)) {
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    if (isCreativeItem(event.getCurrentItem())) {
                        event.setCancelled(true);
                    } else if (PreventTakingNonPlayerInv && event.getWhoClicked().getGameMode() == GameMode.CREATIVE) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDragEvent(InventoryDragEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }

        if (PreventPuttingNonPlayerInv || !(event.getWhoClicked().hasPermission("limitcreative.itemTransfer"))) {
            final Inventory top = event.getView().getTopInventory();
            final Inventory bottom = event.getView().getBottomInventory();

            if (!Objects.equal(top, bottom)) {
                if (event.getOldCursor() != null && event.getOldCursor().getType() != Material.AIR) {
                    if (isCreativeItem(event.getOldCursor())) {
                        event.setCancelled(true);
                    } else if (PreventTakingNonPlayerInv && event.getWhoClicked().getGameMode() == GameMode.CREATIVE) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryPickupItemEvent(InventoryPickupItemEvent event) {
        if (disallowedWorlds.contains(event.getItem().getWorld().getName())) {
            return;
        }

        if (PreventHopper) {
            if (isCreativeItem(event.getItem().getItemStack())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (PreventSurvivalPickup || !(event.getPlayer().hasPermission("limitcreative.pickupcreativeitem"))) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE && isCreativeItem(event.getItem().getItemStack())) {
                event.setCancelled(true);
            }
        }
        if (PreventCreativePickup || !(event.getPlayer().hasPermission("limitcreative.pickupsurvivalitem"))) {
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE
                    && !(isCreativeItem(event.getItem().getItemStack()))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (PreventSurvivalDrop && !(event.getPlayer().hasPermission("limitcreative.dropsurvivalitem"))) {
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE
                    && !(isCreativeItem(event.getItemDrop().getItemStack()))) {
                event.setCancelled(true);
            }
        }
        if (PreventCreativeDrop && !(event.getPlayer().hasPermission("limitcreative.dropcreativeitem"))) {
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE
                    && isCreativeItem(event.getItemDrop().getItemStack())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPush(BlockPistonExtendEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        for (final Block block : event.getBlocks()) {
            if (StorageApi.isMarked(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRetract(BlockPistonRetractEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        if (event.isSticky()) {
            final Block block = event.getBlock().getRelative(event.getDirection()).getRelative(event.getDirection());
            if (StorageApi.isMarked(block)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        if (this.isCreativeItem(event.getSource())
                || isCreativeItem(((Furnace) event.getBlock().getState()).getInventory().getFuel())) {
            event.setCancelled(true);
        }
    }

    private ItemStack setCreativeItem(String who, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.getType() != Material.BOOK_AND_QUILL) {
            if (!isCreativeItem(item)) {
                final ItemMeta meta = item.getItemMeta();
                List<String> lore = new ArrayList<String>();
                if (meta.hasLore()) {
                    lore = meta.getLore();
                }
                lore.add(0, creativeMessage.replace("%Name%", who));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

}
