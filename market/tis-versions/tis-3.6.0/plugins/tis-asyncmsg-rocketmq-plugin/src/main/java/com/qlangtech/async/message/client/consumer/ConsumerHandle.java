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
//package com.qlangtech.async.message.client.consumer;
//
//import com.qlangtech.tis.async.message.client.consumer.AsyncMsg;
//import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
//
///*
// * 消息处理Handle
// * Created by binggun on 2015/6/8
// * INFO:这个方法使用于 ONS
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public abstract class ConsumerHandle implements IConsumerHandle {
//
//    // 支持 || 关系
//    private String subExpression;
//
//    /**
//     * 处理消息Handle，业务逻辑在此处理
//     *
//     * @param message
//     * @return true:处理成功;false:处理失败，重新投递
//     */
//    public abstract boolean consume(AsyncMsg message);
//
//    public String getSubExpression() {
//        return subExpression;
//    }
//
//    public void setSubExpression(String subExpression) {
//        this.subExpression = subExpression;
//    }
//}
