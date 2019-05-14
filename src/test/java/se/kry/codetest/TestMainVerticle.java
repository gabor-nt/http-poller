package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Start a web server on localhost responding to path /service on port 8080")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void start_http_server(Vertx vertx, VertxTestContext testContext) {
    WebClient.create(vertx)
        .get(8080, "::1", "/service")
        .send(response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          JsonArray body = response.result().bodyAsJsonArray();
          assertEquals(0, body.size());
          testContext.completeNow();
        }));
  }

  @Test
  @DisplayName("post a new service")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void post_new_service(Vertx vertx, VertxTestContext testContext) {
    JsonObject payload = new JsonObject().put("url", "http://www.example.com").put("name", "example");
    WebClient.create(vertx)
        .post(8080, "::1", "/service")
        .sendJsonObject(payload, response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          String body = response.result().bodyAsString();
          assertEquals("OK", body);

          WebClient.create(vertx)
              .get(8080, "::1", "/service")
              .send(r -> testContext.verify(() -> {
                assertEquals(200, r.result().statusCode());
                JsonArray b = r.result().bodyAsJsonArray();
                assertEquals(1, b.size());
                JsonObject service = b.getJsonObject(0);
                assertEquals("http://www.example.com", service.getString("url"));
                assertEquals("example", service.getString("name"));
                assertEquals("UNKNOWN", service.getString("status"));
                assertNotNull(service.getString("createdAt"));
                testContext.completeNow();
              }));
        }));
  }

  @Test
  @DisplayName("post a new service without name")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void post_new_service_without_name(Vertx vertx, VertxTestContext testContext) {
    JsonObject payload = new JsonObject().put("url", "http://www.example.com");
    WebClient.create(vertx)
        .post(8080, "::1", "/service")
        .sendJsonObject(payload, response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          String body = response.result().bodyAsString();
          assertEquals("OK", body);

          WebClient.create(vertx)
              .get(8080, "::1", "/service")
              .send(r -> testContext.verify(() -> {
                assertEquals(200, r.result().statusCode());
                JsonArray b = r.result().bodyAsJsonArray();
                assertEquals(1, b.size());
                JsonObject service = b.getJsonObject(0);
                assertEquals("http://www.example.com", service.getString("url"));
                assertEquals("http://www.example.com", service.getString("name"));
                assertEquals("UNKNOWN", service.getString("status"));
                assertNotNull(service.getString("createdAt"));
                testContext.completeNow();
              }));
        }));
  }


  @Test
  @DisplayName("post a new service with invalid url")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void post_new_service_with_invalid_url(Vertx vertx, VertxTestContext testContext) {
    JsonObject payload = new JsonObject().put("url", "www.example.com");
    WebClient.create(vertx)
        .post(8080, "::1", "/service")
        .sendJsonObject(payload, response -> testContext.verify(() -> {
          assertEquals(400, response.result().statusCode());
          String body = response.result().bodyAsString();
          assertEquals("Invalid url: www.example.com", body);

          WebClient.create(vertx)
              .get(8080, "::1", "/service")
              .send(r -> testContext.verify(() -> {
                assertEquals(200, r.result().statusCode());
                JsonArray b = r.result().bodyAsJsonArray();
                assertEquals(0, b.size());
                testContext.completeNow();
              }));
        }));
  }

  @Test
  @DisplayName("delete an existing service")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void delete_existing_service(Vertx vertx, VertxTestContext testContext) throws Exception {
    String uri = "/service/" + URLEncoder.encode("http://www.example.com", "UTF-8");
    WebClient.create(vertx)
        .delete(8080, "::1", uri)
        .send(response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          String body = response.result().bodyAsString();
          assertEquals("OK", body);

          WebClient.create(vertx)
              .get(8080, "::1", "/service")
              .send(r -> testContext.verify(() -> {
                assertEquals(200, r.result().statusCode());
                JsonArray b = r.result().bodyAsJsonArray();
                assertEquals(0, b.size());
                testContext.completeNow();
              }));
        }));
  }
}
