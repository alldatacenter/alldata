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

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.offline.FileSystemFactory;
import junit.framework.TestCase;

import java.util.List;

/**
 * @author: baisui 百岁
 * @create: 2020-11-24 17:50
 **/
public class TestHdfsFileSystemFactory extends TestCase {
    public void testPluginGet() {
        List<Descriptor<FileSystemFactory>> descList
                = TIS.get().getDescriptorList(FileSystemFactory.class);
        assertNotNull(descList);
        assertEquals(1, descList.size());
    }
}
