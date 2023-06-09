///* * Copyright 2020 QingLang, Inc.
// *
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.qlangtech.async.message.client.util;
//
//import com.caucho.hessian.io.HessianInput;
//import com.caucho.hessian.io.HessianOutput;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//
///*
// * Created with IntelliJ IDEA.
// * User: shipf
// * Date: 14-6-20
// * Time: 下午2:39
// * To change this template use File | Settings | File Templates.
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public class HessianUtil {
//
//    public static byte[] serialize(Object obj) throws IOException {
//        if (obj == null)
//            throw new NullPointerException();
//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        HessianOutput ho = new HessianOutput(os);
//        ho.writeObject(obj);
//        return os.toByteArray();
//    }
//
//    public static Object deserialize(byte[] by) throws IOException {
//        if (by == null)
//            throw new NullPointerException();
//        ByteArrayInputStream is = new ByteArrayInputStream(by);
//        HessianInput hi = new HessianInput(is);
//        return hi.readObject();
//    }
//}
