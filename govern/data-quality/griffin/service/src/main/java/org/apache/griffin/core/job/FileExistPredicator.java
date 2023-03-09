/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.job;

import static org.apache.griffin.core.job.JobInstance.PATH_CONNECTOR_CHARACTER;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.griffin.core.job.entity.SegmentPredicate;
import org.apache.griffin.core.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileExistPredicator implements Predicator {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(FileExistPredicator.class);

    private static final String PREDICT_PATH = "path";
    private static final String PREDICT_ROOT_PATH = "root.path";

    private SegmentPredicate predicate;

    public FileExistPredicator(SegmentPredicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean predicate() throws IOException {
        Map<String, Object> config = predicate.getConfigMap();
        String[] paths = null;
        String rootPath = null;
        if (config != null && !StringUtils.isEmpty((String) config.get(PREDICT_PATH))) {
            paths = ((String) config.get(PREDICT_PATH))
                .split(PATH_CONNECTOR_CHARACTER);
            rootPath = (String) config.get(PREDICT_ROOT_PATH);
        }
        if (ArrayUtils.isEmpty(paths) || StringUtils.isEmpty(rootPath)) {
            LOGGER.error("Predicate path is null.Please check predicates " +
                "config root.path and path.");
            throw new NullPointerException();
        }
        for (String path : paths) {
            String hdfsPath = rootPath + path;
            LOGGER.info("Predicate path: {}", hdfsPath);
            if (!FSUtil.isFileExist(hdfsPath)) {
                LOGGER.info("Predicate path: " + hdfsPath + " doesn't exist.");
                return false;
            }
            LOGGER.info("Predicate path: " + hdfsPath + " exists.");
        }
        return true;
    }
}
