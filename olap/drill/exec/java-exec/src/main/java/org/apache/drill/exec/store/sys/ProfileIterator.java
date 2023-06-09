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
package org.apache.drill.exec.store.sys;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.proto.UserBitShared.QueryProfile;
import org.apache.drill.exec.server.QueryProfileStoreContext;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.util.ImpersonationUtil;

/**
 * Base class for Profile Iterators
 */
public abstract class ProfileIterator implements Iterator<Object> {

  private static final int DEFAULT_NUMBER_OF_PROFILES_TO_FETCH = 4000;

  protected final QueryProfileStoreContext profileStoreContext;
  protected final String queryingUsername;
  protected final boolean isAdmin;

  private final int maxRecords;

  protected ProfileIterator(ExecutorFragmentContext context, int maxRecords) {
    this.profileStoreContext = context.getProfileStoreContext();
    this.queryingUsername = context.getQueryUserName();
    this.isAdmin = hasAdminPrivileges(context);
    this.maxRecords = maxRecords;
  }

  //Returns an iterator for authorized profiles
  protected Iterator<Entry<String, QueryProfile>> getAuthorizedProfiles(String username, boolean isAdministrator) {
    if (maxRecords == 0) {
      return Collections.emptyIterator();
    }

    if (isAdministrator) {
      return getProfiles(0, maxRecords);
    }


    /*
      For non-administrators getting profiles by range might not return exact number of requested profiles
      since some extracted profiles may belong to different users.
      In order not to extract all profiles, proceed extracting profiles based on max between default and given maxRecords
      until number of authorized user's profiles equals to the provided limit.
      Using max between default and given maxRecords will be helpful in case maxRecords number is low (ex: 1).
      Reading profiles in a bulk will be much faster.
     */
    List<Entry<String, QueryProfile>> authorizedProfiles = new LinkedList<>();
    int skip = 0;
    int take = Math.max(maxRecords, DEFAULT_NUMBER_OF_PROFILES_TO_FETCH);

    while (skip < Integer.MAX_VALUE) {
      Iterator<Entry<String, QueryProfile>> profiles = getProfiles(skip, take);
      int fetchedProfilesCount = 0;
      while (profiles.hasNext()) {
        fetchedProfilesCount++;
        Entry<String, QueryProfile> profileKVPair = profiles.next();
        // check if user matches
        if (profileKVPair.getValue().getUser().equals(username)) {
          authorizedProfiles.add(profileKVPair);
          // if we have all needed authorized profiles, return iterator
          if (authorizedProfiles.size() == maxRecords) {
            return authorizedProfiles.iterator();
          }
        }
      }

      // if returned number of profiles is less then given range then there are no more profiles in the store
      // return all found authorized profiles
      if (fetchedProfilesCount != take) {
        return authorizedProfiles.iterator();
      }

      try {
        // since we request profiles in batches, define number of profiles to skip
        // if we hit integer overflow return all found authorized profiles
        skip = Math.addExact(skip, take);
      } catch (ArithmeticException e) {
        return authorizedProfiles.iterator();
      }
    }

    return authorizedProfiles.iterator();
  }

  protected long computeDuration(long startTime, long endTime) {
    if (endTime > startTime && startTime > 0) {
      return (endTime - startTime);
    } else {
      return 0;
    }
  }

  /**
   * Returns profiles based of given range.
   *
   * @param skip number of profiles to skip
   * @param take number of profiles to return
   * @return profiles iterator
   */
  protected abstract Iterator<Entry<String, QueryProfile>> getProfiles(int skip, int take);

  /**
   * Checks is current user has admin privileges.
   *
   * @param context fragment context
   * @return true if user is admin, false otherwise
   */
  private boolean hasAdminPrivileges(ExecutorFragmentContext context) {
    OptionManager options = context.getOptions();
    if (context.isUserAuthenticationEnabled() &&
        !ImpersonationUtil.hasAdminPrivileges(
            context.getQueryUserName(),
            ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(options),
            ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(options))) {
      return false;
    }

    //Passed checks
    return true;
  }

}
