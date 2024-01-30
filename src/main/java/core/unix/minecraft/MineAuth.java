package core.unix.minecraft;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.logging.Level;

public class MineAuth extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String botToken = getConfig().getString("token");

        if ("INSERT_TOKEN_HERE".equals(botToken)) {
            getLogger().log(Level.WARNING, "Please provide bot token at plugins/MineAuth/config.yml");
            getLogger().log(Level.WARNING, "MineAuth will stop now. Bye!");
            return;
        }

        getLogger().log(Level.INFO, "Trying to initialize MineAuth bot");

        // This initializes the Discord bot and attaches this plugin instance to it
        BotHandler.createBotWithToken(botToken, this);
    }

    public String[] getWhiteListedPlayerNames() {
        Set<OfflinePlayer> whitelistedUsers = Bukkit.getWhitelistedPlayers();

        return whitelistedUsers.stream().map(OfflinePlayer::getName).toArray(String[]::new);
    }

    public WhitelistUserResult setUserWhitelist(String username, boolean whitelisted) {
        OfflinePlayer offlinePlayer = getOfflinePlayer(username);

        // Turns out this does not work for offline players
        // And always return true
        // TODO: Update offline player existence check logic
        boolean success = offlinePlayer.getUniqueId() != null;
        if (!success) return WhitelistUserResult.USER_DOES_NOT_EXIST;

        boolean isWhitelisted = offlinePlayer.isWhitelisted();
        if (isWhitelisted == whitelisted) return WhitelistUserResult.NO_CHANGE;

        // Ensure write operation is done on main thread
        Bukkit.getScheduler().runTask(this, () -> offlinePlayer.setWhitelisted(whitelisted));

        getLogger().log(
                Level.INFO,
                String.format("User %s has been %s.", username,
                        whitelisted ? "whitelisted" : "de-whitelisted")
        );

        return WhitelistUserResult.SUCCESS;
    }

    public WhitelistSettingResult setWhitelistAndEnforce(boolean status) {
        boolean isWhitelistEnabled = Bukkit.hasWhitelist();
        boolean isWhitelistEnforced = Bukkit.isWhitelistEnforced();

        if (isWhitelistEnabled == status && isWhitelistEnforced == status) return WhitelistSettingResult.NO_CHANGE;

        // Ensure write operation is done on main thread
        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.setWhitelist(status);
            Bukkit.setWhitelistEnforced(status);
        });

        String logMessage = status
                ? "Whitelist system has been enabled and set to enforce"
                : "Whitelist system has been disabled and will not be enforced";

        getLogger().log(Level.INFO, logMessage);

        return WhitelistSettingResult.SUCCESS;
    }

    public OfflinePlayer getOfflinePlayer(String username) {
        return Bukkit.getOfflinePlayer(username);
    }

}

