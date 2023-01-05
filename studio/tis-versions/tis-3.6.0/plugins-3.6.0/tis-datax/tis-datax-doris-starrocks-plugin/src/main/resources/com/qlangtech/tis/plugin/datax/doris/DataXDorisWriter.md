## loadProps

StreamLoad 的请求参数,默认传入的数据均会被转为字符串，并以 **\t** 作为列分隔符，**\n** 作为行分隔符，组成csv文件进行 [StreamLoad导入参数说明](http://doris.apache.org/master/zh-CN/administrator-guide/load-data/stream-load-manual.html#%E5%AF%BC%E5%85%A5%E4%BB%BB%E5%8A%A1%E5%8F%82%E6%95%B0)。 如需更改列分隔符， 则正确配置 loadProps 即可：

```json
 {
  "column_separator": "\\x01",
  "line_delimiter": "\\x02"
}
```
