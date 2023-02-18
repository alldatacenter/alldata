package com.datasophon.api.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class ConfigBean {
    @Value("${server.port}")
    private String serverPort;

}
