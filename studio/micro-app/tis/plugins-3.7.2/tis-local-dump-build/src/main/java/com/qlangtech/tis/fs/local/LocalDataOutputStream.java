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

import com.qlangtech.tis.fs.TISFSDataOutputStream;
import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author: baisui 百岁
 * @create: 2021-03-02 15:38
 **/
public class LocalDataOutputStream extends TISFSDataOutputStream {
    public LocalDataOutputStream(File f, boolean overwrite) throws IOException {
        super(new BufferedOutputStream(FileUtils.openOutputStream(f, !overwrite)));
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write(b);
    }

    @Override
    public long getPos() throws IOException {
        throw new UnsupportedOperationException();
    }
}
