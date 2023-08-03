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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.contextenricher.externalretrievers;

import org.apache.ranger.plugin.contextenricher.RangerAbstractContextEnricher;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class GetFromDataFile {
    private static final Logger LOG = LoggerFactory.getLogger(GetFromDataFile.class);

    public Map<String, Map<String, String>> getFromDataFile(String dataFile, String attrName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getFromDataFile(dataFile={}, attrName={})", dataFile, attrName);
        }

        Map<String, Map<String, String>> ret = new HashMap<>();

        // create an instance so that readProperties() can be used!
        RangerAbstractContextEnricher ce = new RangerAbstractContextEnricher() {
            @Override
            public void enrich(RangerAccessRequest rangerAccessRequest) {
            }
        };

        Properties prop = ce.readProperties(dataFile);

        if (prop == null) {
            LOG.warn("getFromDataFile({}, {}): failed to read file", dataFile, attrName);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("read from datafile {}: {}", dataFile, prop);
            }

            // reformat UserAttrsProp into UserStore format:
            // format of UserAttrsProp: Map<String, String>
            // format of UserStore: Map<String, Map<String, String>>
            for (String user : prop.stringPropertyNames()) {
                Map<String, String> userAttrs = new HashMap<>();

                userAttrs.put(attrName, prop.getProperty(user));

                ret.put(user, userAttrs);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getFromDataFile(dataFile={}, attrName={}): ret={}", dataFile, attrName, ret);
        }

        return ret;
    }
}
