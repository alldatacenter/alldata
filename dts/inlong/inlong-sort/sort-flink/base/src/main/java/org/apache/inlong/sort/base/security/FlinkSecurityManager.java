/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.base.security;

import org.apache.inlong.sort.base.UserSystemExitException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.flink.configuration.ClusterOptions;
import org.apache.flink.configuration.ClusterOptions.UserSystemExitMode;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.IllegalConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Permission;

public class FlinkSecurityManager extends SecurityManager {

    static final Logger LOG = LoggerFactory.getLogger(FlinkSecurityManager.class);
    private static FlinkSecurityManager flinkSecurityManager;
    private final SecurityManager originalSecurityManager;
    private final ThreadLocal<Boolean> monitorUserSystemExit;
    private final UserSystemExitMode userSystemExitMode;
    private final boolean haltOnSystemExit;

    @VisibleForTesting
    FlinkSecurityManager(UserSystemExitMode userSystemExitMode, boolean haltOnSystemExit) {
        this(userSystemExitMode, haltOnSystemExit, System.getSecurityManager());
    }

    @VisibleForTesting
    FlinkSecurityManager(UserSystemExitMode userSystemExitMode, boolean haltOnSystemExit,
            SecurityManager originalSecurityManager) {
        this.monitorUserSystemExit = new InheritableThreadLocal();
        this.userSystemExitMode = (UserSystemExitMode) Preconditions.checkNotNull(userSystemExitMode);
        this.haltOnSystemExit = haltOnSystemExit;
        this.originalSecurityManager = originalSecurityManager;
    }

    @VisibleForTesting
    static FlinkSecurityManager fromConfiguration(Configuration configuration) {
        UserSystemExitMode userSystemExitMode =
                (UserSystemExitMode) configuration.get(ClusterOptions.INTERCEPT_USER_SYSTEM_EXIT);
        boolean haltOnSystemExit = (Boolean) configuration.get(ClusterOptions.HALT_ON_FATAL_ERROR);
        if (userSystemExitMode == UserSystemExitMode.DISABLED && !haltOnSystemExit) {
            return null;
        } else {
            LOG.info("FlinkSecurityManager is created with {} user system exit mode and {} exit", userSystemExitMode,
                    haltOnSystemExit ? "forceful" : "graceful");
            return new FlinkSecurityManager(userSystemExitMode, haltOnSystemExit);
        }
    }

    public static void setFromConfiguration(Configuration configuration) {
        FlinkSecurityManager flinkSecurityManager = fromConfiguration(configuration);
        if (flinkSecurityManager != null) {
            try {
                System.setSecurityManager(flinkSecurityManager);
            } catch (Exception var3) {
                throw new IllegalConfigurationException(String.format(
                        "Could not register security manager due to no permission to set a SecurityManager. Either update your existing SecurityManager to allow the permission or do not use security manager features (e.g., '%s: %s', '%s: %s')",
                        ClusterOptions.INTERCEPT_USER_SYSTEM_EXIT.key(),
                        ClusterOptions.INTERCEPT_USER_SYSTEM_EXIT.defaultValue(),
                        ClusterOptions.HALT_ON_FATAL_ERROR.key(), ClusterOptions.HALT_ON_FATAL_ERROR.defaultValue()),
                        var3);
            }
        }

        FlinkSecurityManager.flinkSecurityManager = flinkSecurityManager;
    }

    public static void monitorUserSystemExitForCurrentThread() {
        if (flinkSecurityManager != null) {
            flinkSecurityManager.monitorUserSystemExit();
        }

    }

    public static void unmonitorUserSystemExitForCurrentThread() {
        if (flinkSecurityManager != null) {
            flinkSecurityManager.unmonitorUserSystemExit();
        }

    }

    public void checkPermission(Permission perm) {
        if (this.originalSecurityManager != null) {
            this.originalSecurityManager.checkPermission(perm);
        }

    }

    public void checkPermission(Permission perm, Object context) {
        if (this.originalSecurityManager != null) {
            this.originalSecurityManager.checkPermission(perm, context);
        }

    }

    public void checkExit(int status) {
        if (this.userSystemExitMonitored()) {
            switch (this.userSystemExitMode) {
                case DISABLED:
                    break;
                case LOG:
                    LOG.warn("Exiting JVM with status {} is monitored: The system will exit due to this call.", status,
                            new UserSystemExitException());
                    break;
                case THROW:
                    throw new UserSystemExitException();
                default:
                    LOG.warn("No valid check exit mode configured: {}", this.userSystemExitMode);
            }
        }

        if (this.originalSecurityManager != null) {
            this.originalSecurityManager.checkExit(status);
        }

        if (this.haltOnSystemExit) {
            Runtime.getRuntime().halt(status);
        }

    }

    @VisibleForTesting
    void monitorUserSystemExit() {
        this.monitorUserSystemExit.set(true);
    }

    @VisibleForTesting
    void unmonitorUserSystemExit() {
        this.monitorUserSystemExit.set(false);
    }

    @VisibleForTesting
    boolean userSystemExitMonitored() {
        return Boolean.TRUE.equals(this.monitorUserSystemExit.get());
    }

    public static void forceProcessExit(int exitCode) {
        System.setSecurityManager((SecurityManager) null);
        if (flinkSecurityManager != null && flinkSecurityManager.haltOnSystemExit) {
            Runtime.getRuntime().halt(exitCode);
        } else {
            System.exit(exitCode);
        }

    }
}
