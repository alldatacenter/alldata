#商城PC服务端

#项目配置

1、导入mall.sql，配置mysql账号密码。

2、配置tomcat端口

tomcat7:

​	mall-pc-manager: 8889JMX: 1100

​	mall-pc-content: 8890 JMX：1101

​	mall-pc-sso: 8083 JMX: 1104

​	mall-pc-search: 8891 JMX:1103

​	mall-pc-front-end: 7777 JMX 1102

#环境配置

1、dubbo server配置：

​		1.1 修改以下文件：

​		mall-pc-content-service\src\main\resources\spring\applicationContext-service.xml

​		mall-pc-manager-service\src\main\resources\spring\applicationContext-service.xml

​		mall-pc-search-service\src\main\resources\spring\applicationContext-service.xml

​		mall-pc-sso-service\src\main\resources\spring\applicationContext-service.xml

 		1.2 修改内容：

```xml
<!--dubbo与zookeeper直连-->
<dubbo:registry protocol="zookeeper" address="127.0.0.1:2181" />
<!--dubbo与zookeeper分布式部署-->
<dubbo:registry protocol="zookeeper" address="zookeeper://192.168.172.129:2181?backup=192.168.172.130:2181,192.168.172.131:2181" 
                />
```
2、dubbo customer配置：

​	1.1 修改mall-pc-front-web\src\main\resources\spring\springmvc.xml

​	1.2 修改内容同上

#商城运行顺序

1、启动zookeeper服务（windows) dault

2、启动redis服务（windows) default

3、启动activemq服务（windows) default

4、运行mall-pc-manager

5、运行mall-pc-content

6、运行mall-pc-sso

7、运行mall-pc-search

8、运行mall-pc-front-end，cd进入mall-pc-front-end项目根目录，执行npm install，然后执行npm run dev

9、访问localhost:9090页面。