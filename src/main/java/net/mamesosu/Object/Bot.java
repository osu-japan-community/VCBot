package net.mamesosu.Object;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.mamesosu.Event.VoiceChat;

public class Bot {

    private String TOKEN;

    private JDA jda;
    private int id;
    private boolean isBotJoined = false;
    public Bot () {
        Dotenv dotenv = Dotenv.configure()
                .load();

        TOKEN = dotenv.get("TOKEN");
        id = 0;
    }

    public String getTOKEN() {
        return TOKEN;
    }

    public boolean getBotJoined() {
        return isBotJoined;
    }

    public void setBotJoined(boolean isBotJoined) {
        this.isBotJoined = isBotJoined;
    }

    public JDA getJda() {
        return jda;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void loadJDA() {
        jda = JDABuilder.createDefault(this.TOKEN)
                .setRawEventsEnabled(true)
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.GUILD_EMOJIS_AND_STICKERS
                ).enableCache(
                        CacheFlag.MEMBER_OVERRIDES,
                        CacheFlag.ROLE_TAGS,
                        CacheFlag.EMOJI
                )
                .disableCache(
                        CacheFlag.STICKER,
                        CacheFlag.SCHEDULED_EVENTS
                ).setActivity(
                        Activity.playing("for test"))
                .addEventListeners(
                    new VoiceChat()
                )
                .build();
    }
}
