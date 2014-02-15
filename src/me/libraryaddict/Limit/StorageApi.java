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
    private static HashMap<Block, String> markedBlocks = new HashMap<Block, String>();
    private static JavaPlugin mainPlugin;
    private static Connection connection;
    private static boolean useMysql;
    private static String mysqlDatabase, mysqlUsername, mysqlPassword, mysqlHost;

    public static void setMainPlugin(JavaPlugin plugin) {
        mainPlugin = plugin;
    }

    public static void setMysqlDetails(String sqlUsername, String sqlPassword, String sqlHost, String sqlDatabase) {
        mysqlDatabase = sqlDatabase;
        mysqlUsername = sqlUsername;
        mysqlHost = sqlHost;
        mysqlPassword = sqlPassword;
    }

    private static Connection connectMysql() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String conn = "jdbc:mysql://" + mysqlHost + "/" + mysqlDatabase;
            return DriverManager.getConnection(conn, mysqlUsername, mysqlPassword);
        } catch (Exception ex) {
            System.err.println("[LimitCreative] Unknown error while fetching MySQL connection. Is the mysql details correct? "
                    + ex.getMessage());
        }
        return null;
    }

    public static void saveBlocksToMysql() {
        for (Block block : markedBlocks.keySet()) {
            markBlock(block, markedBlocks.get(block));
        }
    }

    private static Connection getConnection() {
        try {
            if (connection == null) {
                connection = connectMysql();
                DatabaseMetaData dbmd = connection.getMetaData();
                ResultSet rs = dbmd.getTables(null, null, "LimitCreative", null);
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
            } catch (Exception ex) {
                connection = connectMysql();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return connection;
    }

    public static void markBlock(Block block, final String msg) {
        markedBlocks.put(block, msg);
        final String world = block.getWorld().getName();
        final int x = block.getX(), y = block.getY(), z = block.getZ();
        Bukkit.getScheduler().scheduleAsyncDelayedTask(mainPlugin, new Runnable() {
            public void run() {
                if (useMysql) {
                    try {
                        getConnection();
                        if (connection != null) {
                            PreparedStatement stmt = connection
                                    .prepareStatement("INSERT INTO LimitCreative (`world`, `x`, `y`, `z`, `lore`) VALUES (?, ?, ?, ?, ?);");
                            stmt.setString(1, world);
                            stmt.setInt(2, x);
                            stmt.setInt(3, y);
                            stmt.setInt(4, z);
                            stmt.setString(5, msg);
                            stmt.execute();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    File file = new File(mainPlugin.getDataFolder(), "blocks.yml");
                    try {
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        config.set(world + "." + x + "." + y + "." + z, msg);
                        config.save(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static String unmarkBlock(Block block) {
        String msg = markedBlocks.remove(block);
        final String world = block.getWorld().getName();
        final int x = block.getX(), y = block.getY(), z = block.getZ();
        Bukkit.getScheduler().scheduleAsyncDelayedTask(mainPlugin, new Runnable() {
            public void run() {
                if (useMysql) {
                    try {
                        getConnection();
                        if (connection != null) {
                            PreparedStatement stmt = connection
                                    .prepareStatement("DELETE FROM `LimitCreative` WHERE `world`=? AND `x`=? AND `y`=? AND `z`=?");
                            stmt.setString(1, world);
                            stmt.setInt(2, x);
                            stmt.setInt(3, y);
                            stmt.setInt(4, z);
                            stmt.execute();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    File file = new File(mainPlugin.getDataFolder(), "blocks.yml");
                    if (file.exists()) {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        String blockPath = world + "." + x + "." + y + "." + z;
                        if (config.contains(blockPath)) {
                            config.set(blockPath, null);
                        }
                        try {
                            config.save(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        return msg;
    }

    public static boolean isMarked(Block block) {
        return markedBlocks.containsKey(block);
    }

    public static void loadBlocksFromMysql() {
        try {
            getConnection();
            if (connection != null) {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM LimitCreative WHERE `world`=?");
                for (World world : Bukkit.getWorlds()) {
                    stmt.setString(1, world.getName());
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        Block block = world.getBlockAt(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
                        markedBlocks.put(block, rs.getString("lore"));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void loadBlocksFromFlatfile() {
        File file = new File(mainPlugin.getDataFolder(), "blocks.yml");
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

}
