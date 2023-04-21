package org.dromara.cloudeon.vertx;

import io.vertx.core.AbstractVerticle;

public class CmdVerticle extends AbstractVerticle {
  // 接收command消息，然后用blockexecute执行，并且要同时执行多个command
  @Override
  public void start() {
    vertx.eventBus().consumer("command", message -> {
      Object actionId = message.body();
      // 1. 从message中获取command
      vertx.executeBlocking(future -> {
        System.out.println("开始处理命令：" + actionId);
        // Imagine this was a call to a blocking API to get the result
        try {
          Thread.sleep(5000);
        } catch (Exception ignore) {
        }
        String result = "ok!!! " + actionId;

        System.out.println("完成处理命令：" + result);
        future.complete(result);
      }, false, res -> {
        if (res.succeeded()) {
          message.reply(res.result());
        } else {
          message.fail(1, res.cause().getMessage());
        }
      });
      System.out.println("CmdVerticle end .... "+actionId);
    });
  }
}
