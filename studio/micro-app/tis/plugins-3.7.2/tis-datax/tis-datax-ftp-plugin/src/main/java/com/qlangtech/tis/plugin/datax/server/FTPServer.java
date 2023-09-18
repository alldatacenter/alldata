/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.datax.server;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.datax.plugin.ftp.common.FtpHelper;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-14 17:07
 **/
public class FTPServer extends ParamsConfig {

    public static final String FTP_SERVER = "FTPServer";

    @FormField(ordinal = 0, identity = true, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 3, type = FormFieldType.ENUM, validate = {Validator.require})
    public String protocol;
    @FormField(ordinal = 6, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.hostWithoutPort})
    public String host;
    @FormField(ordinal = 9, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer})
    public Integer port;
    @FormField(ordinal = 12, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer timeout;
    @FormField(ordinal = 15, type = FormFieldType.ENUM, validate = {Validator.require})
    public String connectPattern;
    @FormField(ordinal = 18, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String username;
    @FormField(ordinal = 21, type = FormFieldType.PASSWORD, validate = {Validator.require})
    public String password;


    public static FTPServer getServer(String ftpServerId) {
        if (StringUtils.isEmpty(ftpServerId)) {
            throw new IllegalArgumentException("param ftpServerId can not be null");
        }
        FTPServer ftpServer = ParamsConfig.getItem(ftpServerId, FTP_SERVER);
        return (ftpServer);
    }


    @Override
    public FTPServer createConfigInstance() {
        return this;
    }

    public FtpHelper createFtpHelper(Integer timeout) {
        return FtpHelper.createFtpClient(this.protocol, this.host, this.username, this.password, this.port, timeout, this.connectPattern);
    }

    public <T> T useFtpHelper(FtpHelperVisitor<T> helperConsumer) {
        try (FtpHelper ftpHelper = this.createFtpHelper(this.timeout)) {
            return helperConsumer.accept(ftpHelper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public interface FtpHelperVisitor<T> {
        T accept(FtpHelper helper) throws Exception;
    }

    @Override
    public String identityValue() {
        return this.name;
    }

    @TISExtension
    public static class DefaultDesc extends Descriptor<ParamsConfig> {
        @Override
        public String getDisplayName() {
            return FTP_SERVER;
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            FTPServer server = (FTPServer) postFormVals.newInstance(this, msgHandler);
            try (FtpHelper ftpHelper = server.createFtpHelper(2000)) {
            } catch (Exception e) {
                throw TisException.create("请检查配置参数是否正确", e);
            }
            //  return super.verify(msgHandler, context, postFormVals);
            return true;
        }
    }
}
