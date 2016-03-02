# slackrtm4j
slack real time messaging api client.

# important
this project is still work in progress.

# usage

```java

Slack slack = Slack.configure().token("PUT-YOUR-API-TOKEN");

public class EchoVerticle extends AbstractVerticle {
    
    @Override
    public void start() throws Exception {
        super.start();
        EventBus eb = vertx.eventBus();
        eb.consumer("rtm.message", message -> {
            JsonObject json = 
        });
    }

}

slack.start();
```
