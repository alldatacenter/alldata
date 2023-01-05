///**
// * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
// *
// * This program is free software: you can use, redistribute, and/or modify
// * it under the terms of the GNU Affero General Public License, version 3
// * or later ("AGPL"), as published by the Free Software Foundation.
// *
// * This program is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// * FITNESS FOR A PARTICULAR PURPOSE.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program. If not, see <http://www.gnu.org/licenses/>.
// */
//package com.qlangtech.tis.extension.util;
//
///**
// * @author: baisui 百岁
// * @create: 2020-04-01 14:52
// */
//import java.util.concurrent.Executors;
//import java.util.concurrent.ThreadFactory;
//
///**
// * {@link ThreadFactory} that creates daemon threads.
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public class DaemonThreadFactory implements ThreadFactory {
//
//    private final ThreadFactory core;
//
//    public DaemonThreadFactory() {
//        this(Executors.defaultThreadFactory());
//    }
//
//    public DaemonThreadFactory(ThreadFactory core) {
//        this.core = core;
//    }
//
//    public Thread newThread(Runnable r) {
//        Thread t = core.newThread(r);
//        t.setDaemon(true);
//        return t;
//    }
//}
