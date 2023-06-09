/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.util;

//import edu.umd.cs.findbugs.annotations.CheckForNull;
//import edu.umd.cs.findbugs.annotations.Nullable;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class Util {


    /**
     * Converts a {@link File} into a {@link Path} and checks runtime exceptions.
     *
     * @throws IOException if {@code f.toPath()} throws {@link InvalidPathException}.
     */
    public static Path fileToPath(File file) throws IOException {
        try {
            return file.toPath();
        } catch (InvalidPathException e) {
            throw new IOException(e);
        }
    }

    /**
     * Returns a file name by changing its extension.
     *
     * @param ext For example, ".zip"
     */
    public static File changeExtension(File dst, String ext) {
        String p = dst.getPath();
        int pos = p.lastIndexOf('.');
        if (pos < 0) return new File(p + ext);
        else return new File(p.substring(0, pos) + ext);
    }

    /**
     * Convert empty string to null, and trim whitespace.
     *
     * @since 1.154
     */
    public static String fixEmptyAndTrim(String s) {
        if (s == null) return null;
        return fixEmpty(s.trim());
    }

    /**
     * Convert empty string to null.
     */
    public static String fixEmpty(String s) {
        if (s == null || s.length() == 0) return null;
        return s;
    }


    /**
     * Convert null to "".
     */
    public static String fixNull(String s) {
        if (s == null)
            return "";
        else
            return s;
    }

    /**
     * Null-safe String intern method.
     *
     * @return A canonical representation for the string object. Null for null input strings
     */

    public static String intern(String s) {
        return s == null ? s : s.intern();
    }

    public static String join(Collection<?> strings, String separator) {
        return strings.stream().map((r) -> r.toString()).collect(Collectors.joining(separator));
    }

    /**
     * Combines all the given collections into a single list.
     */
    public static <T> List<T> join(Collection<? extends T>... items) {
        int size = 0;
        for (Collection<? extends T> item : items) size += item.size();
        List<T> r = new ArrayList<T>(size);
        for (Collection<? extends T> item : items) r.addAll(item);
        return r;
    }

    public static void deleteRecursive(File destDir) {
        try {
            FileUtils.forceDelete(destDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
