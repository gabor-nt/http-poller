package se.kry.codetest.repository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import se.kry.codetest.DBConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServiceRepository {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final DBConnector connector;

  private final HashMap<String, JsonObject> services = new HashMap<>();

  public ServiceRepository(DBConnector connector) {
    this.connector = connector;
  }

  public Future<Boolean> init() {
    Future<Boolean> statusFuture = Future.future();
    connector.query("SELECT * FROM service;").setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        asyncResult.result().getRows().forEach(row -> services.put(row.getString("url"), row.put("status", "UNKNOWN")));
        logger.info("Services from DB:" + services.keySet());
        statusFuture.complete(true);
      } else {
        logger.error("DB connection issue", asyncResult.cause());
        statusFuture.fail(asyncResult.cause());
      }
    });
    return statusFuture;
  }

  public List<JsonObject> getServices() {
    return new ArrayList<>(services.values());
  }

  public Future<ResultSet> remove(String service) {
    services.remove(service);
    return connector.query("DELETE FROM service WHERE url=?", new JsonArray().add(service));
  }

  public Future<ResultSet> upsert(JsonObject service) {
    services.put(service.getString("url"), service);
    return connector.query("INSERT OR REPLACE INTO service (url, name, createdAt)" +
            " values (?," +
            " ?," +
            " COALESCE((SELECT createdAt FROM service WHERE url = ?), ?)" +
            ")",
        new JsonArray()
            .add(service.getString("url"))
            .add(service.getString("name"))
            .add(service.getString("url"))
            .add(service.getString("createdAt"))
    );
  }
}
