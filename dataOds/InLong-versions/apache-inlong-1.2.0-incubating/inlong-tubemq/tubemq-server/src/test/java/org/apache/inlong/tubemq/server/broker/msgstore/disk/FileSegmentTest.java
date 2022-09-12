/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.broker.msgstore.disk;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * FileSegment test.
 */
public class FileSegmentTest {

    FileSegment fileSegment;

    @org.junit.Test
    public void append() {
        long start = 0;
        File file = null;
        try {
            file = File.createTempFile("testdata", null);
            // create FileSegment
            fileSegment = new FileSegment(start, file, true, SegmentType.DATA);
            String data = "abc";
            byte[] bytes = data.getBytes();
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            // append data to FileSegment.
            long appendTime = System.currentTimeMillis();
            fileSegment.append(buf, appendTime, appendTime);
            fileSegment.append(buf, appendTime, appendTime);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileSegment.close();
            if (file != null) {
                file.deleteOnExit();
            }
        }
    }

    @org.junit.Test
    public void getViewRef() {
        long start = 0;
        File file = null;
        try {
            file = File.createTempFile("testdata", null);
            // create FileSegment.
            fileSegment = new FileSegment(start, file, true, SegmentType.DATA);
            String data = "abc";
            byte[] bytes = data.getBytes();
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            // append data to fileSegment.
            long appendTime = System.currentTimeMillis();
            long offset = fileSegment.append(buf, appendTime, appendTime);
            int limit = 1000;
            // get view of fileSegment.
            ByteBuffer readBuffer = ByteBuffer.allocate(limit);
            fileSegment.read(readBuffer, 0);
            byte[] readBytes = readBuffer.array();
            String readData = new String(readBytes);
            readData.substring(0, data.length());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileSegment.close();
            if (file != null) {
                file.deleteOnExit();
            }
        }
    }
}