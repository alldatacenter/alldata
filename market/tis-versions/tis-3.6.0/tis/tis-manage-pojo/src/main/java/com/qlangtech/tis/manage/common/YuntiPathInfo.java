/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.common;

import java.util.HashMap;
import java.util.Iterator;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class YuntiPathInfo extends HashMap<String, String> {

    private static final long serialVersionUID = 1L;

    public static final String YUNTI_PATH = "yuntiPath";

    public static final String YUNTI_TOKEN = "yuntiToken";

    @SuppressWarnings("all")
    public YuntiPathInfo(String jsonText) {
        try {
            JSONTokener tokener = new JSONTokener(jsonText);
            JSONObject json = new JSONObject(tokener);
            Iterator it = json.keys();
            String key = null;
            while (it.hasNext()) {
                key = (String) it.next();
                this.put(key, json.getString(key));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static String createYuntiPathInfo(String yuntiPath, String yuntiToken) {
        try {
            JSONObject json = new JSONObject();
            json.put(YUNTI_PATH, yuntiPath);
            json.put(YUNTI_TOKEN, yuntiToken);
            return json.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPath() {
        return this.get(YUNTI_PATH);
    }

    public String getUserToken() {
        return StringUtils.trimToEmpty(this.get(YUNTI_TOKEN));
    }
}
