/**
 * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
 * <p>
 *   This program is free software: you can use, redistribute, and/or modify
 *   it under the terms of the GNU Affero General Public License, version 3
 *   or later ("AGPL"), as published by the Free Software Foundation.
 * <p>
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *   FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.qlangtech.tis.hdfs.impl;

import java.io.IOException;
import com.qlangtech.tis.fs.TISFSDataOutputStream;
import org.apache.hadoop.fs.FSDataOutputStream;

/* *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年12月9日
 */
public class HdfsDataOutputStream extends TISFSDataOutputStream {

    public HdfsDataOutputStream(FSDataOutputStream output) {
        super(output);
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write(b);
    }

    @Override
    public long getPos() throws IOException {
        return ((FSDataOutputStream) this.out).getPos();
    }
}
