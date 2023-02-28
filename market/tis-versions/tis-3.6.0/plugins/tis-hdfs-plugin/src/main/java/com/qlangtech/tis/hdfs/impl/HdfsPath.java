/**
 * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
 * <p>
 * This program is free software: you can use, redistribute, and/or modify
 * it under the terms of the GNU Affero General Public License, version 3
 * or later ("AGPL"), as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.qlangtech.tis.hdfs.impl;

import com.qlangtech.tis.fs.IPath;
import org.apache.hadoop.fs.Path;

/* *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年11月23日
 */
public class HdfsPath implements IPath {

    private final Path path;

    public HdfsPath(String path) {
        this(new Path(path));
    }

    public HdfsPath(Path path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return this.path.toString();
    }

    @Override
    public String getName() {
        return path.getName();
    }

    public HdfsPath(IPath parent, String name) {
        this.path = new Path(parent.unwrap(Path.class), name);
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (clazz != Path.class) {
            throw new IllegalStateException(clazz + " is not type of " + Path.class);
        }
        return clazz.cast(this.path);
    }
}
