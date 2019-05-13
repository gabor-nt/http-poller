package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  private HashMap<String, JsonObject> services = new HashMap<>();
  private DBConnector connector;
  private BackgroundPoller poller = new BackgroundPoller();
  private Logger logger = LoggerFactory.getLogger(this.getClass());


  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    connector.query("SELECT * FROM service;").setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        asyncResult.result().getRows().forEach(row -> services.put(row.getString("url"), row.put("status", "UNKNOWN")));
        logger.info("Services from DB:" + services.keySet());
      } else {
        logger.error("DB connection issue", asyncResult.cause());
      }
    });
    vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(services));
    setRoutes(router);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8080, result -> {
          if (result.succeeded()) {
            logger.info("KRY code test service started");
            startFuture.complete();
          } else {
            startFuture.fail(result.cause());
          }
        });
  }

  private void setRoutes(Router router) {
    setStaticRoute(router);
    setListServices(router);
    setPostService(router);
    setDeleteService(router);
  }

  private void setDeleteService(Router router) {
    router.delete("/service/:service").handler(req -> {
      try {
        String service = URLDecoder.decode(req.pathParam("service"), "UTF-8");
        logger.info("Deleting: " + service);
        services.remove(service);
        connector = new DBConnector(vertx);
        connector.query("DELETE FROM service WHERE url=?", new JsonArray().add(service)).setHandler(asyncResult -> {
          if (asyncResult.succeeded()) {
            req.response()
                .putHeader("content-type", "text/plain")
                .end("OK");
          } else {
            logger.error("Failed to delete", asyncResult.cause());
            req.response()
                .putHeader("content-type", "text/plain")
                .end("Eek");
          }
        });
      } catch (UnsupportedEncodingException e) {
        req.response()
            .putHeader("content-type", "text/plain")
            .end("Eek");
      }
    });
  }

  private void setPostService(Router router) {
    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      JsonObject service = buildServiceObject(jsonBody.getString("url"), jsonBody.getString("name"));
      connector = new DBConnector(vertx);
      connector.query("INSERT INTO service (url, name, createdAt) values (?, ?, ?)",
          new JsonArray()
              .add(service.getString("url"))
              .add(service.getString("name"))
              .add(service.getString("createdAt"))
      ).setHandler(asyncResult -> {
        if (asyncResult.succeeded()) {
          services.put(service.getString("url"), service);
          req.response()
              .putHeader("content-type", "text/plain")
              .end("OK");
        } else {
          logger.error("post failed", asyncResult.cause());
          req.response()
              .putHeader("content-type", "text/plain")
              .end("Eek");
        }
      });
    });
  }

  private JsonObject buildServiceObject(String url, String name) {
    return new JsonObject()
        .put("url", url)
        .put("name", name != null ? name : url)
        .put("createdAt", Instant.now())
        .put("status", "UNKNOWN");
  }

  private void setListServices(Router router) {
    router.get("/service").handler(req -> {
      List<JsonObject> jsonServices = new ArrayList<>(services.values());
      req.response()
          .putHeader("content-type", "application/json")
          .end(new JsonArray(jsonServices).encode());
    });
  }

  private void setStaticRoute(Router router) {
    router.route("/*").handler(StaticHandler.create());
  }

}



