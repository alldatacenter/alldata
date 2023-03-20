package cn.datax.common.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.Collections;

@Configuration
public class MessageConverterConfig {

    @Bean
    public MappingJackson2HttpMessageConverter customMappingJackson2HttpMessageConverter() {
        return new CustomMappingJackson2HttpMessageConverter();
    }

    static class CustomMappingJackson2HttpMessageConverter extends MappingJackson2HttpMessageConverter {
        public CustomMappingJackson2HttpMessageConverter() {
            setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));
        }
    }
}