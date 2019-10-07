package com.github.hlvx.pubsub;

import com.github.hlvx.pubsub.models.Response;
import com.github.hlvx.websocket.annotations.Context;
import com.github.hlvx.websocket.annotations.TextCommand;
import com.github.hlvx.websocket.models.WebSocketContext;
import com.github.hlvx.websocket.servers.WebSocketServer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PubSubService extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(PubSubService.class);
    private WebSocketServer webSocketServer;
    private Map<String, Set<WebSocketContext>> topicListeners = new HashMap<>();
    private Map<WebSocketContext, Set<String>> clientTopics = new HashMap<>();

    @Override
    public void start() throws Exception {
        webSocketServer = new WebSocketServer(getVertx());
        webSocketServer.setConnectHandler(client -> {
            LOGGER.info("Client connected " + client.remoteAddress());
        });
        webSocketServer.setDisconnectHandler(client -> {
            clientTopics.remove(client);
            LOGGER.info("Client disconnected " + client.remoteAddress());
        });
        webSocketServer.addServices(this);
        webSocketServer.start(config().getInteger("PORT", 4242), h -> {
            LOGGER.info("PubSub service started on port {}", h.result().actualPort());
        });
    }

    @Override
    public void stop() throws Exception {
        webSocketServer.close(h -> {
            LOGGER.info("PubSub service closed");
        });
    }

    @TextCommand(command = "UNSUBSCRIBE")
    public Response unsubscribeHandler(@Context WebSocketContext context, JsonObject data) {
        Response resp = new Response(Response.Type.RESPONSE, data.getString("nonce"));

        JsonArray topics = data.getJsonArray("topics");
        if (topics == null || topics.isEmpty()) {
            resp.setError(Response.Error.ERR_BADTOPIC);
            return resp;
        }

        for (int i = 0; i < topics.size(); ++i) {
            String topic = topics.getString(i);
            Set<WebSocketContext> listeners = topicListeners.get(topic);
            if (listeners != null) {
                listeners.remove(context);
                if (listeners.isEmpty()) topicListeners.remove(topic);
            }

            Set<String> cTopics = clientTopics.get(context);
            if (cTopics != null) cTopics.remove(topic);
        }
        return resp;
    }

    @TextCommand(command = "SUBSCRIBE")
    public Response subscribeHandler(@Context WebSocketContext context, JsonObject data) {
        Response resp = new Response(Response.Type.RESPONSE, data.getString("nonce"));

        JsonArray topics = data.getJsonArray("topics");
        if (topics == null || topics.isEmpty()) {
            resp.setError(Response.Error.ERR_BADTOPIC);
            return resp;
        }

        for (int i = 0; i < topics.size(); ++i) {
            String topic = topics.getString(i);
            Set<WebSocketContext> listeners = topicListeners.get(topic);
            if (listeners == null) {
                listeners = new HashSet<>();
                topicListeners.put(topic, listeners);
            }
            listeners.add(context);

            Set<String> cTopics = clientTopics.get(context);
            if (cTopics == null) {
                cTopics = new HashSet<>();
                clientTopics.put(context, cTopics);
            }
            cTopics.add(topic);
        }

        return resp;
    }

    public static void main(String[] args) throws Exception {
        Vertx.vertx().deployVerticle(new PubSubService());
    }
}
