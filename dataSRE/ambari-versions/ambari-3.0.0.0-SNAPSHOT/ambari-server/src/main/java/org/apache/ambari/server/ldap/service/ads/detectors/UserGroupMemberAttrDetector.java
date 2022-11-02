/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import javax.inject.Inject;

import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.directory.api.ldap.model.entry.Entry;

public class UserGroupMemberAttrDetector extends OccurrenceAndWeightBasedDetector {

  private enum UserGroupMemberAttr {
    MEMBER_OF("memberOf", 1),
    IS_MEMBER_OF("ismemberOf", 1);

    private String attrName;
    private Integer weight;

    UserGroupMemberAttr(String attr, Integer weght) {
      this.attrName = attr;
      this.weight = weght;
    }

    Integer weight() {
      return this.weight;
    }

    String attrName() {
      return this.attrName;
    }

  }

  @Inject
  public UserGroupMemberAttrDetector() {
    for (UserGroupMemberAttr userGroupMemberAttr : UserGroupMemberAttr.values()) {
      occurrenceMap().put(userGroupMemberAttr.attrName(), 0);
      weightsMap().put(userGroupMemberAttr.attrName(), userGroupMemberAttr.weight);
    }
  }

  @Override
  protected boolean applies(Entry entry, String attribute) {
    return entry.containsAttribute(attribute);
  }

  @Override
  public String detectedProperty() {
    return AmbariServerConfigurationKey.USER_GROUP_MEMBER_ATTRIBUTE.key();
  }
}
