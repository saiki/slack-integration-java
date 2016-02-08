package jp.saiki;

import com.google.gson.Gson;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by akio on 2016/02/08.
 */
public class RtmStart extends AbstractVerticle {

    HttpClient client = null;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.client = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
        String token = vertx.getOrCreateContext().get("token");
        HttpClientRequest request = client.get(443, "slack.com", "/api/rtm.start?token="+token);
        request.handler( response -> {
            response.bodyHandler(body -> {
                System.out.println("Got data " + body.toString("ISO-8859-1"));
                JsonObject json = body.toJsonObject();
                vertx.eventBus().send("rtm.start", json);
                vertx.eventBus().send("rtm.start.hook", json);
            });
        });
        super.start(startFuture);
    }

    @Override
    public void stop() {
        client.close();
    }
}
