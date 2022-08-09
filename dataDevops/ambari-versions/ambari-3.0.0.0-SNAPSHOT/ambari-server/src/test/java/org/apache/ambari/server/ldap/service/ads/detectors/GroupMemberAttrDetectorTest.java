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

import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Test suite for the attribute detector implementation
 */
public class GroupMemberAttrDetectorTest {

  private static final Logger LOG = LoggerFactory.getLogger(GroupMemberAttrDetector.class);

  @TestSubject
  GroupMemberAttrDetector groupMemberAttrDetector = new GroupMemberAttrDetector();

  @Test
  public void testShouldDetectAttributeBasedOnOccurrence() throws Exception {
    // GIVEN
    // Mimic a sample set of entries where group membership attributes  are different
    List<Entry> sampleEntryList = Lists.newArrayList();

    sampleEntryList.addAll(getSampleEntryList(GroupMemberAttrDetector.GroupMemberAttr.MEMBER_UID, 2));

    // this is the expected property to be detected as in the sample set the most entries have it
    sampleEntryList.addAll(getSampleEntryList(GroupMemberAttrDetector.GroupMemberAttr.UNIQUE_MEMBER, 7));
    sampleEntryList.addAll(getSampleEntryList(GroupMemberAttrDetector.GroupMemberAttr.MEMBER, 5));

    // WHEN
    for (Entry entry : sampleEntryList) {
      groupMemberAttrDetector.collect(entry);
    }

    // The most frequently encountered attribute will be selected
    Map<String, String> detectedAttributeMap = groupMemberAttrDetector.detect();

    // THEN
    Assert.assertEquals(1, detectedAttributeMap.size());
    Map.Entry<String, String> selectedEntry = detectedAttributeMap.entrySet().iterator().next();

    Assert.assertEquals("The selected configuration property is not the expected one", groupMemberAttrDetector.detectedProperty(), selectedEntry.getKey());
    Assert.assertEquals("The selected configuration property value is not the expected one", GroupMemberAttrDetector.GroupMemberAttr.UNIQUE_MEMBER.attrName(), selectedEntry.getValue());


  }

  @Test
  public void testShouldDetectorPassWhenEmptySampleSetProvided() throws Exception {
    // GIVEN
    List<Entry> sampleEntryList = Lists.newArrayList();

    // WHEN
    // WHEN
    for (Entry entry : sampleEntryList) {
      groupMemberAttrDetector.collect(entry);
    }

    Map<String, String> detectedAttributeMap = groupMemberAttrDetector.detect();
    // THEN
    Assert.assertEquals(1, detectedAttributeMap.size());
    Map.Entry<String, String> selectedEntry = detectedAttributeMap.entrySet().iterator().next();

    Assert.assertEquals("The selected configuration property is not the expected one", groupMemberAttrDetector.detectedProperty(), selectedEntry.getKey());
    Assert.assertEquals("The selected configuration property value is not the expected one", "N/A", selectedEntry.getValue());

  }

  private List<Entry> getSampleEntryList(GroupMemberAttrDetector.GroupMemberAttr member, int count) {
    List<Entry> entryList = Lists.newArrayList();
    for (int i = 0; i < count; i++) {
      Entry entry = new DefaultEntry();
      try {
        entry.setDn("dn=" + member.name() + "-" + i);
        entry.add(new DefaultAttribute(member.attrName(), new Value("xxx")));
        entryList.add(entry);
      } catch (Exception e) {
        LOG.error(e.getMessage());
      }
    }
    return entryList;
  }
}