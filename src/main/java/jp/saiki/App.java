package jp.saiki;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.vertx.core.*;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.Json;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeMap;
import java.util.function.Consumer;

public class App {

    private static String reconnectUrl = "";

    public static void main(String... args) {
        Vertx vertx = Vertx.vertx();

        HttpClient startClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
        HttpClient chatClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        String token = args[0];

        startClient.getNow(443, "slack.com", "/api/rtm.start?token=" + token, resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> {
                System.out.println("Got data " + body.toString("ISO-8859-1"));
                Gson gson = new Gson();
                try {
                    io.vertx.core.json.JsonObject json = body.toJsonObject();
                    URI url = new URI(json.getString("url"));
                    chatClient.websocket(443, url.getHost(), url.getPath(), socket -> {
                        socket.handler(buffer -> {
                            io.vertx.core.json.JsonObject jsonMessage = buffer.toJsonObject();
                            String type = jsonMessage.getString("type");
                            if ( type == null || "pong".equals(type) || "hello".equals(type) || "presence_change".equals(type) ) {
                                return;
                            }
                            if ( "reconnect_url".equals(type) ) {
                                reconnectUrl = jsonMessage.getString("url");
                                return;
                            }
                            Message message = gson.fromJson(buffer.toString("UTF-8"), Message.class);
                            Message response = new Message(1);
                            response.setChannel(message.getChannel());
                            response.setText(message.getText());
                            socket.writeFinalTextFrame(gson.toJson(response));
                            System.out.println(buffer.toString("UTF-8"));
                        });
                        socket.exceptionHandler(throwable -> {
                            System.err.println(throwable.getMessage());
                            throwable.printStackTrace(System.err);
                        });
                        socket.drainHandler(Void -> System.out.println("drain"));
                        socket.closeHandler(Void -> System.out.println("close."));

                        Ping ping = new Ping();
                        ping.setId(1L);
                        vertx.setPeriodic(5000, id -> {
                            socket.writeFinalTextFrame(Json.encode(ping));
//                            ping.setId(ping.getId()+1L);
                        });

                        RtmStartResponse result = gson.fromJson(body.toString("UTF-8"), RtmStartResponse.class);
                        System.out.println(result.getSelf());
                        Arrays.stream(result.getChannels()).filter( channel -> {
                            if ( channel.getMembers() == null || channel.getMembers().length == 0 ) {
                                return false;
                            }
                            return Arrays.stream(channel.getMembers()).anyMatch( member -> {
                                return result.getSelf().getId().equals(member);
                            });
                        }).forEach( channel -> {
                            System.out.println("id: " + channel.getId());
                            System.out.println("name: " + channel.getName());
                            Message message = new Message(1);
                            message.channel = channel.getId();
                            message.text = "hello, " + channel.getName() + " channel";
                            socket.writeFinalTextFrame(gson.toJson(message));
                        });
                    }, throwable -> {
                        System.err.println(throwable.getMessage());
                        throwable.printStackTrace(System.err);
                    });
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            });
            startClient.close();
        });
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Setter
    public static class Self {

        private String id;

        private String name;

//        private Date created;

        private String manualPresence;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Setter
    public static class RtmStartResponse {

        private boolean ok;

        private String url;

        private Self self;

        private Channel[] channels;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Setter
    public static class Channel {

        private String id;

        private String name;

        private boolean isChannel;

//        private Date created;

        private String creator;

        private boolean isArchived;

        private boolean isGeneral;

        private String[] members;

        private boolean isMember;

    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Setter
    public static class ReConnectUrl {

        private String type;

        private String url;
    }

    @ToString
    @EqualsAndHashCode
    public static class Ping {

        @Getter
        @Setter
        private long id;
        @Getter
        private String type = "ping";
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Setter
    public static class Message {

        private long id;

        private String type = "message";

        private String channel;

        private String text;

        private long replyTo;

        private String user;

        public Message() {

        }

        public Message(final int id) {
            this.id = id;
        }

        public Message(final int id, final String type) {
            this.id = id;
        }
    }
}
