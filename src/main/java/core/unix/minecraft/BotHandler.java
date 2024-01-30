package core.unix.minecraft;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.awt.*;
import java.util.Collections;
import java.util.logging.Level;

class BotHandler extends ListenerAdapter {

    private final MineAuth authPlugin;

    private BotHandler(MineAuth plugin) {
        authPlugin = plugin;
    }

    public static void createBotWithToken(String token, MineAuth authPlugin) {

        BotHandler handler = new BotHandler(authPlugin);

        // args[0] would be the token (using an environment variable or config file is preferred for security)
        // We don't need any intents for this bot. Slash commands work without any intents!
        JDA jda = JDABuilder.createLight(token, Collections.emptyList())
                .addEventListeners(handler)
                .setActivity(Activity.playing("Type /whitelist help"))
                .disableCache(
                        CacheFlag.ONLINE_STATUS,
                        CacheFlag.MEMBER_OVERRIDES,
                        CacheFlag.FORUM_TAGS,
                        CacheFlag.CLIENT_STATUS,
                        CacheFlag.EMOJI,
                        CacheFlag.ACTIVITY,
                        CacheFlag.ROLE_TAGS,
                        CacheFlag.SCHEDULED_EVENTS,
                        CacheFlag.STICKER,
                        CacheFlag.VOICE_STATE
                )
                .disableIntents(
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.DIRECT_MESSAGE_TYPING,
                        GatewayIntent.GUILD_MESSAGE_TYPING,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_INVITES,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                        GatewayIntent.SCHEDULED_EVENTS
                )
                .build();

        // Sets the global command list to the provided commands (removing all others)
        jda.updateCommands().addCommands(
                Commands.slash("ping", "Calculate ping of the bot"),
                Commands.slash("whitelist", "Get whitelist")
                        .addSubcommands(
                                new SubcommandData("help", "Show whitelist usage"),
                                new SubcommandData("on", "Turn on and enforce whitelist system"),
                                new SubcommandData("off", "Turn off whitelist system and do not enforce"),
                                new SubcommandData("list", "List whitelisted users"),
                                new SubcommandData("add", "Add user to whitelist")
                                        .addOption(OptionType.STRING, "user", "Minecraft username to whitelist", true),
                                new SubcommandData("remove", "Remove user from whitelist")
                                        .addOption(OptionType.STRING, "user", "Minecraft username to remove from whitelist", true)
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)) // only usable with ban permissions
                        .setGuildOnly(true) // Ban command only works inside a guild
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        authPlugin.getLogger().log(Level.FINEST, String.format("Bot received command: %s", event.toString()));

        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Authorization Fail");
            eb.setDescription("You must have ban permissions to use MineAuth");
            eb.setColor(Color.red);
            return;
        }

        // make sure we handle the right command
        switch (event.getName()) {
            case "ping":
                long time = System.currentTimeMillis();
                event.reply("Pong!").setEphemeral(true) // reply or acknowledge
                        .flatMap(v ->
                                event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                        ).queue(); // Queue both reply and edit
                break;

            case "whitelist":
                String subcommand = event.getSubcommandName();

                EmbedBuilder eb = new EmbedBuilder();

                if ("help".equals(subcommand)) {
                    eb.setTitle("Whitelist Help");

                    eb.addField("/whitelist on", "Turn on and enforce whitelist system", false);
                    eb.addField("/whitelist off", "Turn off whitelist system and do not enforce", false);
                    eb.addField("/whitelist list", "List whitelisted users", false);
                    eb.addField("/whitelist add [user]", "Add user to whitelist", false);
                    eb.addField("/whitelist remove [user]", "Remove user from whitelist", false);

                    eb.setColor(Color.GRAY);

                    event.replyEmbeds(eb.build()).queue();
                }

                else if ("list".equals(subcommand)) {
                    String[] whitelistedPlayerNames = authPlugin.getWhiteListedPlayerNames();

                    String response;

                    eb.setTitle("Whitelisted Players");

                    if (whitelistedPlayerNames.length == 0) {
                        response = "There are no whitelisted players";
                        eb.setColor(Color.RED);

                    } else {
                        response = "```\n" + String.join("\n", whitelistedPlayerNames) + "\n```";
                        eb.setColor(Color.GREEN);
                    }

                    eb.setDescription(response);

                    event.replyEmbeds(eb.build()).queue();
                }

                else if ("add".equals(subcommand) || "remove".equals(subcommand)) {
                    boolean whitelist = "add".equals(subcommand);

                    String username = event.getOption("user").getAsString();

                    WhitelistUserResult result = authPlugin.setUserWhitelist(username, whitelist);

                    eb.setTitle("Whitelist " + (whitelist ? "Add" : "Remove"));

                    if (result == WhitelistUserResult.USER_DOES_NOT_EXIST) {
                        eb.setDescription("There is no such user");
                        eb.setColor(Color.RED);
                    }
                    else if (result == WhitelistUserResult.NO_CHANGE) {
                        if (whitelist) eb.setDescription(String.format("User `%s` is already on whitelist", username));
                        else eb.setDescription(String.format("User `%s` was already not on whitelist", username));
                        eb.setColor(Color.YELLOW);
                    }
                    else if (result == WhitelistUserResult.SUCCESS) {
                        if (whitelist) eb.setDescription(String.format("User `%s` added to whitelist", username));
                        else eb.setDescription(String.format("User `%s` removed from whitelist", username));
                        eb.setColor(whitelist ? Color.GREEN : Color.GRAY);
                    }

                    event.replyEmbeds(eb.build()).queue();
                }

                else if ("on".equals(subcommand) || "off".equals(subcommand)) {
                    boolean turnOn = "on".equals(subcommand);

                    WhitelistSettingResult result = authPlugin.setWhitelistAndEnforce(turnOn);

                    eb.setTitle("Whitelist System");

                    if (result == WhitelistSettingResult.NO_CHANGE) {
                        if (turnOn) eb.setDescription("Whitelist system is already enabled");
                        else eb.setDescription("Whitelist system is already disabled");
                        eb.setColor(Color.YELLOW);
                    }
                    else if (result == WhitelistSettingResult.SUCCESS) {
                        if (turnOn) eb.setDescription("Whitelist system has been enabled");
                        else eb.setDescription("Whitelist system has been disabled");
                        eb.setColor(turnOn ? Color.GREEN : Color.GRAY);
                    }

                    event.replyEmbeds(eb.build()).queue();

                }

                break;

            default:
                authPlugin.getLogger().log(Level.INFO, String.format("Unknown command %s used by %#s%n", event.getName(), event.getUser()));
        }
    }
}