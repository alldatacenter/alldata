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
package io.datavines.server.api.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Properties;

@Configuration
public class KaptchaConfig {

    @Value("${kaptcha.border:yes}")
    private String kaptchaBorder;

    @Value("${kaptcha.border.color:105,179,90}")
    private String kaptchaBorderColor;

    @Value("${kaptcha.textproducer.font.color:blue}")
    private String kaptchaTextproducerFontColor;

    @Value("${kaptcha.image.width:125}")
    private String kaptchaImageWidth;

    @Value("${kaptcha.image.height:65}")
    private String kaptchaImageHeight;

    @Value("${kaptcha.textproducer.font.size:45}")
    private String kaptchaTextproducerFontSize;

    @Value("${kaptcha.session.key:code}")
    private String kaptchaSessionKey;

    @Value("${kaptcha.textproducer.char.length:4}")
    private String getKaptchaTextproducerCharLength;

    @Value("${kaptcha.textproducer.font.names:cmr10}")
    private String kaptchaTextproducerFontNames;

    @Bean
    public DefaultKaptcha captchaProducer(){
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        properties.setProperty("kaptcha.border", kaptchaBorder);
        properties.setProperty("kaptcha.border.color", kaptchaBorderColor);
        properties.setProperty("kaptcha.textproducer.font.color", kaptchaTextproducerFontColor);
        properties.setProperty("kaptcha.image.width", kaptchaImageWidth);
        properties.setProperty("kaptcha.image.height", kaptchaImageHeight);
        properties.setProperty("kaptcha.textproducer.font.size", kaptchaTextproducerFontSize);
        properties.setProperty("kaptcha.session.key", kaptchaSessionKey);
        properties.setProperty("kaptcha.textproducer.char.length", getKaptchaTextproducerCharLength);
        properties.setProperty("kaptcha.textproducer.font.names", kaptchaTextproducerFontNames);
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
