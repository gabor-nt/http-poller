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
import se.kry.codetest.repository.ServiceRepository;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  private BackgroundPoller poller;
  private ServiceRepository serviceRepository;
  private Logger logger = LoggerFactory.getLogger(this.getClass());


  @Override
  public void start(Future<Void> startFuture) {
    poller = new BackgroundPoller(vertx);
    serviceRepository = new ServiceRepository(new DBConnector(vertx));
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    serviceRepository.init().setHandler(result -> {
      if (result.succeeded()) {
        vertx.setPeriodic(1000 * 6, timerId -> poller.pollServices(serviceRepository.getServices()));
        setRoutes(router);
        vertx
            .createHttpServer()
            .requestHandler(router)
            .listen(8080, r -> {
              if (r.succeeded()) {
                logger.info("KRY code test service started");
                startFuture.complete();
              } else {
                startFuture.fail(r.cause());
              }
            });
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
        serviceRepository.remove(service).setHandler(asyncResult -> {
          if (asyncResult.succeeded()) {
            logger.info("Delete successful");
            req.response()
                .putHeader("content-type", "text/plain")
                .end("OK");
          } else {
            logger.error("Failed to delete", asyncResult.cause());
            req.response()
                .setStatusCode(500)
                .putHeader("content-type", "text/plain")
                .end("Internal error");
          }
        });
      } catch (UnsupportedEncodingException e) {
        req.response()
            .setStatusCode(400)
            .putHeader("content-type", "text/plain")
            .end("Invalid parameter: " + req.pathParam("service"));
      }
    });
  }

  private void setPostService(Router router) {
    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      JsonObject service;
      try {
        service = buildServiceObject(jsonBody.getString("url"), jsonBody.getString("name"));
      } catch (MalformedURLException e) {
        req.response()
            .setStatusCode(400)
            .putHeader("content-type", "text/plain")
            .end("Invalid url: " + jsonBody.getString("url"));
        return;
      }
      serviceRepository.upsert(service).setHandler(asyncResult -> {
        if (asyncResult.succeeded()) {
          logger.info("Post successful");
          req.response()
              .putHeader("content-type", "text/plain")
              .end("OK");
        } else {
          logger.error("Post failed", asyncResult.cause());
          req.response()
              .setStatusCode(500)
              .putHeader("content-type", "text/plain")
              .end("Internal error");
        }
      });
    });
  }

  private JsonObject buildServiceObject(String url, String name) throws MalformedURLException {
    return new JsonObject()
        .put("url", new URL(url).toString())
        .put("name", name != null ? name : url)
        .put("createdAt", Instant.now())
        .put("status", "UNKNOWN");
  }

  private void setListServices(Router router) {
    router.get("/service").handler(req -> {
      logger.info("List");
      List<JsonObject> jsonServices = serviceRepository.getServices();
      req.response()
          .putHeader("content-type", "application/json")
          .end(new JsonArray(jsonServices).encode());
    });
  }

  private void setStaticRoute(Router router) {
    router.route("/*").handler(StaticHandler.create());
  }

}



