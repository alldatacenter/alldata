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

package com.qlangtech.tis.plugins.hudi;

import com.qlangtech.tis.offline.FileSystemFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hudi.common.fs.IExtraHadoopFileSystemGetter;

import java.io.IOException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-02 12:11
 **/
public class TISHadoopFileSystemGetter implements IExtraHadoopFileSystemGetter {
    private Configuration configuration;

    @Override
    public FileSystem getHadoopFileSystem(String path) {
        FileSystemFactory fsFactory = FileSystemFactory.getFsFactory(HUDI_FILESYSTEM_NAME);
        if (fsFactory == null) {
            throw new IllegalStateException(
                    "fs identity:" + HUDI_FILESYSTEM_NAME + " relevant fileSystemFactory can not be null");
        }
        //return fsFactory.getFileSystem().unwrap();
        if (configuration == null) {
            synchronized (TISHadoopFileSystemGetter.class) {
                if (configuration == null) {
                    configuration = fsFactory.getConfiguration();
                }
            }
        }
        try {
            return new Path(path).getFileSystem(configuration);
        } catch (ClassCastException e) {
            throw new RuntimeException(Configuration.class.getClassLoader()
                    + ",cast from:" + fsFactory.getConfiguration().getClass().getClassLoader(), e);
        } catch (IOException e) {
            throw new RuntimeException("path:" + path, e);
        }
    }
}
