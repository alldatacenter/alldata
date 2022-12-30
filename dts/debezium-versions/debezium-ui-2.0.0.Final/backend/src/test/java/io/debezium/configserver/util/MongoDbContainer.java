/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.util;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class MongoDbContainer extends MongoDBContainer {

    private static final int CONTAINER_EXIT_CODE_OK = 0;
    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 60;

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbContainer.class);

    public MongoDbContainer(String imageName) {
        this(DockerImageName.parse(imageName));
    }

    public MongoDbContainer(DockerImageName imageName) {
        super(imageName);
        withExposedPorts(27017)
            .withCommand("--replSet", "rs0", "--auth")
            .withEnv("MONGO_INITDB_DATABASE", "admin")
            .withEnv("MONGO_INITDB_ROOT_USERNAME", "admin")
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", "admin")
            .withLogConsumer(new Slf4jLogConsumer(LOGGER))
            .withCopyFileToContainer(MountableFile.forClasspathResource("initialize-mongo-single.js"), "/docker-entrypoint-initdb.d/")
            .waitingFor(Wait.forLogMessage(".*waiting for connections on port.*", 2));
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        try {
            initReplicaSet();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String[] buildMongoEvalCommand(final String command) {
        if (Arrays.asList(getCommandParts()).contains("--auth")) {
            Map<String, String> envMap = getEnvMap();
            String username = envMap.getOrDefault("MONGO_INITDB_ROOT_USERNAME", "admin");
            String password = envMap.getOrDefault("MONGO_INITDB_ROOT_PASSWORD", "admin");
            return new String[]{"mongo", "-u", username, "-p", password, "--eval", command};
        }
        else {
            return new String[]{"mongo", "--eval", command};
        }
    }

    private void checkMongoNodeExitCode(final Container.ExecResult execResult) {
        if (execResult.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format("An error occurred: %s\n%s", execResult.getStdout(), execResult.getStderr());
            LOGGER.error(errorMessage);
            throw new ReplicaSetInitializationException(errorMessage);
        }
    }

    private String buildMongoWaitCommand() {
        return String.format(
                "var attempt = 0; " +
                "while (%s) " +
                "{ " +
                    "if (attempt > %d) {quit(1);} " +
                    "print('%s ' + attempt); sleep(100);  attempt++; " +
                " }",
                "db.runCommand( { isMaster: 1 } ).ismaster==false",
                AWAIT_INIT_REPLICA_SET_ATTEMPTS,
                "An attempt to await for a single node replica set initialization:"
        );
    }

    private void checkMongoNodeExitCodeAfterWaiting(
            final Container.ExecResult execResultWaitForMaster
    ) {
        if (execResultWaitForMaster.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format(
                    "A single node replica set was not initialized in a set timeout: %d attempts",
                    AWAIT_INIT_REPLICA_SET_ATTEMPTS
            );
            LOGGER.error(errorMessage);
            throw new ReplicaSetInitializationException(errorMessage);
        }
    }

    private void initReplicaSet() throws IOException, InterruptedException {
        LOGGER.debug("Initializing a single node replica set...");
        final ExecResult execResultInitRs = execInContainer(buildMongoEvalCommand("rs.initiate();"));
        LOGGER.debug(execResultInitRs.getStdout());
        checkMongoNodeExitCode(execResultInitRs);

        LOGGER.debug(
                "Awaiting for a single node replica set initialization up to {} attempts",
                AWAIT_INIT_REPLICA_SET_ATTEMPTS
        );
        final ExecResult execResultWaitForMaster = execInContainer(
                buildMongoEvalCommand(buildMongoWaitCommand())
        );
        LOGGER.debug(execResultWaitForMaster.getStdout());

        checkMongoNodeExitCodeAfterWaiting(execResultWaitForMaster);
    }

    public static class ReplicaSetInitializationException extends RuntimeException {
        ReplicaSetInitializationException(final String errorMessage) {
            super(errorMessage);
        }
    }

}
