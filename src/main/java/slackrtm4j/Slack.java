package slackrtm4j;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

/**
 * Created by akio on 2016/02/20.
 */
public class Slack {

    private static final String CLIENT_VERTICLE_ID = "slackrtm4j.client.Client";
    private static final long DEFAULT_PING_INTERVAL = 5L * 1000;
    protected Vertx vertx;

    public static Slack configure() {
        return configure(Vertx.vertx());
    }

    public static Slack configure(Vertx vertx) {
        Slack instance = new Slack();
        instance.vertx = vertx;

        return instance;
    }

    public Slack token(String token) {
        this.vertx.getOrCreateContext().config().put("token", token);
        return this;
    }

    public Slack start() {
        if ( ! this.vertx.getOrCreateContext().config().containsKey("token") ) {
            throw new IllegalStateException("token required.");
        }
        if ( ! this.vertx.getOrCreateContext().config().containsKey("ping.interval") ) {
            this.vertx.getOrCreateContext().config().put("ping.interval", DEFAULT_PING_INTERVAL);
        }
        this.vertx.deployVerticle(CLIENT_VERTICLE_ID, new DeploymentOptions().setWorker(true), stringAsyncResult -> {

        });
        return this;
    }
}
