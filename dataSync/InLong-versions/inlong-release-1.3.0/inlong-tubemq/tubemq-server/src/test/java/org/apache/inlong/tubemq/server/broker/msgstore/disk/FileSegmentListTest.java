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
import org.apache.inlong.tubemq.server.broker.utils.DataStoreUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * FileSegmentList test
 */
public class FileSegmentListTest {

    FileSegmentList fileSegmentList;

    @Test
    public void getView() {
        File file = null;
        try {
            file = File.createTempFile("data",
                DataStoreUtils.nameFromOffset(0L, DataStoreUtils.DATA_FILE_SUFFIX));
            // create FileSegmentList.
            fileSegmentList = new FileSegmentList();
            fileSegmentList.append(new FileSegment(0, file, SegmentType.DATA));
            Segment fileSegment = fileSegmentList.last();
            String data = "abc";
            // append data to last FileSegment.
            long appendTime = System.currentTimeMillis();
            fileSegment.append(ByteBuffer.wrap(data.getBytes()), appendTime, appendTime);
            fileSegment.flush(true);
            // get view
            Segment[] segmentList = fileSegmentList.getView();
            Assert.assertTrue(segmentList.length == 1);
            file.delete();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (file != null) {
                file.deleteOnExit();
            }
        }
    }

    @Test
    public void append() {
        File file = null;
        try {
            file = File.createTempFile("data",
                DataStoreUtils.nameFromOffset(0L, DataStoreUtils.DATA_FILE_SUFFIX));
            file.createNewFile();
            // create FileSegmentList.
            fileSegmentList = new FileSegmentList();
            Segment fileSegment = new FileSegment(100L, file, true, SegmentType.DATA);
            String data = "abc";
            // append data to last FileSegment.
            long appendTime = System.currentTimeMillis();
            fileSegment.append(ByteBuffer.wrap(data.getBytes()), appendTime, appendTime);
            fileSegment.flush(true);
            fileSegmentList.append(fileSegment);
            Segment[] segmentList = fileSegmentList.getView();
            Assert.assertTrue(segmentList.length == 1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (file != null) {
                file.deleteOnExit();
            }
        }
    }
}
