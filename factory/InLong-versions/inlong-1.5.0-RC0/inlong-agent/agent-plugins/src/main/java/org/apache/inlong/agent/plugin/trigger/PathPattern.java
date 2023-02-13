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

package org.apache.inlong.agent.plugin.trigger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.inlong.agent.plugin.filter.DateFormatRegex;
import org.apache.inlong.agent.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Path pattern for file filter.
 * It’s identified by watchDir, which matches {@link PathPattern#whiteList} and filters {@link PathPattern#blackList}.
 */
public class PathPattern {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathPattern.class);

    private final String rootDir;
    private final Set<String> subDirs;
    // regex for those files should be matched
    private final Set<DateFormatRegex> whiteList;
    // regex for those files should be filtered
    private final Set<String> blackList;

    public PathPattern(String rootDir, Set<String> whiteList, Set<String> blackList) {
        this(rootDir, whiteList, blackList, null);
    }

    public PathPattern(String rootDir, Set<String> whiteList, Set<String> blackList, String offset) {
        this.rootDir = rootDir;
        this.subDirs = new HashSet<>();
        this.blackList = blackList;
        if (offset != null && StringUtils.isNotBlank(offset)) {
            this.whiteList = whiteList.stream()
                    .map(whiteRegex -> DateFormatRegex.ofRegex(whiteRegex).withOffset(offset))
                    .collect(Collectors.toSet());
            updateDateFormatRegex();
        } else {
            this.whiteList = whiteList.stream()
                    .map(whiteRegex -> DateFormatRegex.ofRegex(whiteRegex))
                    .collect(Collectors.toSet());
        }
    }

    public static Set<PathPattern> buildPathPattern(Set<String> whiteList, String offset, Set<String> blackList) {
        Set<String> commonWatchDir = PathUtils.findCommonRootPath(whiteList);
        return commonWatchDir.stream().map(rootDir -> {
            Set<String> commonWatchDirWhiteList =
                    whiteList.stream()
                            .filter(whiteRegex -> whiteRegex.startsWith(rootDir))
                            .collect(Collectors.toSet());
            return new PathPattern(rootDir, commonWatchDirWhiteList, blackList, offset);
        }).collect(Collectors.toSet());
    }

    /**
     * cleanup local cache, subDirs is only used to filter duplicated directories
     * in one term watch key check.
     */
    public void cleanup() {
        subDirs.clear();
    }

    /**
     * Research all children files with {@link PathPattern#rootDir} matched whiteList and filtered by blackList.
     *
     * @param maxNum
     * @return
     */
    public Collection<File> walkSuitableFiles(int maxNum) {
        Collection<File> suitableFiles = new ArrayList<>();
        walkSuitableFiles(suitableFiles, new File(rootDir), maxNum);
        return suitableFiles;
    }

    private void walkSuitableFiles(Collection<File> suitableFiles, File file, int maxNum) {
        if (suitableFiles.size() > maxNum) {
            LOGGER.warn("Suitable files exceed max num {}, just return.", maxNum);
            return;
        }

        if (suitable(file.getAbsolutePath())) {
            if (file.isFile()) {
                suitableFiles.add(file);
            } else if (file.isDirectory()) {
                Stream.of(file.listFiles()).forEach(subFile -> walkSuitableFiles(suitableFiles, subFile, maxNum));
            }
        }
    }

    /**
     * Check whether path is suitable for match whiteList and filtered by blackList
     *
     * @param path pathString
     * @return true if suit else false.
     */
    public boolean suitable(String path) {
        // remove blacklist path
        if (blackList.contains(path)) {
            LOGGER.info("find blacklist path {}, ignore it.", path);
            return false;
        }
        // remove common root path
        String briefSubDir = StringUtils.substringAfter(path, rootDir);
        // if already watched, then stop deep find
        if (subDirs.contains(briefSubDir)) {
            LOGGER.info("already watched {}", path);
            return false;
        }

        subDirs.add(briefSubDir);
        File file = new File(path);
        return whiteList.stream()
                .filter(whiteRegex -> whiteRegex.match(file))
                .findAny()
                .isPresent();
    }

    /**
     * when a new file is found, update regex since time may change.
     */
    public void updateDateFormatRegex() {
        whiteList.forEach(DateFormatRegex::setRegexWithCurrentTime);
    }

    /**
     * when job is retry job, the time for searching file should be specified.
     */
    public void updateDateFormatRegex(String time) {
        whiteList.forEach(whiteRegex -> whiteRegex.setRegexWithTime(time));
    }

    @Override
    public String toString() {
        return rootDir;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(rootDir, false);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof PathPattern) {
            PathPattern entity = (PathPattern) object;
            return entity.rootDir.equals(this.rootDir);
        } else {
            return false;
        }
    }

    public String getRootDir() {
        return rootDir;
    }

    public String getSuitTime() {
        // todo: Adapt to datetime in the case of multiple regex
        return whiteList.stream().findAny().get().getFormattedTime();
    }
}
