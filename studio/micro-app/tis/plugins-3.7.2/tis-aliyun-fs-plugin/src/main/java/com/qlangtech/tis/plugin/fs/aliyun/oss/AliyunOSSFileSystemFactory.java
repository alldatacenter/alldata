///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.qlangtech.tis.plugin.fs.aliyun.oss;
//
//import com.qlangtech.tis.annotation.Public;
//import com.qlangtech.tis.config.ParamsConfig;
//import com.qlangtech.tis.config.aliyun.IHttpToken;
//import com.qlangtech.tis.extension.Descriptor;
//import com.qlangtech.tis.fs.ITISFileSystem;
//import com.qlangtech.tis.offline.FileSystemFactory;
//import com.qlangtech.tis.plugin.annotation.FormField;
//import com.qlangtech.tis.plugin.annotation.FormFieldType;
//import com.qlangtech.tis.plugin.annotation.Validator;
//
//import java.io.File;
//
///**
// * 基于阿里云OSS的
// *
// * @author 百岁（baisui@qlangtech.com）
// * @create: 2020-04-12 20:03
// * @date 2020/04/13
// */
//@Public
//public class AliyunOSSFileSystemFactory extends FileSystemFactory {
//
//    @FormField(identity = true, ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
//    public String name;
//
//    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String rootDir;
//
//    @FormField(ordinal = 3, type = FormFieldType.SELECTABLE, validate = {Validator.require, Validator.identity})
//    public String aliyunToken;
//
//    //example: http://oss-cn-hangzhou.aliyuncs.com
//    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.url})
//    public String endpoint;
//
//    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String bucketName;
//
//    @Override
//    public <Configuration> Configuration getConfiguration() {
//        throw new UnsupportedOperationException();
//        // return null;
//    }
//
//    @Override
//    public void setConfigFile(File cfgDir) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public String identityValue() {
//        return this.name;
//    }
//
//    @Override
//    public String getFSAddress() {
//        return this.endpoint;
//    }
//
//    private ITISFileSystem ossFs;
//
//    @Override
//    public ITISFileSystem getFileSystem() {
//        if (ossFs == null) {
//
//            IHttpToken aliyunToken = IHttpToken.getToken(this.aliyunToken);// ParamsConfig.getItem(this.aliyunToken, IHttpToken.KEY_DISPLAY_NAME);
//            ossFs = new AliyunOSSFileSystem(aliyunToken, this.endpoint, this.bucketName, this.rootDir);
//        }
//        return ossFs;
//    }
//
//    // @TISExtension
//    public static class DefaultDescriptor extends Descriptor<FileSystemFactory> {
//
//        public DefaultDescriptor() {
//            super();
//            this.registerSelectOptions(IHttpToken.KEY_FIELD_ALIYUN_TOKEN, () -> ParamsConfig.getItems(IHttpToken.KEY_DISPLAY_NAME));
//        }
//    }
//}
