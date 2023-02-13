package com.alibaba.tesla.appmanager.spring.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
@Slf4j
class AppManagerWebConfig extends WebMvcConfigurationSupport {

    /**
     * 增加 Message 转换器，为流式处理文件提供支持
     */
    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.addDefaultHttpMessageConverters(converters);
        converters.add(new AbstractHttpMessageConverter<InputStream>(MediaType.APPLICATION_OCTET_STREAM) {
            @Override
            protected boolean supports(Class<?> clazz) {
                return InputStream.class.isAssignableFrom(clazz);
            }

            @Override
            protected InputStream readInternal(Class<? extends InputStream> clazz, HttpInputMessage inputMessage) throws
                IOException, HttpMessageNotReadableException {
                return inputMessage.getBody();
            }

            @Override
            protected void writeInternal(InputStream inputStream, HttpOutputMessage outputMessage) throws IOException,
                HttpMessageNotWritableException {
                IOUtils.copy(inputStream, outputMessage.getBody());
            }
        });
        super.configureMessageConverters(converters);
    }
}