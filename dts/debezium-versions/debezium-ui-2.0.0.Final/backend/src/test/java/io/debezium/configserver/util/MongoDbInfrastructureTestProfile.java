/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.util;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Collections;
import java.util.List;

public class MongoDbInfrastructureTestProfile implements QuarkusTestProfile {
    @Override
    public List<TestResourceEntry> testResources() {
        return Collections.singletonList(new TestResourceEntry(MongoDbInfrastructureTestResourceLifecycleManager.class));
    }
}
