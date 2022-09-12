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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * path pattern for file filter.
 */
public class PathPattern {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathPattern.class);

    private final DateFormatRegex dateFormatRegex;

    private final String watchDir;
    private final String rootDir;
    private final Set<String> subDirs;

    public PathPattern(String watchDir) {
        this.watchDir = watchDir;
        rootDir = findRoot(watchDir);
        subDirs = new HashSet<>();
        dateFormatRegex = DateFormatRegex.ofRegex(watchDir);
    }

    public PathPattern(String watchDir, String offset) {
        this.watchDir = watchDir;
        rootDir = findRoot(watchDir);
        subDirs = new HashSet<>();
        dateFormatRegex = DateFormatRegex.ofRegex(watchDir).withOffset(offset);
    }

    /**
     * find last existing path by pattern.
     */
    private String findRoot(String watchDir) {
        Path currentPath = Paths.get(watchDir);
        if (!Files.exists(currentPath)) {
            Path parentPath = currentPath.getParent();
            if (parentPath != null) {
                return findRoot(parentPath.toString());
            }
        }
        if (Files.isDirectory(currentPath)) {
            return watchDir;
        }
        return currentPath.getParent().toString();
    }

    /**
     * walk all suitable files under directory.
     */
    private void walkAllSuitableFiles(File dirPath, final Collection<File> collectResult,
            int maxNum) throws IOException {
        if (collectResult.size() > maxNum) {
            LOGGER.warn("max num of files is {}, please check", maxNum);
            return;
        }
        if (dirPath.isFile() && dateFormatRegex.withFile(dirPath).match()) {
            collectResult.add(dirPath);
        } else if (dirPath.isDirectory()) {
            try (final Stream<Path> pathStream = Files.list(dirPath.toPath())) {
                pathStream.forEach(path -> {
                    try {
                        walkAllSuitableFiles(path.toFile(), collectResult, maxNum);
                    } catch (IOException ex) {
                        LOGGER.warn("cannot add {}, please check it", path, ex);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("error caught", e);
            }
        }
    }

    /**
     * walk root directory
     */
    public void walkAllSuitableFiles(final Collection<File> collectResult,
            int maxNum) throws IOException {
        walkAllSuitableFiles(new File(rootDir), collectResult, maxNum);
    }

    /**
     * cleanup local cache, subDirs is only used to filter duplicated directories
     * in one term watch key check.
     */
    public void cleanup() {
        subDirs.clear();
    }

    /**
     * whether path is suitable
     *
     * @param pathStr pathString
     * @return true if suit else false.
     */
    public boolean suitForWatch(String pathStr) {
        // remove common root dir
        String briefSubDir = StringUtils.substringAfter(pathStr, rootDir);
        // if already watched, then stop deep find
        if (subDirs.contains(briefSubDir)) {
            LOGGER.info("already watched {}", pathStr);
            return false;
        }
        boolean matched = dateFormatRegex.withFile(new File(pathStr)).match();
        if (matched) {
            LOGGER.info("add path {}", pathStr);
            subDirs.add(briefSubDir);
        }
        return matched;
    }

    /**
     * when a new file is found, update regex since time may change.
     */
    public void updateDateFormatRegex() {
        dateFormatRegex.setRegexWithCurrentTime(this.watchDir);
    }

    /**
     * when job is retry job, the time for searching file should be specified.
     */
    public void updateDateFormatRegex(String time) {
        dateFormatRegex.setRegexWithTime(this.watchDir, time);
    }

    @Override
    public String toString() {
        return watchDir;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(watchDir, false);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof PathPattern) {
            PathPattern entity = (PathPattern) object;
            return entity.watchDir.equals(this.watchDir);
        } else {
            return false;
        }
    }

    public String getRootDir() {
        return rootDir;
    }

    public String getSuitTime() {
        return dateFormatRegex.getFormattedTime();
    }
}
