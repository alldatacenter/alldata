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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import datart.core.common.MessageResolver;
import datart.core.common.UUIDGenerator;
import datart.core.data.provider.*;
import datart.data.provider.jdbc.SqlScriptRender;
import datart.data.provider.local.LocalDB;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class HttpDataProvider extends DefaultDataProvider {

    public static final String URL = "url";

    public static final String PROPERTY = "property";

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    public static final String TIMEOUT = "timeout";

    public static final String REQUEST_METHOD = "method";

    private static final int DEFAULT_REQUEST_TIMEOUT = 30 * 1_000;

    private static final String RESPONSE_PARSER = "responseParser";

    private static final String DEFAULT_PARSER = "datart.data.provider.ResponseJsonParser";

    private static final String QUERY_PARAM = "queryParam";

    private static final String BODY = "body";

    private static final String HEADER = "headers";

    private static final String CONTENT_TYPE = "contentType";

    private static final String I18N_PREFIX = "config.template.http.";

    private final static ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public HttpDataProvider() {
    }

    @Override
    public Dataframes loadDataFromSource(DataProviderSource config) throws IOException, ClassNotFoundException, URISyntaxException, SQLException {

        List<HttpRequestParam> requestParams = convertRequestParams(config);
        if (CollectionUtils.isEmpty(requestParams)) {
            return Dataframes.of(config.getSourceId());
        }
        String dataKey = DigestUtils.md5Hex(requestParams.stream()
                .map(HttpRequestParam::toCacheKey)
                .collect(Collectors.joining(",")));

        Dataframes dataframes = Dataframes.of(dataKey);

        // check cache
        Object cacheEnable = config.getProperties().get("cacheEnable");
        if (cacheEnable != null && Boolean.parseBoolean(cacheEnable.toString())) {
            if (!LocalDB.checkCacheExpired(dataframes.getKey())) {
                return dataframes;
            }
        }

        for (HttpRequestParam requestParam : requestParams) {
            Dataframe dataframe = new HttpDataFetcher(requestParam).fetchAndParse();
            dataframe.setName(requestParam.getName());
            dataframes.add(dataframe);
        }

        return dataframes;
    }

    @Override
    public String getQueryKey(DataProviderSource config, QueryScript script, ExecuteParam executeParam) throws Exception {
        List<HttpRequestParam> requestParams = convertRequestParams(config);
        String requestKey = DigestUtils.md5Hex(requestParams.stream()
                .map(HttpRequestParam::toCacheKey)
                .collect(Collectors.joining(",")));
        SqlScriptRender render = new SqlScriptRender(script, executeParam, LocalDB.SQL_DIALECT);
        return "Q" + DigestUtils.md5Hex(requestKey + render.render(true, true, true));
    }

    @Override
    public boolean cacheExists(DataProviderSource config, String cacheKey) throws SQLException {
        return false;
    }

    @Override
    public String getConfigFile() {
        return "http-data-provider.json";
    }

    @Override
    public String getConfigDisplayName(String name) {
        return MessageResolver.getMessage(I18N_PREFIX + name);
    }

    @Override
    public String getConfigDescription(String name) {
        String message = MessageResolver.getMessage(I18N_PREFIX + name + ".desc");
        if (message.startsWith(I18N_PREFIX)) {
            return null;
        } else {
            return message;
        }
    }

    private List<HttpRequestParam> convertRequestParams(DataProviderSource source) throws ClassNotFoundException {

        List<HttpRequestParam> requestParams = new ArrayList<>();

        List<Map<String, Object>> schemas;
        if (source.getProperties().containsKey(SCHEMAS)) {
            schemas = (List<Map<String, Object>>) source.getProperties().get(SCHEMAS);
        } else {
            schemas = Collections.singletonList(source.getProperties());
        }
        if (CollectionUtils.isEmpty(schemas)) {
            return requestParams;
        }

        for (Map<String, Object> schema : schemas) {
            requestParams.add(convert2RequestParam(schema));
        }
        return requestParams;
    }

    private HttpRequestParam convert2RequestParam(Map<String, Object> schema) throws ClassNotFoundException {

        HttpRequestParam httpRequestParam = new HttpRequestParam();

        httpRequestParam.setName(StringUtils.isNoneBlank(schema.getOrDefault(TABLE, "").toString()) ? schema.get(TABLE).toString() : "TEST" + UUIDGenerator.generate());

        httpRequestParam.setUrl(schema.get(URL).toString());

        httpRequestParam.setPassword(schema.get(PASSWORD).toString());

        httpRequestParam.setUsername(schema.get(USERNAME).toString());

        httpRequestParam.setMethod(HttpMethod.resolve(schema.getOrDefault(REQUEST_METHOD, HttpMethod.GET.name()).toString()));

        httpRequestParam.setTimeout(Integer.parseInt(schema.getOrDefault(TIMEOUT, DEFAULT_REQUEST_TIMEOUT + "").toString()));

        httpRequestParam.setTargetPropertyName(schema.get(PROPERTY).toString());

        httpRequestParam.setContentType(schema.getOrDefault(CONTENT_TYPE, "application/json").toString());

        String parserClass = DEFAULT_PARSER;
        Object parser = schema.get(RESPONSE_PARSER);
        if (parser != null && StringUtils.isNotBlank(parser.toString())) {
            parserClass = parser.toString();
        }
        Class<? extends HttpResponseParser> aClass = (Class<? extends HttpResponseParser>) Class.forName(parserClass);
        httpRequestParam.setResponseParser(aClass);
        Object body = schema.get(BODY);
        if (body != null) {
            httpRequestParam.setBody(body.toString());
        }
        httpRequestParam.setQueryParam(new TreeMap<>((Map<String, String>) schema.get(QUERY_PARAM)));

        httpRequestParam.setHeaders(new TreeMap<>((Map<String, String>) schema.get(HEADER)));

        httpRequestParam.setColumns(parseColumns(schema));

        return httpRequestParam;
    }

    private void replaceVariables(DataProviderSource config, Map<String, Object> schema) {
        try {
            if (CollectionUtils.isEmpty(config.getVariables())) {
                return;
            }
            String url = schema.get(URL).toString();
            if (StringUtils.isNotBlank(url)) {
                schema.put(URL, replaceVariables(config.getVariables(), url, true));
            }
            String username = schema.get(USERNAME).toString();
            if (StringUtils.isNotBlank(username)) {
                schema.put(USERNAME, replaceVariables(config.getVariables(), username, false));
            }
            String password = schema.get(PASSWORD).toString();
            if (StringUtils.isNotBlank(password)) {
                schema.put(PASSWORD, replaceVariables(config.getVariables(), username, false));
            }
            String body = schema.get(BODY).toString();
            if (StringUtils.isNotBlank(body)) {
                schema.put(BODY, replaceVariables(config.getVariables(), username, false));
            }
            Map<String, String> header = (Map<String, String>) schema.get(HEADER);
            if (!CollectionUtils.isEmpty(header)) {
                HashMap<Object, Object> newHeader = new HashMap<>();
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    String key = replaceVariables(config.getVariables(), entry.getKey(), false);
                    String value = replaceVariables(config.getVariables(), entry.getValue(), false);
                    newHeader.put(key, value);
                }
                schema.put(HEADER, newHeader);
            }

            Map<String, String> queryParam = (Map<String, String>) schema.get(QUERY_PARAM);
            if (!CollectionUtils.isEmpty(queryParam)) {
                HashMap<String, String> newQueryParam = new HashMap<>();
                for (Map.Entry<String, String> entry : queryParam.entrySet()) {
                    String key = replaceVariables(config.getVariables(), entry.getKey(), true);
                    String value = replaceVariables(config.getVariables(), entry.getValue(), true);
                    newQueryParam.put(key, value);
                }
                schema.put(QUERY_PARAM, newQueryParam);
            }

        } catch (Exception e) {
            log.error("http data source variable replace error", e);
        }
    }

    private String replaceVariables(List<ScriptVariable> variables, String str, boolean urlEncode) {
        if (CollectionUtils.isEmpty(variables) || StringUtils.isBlank(str)) {
            return str;
        }
        for (ScriptVariable variable : variables) {
            String value = variable.valueToString();
            if (urlEncode) {
                try {
                    value = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
                } catch (Exception ignored) {
                }
            }
            str = str.replaceAll(variable.getNameWithQuote(), value);
        }
        return str;
    }

}
