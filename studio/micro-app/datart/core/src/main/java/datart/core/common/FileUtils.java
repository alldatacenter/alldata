/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.common;

import datart.core.base.exception.Exceptions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;

public class FileUtils {


    public static String concatPath(String... paths) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            if (StringUtils.isBlank(path)) {
                continue;
            }
            path = StringUtils.appendIfMissing(path, "/");
            if (i != 0) {
                path = StringUtils.removeStart(path, "/");
            }
            if (i == paths.length - 1) {
                path = StringUtils.removeEnd(path, "/");
            }
            stringBuilder.append(path);
        }
        return StringUtils.removeEnd(stringBuilder.toString(), "/");
    }

    public static void mkdirParentIfNotExist(String path) {
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
    }

    public static String withBasePath(String path) {
        String fileBasePath = Application.getFileBasePath();
        if (path.startsWith(fileBasePath)) {
            return path;
        }
        return concatPath(fileBasePath, path);
    }

    public static void delete(String path) {
        delete(new File(path));
    }

    public static void delete(File file) {
        if (file.exists()) {
            try {
                file.delete();
            } catch (Exception ignored) {
            }
        }
    }

    public static Set<String> walkDir(File file, String extension, boolean recursion) {
        if (file == null || !file.exists()) {
            return Collections.emptySet();
        }
        if (file.isFile()) {
            return Collections.singleton(file.getName());
        } else {
            File[] files = file.listFiles(pathname -> extension == null || pathname.getName().endsWith(extension));
            if (files == null) {
                return Collections.emptySet();
            }
            Set<String> names = new LinkedHashSet<>();
            for (File f : files) {
                if (f.isFile()) {
                    names.add(f.getName());
                } else if (recursion) {
                    names.addAll(walkDir(f, extension, recursion));
                }
            }
            return names;
        }
    }

    public static Map<String, byte[]> walkDirAsStream(File baseDir, String extension, boolean recursion) {
        Map<String, byte[]> bytes = new HashMap<>();
        Set<String> files = walkDir(baseDir, extension, recursion);
        if (CollectionUtils.isEmpty(files)) {
            return Collections.emptyMap();
        }
        for (String name : files) {
            File file = new File(FileUtils.concatPath(baseDir.getAbsolutePath(), name));
            if (file.exists() && file.isFile()) {
                try {
                    FileInputStream inputStream = new FileInputStream(file);
                    byte[] buffer = new byte[inputStream.available()];
                    inputStream.read(buffer);
                    bytes.put(name, buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return bytes;
    }


    public static void save(String path, byte[] content, boolean cover) throws IOException {
        File file = new File(path);
        if (!cover && file.exists()) {
            Exceptions.msg("file already exists : " + path);
        }
        mkdirParentIfNotExist(path);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content);
        }
    }

}
