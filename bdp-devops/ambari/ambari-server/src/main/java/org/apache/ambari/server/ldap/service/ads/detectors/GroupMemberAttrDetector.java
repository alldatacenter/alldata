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

public class GroupMemberAttrDetector extends OccurrenceAndWeightBasedDetector {

  enum GroupMemberAttr {

    MEMBER("member", 1),
    MEMBER_UID("memberUid", 1),
    UNIQUE_MEMBER("uniqueMember", 1);

    private String attrName;
    private Integer weight;

    GroupMemberAttr(String attr, Integer weght) {
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
  public GroupMemberAttrDetector() {
    for (GroupMemberAttr groupMemberAttr : GroupMemberAttr.values()) {
      occurrenceMap().put(groupMemberAttr.attrName(), 0);
      weightsMap().put(groupMemberAttr.attrName(), groupMemberAttr.weight());
    }
  }

  @Override
  protected boolean applies(Entry entry, String attribute) {
    return entry.containsAttribute(attribute);
  }

  @Override
  public String detectedProperty() {
    return AmbariServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE.key();
  }
}
