package org.dromara.cloudeon.vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

import java.util.Random;

public class CmdMain {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new CmdVerticle(), new DeploymentOptions()
                    .setWorker(true)
                    .setWorkerPoolName("dedicated-pool")
                    .setMaxWorkerExecuteTime(2000)
                    .setWorkerPoolSize(5)
            )
            .onSuccess(id -> {
              System.out.println("Deployed worker verticle " + id);
            });

    EventBus eventBus = vertx.eventBus();

    vertx.createHttpServer().requestHandler(request -> {
      int i = new Random().nextInt(100);
      eventBus.request("command", i, reply -> {
        if (reply.succeeded()) {
          request.response().putHeader("content-type", "text/plain").end(reply.result().body().toString());
        } else {
          reply.cause().printStackTrace();
        }
      });

      System.out.println(" http server main thread end , request: " + i + " ....");
    }).listen(8080);
  }



}
