/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.security.authentication.kerberos;

import static java.util.stream.Collectors.toSet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.AccountDisabledException;
import org.apache.ambari.server.security.authentication.AmbariUserDetailsImpl;
import org.apache.ambari.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.apache.ambari.server.security.authentication.TooManyLoginFailuresException;
import org.apache.ambari.server.security.authentication.tproxy.AmbariTProxyConfiguration;
import org.apache.ambari.server.security.authentication.tproxy.TrustedProxyAuthenticationDetails;
import org.apache.ambari.server.security.authentication.tproxy.TrustedProxyAuthenticationNotAllowedException;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.google.inject.Provider;

/**
 * AmbariProxiedUserDetailsService is a {@link UserDetailsService} that handles proxied users via the
 * Trusted Proxy protocol.
 * <p>
 * If trusted proxy support is enabled and a proxied user is declared, the proxy user and remote host is validated.
 * If all criteria is met and  a user account with the the proxied user's username exist, that user
 * account will be set as the authenticated user.
 */
@Component
public class AmbariProxiedUserDetailsService implements UserDetailsService {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariProxiedUserDetailsService.class);

  private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("^(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$");
  private static final Pattern IP_ADDRESS_RANGE_PATTERN = Pattern.compile("^((?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3})/(\\d{1,2})$");

  @Inject
  private Provider<AmbariTProxyConfiguration> ambariTProxyConfigurationProvider;

  private final Configuration configuration;

  private final Users users;

  /**
   * Constructor.
   * <p>
   * Given the Ambari {@link Configuration}, initializes the {@link KerberosName} class using
   * the <code>auth-to-local</code> rules from {@link AmbariKerberosAuthenticationProperties#getAuthToLocalRules()}.
   *
   * @param configuration the Ambari configuration data
   * @param users         the Ambari users access object
   */
  AmbariProxiedUserDetailsService(Configuration configuration, Users users) {
    this.configuration = configuration;
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String proxiedUserName) throws UsernameNotFoundException {
    return loadProxiedUser(proxiedUserName, null, null);
  }

  public UserDetails loadProxiedUser(String proxiedUserName,
                                     String proxyUserName,
                                     TrustedProxyAuthenticationDetails trustedProxyAuthenticationDetails)
      throws AuthenticationException {

    LOG.info("Proxy user {} specified {} as proxied user.", proxyUserName, proxiedUserName);

    if (StringUtils.isEmpty(proxiedUserName)) {
      String message = "No proxied username was specified.";
      LOG.warn(message);
      throw new UsernameNotFoundException(message);
    }

    if (trustedProxyAuthenticationDetails == null) {
      String message = "Trusted proxy details have not been provided.";
      LOG.warn(message);
      throw new TrustedProxyAuthenticationNotAllowedException(message);
    }

    AmbariTProxyConfiguration tProxyConfiguration = ambariTProxyConfigurationProvider.get();

    // Make sure the trusted proxy support is enabled
    if (!tProxyConfiguration.isEnabled()) {
      String message = "Trusted proxy support is not enabled.";
      LOG.warn(message);
      throw new TrustedProxyAuthenticationNotAllowedException(message);
    }

    // Validate the host...
    if (!validateHost(tProxyConfiguration, proxyUserName, trustedProxyAuthenticationDetails.getRemoteAddress())) {
      String message = String.format("Trusted proxy is not allowed for %s -> %s: host match not found.", proxyUserName, proxiedUserName);
      LOG.warn(message);
      throw new TrustedProxyAuthenticationNotAllowedException(message);
    }

    // Validate the user...
    if (!validateUser(tProxyConfiguration, proxyUserName, proxiedUserName)) {
      String message = String.format("Trusted proxy is not allowed for %s -> %s: user match not found.", proxyUserName, proxiedUserName);
      LOG.warn(message);
      throw new TrustedProxyAuthenticationNotAllowedException(message);
    }

    // Retrieve the userEntity so we can validate the groups now....
    // We also need this later if all validations succeed.
    UserEntity userEntity = users.getUserEntity(proxiedUserName);
    if (userEntity == null) {
      String message = String.format("Failed to find an account for the proxied user, %s.", proxiedUserName);
      LOG.warn(message);
      throw new UsernameNotFoundException(message);
    }

    // Validate the proxied user's groups
    if (!validateGroup(tProxyConfiguration, proxyUserName, userEntity)) {
      String message = String.format("Trusted proxy is not allowed for %s -> %s: group match not found.", proxyUserName, proxiedUserName);
      LOG.warn(message);
      throw new TrustedProxyAuthenticationNotAllowedException(message);
    }

    return createUserDetails(userEntity);
  }

  boolean validateGroup(AmbariTProxyConfiguration tProxyConfiguration, String proxyUserName, UserEntity userEntity) {
    String allowedGroups = tProxyConfiguration.getAllowedGroups(proxyUserName);

    if (StringUtils.isNotEmpty(allowedGroups)) {
      Set<String> groupSpecs = Arrays.stream(allowedGroups.split("\\s*,\\s*"))
          .map(s -> s.trim().toLowerCase())
          .collect(toSet());

      if (groupSpecs.contains("*")) {
        return true;
      } else {
        Set<MemberEntity> memberEntities = userEntity.getMemberEntities();
        if (memberEntities != null) {
          for (MemberEntity memberEntity : memberEntities) {
            GroupEntity group = memberEntity.getGroup();
            if (group != null) {
              String groupName = group.getGroupName();
              if (StringUtils.isNotEmpty(groupName) && groupSpecs.contains(groupName.toLowerCase())) {
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }

  boolean validateUser(AmbariTProxyConfiguration tProxyConfiguration, String proxyUserName, String proxiedUserName) {
    String allowedUsers = tProxyConfiguration.getAllowedUsers(proxyUserName);

    if (StringUtils.isNotEmpty(allowedUsers)) {
      String[] userSpecs = allowedUsers.split("\\s*,\\s*");
      for (String userSpec : userSpecs) {
        if ("*".equals(userSpec) || (userSpec.equalsIgnoreCase(proxiedUserName))) {
          return true;
        }
      }
    }

    return false;
  }

  boolean validateHost(AmbariTProxyConfiguration tProxyConfiguration, String proxyUserName, String remoteAddress) {
    String allowedHosts = tProxyConfiguration.getAllowedHosts(proxyUserName);

    if (StringUtils.isNotEmpty(allowedHosts)) {
      Set<String> hostSpecs = Arrays.stream(allowedHosts.split("\\s*,\\s*"))
          .map(s -> s.trim().toLowerCase())
          .collect(toSet());

      if (hostSpecs.contains("*")) {
        return true;
      } else {
        for (String hostSpec : hostSpecs) {
          if (isIPAddress(hostSpec)) {
            if (hostSpec.equals(remoteAddress)) {
              return true;
            }
          } else if (isIPAddressRange(hostSpec)) {
            if (isInIpAddressRange(hostSpec, remoteAddress)) {
              return true;
            }
          } else if (matchesHostname(hostSpec, remoteAddress)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  boolean matchesHostname(String hostSpec, String remoteAddress) {
    // We assume this is a hostname... convert it to an IP address
    try {
      String ipAddress = getIpAddress(hostSpec);
      if (StringUtils.isNotEmpty(ipAddress) && ipAddress.equals(remoteAddress)) {
        return true;
      }
    } catch (Throwable t) {
      LOG.warn("Invalid hostname in host specification, skipping: " + hostSpec, t);
    }

    return false;
  }

  String getIpAddress(String hostname) throws UnknownHostException {
    InetAddress inetAddress = InetAddress.getByName(hostname);
    return (inetAddress == null) ? null : inetAddress.getHostAddress();
  }

  /**
   * Determines if an IP address is in the subnet specified by the CIDR value.
   * <p>
   * The logic for ths method comes from an example found at
   * https://stackoverflow.com/questions/4209760/validate-an-ip-address-with-mask.
   * <p>
   * A CIDR is an IP address with a mask, denoting a subnet.  For example, <code>192.168.1.0/24</code> contains all
   * IP addresses in the range of <code>192.168.1.0</code> through <code>192.168.1.255</code>. See https://www.ipaddressguide.com/cidr
   * for an on-line calculator.
   *
   * @param cidr      a CIDR
   * @param ipAddress the IP address to test
   * @return <code>true</code> if the IP Address is withing the range specified by the CIDR; <code>false</code> otherwise.
   */
  boolean isInIpAddressRange(String cidr, String ipAddress) {
    Matcher matcher = IP_ADDRESS_RANGE_PATTERN.matcher(cidr);

    if (matcher.matches() && matcher.groupCount() == 2) {
      try {
        String hostSpecIPAddress = matcher.group(1);
        String hostSpecBits = matcher.group(2);
        int hostSpecIPAddressInt = ipAddressToInt(hostSpecIPAddress);
        int remoteAddressInt = ipAddressToInt(ipAddress);

        int mask = -1 << (32 - Integer.valueOf(hostSpecBits));
        return ((hostSpecIPAddressInt & mask) == (remoteAddressInt & mask));
      } catch (Throwable t) {
        LOG.warn("Invalid CIDR in host specification, skipping: " + cidr, t);
      }
    }

    return false;
  }

  private int ipAddressToInt(String s) throws UnknownHostException {
    InetAddress inetAddress = InetAddress.getByName(s);
    byte[] b = inetAddress.getAddress();
    return ((b[0] & 0xFF) << 24) |
        ((b[1] & 0xFF) << 16) |
        ((b[2] & 0xFF) << 8) |
        ((b[3] & 0xFF) << 0);
  }

  private boolean isIPAddressRange(String hostSpec) {
    return IP_ADDRESS_RANGE_PATTERN.matcher(hostSpec).matches();
  }

  private boolean isIPAddress(String hostSpec) {
    return IP_ADDRESS_PATTERN.matcher(hostSpec).matches();
  }

  private UserDetails createUserDetails(UserEntity userEntity) {
    String username = userEntity.getUserName();

    // Ensure the user account is allowed to log in
    try {
      users.validateLogin(userEntity, username);
    } catch (AccountDisabledException | TooManyLoginFailuresException e) {
      if (configuration.showLockedOutUserMessage()) {
        throw e;
      } else {
        // Do not give away information about the existence or status of a user
        throw new InvalidUsernamePasswordCombinationException(username, false, e);
      }
    }

    return new AmbariUserDetailsImpl(new User(userEntity), null, users.getUserAuthorities(userEntity));
  }
}
