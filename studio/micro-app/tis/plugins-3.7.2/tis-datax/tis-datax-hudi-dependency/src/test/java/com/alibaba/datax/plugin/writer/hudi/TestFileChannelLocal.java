/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.datax.plugin.writer.hudi;

import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-05-26 16:41
 **/
public class TestFileChannelLocal {

    @Test
    public void testLock() throws Exception {
        File nodeExcludeLock = new File("initial.lock");
        File wfile = new File("write");
        FileUtils.touch(nodeExcludeLock);
        RandomAccessFile raf = new RandomAccessFile(nodeExcludeLock, "rw");
        int i = 0;

        try (FileChannel channel = raf.getChannel()) {
            while (i++ < 100) {
                // 服务器节点级别的排他
                try (FileLock fileLock = channel.tryLock()) {

                    int val = 0;
                    try {
                        val = Integer.parseInt(FileUtils.readFileToString(wfile, TisUTF8.get()));
                    } catch (Throwable e) {

                    }

                    FileUtils.write(wfile, String.valueOf(val + 1));
                    System.out.println(val + 1);
                    Thread.sleep(1000);
//                    raf.seek(0);
//                    int currVal = 0;
//                    try {
//                        currVal = raf.readInt();
//                    } catch (Throwable e) {
//
//                    }
//                    raf.seek(0);
//                    raf.write(currVal + 1);

                    // System.out.println("i:" + currVal);
                }

            }
        }

    }
}
