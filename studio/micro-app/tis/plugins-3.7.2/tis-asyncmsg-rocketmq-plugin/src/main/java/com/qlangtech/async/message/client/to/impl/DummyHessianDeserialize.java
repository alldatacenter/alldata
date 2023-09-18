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
//package com.qlangtech.async.message.client.to.impl;
//
////import com.qlangtech.async.message.client.util.HessianUtil;
//
//import com.qlangtech.tis.async.message.client.consumer.impl.AbstractAsyncMsgDeserialize;
//import com.qlangtech.tis.extension.Descriptor;
//import com.qlangtech.tis.extension.TISExtension;
//import com.qlangtech.tis.manage.common.TisUTF8;
//import com.qlangtech.tis.plugin.annotation.FormField;
//import com.qlangtech.tis.plugin.annotation.Validator;
//import com.qlangtech.tis.realtime.transfer.DTO;
//
//import java.io.IOException;
//
///* *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public class DummyHessianDeserialize extends DefaultJSONFormatDeserialize {
//
////    @FormField(validate = {Validator.require})
////    public String testProp;
////    @Override
////    public DTO deserialize(byte[] content) throws IOException {
////        return (T) new String(content, TisUTF8.get());
////    }
//
//    @TISExtension()
//    public static class DefaultDescriptor extends Descriptor<AbstractAsyncMsgDeserialize> {
//
//        @Override
//        public String getDisplayName() {
//            return "default";
//        }
//    }
//}
