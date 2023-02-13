由于org.apache.flink.streaming.connectors.elasticsearch7.Elasticsearch7ApiCallBridge 类中的属性
``` java

private final List<HttpHost> httpHosts;

```
在Flink提交任务生成executeGraph过程中会反序列化中由于TIS Pluing中其他插件中也有相同的HttpHost类导致在反序列过程中有类冲突的异常所以需要将
包org.apache.http下的类内嵌化所以就先加一个tis-elasticsearch7-sink的module被tis-elasticsearch7-sink-plugin依赖
