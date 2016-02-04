package jp.saiki;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.vertx.core.*;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

/**
 * Created by MSD010063 on 2016/02/03.
 */
public class App {

    public static void main(String... args) {
        Vertx vertx = Vertx.vertx();

        HttpClient startClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
        HttpClient chatClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        String token = args[0];

        startClient.getNow(443, "slack.com", "/api/rtm.start?token="+token, resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> {
                System.out.println("Got data " + body.toString("ISO-8859-1"));
                Gson gson = new Gson();
                RtmStartResponse result = gson.fromJson(body.toString("ISO-8859-1"), RtmStartResponse.class);
                try {
                    URI url = new URI(result.getUrl());
                    chatClient.websocket(443, url.getHost(), url.getPath(), socket -> {
                        socket.handler(buffer -> {
                            System.out.println("handler");
                            System.out.println(buffer.toString("ISO-8859-1"));
                        });
                        socket.exceptionHandler(throwable -> {
                            System.err.println(throwable.getMessage());
                            throwable.printStackTrace(System.err);
                        });
                        socket.drainHandler( Void -> {
                            System.out.println("drain");
                        });
                        socket.frameHandler( frame -> {
                            System.out.println("frame");
                            if ( frame.isBinary() ) {
                                System.out.println(frame.binaryData());
                            } else if ( frame.isText() ) {
                                System.out.println(frame.textData());
                            }
                        });
                        for ( int i = 0; i < result.getChannels().length; i++ ) {
                            System.out.println("id: "+result.getChannels()[i].getId());
                            System.out.println("name: "+result.getChannels()[i].getName());
                            Message message = new Message(i);
                            message.channel = result.getChannels()[i].getId();
                            message.text = "hello, "+result.getChannels()[i].getName()+" channel";
                            socket.writeFinalTextFrame(gson.toJson(message));

                        }
                    }, throwable -> {
                        System.err.println(throwable.getMessage());
                        throwable.printStackTrace(System.err);
                    });
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

            });
        });
    }

    @ToString
    @EqualsAndHashCode
    public static class RtmStartResponse {

        @Getter
        @Setter
        private boolean ok;
        @Getter
        @Setter
        private String url;
        @Getter
        @Setter
        private Channel[] channels;
    }

    @ToString
    @EqualsAndHashCode
    public static class Channel {
        @Getter
        @Setter
        private String id;
        @Getter
        @Setter
        private String name;
    }

    @ToString
    @EqualsAndHashCode
    public static class Message {

        @Getter
        @Setter
        private long id;
        @Getter
        @Setter
        private String type;
        @Getter
        @Setter
        private String channel;
        @Getter
        @Setter
        private String text;

        public Message(final int id) {
            this.id = id;
            this.type = "message";
        }

        public Message(final int id, final String type) {
            this.id = id;
            this.type = type;
        }
    }
}
