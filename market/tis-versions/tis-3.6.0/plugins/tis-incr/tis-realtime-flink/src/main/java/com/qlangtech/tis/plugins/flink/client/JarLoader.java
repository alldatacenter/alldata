///**
// * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
// * <p>
// *   This program is free software: you can use, redistribute, and/or modify
// *   it under the terms of the GNU Affero General Public License, version 3
// *   or later ("AGPL"), as published by the Free Software Foundation.
// * <p>
// *  This program is distributed in the hope that it will be useful, but WITHOUT
// *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// *   FITNESS FOR A PARTICULAR PURPOSE.
// * <p>
// *  You should have received a copy of the GNU Affero General Public License
// *  along with this program. If not, see <http://www.gnu.org/licenses/>.
// */
//
//package com.qlangtech.tis.plugins.flink.client;
//
//import java.io.File;
//import java.net.MalformedURLException;
//import java.net.URL;
//
///**
// * @author: 百岁（baisui@qlangtech.com）
// * @create: 2021-10-10 17:59
// **/
//public interface JarLoader {
//
//    URL findByName(String name) throws MalformedURLException, Exception;
//
//    default File downLoad(String path) throws Exception {
//        return downLoad(path, false);
//    }
//
//    File downLoad(String path, boolean cache) throws Exception;
//
//    default URL find(String path) throws Exception {
//        return find(path, false);
//    }
//
//    URL find(String path, boolean cache) throws Exception;
//
//}
