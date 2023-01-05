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

package com.qlangtech.tis.extension.util;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-10 09:34
 **/

import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.util.AtomicFileWriter;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * Represents a text file.
 * <p>
 * Provides convenience methods for reading and writing to it.
 *
 * @author Kohsuke Kawaguchi
 */
public class TextFile {

    public final File file;

    public TextFile(File file) {
        this.file = file;
    }

    public boolean exists() {
        return file.exists();
    }

    public void delete() {
        // file.delete();
        FileUtils.deleteQuietly(file);
    }

    /**
     * Reads the entire contents and returns it.
     */
    public String read() throws IOException {
        return FileUtils.readFileToString(this.file, TisUTF8.get());
    }

    /**
     * @throws RuntimeException in the case of {@link IOException} in {@link '#linesStream()'}
     * @deprecated This method does not properly propagate errors and may lead to file descriptor leaks
     * if the collection is not fully iterated. Use {@link '#linesStream()'} instead.
     */

    public Iterator<String> lines() throws Exception {
        return FileUtils.lineIterator(this.file, TisUTF8.getName());
    }

    /**
     * Overwrites the file by the given string.
     */
    public void write(String text) throws IOException {
        file.getParentFile().mkdirs();
        try (AtomicFileWriter w = new AtomicFileWriter(file)) {
            try {
                w.write(text);
                w.commit();
            } finally {
                w.abort();
            }
        }
    }

//    /**
//     * Creates a new {@link jenkins.util.io.LinesStream} of the file.
//     * <p>
//     * Note: The caller is responsible for closing the returned
//     * {@code LinesStream}.
//     * @throws IOException if the file cannot be converted to a
//     * {@link java.nio.file.Path} or if the file cannot be opened for reading
//     * @since 2.111
//     */
//    @CreatesObligation
//    public @NonNull LinesStream linesStream() throws IOException {
//        return new LinesStream(Util.fileToPath(file));
//    }

//    /**
//     * Overwrites the file by the given string.
//     */
//    public void write(String text) throws IOException {
//        file.getParentFile().mkdirs();
//        try (AtomicFileWriter w = new AtomicFileWriter(file)) {
//            try {
//                w.write(text);
//                w.commit();
//            } finally {
//                w.abort();
//            }
//        }
//    }

    /**
     * Reads the first N characters or until we hit EOF.
     */
    public @NonNull
    String head(int numChars) throws IOException {
        char[] buf = new char[numChars];
        int read = 0;
        try (Reader r = new FileReader(file)) {
            while (read < numChars) {
                int d = r.read(buf, read, buf.length - read);
                if (d < 0)
                    break;
                read += d;
            }

            return new String(buf, 0, read);
        }
    }

    /**
     * Efficiently reads the last N characters (or shorter, if the whole file is shorter than that.)
     *
     * <p>
     * This method first tries to just read the tail section of the file to get the necessary chars.
     * To handle multi-byte variable length encoding (such as UTF-8), we read a larger than
     * necessary chunk.
     *
     * <p>
     * Some multi-byte encoding, such as Shift-JIS (http://en.wikipedia.org/wiki/Shift_JIS) doesn't
     * allow the first byte and the second byte of a single char to be unambiguously identified,
     * so it is possible that we end up decoding incorrectly if we start reading in the middle of a multi-byte
     * character. All the CJK multi-byte encodings that I know of are self-correcting; as they are ASCII-compatible,
     * any ASCII characters or control characters will bring the decoding back in sync, so the worst
     * case we just have some garbage in the beginning that needs to be discarded. To accommodate this,
     * we read additional 1024 bytes.
     *
     * <p>
     * Other encodings, such as UTF-8, are better in that the character boundary is unambiguous,
     * so there can be at most one garbage char. For dealing with UTF-16 and UTF-32, we read at
     * 4 bytes boundary (all the constants and multipliers are multiples of 4.)
     *
     * <p>
     * Note that it is possible to construct a contrived input that fools this algorithm, and in this method
     * we are willing to live with a small possibility of that to avoid reading the whole text. In practice,
     * such an input is very unlikely.
     *
     * <p>
     * So all in all, this algorithm should work decently, and it works quite efficiently on a large text.
     */
    public @NonNull
    String fastTail(int numChars, Charset cs) throws IOException {

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long len = raf.length();
            // err on the safe side and assume each char occupies 4 bytes
            // additional 1024 byte margin is to bring us back in sync in case we started reading from non-char boundary.
            long pos = Math.max(0, len - (numChars * 4 + 1024));
            raf.seek(pos);

            byte[] tail = new byte[(int) (len - pos)];
            raf.readFully(tail);

            String tails = cs.decode(java.nio.ByteBuffer.wrap(tail)).toString();

            return tails.substring(Math.max(0, tails.length() - numChars));
        }
    }

    /**
     * Uses the platform default encoding.
     */
    public @NonNull
    String fastTail(int numChars) throws IOException {
        return fastTail(numChars, Charset.defaultCharset());
    }


    public String readTrim() throws IOException {
        return read().trim();
    }

    @Override
    public String toString() {
        return file.toString();
    }
}
