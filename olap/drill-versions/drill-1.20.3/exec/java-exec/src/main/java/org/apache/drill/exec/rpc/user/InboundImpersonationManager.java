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
package org.apache.drill.exec.rpc.user;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared.UserCredentials;
import org.apache.drill.exec.server.options.OptionMetaData;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.server.options.TypeValidators.StringValidator;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Helper class to manage inbound impersonation.
 * <p/>
 * Impersonation policies format:
 * [
 *   {
 *    proxy_principals : { users : ["..."], groups : ["..."] },
 *    target_principals : { users : ["..."], groups : ["..."] }
 *   },
 *   {
 *    proxy_principals : { users : ["..."], groups : ["..."] },
 *    target_principals : { users : ["..."], groups : ["..."] }
 *   },
 *   ...
 * ]
 */
public class InboundImpersonationManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InboundImpersonationManager.class);

  private static final String STAR = "*";
  private static final ObjectMapper impersonationPolicyMapper = new ObjectMapper();

  private List<ImpersonationPolicy> impersonationPolicies;
  private String policiesString; // used to test if policies changed

  static {
    impersonationPolicyMapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
    impersonationPolicyMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
  }

  private static class ImpersonationPolicy {
    public UserGroupDefinition proxy_principals = new UserGroupDefinition();
    public UserGroupDefinition target_principals = new UserGroupDefinition();
  }

  private static class UserGroupDefinition {
    public Set<String> users = Sets.newHashSet();
    public Set<String> groups = Sets.newHashSet();
  }

  private static List<ImpersonationPolicy> deserializeImpersonationPolicies(final String impersonationPolicies)
      throws IOException {
    return impersonationPolicyMapper.readValue(impersonationPolicies,
        new TypeReference<List<ImpersonationPolicy>>() {});
  }

  /**
   * Validator for impersonation policies.
   */
  public static class InboundImpersonationPolicyValidator extends StringValidator {

    public InboundImpersonationPolicyValidator(String name) {
      super(name, null);
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      super.validate(v, metaData, manager);

      final List<ImpersonationPolicy> policies;
      try {
        policies = deserializeImpersonationPolicies(v.string_val);
      } catch (final IOException e) {
        throw UserException.validationError()
            .message("Invalid impersonation policies.\nDetails: %s", e.getMessage())
            .build(logger);
      }

      for (final ImpersonationPolicy policy : policies) {
        if (policy.proxy_principals.users.contains(STAR) ||
            policy.proxy_principals.groups.contains(STAR)) {
          throw UserException.validationError()
              .message("Proxy principals cannot have a wildcard entry.")
              .build(logger);
        }
      }
    }
  }

  /**
   * Checks if the proxy user is authorized to impersonate the target user based on the policies.
   *
   * @param proxyName  proxy user name
   * @param targetName target user name
   * @param policies   impersonation policies
   * @return true iff proxy user is authorized to impersonate the target user
   */
  private static boolean hasImpersonationPrivileges(final String proxyName, final String targetName,
                                                    final List<ImpersonationPolicy> policies) {
    final UserGroupInformation proxyUgi = ImpersonationUtil.createProxyUgi(proxyName);
    final Set<String> proxyGroups = Sets.newHashSet(proxyUgi.getGroupNames());
    final UserGroupInformation targetUgi = ImpersonationUtil.createProxyUgi(targetName);
    final Set<String> targetGroups = Sets.newHashSet(targetUgi.getGroupNames());
    for (final ImpersonationPolicy definition : policies) {
      // check if proxy user qualifies within this policy
      if (definition.proxy_principals.users.contains(proxyName) ||
          !Sets.intersection(definition.proxy_principals.groups, proxyGroups).isEmpty()) {
        // check if target qualifies within this policy
        if (definition.target_principals.users.contains(targetName) ||
            definition.target_principals.users.contains(STAR) ||
            !Sets.intersection(definition.target_principals.groups, targetGroups).isEmpty() ||
            definition.target_principals.groups.contains(STAR)) {
          return true;
        }
      }
    }
    return false;
  }

  @VisibleForTesting
  public static boolean hasImpersonationPrivileges(final String proxyName, final String targetName,
                                                   final String policiesString) throws IOException {
    return hasImpersonationPrivileges(proxyName, targetName,
        deserializeImpersonationPolicies(policiesString));
  }

  /**
   * Check if the current session user, as a proxy user, is authorized to impersonate the given target user
   * based on the system's impersonation policies.
   *
   * @param targetName target user name
   * @param session    user session
   */
  public void replaceUserOnSession(final String targetName, final UserSession session) {
    final String policiesString = session.getOptions().getOption(ExecConstants.IMPERSONATION_POLICY_VALIDATOR);
    if (!policiesString.equals(this.policiesString)) {
      try {
        impersonationPolicies = deserializeImpersonationPolicies(policiesString);
        this.policiesString = policiesString;
      } catch (final IOException e) {
        // This never happens. Impersonation policies must have been validated.
        logger.warn("Impersonation policies must have been validated.");
        throw new DrillRuntimeException("Failure while checking for impersonation policies.", e);
      }
    }

    final String proxyName = session.getCredentials().getUserName();
    if (!hasImpersonationPrivileges(proxyName, targetName, impersonationPolicies)) {
      throw UserException.permissionError()
          .message("Proxy user '%s' is not authorized to impersonate target user '%s'.", proxyName, targetName)
          .build(logger);
    }

    // replace session's user credentials
    final UserCredentials newCredentials = UserCredentials.newBuilder()
        .setUserName(targetName)
        .build();
    session.replaceUserCredentials(this, newCredentials);
  }
}
