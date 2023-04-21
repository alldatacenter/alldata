package org.dromara.cloudeon;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.dromara.cloudeon.verticle.CommandExecuteVerticle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
@Slf4j
@SpringBootApplication
public class CloudEonApplication {

	public static void main(String[] args) {

		SpringApplication.run(CloudEonApplication.class, args);
	}


    @Bean("cloudeonVertx")
    public Vertx cloudeonVertx() {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new CommandExecuteVerticle(), new DeploymentOptions()
                        .setWorker(true)
                        .setWorkerPoolName("dedicated-pool")
                        .setMaxWorkerExecuteTime(2000)
                        .setWorkerPoolSize(5)
                )
                .onSuccess(id -> {
                    log.info("Deployed CommandExecuteVerticle verticle " + id);
                });

        return vertx;
    }


}
