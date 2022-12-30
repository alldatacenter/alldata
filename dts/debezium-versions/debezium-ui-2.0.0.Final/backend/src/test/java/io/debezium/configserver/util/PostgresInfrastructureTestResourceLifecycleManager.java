/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.util;

import java.util.Map;

public class PostgresInfrastructureTestResourceLifecycleManager extends AbstractInfrastructureTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Infrastructure.startContainers(Infrastructure.DATABASE.POSTGRES);
        return super.start();
    }

    @Override
    public void stop() {
        Infrastructure.stopContainers(Infrastructure.DATABASE.POSTGRES);
        super.stop();
    }
}

