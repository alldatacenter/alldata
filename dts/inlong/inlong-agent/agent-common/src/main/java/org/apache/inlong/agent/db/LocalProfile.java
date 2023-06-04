/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.db;

import org.apache.inlong.agent.conf.JobProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * profile from local file
 */
public class LocalProfile {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalProfile.class);

    private static final String JSON_SUFFIX = ".json";
    private static final String PROPERTIES_SUFFIX = ".properties";
    private static final String PARENT_PATH = "/jobs";

    private final Path filePath;

    public LocalProfile(String parentConfPath) {
        String fileName = parentConfPath + PARENT_PATH;
        this.filePath = Paths.get(fileName);
    }

    /**
     * If filePath is a directory, read all local file and construct job profiles
     *
     * @return list of JobProfiles
     */
    public List<JobProfile> readFromLocal() {
        List<JobProfile> profileList = new ArrayList<>();
        try {
            if (Files.isDirectory(this.filePath)) {
                // list parent path and find files which name is end with .json or .properties
                try (final Stream<Path> pathStream = Files.list(this.filePath)) {
                    for (Iterator<Path> it = pathStream.iterator(); it.hasNext();) {
                        String childPath = it.next().toString();
                        JobProfile jobProfile = null;
                        if (childPath.endsWith(JSON_SUFFIX)) {
                            jobProfile = JobProfile.parseJsonFile(childPath);
                        } else if (childPath.endsWith(PROPERTIES_SUFFIX)) {
                            jobProfile = JobProfile.parsePropertiesFile(childPath);
                        }
                        if (jobProfile != null && jobProfile.allRequiredKeyExist()) {
                            profileList.add(jobProfile);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("error caught", e);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("error in reading files {}", this.filePath);
        }
        return profileList;
    }
}
