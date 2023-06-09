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
package org.apache.drill.exec.server.rest.auth;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.eclipse.jetty.security.AbstractLoginService.RolePrincipal;

import java.security.Principal;
import java.util.List;


/**
 * Captures Drill user credentials and privilege's of the session user.
 */
public class DrillUserPrincipal implements Principal {
  public static final String ANONYMOUS_USER = "anonymous";

  public static final String AUTHENTICATED_ROLE = "authenticated";

  public static final String ADMIN_ROLE = "admin";

  public static final String[] ADMIN_USER_ROLES = new String[]{AUTHENTICATED_ROLE, ADMIN_ROLE};

  public static final String[] NON_ADMIN_USER_ROLES = new String[]{AUTHENTICATED_ROLE};

  public static final List<RolePrincipal> ADMIN_PRINCIPALS =
      ImmutableList.of(new RolePrincipal(AUTHENTICATED_ROLE), new RolePrincipal(ADMIN_ROLE));

  public static final List<RolePrincipal> NON_ADMIN_PRINCIPALS =
      ImmutableList.of(new RolePrincipal(AUTHENTICATED_ROLE));

  private final String userName;

  private final boolean isAdmin;

  public DrillUserPrincipal(final String userName, final boolean isAdmin) {
    this.userName = userName;
    this.isAdmin = isAdmin;
  }

  public boolean isAdminUser() { return isAdmin; }

  @Override
  public String getName() {
    return userName;
  }

  /**
   * Is the user identified by this user principal can manage (read) the profile owned by the given user?
   *
   * @param profileOwner Owner of the profile.
   * @return true/false
   */
  public boolean canManageProfileOf(final String profileOwner) {
    return isAdmin || userName.equals(profileOwner);
  }

  /**
   * Is the user identified by this user principal can manage (cancel) the query issued by the given user?
   *
   * @param queryUser User who launched the query.
   * @return true/false
   */
  public boolean canManageQueryOf(final String queryUser) {
    return isAdmin || userName.equals(queryUser);
  }

  /**
   * {@link DrillUserPrincipal} for anonymous (auth disabled) mode.
   */
  public static class AnonDrillUserPrincipal extends DrillUserPrincipal {

    public AnonDrillUserPrincipal() {
      super(ANONYMOUS_USER, true /* in anonymous (auth disabled) mode all users are admins */);
    }
  }
}
