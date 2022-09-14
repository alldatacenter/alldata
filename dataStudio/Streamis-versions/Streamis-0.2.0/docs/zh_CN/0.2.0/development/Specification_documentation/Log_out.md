## 日志规范

1.	【**约定**】Streamis 选择引用 Linkis Commons 通用模块，其中已包含了日志框架，主要以 **slf4j** 和 **Log4j2** 作为日志打印框架，去除了Spring-Cloud包中自带的logback。
由于Slf4j会随机选择一个日志框架进行绑定，所以以后在引入新maven包的时候，需要将诸如slf4j-log4j等桥接包exclude掉，不然日志打印会出现问题。但是如果新引入的maven包依赖log4j等包，不要进行exclude，不然代码运行可能会报错。

2.	【**配置**】log4j2的配置文件默认为 log4j2.xml ，需要放置在 classpath 中。如果需要和 springcloud 结合，可以在 application.yml 中加上 logging:config:classpath:log4j2-spring.xml (配置文件的位置)。

3.	【**强制**】类中不可直接使用日志系统（log4j2、Log4j、Logback）中的API。
  
   * 如果是Scala代码，强制继承Logging trait
   * java采用 LoggerFactory.getLogger(getClass)。

4.	【**强制**】严格区分日志级别。其中：

   * Fatal级别的日志，在初始化的时候，就应该抛出来，并使用System.out(-1)退出。
   * ERROR级别的异常为开发人员必须关注和处理的异常，不要随便用ERROR级别的。
   * Warn级别是用户操作异常日志和一些方便日后排除BUG的日志。
   * INFO为关键的流程日志。
   * DEBUG为调式日志，非必要尽量少写。

5.	【**强制**】要求：INFO级别的日志，每个小模块都必须有，关键的流程、跨模块级的调用，都至少有INFO级别的日志。守护线程清理资源等必须有WARN级别的日志。

6.	【**强制**】异常信息应该包括两类信息：案发现场信息和异常堆栈信息。如果不处理，那么通过关键字throws往上抛出。 正例：logger.error(各类参数或者对象toString + "_" + e.getMessage(), e);