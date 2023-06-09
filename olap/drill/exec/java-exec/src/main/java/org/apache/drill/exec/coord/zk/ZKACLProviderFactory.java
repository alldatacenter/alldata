/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.coord.zk;

import org.apache.drill.common.config.DrillConfig;
import static org.apache.drill.exec.ExecConstants.ZK_ACL_PROVIDER;
import static org.apache.drill.exec.ExecConstants.ZK_APPLY_SECURE_ACL;

import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;

import java.lang.reflect.Constructor;
import java.util.Collection;

/**
 * This factory returns a {@link ZKACLProviderDelegate} which will be used to set ACLs on Drill ZK nodes
 * If secure ACLs are required, the {@link ZKACLProviderFactory} looks up and instantiates a {@link ZKACLProviderDelegate}
 * specified in the config file. Else it returns the {@link ZKDefaultACLProvider}
 */
public class ZKACLProviderFactory {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ZKACLProviderFactory.class);

    public static ZKACLProviderDelegate getACLProvider(DrillConfig config, String drillClusterPath, BootStrapContext context)
            throws DrillbitStartupException {
        ZKACLContextProvider stateProvider = new ZKACLContextProviderImpl(drillClusterPath);

        if (config.getBoolean(ZK_APPLY_SECURE_ACL)) {
            logger.trace("Using secure ZK ACL. Drill cluster path " + drillClusterPath);
            ZKACLProviderDelegate aclProvider = findACLProvider(config, stateProvider, context);
            return aclProvider;
        }
        logger.trace("Using un-secure default ZK ACL");
        final ZKDefaultACLProvider defaultAclProvider = new ZKDefaultACLProvider(stateProvider);
        return new ZKACLProviderDelegate(defaultAclProvider);
    }

    public static ZKACLProviderDelegate findACLProvider(DrillConfig config, ZKACLContextProvider contextProvider,
                                                        BootStrapContext context) throws DrillbitStartupException {
        if (!config.hasPath(ZK_ACL_PROVIDER)) {
            throw new DrillbitStartupException(String.format("BOOT option '%s' is missing in config.", ZK_ACL_PROVIDER));
        }

        final String aclProviderName = config.getString(ZK_ACL_PROVIDER);

        if (Strings.isNullOrEmpty(aclProviderName)) {
            throw new DrillbitStartupException(String.format("Invalid value '%s' for BOOT option '%s'", aclProviderName,
                    ZK_ACL_PROVIDER));
        }

        ScanResult scan = context.getClasspathScan();
        final Collection<Class<? extends ZKACLProvider>> aclProviderImpls =
                scan.getImplementations(ZKACLProvider.class);
        logger.debug("Found ZkACLProvider implementations: {}", aclProviderImpls);

        for (Class<? extends ZKACLProvider> clazz : aclProviderImpls) {
          final ZKACLProviderTemplate template = clazz.getAnnotation(ZKACLProviderTemplate.class);
          if (template == null) {
            logger.warn("{} doesn't have {} annotation. Skipping.", clazz.getCanonicalName(),
                    ZKACLProviderTemplate.class);
            continue;
          }

          if (template.type().equalsIgnoreCase(aclProviderName)) {
            Constructor<?> validConstructor = null;
            Class constructorArgumentClass = ZKACLContextProvider.class;
            for (Constructor<?> c : clazz.getConstructors()) {
              Class<?>[] params = c.getParameterTypes();
              if (params.length == 1 && params[0] == constructorArgumentClass) {
                validConstructor = c;
                break;
              }
            }
            if (validConstructor == null) {
              logger.warn("Skipping ZKACLProvider implementation class '{}' since it doesn't " +
                       "implement a constructor [{}({})]", clazz.getCanonicalName(), clazz.getName(),
                      constructorArgumentClass.getName());
              continue;
            }
            try {
              final ZKACLProvider aclProvider = (ZKACLProvider) validConstructor.newInstance(contextProvider);
              return new ZKACLProviderDelegate(aclProvider);
            } catch (ReflectiveOperationException e ) {
               throw new DrillbitStartupException(
                  String.format("Failed to create and initialize the ZKACLProvider class '%s'",
                                    clazz.getCanonicalName()), e);
            }
          }
        }
        String errMsg = String.format("Failed to find the implementation of '%s' for type '%s'",
                ZKACLProvider.class.getCanonicalName(), aclProviderName);
        logger.error(errMsg);
        throw new DrillbitStartupException(errMsg);
    }
}
