/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.data.provider;

import datart.core.data.provider.Column;
import lombok.Data;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;

@Data
public class HttpRequestParam {
    // name for the data
    private String name;

    private String url;

    private HttpMethod method;

    private String targetPropertyName;

    private String username;

    private String password;

    private int timeout;

    private Class<? extends HttpResponseParser> responseParser;

    private TreeMap<String, String> headers;

    private TreeMap<String, String> queryParam;

    private String body;

    private String contentType;

    private List<Column> columns;

    public String toCacheKey() {
        return new StringJoiner(":").add(url)
                .add(headers.toString())
                .add(queryParam.toString())
                .add(body).toString();
    }
}
