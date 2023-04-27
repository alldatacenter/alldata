package com.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author AllDataDC
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class DataCompareApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(DataCompareApplication.class, args);
    }
}