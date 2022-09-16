/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.sort.iceberg.flink;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompactTableProperties {
    public static final String COMPACT_PREFIX = "write.compact.";

    public static final String COMPACT_ENABLED = "write.compact.enable";
    public static final boolean COMPACT_ENABLED_DEFAULT = false;

    public static final String COMPACT_INTERVAL = "write.compact.snapshot.interval";
    public static final int COMPACT_INTERVAL_DEFAULT = 5;

    public static final String COMPACT_RESOUCE_POOL = "write.compact.resource.name";
    public static final String COMPACT_RESOUCE_POOL_DEFAULT = "default";

    // Supported by spark rewrite action option
    public static final String COMPACT_MAX_CONCURRENT_FILE_GROUP_REWRITES
            = "write.compact.max-concurrent-file-group-rewrites";
    public static final int COMPACT_MAX_CONCURRENT_FILE_GROUP_REWRITES_DEFAULT = 1;

    public static final String COMPACT_MAX_FILE_GROUP_SIZE_BYTES = "write.compact.max-file-group-size-bytes";
    public static final long COMPACT_MAX_FILE_GROUP_SIZE_BYTES_DEFAULT = 1024L * 1024L * 1024L * 100L; // 100 Gigabytes

    public static final String COMPACT_PARTIAL_PROGRESS_ENABLED = "write.compact.partial-progress.enabled";
    public static final boolean COMPACT_PARTIAL_PROGRESS_ENABLED_DEFAULT = false;

    public static final String COMPACT_PARTIAL_PROGRESS_MAX_COMMITS = "write.compact.partial-progress.max-commits";
    public static final int COMPACT_PARTIAL_PROGRESS_MAX_COMMITS_DEFAULT = 10;

    public static final String COMPACT_TARGET_FILE_SIZE_BYTES = "write.compact.target-file-size-bytes";
    public static final int COMPACT_TARGET_FILE_SIZE_BYTES_DEFAULT = 512 * 1024 * 1024; // 512 MB

    public static final String COMPACT_USE_STARTING_SEQUENCE_NUMBER = "write.compact.use-starting-sequence-number";
    public static final boolean COMPACT_USE_STARTING_SEQUENCE_NUMBER_DEFAULT = true;

    public static final String COMPACT_MIN_INPUT_FILES = "write.compact.min-input-files";
    public static final int COMPACT_MIN_INPUT_FILES_DEFAULT = 5;

    public static final String COMPACT_DELETE_FILE_THRESHOLD = "write.compact.delete-file-threshold";
    public static final int COMPACT_DELETE_FILE_THRESHOLD_DEFAULT = Integer.MAX_VALUE;

    public static final String COMPACT_MIN_FILE_SIZE_BYTES = "write.compact.min-file-size-bytes";
    public static final double COMPACT_MIN_FILE_SIZE_BYTES_DEFAULT = 0.75d * COMPACT_TARGET_FILE_SIZE_BYTES_DEFAULT;

    public static final String COMPACT_MAX_FILE_SIZE_BYTES = "write.compact.max-file-size-bytes";
    public static final double COMPACT_MAX_FILE_SIZE_BYTES_DEFAULT = 1.80d * COMPACT_TARGET_FILE_SIZE_BYTES_DEFAULT;

    public static final Set<String> TABLE_AUTO_COMPACT_PROPERTIES = Stream.of(
                COMPACT_ENABLED,
                COMPACT_INTERVAL,
                COMPACT_RESOUCE_POOL,
                COMPACT_MAX_CONCURRENT_FILE_GROUP_REWRITES,
                COMPACT_MAX_FILE_GROUP_SIZE_BYTES,
                COMPACT_PARTIAL_PROGRESS_ENABLED,
                COMPACT_PARTIAL_PROGRESS_MAX_COMMITS,
                COMPACT_TARGET_FILE_SIZE_BYTES,
                COMPACT_USE_STARTING_SEQUENCE_NUMBER,
                COMPACT_MIN_INPUT_FILES,
                COMPACT_DELETE_FILE_THRESHOLD,
                COMPACT_MIN_FILE_SIZE_BYTES,
                COMPACT_MAX_FILE_SIZE_BYTES
        ).collect(Collectors.toSet());

    public static final Set<String> ACTION_AUTO_COMPACT_OPTIONS = Stream.of(
            COMPACT_MAX_CONCURRENT_FILE_GROUP_REWRITES,
            COMPACT_MAX_FILE_GROUP_SIZE_BYTES,
            COMPACT_PARTIAL_PROGRESS_ENABLED,
            COMPACT_PARTIAL_PROGRESS_MAX_COMMITS,
            COMPACT_TARGET_FILE_SIZE_BYTES,
            COMPACT_USE_STARTING_SEQUENCE_NUMBER,
            COMPACT_MIN_INPUT_FILES,
            COMPACT_DELETE_FILE_THRESHOLD,
            COMPACT_MIN_FILE_SIZE_BYTES,
            COMPACT_MAX_FILE_SIZE_BYTES
    ).collect(Collectors.toSet());
}
