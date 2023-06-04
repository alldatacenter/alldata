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

package org.apache.inlong.sort.iceberg.sink.collections;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * A tool class for safely creating and deleting directories that wraps exception information.
 */
public class FileIOUtils {

    public static void deleteDirectory(File directory) throws IOException {
        if (directory != null && directory.exists()) {
            try (Stream<Path> files = Files.walk(directory.toPath())) {
                files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
            directory.delete();
            if (directory.exists()) {
                throw new IOException("Unable to delete directory " + directory);
            }
        }
    }

    public static void mkdir(File directory) throws IOException {
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }

        if (directory != null && !directory.isDirectory()) {
            throw new IOException("Unable to create :" + directory);
        }
    }
}
