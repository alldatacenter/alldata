package com.platform.dts.admin;

import com.platform.dts.admin.entity.Common;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.core.env.Environment;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author AllDataDC
 * @date 2022/11/16 11:14
 * @Description: Engine是入口类，该类负责数据的初始化
 **/
@EnableSwagger2
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class Engine {

    private static Logger logger = LoggerFactory.getLogger(Engine.class);

    public static void main(String[] args) throws UnknownHostException {
    	Environment env = new SpringApplication(Engine.class).run(args).getEnvironment();
       //获取参数
        String port = env.getProperty(Common.SERVERPORT)== null ? Common.PORT : env.getProperty(Common.SERVERPORT);
        String context = env.getProperty(Common.SERVERCONTEXTPATH)== null ? Common.CONTEXTPATH : env.getProperty(Common.SERVERCONTEXTPATH);
		String hostAddress = InetAddress.getLocalHost().getHostAddress();
		//拼凑路径
		String localAPIPath = StringUtils.join(Common.PREFEX,Common.LOCALADDRESS,":",port,context,Common.IndexPATH);
		String externalAPIPath = StringUtils.join(Common.PREFEX,hostAddress,":",port,context,Common.DOCPATH);
		String webURLPath = StringUtils.join(Common.PREFEX,hostAddress,":",port,context,Common.IndexPATH);
		//打印日志
		logger.info(
                "Access URLs:\n----------------------------------------------------------\n\t"
                        + "Local-API: \t\t{}\n\t"
                        + "External-API: \t\t{}\n\t"
                        + "web-URL: \t\t{}\n\t"
						+ "----------------------------------------------------------",
				localAPIPath, externalAPIPath, webURLPath);
    }
}
