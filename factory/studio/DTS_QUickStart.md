# 数据集成单任务配置（使用datax）

**1、安装datax**

    安装参考[linux安装dataX-CSDN博客](https://blog.csdn.net/hzp666/article/details/127350768)

**2、修改service-data-dts-dev.yml配置文件，修改执行路径并新增注册配置**

```yml
 #admin配置
 dts:
  job:
    i18n:
    triggerpool:
      fast:
        max: 200
      slow:
        max: 100
    logretentiondays: 30
  executor:
    #项目里用到的只有chunjun的引用，chunjun都是.sh文件，需要自己拼shell命令
    #项目里用到的都是python文件
    chunjunHome: /home/datax/datax/bin/datax.py #修改为自己的datax路径
    chunjunjsonPath: /home/datax/datax/job/    #修改为自己的datax路径
    chunjunlogHome: /home/datax/datax/job-log    #修改为自己的datax路径
    dataxHome: /home/datax/datax/bin/datax.py
    dataxjsonPath: /home/datax/datax/job/
    dataxlogHome: /home/datax/datax/job-log
datasource:
  aes:
    key: AD42F6697B035B75
#执行器配置
xxl:
  job:
    admin:
      # 修改为自己的data-dts服务地址
      addresses: http://192.168.5.163:9536
    executor:
      # 此执行器的名称
      appname: flinkx-executor
      # 此执行器的端口
      port: 9537
      # 此执行器的日志存放路径
      logpath: logs/xxl-job/datax
      # 此执行器的日志保存时间
      logretentiondays: 7   
 
```



**3、新增执行器配置类**

```java
package com.platform.admin.core.conf;

import com.platform.core.executor.impl.JobSpringExecutor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName： XxlJobConfig
 * @Description: xxl-job依赖配置
 * @author：gucheng
 * @version： 1.0
 */
@Configuration  //是否开启xxl-job定时任务，注释掉 //@Configuration 则不开启定时任务
@Data
@Slf4j
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;


    @Bean
    public JobSpringExecutor xxlJobExecutor() {
        System.out.println("=============== xxl-job config init.===============");

        JobSpringExecutor xxlJobSpringExecutor = new JobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppName(appname);
//        xxlJobSpringExecutor.setAddress(address);
//        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
//        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        return xxlJobSpringExecutor;
    }
}
```

**3、代码修改**

    在handler文件夹下创建DataxJobHandler.java

```java

import com.platform.admin.core.thread.JobTriggerPoolHelper;
import com.platform.core.biz.model.ReturnT;
import com.platform.core.biz.model.TriggerParam;
import com.platform.core.handler.IJobHandler;
import com.platform.core.handler.annotation.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @ClassName： DataxJobHandler
 * @Description: executorJobHandler
 * @author：gucheng
 */
@Slf4j
@Component
@JobHandler("executorJobHandler")
public class DataxJobHandler extends IJobHandler {

    @Override
    public ReturnT<String> execute(TriggerParam tgParam) throws Exception {
        log.info("---------Datax定时任务开始执行--------");
        //数据抽取具体的执行方法
        JobTriggerPoolHelper.runJob(tgParam.getJobId());
        System.out.println("---------Datax定时任务执行成功--------");
        return ReturnT.SUCCESS;
    }
}
```



修改FlinkxJsonHelper.java中的buildReader和buildWriter方法，可能是datax的版本问题，生成的json和我下载的datax不匹配，需要把中间的几行代码注释掉

![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-06-51-image.png)

![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-07-10-image.png)

修改JobTriggerPoolHelper.java,注释掉红框代码

![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-13-23-image.png)

![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-14-10-image.png)

**4、页面配置**

配置数据源

jdbc:mysql://192.168.5.166:3306/dssq?useUnicode=true&characterEncoding=utf8

![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-20-20-image.png)

![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-21-13-image.png)

配置执行器

![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-23-19-image.png)

配置任务模版

![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-24-22-image.png)

配置单任务

按步骤选择数据抽取库，数据合并库，映射字段，构建datax的可执行json文件，选择任务模版，点击下一步任务就创建完成了，可以在任务详情里查看

![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-27-53-image.png)



![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-28-15-image.png)



![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-28-36-image.png)



![](/Users/gucheng/Library/Application%20Support/msbmarkdown/images/2023-05-18-14-29-13-image.png)

最后可以运行的json文件

```json
{
	"job": {
		"setting": {
			"speed": {
				"channel": 1,
				"bytes": 0
			},
			"errorLimit": {
				"record": 100
			},
			"restore": {
				"maxRowNumForCheckpoint": 0,
				"isRestore": false,
				"restoreColumnName": "",
				"restoreColumnIndex": 0
			},
			"log": {
				"isLogger": false,
				"level": "debug",
				"path": "",
				"pattern": ""
			}
		},
		"content": [{
			"reader": {
				"name": "mysqlreader",
				"parameter": {
					"username": "root",
					"password": "123456",
					"column": [
						"id",
						"demand_post_id",
						"batch",
						"month",
						"cid",
						"cName",
						"depid",
						"depName",
						"rlwbtype",
						"protype",
						"prolevel",
						"workexperience",
						"technicalability",
						"dno",
						"deliveryNo",
						"resumeNo",
						"qualifiedNo",
						"rcNo",
						"lcNo",
						"urgencydegree",
						"workplace",
						"projectName",
						"intime",
						"outtime",
						"applicationdate",
						"contact",
						"tel",
						"email",
						"state",
						"reason",
						"ks",
						"jjmsNo",
						"projectId",
						"applyNum",
						"empType",
						"postId"
					],
					"splitPk": "",
					"connection": [{
						"table": [
							"hrm_demand"
						],
						"jdbcUrl": [
							"jdbc:mysql://192.168.5.166:3306/dssq?useUnicode=true&characterEncoding=utf8"
						]
					}]
				}
			},
			"writer": {
				"name": "mysqlwriter",
				"parameter": {
					"username": "root",
					"password": "123456",
					"writeMode": "insert",
					"column": [
						"id",
						"demand_post_id",
						"batch",
						"month",
						"cid",
						"cName",
						"depid",
						"depName",
						"rlwbtype",
						"protype",
						"prolevel",
						"workexperience",
						"technicalability",
						"dno",
						"deliveryNo",
						"resumeNo",
						"qualifiedNo",
						"rcNo",
						"lcNo",
						"urgencydegree",
						"workplace",
						"projectName",
						"intime",
						"outtime",
						"applicationdate",
						"contact",
						"tel",
						"email",
						"state",
						"reason",
						"ks",
						"jjmsNo",
						"projectId",
						"applyNum",
						"empType",
						"postId"
					],
					"connection": [{
						"table": [
							"hrm_demand_merge"
						],
						"jdbcUrl": "jdbc:mysql://192.168.5.166:3306/test_merge?useUnicode=true&characterEncoding=utf8"
					}]
				}
			}
		}]
	}
}
```


