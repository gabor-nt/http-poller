package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BackgroundPoller {

  public Future<List<String>> pollServices(HashMap<String, JsonObject> services) {
    //TODO
    return Future.failedFuture("TODO");
  }
}
