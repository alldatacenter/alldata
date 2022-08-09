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

package org.apache.ambari.server.serveraction.kerberos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * DeconstructedPrincipal manages the different parts of a principal and can be used to get a
 * normalized principal value
 * <p/>
 * A "normalized" principal has the following forms:
 * <ul>
 * <li>primary/instance@realm</li>
 * <li>primary@realm</li>
 * </ul>
 * <p/>
 * This class will create a DeconstructedPrincipal from a String containing a principal using
 * {@link DeconstructedPrincipal#valueOf(String, String)}
 */
public class DeconstructedPrincipal {
  /**
   * Regular expression to parse the different principal formats:
   * <ul>
   * <li>primary/instance@REALM</li>
   * <li>primary@REALM</li>
   * <li>primary/instance</li>
   * <li>primary</li>
   * </ul>
   */
  private static Pattern PATTERN_PRINCIPAL = Pattern.compile("^([^ /@]+)(?:/([^ /@]+))?(?:@(.+)?)?$");

  /**
   * A String containing the "primary" component of a principal
   */
  private final String primary;

  /**
   * A String containing the "instance" component of a principal
   */
  private final String instance;

  /**
   * A String containing the "realm" component of a principal
   */
  private final String realm;

  /**
   * A String containing the principal name portion of the principal.
   * The principal name is the combination of the primary and instance components.
   * This value is generated using the primary, instance, and realm components.
   */
  private final String principalName;

  /**
   * A String containing the complete normalized principal
   * The normalized principal is the combination of the primary, instance, and realm components.
   * This value is generated using the primary, instance, and realm components.
   */
  private final String normalizedPrincipal;

  /**
   * Given a principal and a default realm, creates a new DeconstructedPrincipal
   * <p/>
   * If the supplied principal does not have a realm component, the default realm (supplied) will be
   * used.
   *
   * @param principal    a String containing the principal to deconstruct
   * @param defaultRealm a String containing the default realm
   * @return a new DeconstructedPrincipal
   */
  public static DeconstructedPrincipal valueOf(String principal, @Nullable String defaultRealm) {
    if (principal == null) {
      throw new IllegalArgumentException("The principal may not be null");
    }

    Matcher matcher = PATTERN_PRINCIPAL.matcher(principal);

    if (matcher.matches()) {
      String primary = matcher.group(1);
      String instance = matcher.group(2);
      String realm = matcher.group(3);

      if ((realm == null) || realm.isEmpty()) {
        realm = defaultRealm;
      }

      return new DeconstructedPrincipal(primary, instance, realm);
    } else {
      throw new IllegalArgumentException(String.format("Invalid principal value: %s", principal));
    }
  }


  /**
   * Constructs a new DeconstructedPrincipal
   *
   * @param primary  a String containing the "primary" component of the principal
   * @param instance a String containing the "instance" component of the principal
   * @param realm    a String containing the "realm" component of the principal
   */
  protected DeconstructedPrincipal(String primary, String instance, String realm) {
    this.primary = primary;
    this.instance = instance;
    this.realm = realm;

    StringBuilder builder = new StringBuilder();

    if (this.primary != null) {
      builder.append(primary);
    }

    if (this.instance != null) {
      builder.append('/');
      builder.append(this.instance);
    }

    this.principalName = builder.toString();

    if (this.realm != null) {
      builder.append('@');
      builder.append(this.realm);
    }

    this.normalizedPrincipal = builder.toString();
  }

  /**
   * Gets the primary component of this DeconstructedPrincipal
   *
   * @return a String containing the "primary" component of this DeconstructedPrincipal
   */
  public String getPrimary() {
    return primary;
  }

  /**
   * Gets the instance component of this DeconstructedPrincipal
   *
   * @return a String containing the "instance" component of this DeconstructedPrincipal
   */
  public String getInstance() {
    return instance;
  }

  /**
   * Gets the realm component of this DeconstructedPrincipal
   *
   * @return a String containing the "realm" component of this DeconstructedPrincipal
   */
  public String getRealm() {
    return realm;
  }

  /**
   * Gets the constructed principal name for this DeconstructedPrincipal
   * <p/>
   * The principal name is the combination of the primary and instance components:
   * <ul>
   * <li>primary/instance</li>
   * <li>primary</li>
   * </ul>
   *
   * @return a String containing the "realm" component of this DeconstructedPrincipal
   */
  public String getPrincipalName() {
    return principalName;
  }

  /**
   * Gets the constructed normalized  principal for this DeconstructedPrincipal
   * <p/>
   * The normalized principal is the combination of the primary, instance, and realm components:
   * <ul>
   * <li>primary/instance@realm</li>
   * <li>primary@realm</li>
   * </ul>
   *
   * @return a String containing the "realm" component of this DeconstructedPrincipal
   */
  public String getNormalizedPrincipal() {
    return normalizedPrincipal;
  }
}
