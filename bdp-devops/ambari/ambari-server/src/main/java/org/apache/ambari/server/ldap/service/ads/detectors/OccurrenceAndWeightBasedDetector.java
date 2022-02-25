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

import java.util.Map;

import org.apache.ambari.server.ldap.service.AttributeDetector;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * Attribute detector implementation that detects attributes considering their count of occurrence in a sample set of entries.
 * When multiple values are checked these values can be assigned a weight, that represents it's importance.
 */
public abstract class OccurrenceAndWeightBasedDetector implements AttributeDetector<Entry> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OccurrenceAndWeightBasedDetector.class);

  /**
   * A map in which the keys are the attributes that are checked in an entry and the values are the number the key occurs
   * in the sample entry set.
   */
  private Map<String, Integer> occurrenceMap = Maps.newHashMap();

  /**
   * A map in which the keys are the attributes that are checked in an entry and the values are the weight of the attribute.
   */
  private Map<String, Integer> weightsMap = Maps.newHashMap();

  protected Map<String, Integer> occurrenceMap() {
    return occurrenceMap;
  }

  protected Map<String, Integer> weightsMap() {
    return weightsMap;
  }


  /**
   * Checks whether the provided atribute is present in the entry.s
   *
   * @param entry     the entry being procesed
   * @param attribute the attribute being detected
   * @return true if the attribute is present, false otherwise
   */
  protected abstract boolean applies(Entry entry, String attribute);

  /**
   * The configuration key being detected.
   *
   * @return the key as a string
   */
  public abstract String detectedProperty();

  /**
   * Calculates the attribute value based on the two maps.
   *
   * @return a map with a single element, the key is the configuration key, the value is the detected attribute value
   */
  @Override
  public Map<String, String> detect() {
    LOGGER.info("Calculating the most probable attribute/value ...");
    Map<String, String> detectedMap = Maps.newHashMap();

    Map.Entry<String, Integer> selectedEntry = null;

    for (Map.Entry<String, Integer> entry : occurrenceMap().entrySet()) {
      if (selectedEntry == null) {

        selectedEntry = entry;
        LOGGER.debug("Initial attribute / value entry: {}", selectedEntry);
        continue;

      }

      if (selectedEntry.getValue() < entry.getValue()) {

        LOGGER.info("Changing potential attribute / value entry from : [{}] to: [{}]", selectedEntry, entry);
        selectedEntry = entry;

      }
    }

    // check whether the selected entry is valid (has occured in the sample result set)
    String detectedVal = "N/A";

    if (selectedEntry.getValue() > 0) {
      detectedVal = selectedEntry.getKey();
    } else {
      LOGGER.warn("Unable to detect attribute or attribute value");
    }

    LOGGER.info("Detected attribute or value: [{}]", detectedVal);
    detectedMap.put(detectedProperty(), detectedVal);
    return detectedMap;
  }


  /**
   * Collects the information about the attribute to be detected from the provided entry.
   *
   * @param entry a result entry returned by a search operation
   */
  @Override
  public void collect(Entry entry) {
    LOGGER.info("Collecting ldap attributes/values form entry with dn: [{}]", entry.getDn());

    for (String attributeValue : occurrenceMap().keySet()) {
      if (applies(entry, attributeValue)) {

        Integer cnt = occurrenceMap().get(attributeValue).intValue();
        if (weightsMap().containsKey(attributeValue)) {
          cnt = cnt + weightsMap().get(attributeValue);
        } else {
          cnt = cnt + 1;
        }
        occurrenceMap().put(attributeValue, cnt);

        LOGGER.info("Collected potential name attr: {}, count: {}", attributeValue, cnt);

      } else {
        LOGGER.info("The result entry doesn't contain the attribute: [{}]", attributeValue);
      }
    }
  }


}
