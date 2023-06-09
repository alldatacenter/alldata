## column
 * MongoDB的文档列名。是JSONArray结构类型，内部的JSONObject的元祖需要具有，'name'，'type'(可选以下`int`, `long`, `double`, `string`, `array`, `date`, `boolean`, `bytes`),样例：
  ```json
    [{ "name": "frontcat_id", "type": "Array", "splitter": " " },
     { "name": "unique_id", "type": "string"  }    ]
  ```
 * 'splitter'(因为MongoDB支持数组类型，但是Datax框架本身不支持数组类型，所以mongoDB读出来的数组类型要通过这个分隔符合并成字符串)"
 * 类型转换
 
 | DataX 内部类型| MongoDB 数据类型    |
 | -------- | -----  |
 | Long     | int, Long |
 | Double   | double |
 | String   | string, array |
 | Date     | date  |
 | Boolean  | boolean |
 | Bytes    | bytes |
## upsertInfo
 
 指定了传输数据时更新的信息。填写JSONObject格式，需要有两个属性'isUpsert'(当设置为true时，表示针对相同的upsertKey做更新操作),'upsertKey'(upsertKey指定了没行记录的业务主键。用来做更新时使用)【选填】,例子：
 ```json
  {"isUpsert":true,"upsertKey":"unique_id"}
  ```
