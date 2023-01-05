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

import com.qlangtech.tis.fs.IFileSplit;
import com.qlangtech.tis.fs.IPath;
import org.apache.hadoop.mapred.FileSplit;

/* *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年12月10日
 */
public class HdfsFileSplit implements IFileSplit {

    private final FileSplit split;

    private final IPath path;

    public HdfsFileSplit(FileSplit split) {
        super();
        this.split = split;
        this.path = new HdfsPath(split.getPath());
    }

    public IPath getPath() {
        return this.path;
    }

    public long getStart() {
        return split.getStart();
    }

    public long getLength() {
        return split.getLength();
    }
}
