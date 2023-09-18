/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.connector.plugin;

import io.datavines.connector.api.*;

public class FileConnectorFactory implements ConnectorFactory {

    @Override
    public String getCategory() {
        return "file";
    }

    @Override
    public Connector getConnector() {
        return new FileConnector();
    }

    @Override
    public ResponseConverter getResponseConvert() {
        return new FileResponseConverter();
    }

    @Override
    public Dialect getDialect() {
        return new FileDialect();
    }

    @Override
    public ConnectorParameterConverter getConnectorParameterConverter() {
        return new FileConnectorParameterConverter();
    }

    @Override
    public Executor getExecutor() {
        return new FileExecutor();
    }

    @Override
    public TypeConverter getTypeConverter() {
        return new FileTypeConverter();
    }
}
