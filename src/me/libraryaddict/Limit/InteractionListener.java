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
	private final String						creativeMessage;
	private final ArrayList<Material>	disallowedItems	= new ArrayList<Material>();
	private final List<String>				disallowedWorlds;
	private final JavaPlugin				plugin;

	public InteractionListener(JavaPlugin plugin) {
		this.plugin = plugin;
		creativeMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("ItemMessage"));
		disallowedWorlds = getConfig().getStringList("WorldsDisabled");

		for (String disallowed : getConfig().getStringList("DisabledItems")) {
			try {
				disallowedItems.add(Material.valueOf(disallowed.toUpperCase()));
			} catch (Exception ex) {
				try {
					disallowedItems.add(Material.getMaterial(Integer.parseInt(disallowed)));
				} catch (Exception e) {
					System.out.print("[LimitCreative] Cannot parse " + disallowed + " to a valid material");
				}
			}
		}
	}

	private boolean checkEntity(Entity entity) {
		if (getConfig().getBoolean("PreventUsage") && entity != null && entity instanceof Player
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
			ItemMeta meta = item.getItemMeta();
			if (meta.hasLore()) {
				for (String s : meta.getLore()) {
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
			ItemMeta meta = item.getItemMeta();
			if (meta.hasLore()) {
				for (String s : meta.getLore()) {
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
		if (getConfig().getBoolean("MarkBlocks")
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

		if (event.getBlock().getType() == Material.EMERALD_ORE || event.getBlock().getType() == Material.DIAMOND_ORE
				|| event.getBlock().getType() == Material.GOLD_ORE || event.getBlock().getType() == Material.IRON_ORE
				|| event.getBlock().getType() == Material.COAL_ORE || event.getBlock().getType() == Material.REDSTONE_ORE
				|| event.getBlock().getType() == Material.GRAVEL || event.getBlock().getType() == Material.LAPIS_ORE) {

			if (getConfig().getBoolean("PreventOreDrop") && StorageApi.isMarked(event.getBlock())) {
				event.getBlock().breakNaturally(null);
			}
		}

		if (StorageApi.isMarked(event.getBlock())) {
			String message = StorageApi.unmarkBlock(event.getBlock());
			if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
				event.setExpToDrop(0);
				Collection<ItemStack> drops = event.getBlock().getDrops(event.getPlayer().getItemInHand());
				for (ItemStack item : drops) {
					ItemMeta meta = item.getItemMeta();
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
			List<String> lore = event.getContents().getIngredient().getItemMeta().getLore();
			ItemStack[] items = event.getContents().getContents();
			for (int i = 0; i < items.length; i++) {
				if (items[i] != null && items[i].getItemMeta() != null) {
					ItemMeta meta = items[i].getItemMeta();
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
		for (ItemStack item : event.getInventory().getMatrix()) {
			if (isCreativeItem(item)) {
				if (event.getViewers().get(0).getGameMode() != GameMode.CREATIVE
						&& getConfig().getBoolean("PreventCrafting")) {
					event.getInventory().setItem(0, new ItemStack(0, 0));
				} else if (getConfig().getBoolean("RenameCrafting")) {
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
					((Player) event.getWhoClicked()).sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig()
							.getString("DisabledItemsMessage")));
				}
			}
		}
	}

	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (disallowedWorlds.contains(event.getEntity().getWorld().getName())) {
			return;
		}
		if (getConfig().getBoolean("PreventArmor")) {
			if (event.getEntity() instanceof Player && ((Player) event.getEntity()).getGameMode() != GameMode.CREATIVE) {
				ItemStack[] items = ((Player) event.getEntity()).getInventory().getArmorContents();
				for (int i = 0; i < 4; i++) {
					ItemStack item = items[i];
					if (isCreativeItem(item)) {
						items[i] = new ItemStack(Material.AIR);
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
		for (Block block : event.blockList()) {
			if (StorageApi.isMarked(block)) {
				String message = StorageApi.unmarkBlock(block);
				for (ItemStack item : block.getDrops()) {
					ItemMeta meta = item.getItemMeta();
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

		if (getConfig().getBoolean("PreventAdventureMode") && event.getNewGameMode() == GameMode.ADVENTURE) {
			event.setCancelled(true);
		}

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
			ItemStack item = event.getPlayer().getItemInHand();
			if (item != null && item.getType() != Material.AIR && !isCreativeItem(item)) {
				ItemFrame frame = (ItemFrame) event.getRightClicked();
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
			if (getConfig().getBoolean("PreventAnvil")) {
				event.setCancelled(true);
			}
		}
		if (event.getWhoClicked().getGameMode() == GameMode.CREATIVE && event.getAction() == InventoryAction.CLONE_STACK
				&& !isCreativeItem(event.getCurrentItem())) {
			ItemStack item = event.getCurrentItem();
			if (item != null && item.getType() != Material.AIR) {
				item = setCreativeItem(event.getWhoClicked().getName(), event.getCurrentItem().clone());
				item.setAmount(item.getMaxStackSize());
				event.getWhoClicked().setItemOnCursor(item);
				event.setCancelled(true);
			}
		}

		if (getConfig().getBoolean("PreventPuttingNonPlayerInv")
				|| !(event.getWhoClicked().hasPermission("limitcreative.itemTransfer"))) {
			Inventory top = event.getView().getTopInventory();
			Inventory bottom = event.getView().getBottomInventory();

			if (!Objects.equal(top, bottom)) {
				if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
					if (isCreativeItem(event.getCurrentItem())) {
						event.setCancelled(true);
					} else if (getConfig().getBoolean("PreventTakingNonPlayerInv")
							&& event.getWhoClicked().getGameMode() == GameMode.CREATIVE) {
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

		if (getConfig().getBoolean("PreventPuttingNonPlayerInv")
				|| !(event.getWhoClicked().hasPermission("limitcreative.itemTransfer"))) {
			Inventory top = event.getView().getTopInventory();
			Inventory bottom = event.getView().getBottomInventory();

			if (!Objects.equal(top, bottom)) {
				if (event.getOldCursor() != null && event.getOldCursor().getType() != Material.AIR) {
					if (isCreativeItem(event.getOldCursor())) {
						event.setCancelled(true);
					} else if (getConfig().getBoolean("PreventTakingNonPlayerInv")
							&& event.getWhoClicked().getGameMode() == GameMode.CREATIVE) {
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

		if (getConfig().getBoolean("PreventHopper")) {
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
		if (getConfig().getBoolean("PreventSurvivalPickup")
				|| !(event.getPlayer().hasPermission("limitcreative.pickupcreativeitem"))) {
			if (event.getPlayer().getGameMode() != GameMode.CREATIVE && isCreativeItem(event.getItem().getItemStack())) {
				event.setCancelled(true);
			}
		}
		if (getConfig().getBoolean("PreventCreativePickup")
				|| !(event.getPlayer().hasPermission("limitcreative.pickupsurvivalitem"))) {
			if (event.getPlayer().getGameMode() == GameMode.CREATIVE && !(isCreativeItem(event.getItem().getItemStack()))) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
			return;
		}
		if (getConfig().getBoolean("PreventSurvivalDrop")
				&& !(event.getPlayer().hasPermission("limitcreative.dropsurvivalitem"))) {
			if (event.getPlayer().getGameMode() == GameMode.CREATIVE
					&& !(isCreativeItem(event.getItemDrop().getItemStack()))) {
				event.setCancelled(true);
			}
		}
		if (getConfig().getBoolean("PreventCreativeDrop")
				&& !(event.getPlayer().hasPermission("limitcreative.dropcreativeitem"))) {
			if (event.getPlayer().getGameMode() == GameMode.CREATIVE && isCreativeItem(event.getItemDrop().getItemStack())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPush(BlockPistonExtendEvent event) {
		if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
			return;
		}
		for (Block block : event.getBlocks()) {
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
			Block block = event.getBlock().getRelative(event.getDirection()).getRelative(event.getDirection());
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
				ItemMeta meta = item.getItemMeta();
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
