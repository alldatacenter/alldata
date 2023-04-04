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
package datart.server.job;

import datart.server.base.dto.JobFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class WeChartJob extends ScheduleJob {

    public WeChartJob() {
    }

    @Override
    public void doSend() throws Exception {

        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }

        String webHookUrl = jobConfig.getWebHookUrl();
        RestTemplate restTemplate = new RestTemplate();
        for (JobFile jobFile : attachments) {
            restTemplate.postForEntity(webHookUrl, createParam(jobFile.getFile()), Object.class);
        }
    }

    private Map<String, Object> createParam(File file) throws Exception {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String base64 = Base64Utils.encodeToString(bytes);
        String md5 = DigestUtils.md5DigestAsHex(bytes);
        HashMap<String, Object> param = new HashMap<>();
        param.put("msgtype", "image");
        HashMap<String, String> image = new HashMap<>();
        image.put("base64", base64);
        image.put("md5", md5);
        param.put("image", image);
        return param;
    }

}