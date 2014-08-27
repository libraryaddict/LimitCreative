package me.libraryaddict.Limit;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class StorageApi {

   private static Connection                            connection;
   private static JavaPlugin                            mainPlugin;
   private static HashMap<String, HashMap<Loc, String>> markedBlocks = new HashMap<String, HashMap<Loc, String>>();
   private static String                                mysqlDatabase, mysqlUsername, mysqlPassword, mysqlHost;
   private static boolean                               useMysql;

   private static Connection connectMysql() {
      try {
         Class.forName("com.mysql.jdbc.Driver").newInstance();
         final String conn = "jdbc:mysql://" + mysqlHost + "/" + mysqlDatabase;
         return DriverManager.getConnection(conn, mysqlUsername, mysqlPassword);
      } catch (final Exception ex) {
         System.err
               .println("[LimitCreative] Unknown error while fetching MySQL connection. Is the mysql details correct? "
                     + ex.getMessage());
      }
      return null;
   }

   private static Connection getConnection() {
      try {
         if (connection == null) {
            connection = connectMysql();
            final DatabaseMetaData dbmd = connection.getMetaData();
            final ResultSet rs = dbmd.getTables(null, null, "LimitCreative", null);
            if (!rs.next()) {
               connection
                     .createStatement()
                     .execute(
                           "CREATE TABLE IF NOT EXISTS LimitCreative (world VARCHAR(20), x INT(10), y INT(10), z INT(10), lore VARCHAR(300))");
            }
            return connection;
         }
         try {
            connection.createStatement().execute("DO 1");
         } catch (final Exception ex) {
            connection = connectMysql();
         }
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
      return connection;
   }

   public static boolean isMarked(Block block) {
      return markedBlocks.containsKey(block.getWorld().getName())
            && markedBlocks.get(block.getWorld().getName()).containsKey(new Loc(block));
   }

   public static void loadBlocksFromFlatfile() {
      final File file = new File(mainPlugin.getDataFolder(), "blocks.yml");
      if (file.exists()) {
         try {
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (final String worldName : config.getKeys(false)) {
               final World world = Bukkit.getWorld(worldName);
               if (world != null) {
                  for (final String x : config.getConfigurationSection(worldName).getKeys(false)) {
                     for (final String y : config.getConfigurationSection(worldName + "." + x).getKeys(false)) {
                        for (final String z : config.getConfigurationSection(worldName + "." + x + "." + y).getKeys(
                              false)) {
                           if (!markedBlocks.containsKey(worldName)) {
                              markedBlocks.put(worldName, new HashMap());
                           }
                           markedBlocks.get(worldName).put(
                                 new Loc(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z)),
                                 config.getString(worldName + "." + x + "." + y + "." + z));
                        }
                     }
                  }
               }
            }
         } catch (final Exception e) {
            e.printStackTrace();
         }
      }
   }

   public static void loadBlocksFromMysql() {
      try {
         getConnection();
         if (connection != null) {
            final PreparedStatement stmt = connection.prepareStatement("SELECT * FROM LimitCreative WHERE `world`=?");
            for (final World world : Bukkit.getWorlds()) {
               stmt.setString(1, world.getName());
               final ResultSet rs = stmt.executeQuery();
               while (rs.next()) {
                  if (!markedBlocks.containsKey(world.getName())) {
                     markedBlocks.put(world.getName(), new HashMap<Loc, String>());
                  }
                  markedBlocks.get(world.getName()).put(new Loc(rs.getInt("x"), rs.getInt("y"), rs.getInt("z")),
                        rs.getString("lore"));
               }
            }
         }
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   public static void markBlock(Block block, String msg) {
      markBlock(block.getWorld().getName(), new Loc(block), msg);
   }

   public static void markBlock(final String world, final Loc loc, final String msg) {
      if (!markedBlocks.containsKey(world)) {
         markedBlocks.put(world, new HashMap<Loc, String>());
      }
      markedBlocks.get(world).put(loc, msg);
      Bukkit.getScheduler().scheduleAsyncDelayedTask(mainPlugin, new Runnable() {
         @Override
         public void run() {
            if (useMysql) {
               try {
                  final PreparedStatement stmt = getConnection().prepareStatement(
                        "INSERT INTO LimitCreative (`world`, `x`, `y`, `z`, `lore`) VALUES (?, ?, ?, ?, ?);");
                  stmt.setString(1, world);
                  stmt.setInt(2, loc.x);
                  stmt.setInt(3, loc.y);
                  stmt.setInt(4, loc.z);
                  stmt.setString(5, msg);
                  stmt.execute();
               } catch (final Exception ex) {
                  ex.printStackTrace();
               }
            } else {
               final File file = new File(mainPlugin.getDataFolder(), "blocks.yml");
               try {
                  if (!file.exists()) {
                     file.createNewFile();
                  }
                  final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                  config.set(world + "." + loc.x + "." + loc.y + "." + loc.z, msg);
                  config.save(file);
               } catch (final IOException e) {
                  e.printStackTrace();
               }
            }
         }
      });
   }

   public static void saveBlocksToMysql() {
      for (final String world : markedBlocks.keySet()) {
         for (final Loc loc : markedBlocks.get(world).keySet()) {
            markBlock(world, loc, markedBlocks.get(world).get(loc));
         }
      }
   }

   public static void setMainPlugin(JavaPlugin plugin) {
      mainPlugin = plugin;
   }

   public static void setMysqlDetails(String sqlUsername, String sqlPassword, String sqlHost, String sqlDatabase) {
      useMysql = true;
      mysqlDatabase = sqlDatabase;
      mysqlUsername = sqlUsername;
      mysqlHost = sqlHost;
      mysqlPassword = sqlPassword;
   }

   public static String unmarkBlock(Block block) {
      return unmarkBlock(block.getWorld().getName(), new Loc(block));
   }

   public static String unmarkBlock(final String world, final Loc loc) {
      final String msg = markedBlocks.get(world).remove(loc);
      if (markedBlocks.get(world).isEmpty()) {
         markedBlocks.remove(world);
      }
      Bukkit.getScheduler().scheduleAsyncDelayedTask(mainPlugin, new Runnable() {
         @Override
         public void run() {
            if (useMysql) {
               try {
                  final PreparedStatement stmt = getConnection().prepareStatement(
                        "DELETE FROM `LimitCreative` WHERE `world`=? AND `x`=? AND `y`=? AND `z`=?");
                  stmt.setString(1, world);
                  stmt.setInt(2, loc.x);
                  stmt.setInt(3, loc.y);
                  stmt.setInt(4, loc.z);
                  stmt.execute();
               } catch (final Exception ex) {
                  ex.printStackTrace();
               }
            } else {
               final File file = new File(mainPlugin.getDataFolder(), "blocks.yml");
               if (file.exists()) {
                  final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                  final String blockPath = world + "." + loc.x + "." + loc.y + "." + loc.z;
                  if (config.contains(blockPath)) {
                     config.set(blockPath, null);
                  }
                  try {
                     config.save(file);
                  } catch (final IOException e) {
                     e.printStackTrace();
                  }
               }
            }
         }
      });
      return msg;
   }

}
