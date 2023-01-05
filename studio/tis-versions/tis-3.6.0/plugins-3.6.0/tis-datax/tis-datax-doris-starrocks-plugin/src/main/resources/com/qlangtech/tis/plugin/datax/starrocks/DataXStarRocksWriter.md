## loadProps

StreamLoad 的请求参数，默认传入的数据均会被转为字符串，并以 **\t** 作为列分隔符，**\n** 作为行分隔符，组成csv文件进行 [StreamLoad导入参数说明](https://docs.starrocks.io/zh-cn/latest/loading/stream_load_transaction_interface)。 如需更改列分隔符， 则正确配置 loadProps 即可：

```json
 {
    "column_separator": "\\x01",
    "row_delimiter": "\\x02"
}
```
