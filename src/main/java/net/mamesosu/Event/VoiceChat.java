package net.mamesosu.Event;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceSelfMuteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.mamesosu.Handle.PlayerManager;
import net.mamesosu.Main;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class VoiceChat extends ListenerAdapter {

    AudioManager manager;


    private Path getConvertWavPath (String name, String message) throws URISyntaxException, IOException, InterruptedException {
        JSONObject queryJson;

        int id = Main.bot.getId() + 1;

        if(!name.equals("ずんだもん")) name += "さん、";

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:50021/audio_query?speaker=1&text=" + URLEncoder.encode(name + message, StandardCharsets.UTF_8)))
                .version(HttpClient.Version.HTTP_1_1)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        queryJson = new JSONObject(response.body());

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

        return Path.of("%s.wav".formatted(String.valueOf(id)));
    }

    private String getUserName(Member member) {
        return member.getNickname() == null ? member.getEffectiveName() : member.getNickname();
    }

    @Override
    public void onGuildVoiceSelfMute(GuildVoiceSelfMuteEvent e) {

        if (Objects.requireNonNull(e.getVoiceState().getChannel()).getIdLong() != 1090163808556818552L) {
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

            e.getMessage().reply("VCを切断しました！").queue();

            Main.bot.setBotJoined(false);
        }

        else if (e.getChannel().getIdLong() == 1089160068689309713L) {

            if(e.getMember().getUser().isBot()) {
                return;
            }

            //ボイスチャットにプレイヤーが存在しているか
            VoiceChannel voiceChannel = e.getGuild().getVoiceChannelById(1090163808556818552L);

            if (voiceChannel.getMembers().isEmpty()) {
                return;
            }

            try {
                int id = Main.bot.getId() + 1;

                String message;

                if (e.getMessage().getContentRaw().contains("http") || e.getMessage().getContentRaw().contains("http")) {
                    message = "url";
                } else if (e.getMessage().getContentRaw().isEmpty()) {
                    message = "なんかのファイル添付なのだ";
                } else {
                    System.out.println(e.getMessage().getContentRaw());
                    message = e.getMessage().getContentRaw().replaceAll("<@\\d+>", "");
                    message = message.replaceAll("<:\\w+:\\d+>", "");
                    message = message.replaceAll("<\\w+:\\w+:\\d+>", "");
                    message = message.replaceAll("[ -/:-@\\[-`{-~]", "");
                    System.out.println(message);
                }

                Path fileName = getConvertWavPath(getUserName(e.getMember()), message);

                Main.bot.setId(id);
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

        int id = Main.bot.getId() + 1;

        if (event.getChannelJoined() != null || event.getChannelLeft() == null) {

            Path fileName;
            try {
                fileName = getConvertWavPath(getUserName(event.getMember()), "がVCに参加しました");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Main.bot.setId(id);
            PlayerManager.getINSTANCE().loadAndPlay(event.getGuild(),  fileName.toString());
            return;
        }

        if (event.getChannelJoined() == null || event.getChannelLeft() != null) {

            Path fileName;
            try {
                fileName = getConvertWavPath(getUserName(event.getMember()), "がVCから退出しました");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Main.bot.setId(id);
            PlayerManager.getINSTANCE().loadAndPlay(event.getGuild(),  fileName.toString());
            return;
        }

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

        Main.bot.setBotJoined(false);
    }
}
