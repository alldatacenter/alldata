/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.test.tools;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileTools {
    /**
     * 生成一个临时文件
     * @param path
     * @return
     * @throws IOException 
     */
    public static File createALocalTempFile(String path, long size) throws IOException {
        File tmpfile = new File(path);
        if(null != tmpfile.getParentFile())
        {
            tmpfile.getParentFile().mkdirs();
        }
        tmpfile.deleteOnExit();
        if(null != tmpfile.getParentFile()) {
            tmpfile.getParentFile().mkdirs();
        }
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(tmpfile, "rw");
            randomAccessFile.setLength(size);
        } finally {
            if(null != randomAccessFile) {
                randomAccessFile.close();
            }
        }
        
        return tmpfile;
    }
}
