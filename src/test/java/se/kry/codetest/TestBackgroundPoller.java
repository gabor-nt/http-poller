package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestBackgroundPoller {

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_valid_url(Vertx vertx, VertxTestContext testContext) {
    List<JsonObject> services = Collections.singletonList(new JsonObject().put("url", "https://www.kry.se"));
    Optional<Future<JsonObject>> optionalFuture = new BackgroundPoller(vertx).pollServices(services).stream().findFirst();
    assert (optionalFuture.isPresent());
    optionalFuture.ifPresent(future -> future.setHandler(result -> testContext.verify(() -> {
          assertEquals("OK", result.result().getString("status"));
          testContext.completeNow();
        })
    ));
  }

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_invalid_url(Vertx vertx, VertxTestContext testContext) {
    List<JsonObject> services = Collections.singletonList(new JsonObject().put("url", "www.kry.se"));
    Optional<Future<JsonObject>> optionalFuture = new BackgroundPoller(vertx).pollServices(services).stream().findFirst();
    assert (optionalFuture.isPresent());
    optionalFuture.ifPresent(future -> future.setHandler(result -> testContext.verify(() -> {
          assertEquals("FAIL", result.result().getString("status"));
          testContext.completeNow();
        })
    ));
  }
}
