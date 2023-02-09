/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SecureObjectInputStream extends ObjectInputStream {

    public static final List<String> ALLOWED_CLASS_NAMES = Collections.unmodifiableList(
            Arrays.asList("java.util.ArrayList", "com.obs.services.model.PartEtag", "java.lang.Integer",
                    "java.lang.Number", "java.util.Date",
                    "com.obs.services.internal.DownloadResumableClient$TmpFileStatus",
                    "com.obs.services.internal.UploadResumableClient$UploadCheckPoint",
                    "com.obs.services.internal.UploadResumableClient$FileStatus",
                    "com.obs.services.internal.UploadResumableClient$UploadPart",
                    "com.obs.services.internal.DownloadResumableClient$DownloadCheckPoint",
                    "com.obs.services.internal.DownloadResumableClient$DownloadPart",
                    "com.obs.services.internal.DownloadResumableClient$ObjectStatus"));

    public SecureObjectInputStream() throws IOException, SecurityException {
        super();
    }

    public SecureObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();
        // 白名单校验
        if (!ALLOWED_CLASS_NAMES.contains(name)) {
            throw new ClassNotFoundException(name + "not find");
        }
        return super.resolveClass(desc);
    }

}
