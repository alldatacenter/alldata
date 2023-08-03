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

package org.apache.seatunnel.app.utils;

import org.apache.seatunnel.app.domain.request.job.transform.CopyTransformOptions;
import org.apache.seatunnel.app.domain.request.job.transform.FieldMapperTransformOptions;
import org.apache.seatunnel.app.domain.request.job.transform.SQLTransformOptions;
import org.apache.seatunnel.app.domain.request.job.transform.SplitTransformOptions;
import org.apache.seatunnel.app.domain.request.job.transform.Transform;
import org.apache.seatunnel.app.domain.request.job.transform.TransformOptions;
import org.apache.seatunnel.server.common.SeatunnelErrorEnum;
import org.apache.seatunnel.server.common.SeatunnelException;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class TaskOptionUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T extends TransformOptions> T getTransformOption(
            Transform transform, String transformOptionsStr) throws IOException {
        switch (transform) {
            case FIELDMAPPER:
                return convertTransformStrToOptions(
                        transformOptionsStr, FieldMapperTransformOptions.class);
            case MULTIFIELDSPLIT:
                return convertTransformStrToOptions(
                        transformOptionsStr, SplitTransformOptions.class);
            case COPY:
                return convertTransformStrToOptions(
                        transformOptionsStr, CopyTransformOptions.class);
            case SQL:
                return convertTransformStrToOptions(transformOptionsStr, SQLTransformOptions.class);
            case FILTERROWKIND:
            case REPLACE:
            default:
                return null;
        }
    }

    public static <T extends TransformOptions> T convertTransformStrToOptions(
            String transformOptionsStr, Class<? extends TransformOptions> optionClass)
            throws IOException {
        if (StringUtils.isEmpty(transformOptionsStr)) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.ILLEGAL_STATE,
                    optionClass.getName() + " transformOptions can not be empty");
        }
        return (T) OBJECT_MAPPER.readValue(transformOptionsStr, optionClass);
    }
}
