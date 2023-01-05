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

package com.qlangtech.tis.fs.local;

import com.qlangtech.tis.fs.FSDataInputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author: baisui 百岁
 * @create: 2021-03-02 13:44
 **/
public class LocalFSDataInputStream extends FSDataInputStream {

    public LocalFSDataInputStream(InputStream input, int bufferSize) {
        super((new BufferedInputStream(input, bufferSize)));
    }

    @Override
    public void readFully(long position, byte[] buffer, int offset, int length) throws IOException {
        IOUtils.read(this, buffer, offset, length);
    }

    @Override
    public void seek(long position) {
        throw new UnsupportedOperationException();
    }


}
