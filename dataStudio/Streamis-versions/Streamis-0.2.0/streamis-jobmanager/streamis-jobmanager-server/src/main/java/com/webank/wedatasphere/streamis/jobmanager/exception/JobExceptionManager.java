/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.exception;

import java.util.HashMap;
import java.util.Map;

public class JobExceptionManager {
    //30300-30599
    private static Map<String, String> desc = new HashMap<String, String>(32);
    static {
        desc.put("30300", "upload failure(上传失败)");
        desc.put("30301","%s cannot be empty!");
        desc.put("30302", "upload file type should be zip(上传的文件类型应为zip类型)");
    }

    public static JobException createException(int errorCode, Object... format) throws JobException {
        return new JobException(errorCode, String.format(desc.get(String.valueOf(errorCode)), format));
    }
}
