package net.mamesosu.Event;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceSelfMuteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.mamesosu.Handle.PlayerManager;
import net.mamesosu.Main;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoiceChat extends ListenerAdapter {

    AudioManager manager;

    @Override
    public void onGuildVoiceSelfMute(GuildVoiceSelfMuteEvent e) {

        JDA jda = Main.bot.getJda();

        System.out.println(e.getVoiceState().getIdLong());

        if (e.getVoiceState().getChannel().getIdLong() != 1090163808556818552L) {
            return;
        }

        boolean isBotJoined = Main.bot.getBotJoined();

        for (Member m : e.getVoiceState().getChannel().getMembers()) {
            //ボットか他の読み上げbotがいたら参加しないように
            if (m.getUser().getIdLong() == 727508841368911943L || m.getUser().getIdLong() == 1240649156167471186L) {
                isBotJoined = true;
            }
        }

        if (!isBotJoined) {
            manager = e.getGuild().getAudioManager();
            VoiceChannel channel = e.getGuild().getVoiceChannelById(e.getVoiceState().getChannel().getIdLong());

            manager.openAudioConnection(channel);
        }

        Main.bot.setBotJoined(isBotJoined);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {

        if (e.getMessage().getContentRaw().equals("!disconnect")) {
            boolean isBotJoined = false;

            if (e.getMember().getVoiceState() == null || e.getMember().getVoiceState().getChannel() == null) {
                e.getMessage().reply("このコマンドはVCに参加しているときのみ実行できます！").queue();
                return; // 自身がどのVCにも参加していない
            }

            for(Member m : e.getMember().getVoiceState().getChannel().getMembers()) {
                if (m.getUser().getIdLong() == 1240649156167471186L) {
                    isBotJoined = true;
                }
            }

            if (!isBotJoined) {
                e.getMessage().reply("このコマンドはVCに参加しているときのみ実行できます！").queue();
                return; // 自身がどのVCにも参加していない
            }

            e.getGuild().getAudioManager().closeAudioConnection();
        }

        else if (e.getChannel().getIdLong() == 1089160068689309713L) {

            //ボイスチャットにプレイヤーが存在しているか
            VoiceChannel voiceChannel = e.getGuild().getVoiceChannelById(1090163808556818552L);

            if (voiceChannel.getMembers().isEmpty()) {
                return;
            }

            try {

                JSONObject queryJson = null;

                int id = Main.bot.getId() + 1;

                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:50021/audio_query?speaker=1&text=" + e.getMessage().getContentRaw()))
                        .version(HttpClient.Version.HTTP_1_1)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                queryJson = new JSONObject(response.body());

                if (queryJson == null) {
                    System.out.println("Query JSON is null");
                    return;
                }

                httpClient = HttpClient.newHttpClient();
                request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:50021/synthesis?speaker=1"))
                        .version(HttpClient.Version.HTTP_1_1)
                        .POST(HttpRequest.BodyPublishers.ofString(queryJson.toString()))
                        .build();
                HttpResponse<byte[]> r = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (r.statusCode() == 200) {
                    Files.write(Path.of( (id) +".wav"), r.body());
                } else {
                    System.out.println("Error: " + response.statusCode());
                }

                Main.bot.setId(id);

                System.out.println("load");

                Path fileName = Path.of("%s.wav".formatted(String.valueOf(id)));

                PlayerManager.getINSTANCE().loadAndPlay(e.getGuild(),  fileName.toString());
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    //Auto Disconnect
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() != null || event.getChannelLeft() == null) return; // 退出以外は除外

        if (event.getGuild().getSelfMember().getVoiceState() == null ||
                event.getGuild().getSelfMember().getVoiceState().getChannel() == null) {
            return; // 自身がどのVCにも参加していない
        }
        if (event.getGuild().getSelfMember().getVoiceState().getChannel().getIdLong() != event.getChannelLeft().getIdLong()) {
            return; // 退出されたチャンネルが自身のいるVCと異なる
        }

        // VCに残ったユーザーが全員Bot、または誰もいなくなった
        boolean existsUser = event
                .getChannelLeft()
                .getMembers()
                .stream()
                .anyMatch(member -> !member.getUser().isBot()); // Bot以外がいるかどうか

        if (existsUser) {
            return;
        }

        event.getGuild().getAudioManager().closeAudioConnection();
    }
}
