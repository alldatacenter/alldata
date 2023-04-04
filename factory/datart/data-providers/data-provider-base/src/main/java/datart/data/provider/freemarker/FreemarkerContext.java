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
package datart.data.provider.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

import java.io.StringWriter;
import java.util.Map;

@Slf4j
public class FreemarkerContext {

    private static final Configuration conf;

    static {
        conf = new Configuration(Configuration.VERSION_2_3_31);
        //模板从字符串加载
        conf.setTemplateLoader(new StringTemplateLoader());
        // 使freemarker支持 null 值
        conf.setClassicCompatible(true);
    }

    public static String process(String content, Map<String, ?> dataModel) {
        String key = DigestUtils.md5DigestAsHex(content.getBytes());
        try {
            StringTemplateLoader.SCRIPT_MAP.put(key, content);
            Template template = conf.getTemplate(key);
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("freemarker parse error", e);
        }
        return content;
    }

}