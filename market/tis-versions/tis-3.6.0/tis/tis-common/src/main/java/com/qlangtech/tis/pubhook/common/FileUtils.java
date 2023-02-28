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
package com.qlangtech.tis.pubhook.common;

import com.qlangtech.tis.manage.common.TisUTF8;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-2-15
 */
public class FileUtils {

//    public static String readLastLine(File file) {
//        RandomAccessFile randomAccess = null;
//        try {
//            randomAccess = new RandomAccessFile(file, "r");
//            boolean eol = false;
//            int c = -1;
//            long fileLength = randomAccess.length();
//            long size = 1;
//            ww: while (!eol) {
//                long offset = fileLength - (size++);
//                randomAccess.seek(offset);
//                switch(c = randomAccess.read()) {
//                    case -1:
//                    case '\n':
//                    case '\r':
//                        randomAccess.seek(offset + 1);
//                        break ww;
//                }
//            }
//            return randomAccess.readLine();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } finally {
//            try {
//                randomAccess.close();
//            } catch (IOException e) {
//            }
//        }
//    }

    /**
     * 在文件最后追加一行记录
     *
     * @param file
     * @param line
     */
    public static void append(File file, String line) {
        try {
            org.apache.commons.io.FileUtils.write(file, ("\r\n" + line), TisUTF8.get(), true);
        } catch (IOException e) {
            throw new RuntimeException(file.getAbsolutePath(), e);
        }
    // OutputStream wirter = null;
    // try {
    //
    // wirter = new FileOutputStream(file, true);
    // wirter.write(("\r\n" + line).getBytes());
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // } finally {
    // try {
    // wirter.close();
    // } catch (IOException e) {
    // }
    // }
    }

//    public static void main(String[] arg) {
//        append(new File("D:\\tmp\\test.txt"), "11");
//    }
}
