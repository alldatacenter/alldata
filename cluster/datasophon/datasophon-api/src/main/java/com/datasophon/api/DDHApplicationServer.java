package com.datasophon.api;

import com.datasophon.api.master.ActorUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import javax.annotation.PostConstruct;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@ServletComponentScan
@ComponentScan("com.datasophon")
@MapperScan("com.datasophon.dao")
public class DDHApplicationServer extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(DDHApplicationServer.class, args);
    }

    @PostConstruct
    public void run() throws UnknownHostException {
        String hostName = InetAddress.getLocalHost().getHostName();
        CacheUtils.put(Constants.HOSTNAME,hostName);
        ActorUtils.init();
    }
}
