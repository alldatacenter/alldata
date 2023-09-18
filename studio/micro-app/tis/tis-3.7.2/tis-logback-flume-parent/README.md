logback-flume-appender
======================

Logback appender to forward log messages to a Flume agent

## Configuration Entries

* **application**: Application name, if unset it will be inferred from the application.name system property
* **hostname**: Host name, if unset it will be inferred from the box host name via `InetAddress.getLocalHost().getHostName()`
* **type**: Type of logging, will be ignored if unset
* **flumeAgents**: Comma separated list of flume avro agents in format `{hostname}:{port}`
* **flumeProperties**: Additional properties to create the flume RpcClient in format `{key}={value}` separated by `;`
* **batchSize**: Set the batch size of events to report, overriding the default of 50
* **reportingWindow**: Override the maximum time spent trying to fill a batch, between 100ms and 10s
* **additionalAvroHeaders**: Add additional headers to the events in format `{key}={value}` separated by `;`
* **reporterMaxThreadPoolSize**: Set the maximum number of threads for the reporter to use, overriding the default 2
* **reporterMaxQueueSize**: Set the maximum number of event batches to keep in memory, overriding the default 50

**Sample configuration**

```
<configuration debug="true">
     <appender name="flume" class="com.gilt.logback.flume.FlumeLogstashV1Appender">
         <flumeAgents>
             flume-es-1b.gilt.com:5000,
             flume-es-1c.gilt.com:5000,
             flume-es-1d.gilt.com:5000
         </flumeAgents>
         <flumeProperties>
             connect-timeout=4000;
             request-timeout=8000
         </flumeProperties>
         <batchSize>100</batchSize>
         <reportingWindow>1000</reportingWindow>
         <additionalAvroHeaders>
             myHeader=myValue
         </additionalAvroHeaders>
         <application>smapleapp</application>
         <layout class="ch.qos.logback.classic.PatternLayout">
             <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - \(%file:%line\) - %message%n%ex</pattern>
         </layout>
     </appender>

     <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
         <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
             <level>WARN</level>
         </filter>
         <encoder>
             <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - \(%file:%line\) - %message%n%ex</pattern>
         </encoder>
     </appender>

     <logger name="play" level="INFO" />
     <logger name="application" level="DEBUG" />

     <!-- Off these ones as they are annoying, and anyway we manage configuration ourselves -->
     <logger name="com.avaje.ebean.config.PropertyMapLoader" level="OFF" />
     <logger name="com.avaje.ebeaninternal.server.core.XmlConfigLoader" level="OFF" />
     <logger name="com.avaje.ebeaninternal.server.lib.BackgroundThread" level="OFF" />
     <logger name="com.gargoylesoftware.htmlunit.javascript" level="OFF" />

     <root level="INFO">
         <appender-ref ref="flume" />
         <appender-ref ref="console"/>
     </root>
 </configuration>
 ```
 