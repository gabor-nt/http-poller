package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

public class BackgroundPoller {
  private final WebClient client;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public BackgroundPoller(Vertx vertx) {
    client = WebClient.create(vertx);
  }

  public List<Future<JsonObject>> pollServices(List<JsonObject> services) {
    return services.parallelStream().map(this::test).collect(Collectors.toList());
  }

  private Future<JsonObject> test(JsonObject service) {
    String url = service.getString("url");
    logger.info("Testing:" + url);
    Future<JsonObject> statusFuture = Future.future();
    try {
      client.getAbs(url)
          .send(response -> {
            if (response.succeeded()) {
              statusFuture.complete(service.put("status", 200 == response.result().statusCode() ? "OK" : "FAIL"));
            } else {
              statusFuture.complete(service.put("status", "FAIL"));
            }
          });
    } catch (Exception e) {
      logger.error("Failed to test " + url, e);
      statusFuture.complete(service.put("status", "FAIL"));
    }
    return statusFuture;
  }
}
