## path

要读取的文件路径，如果要读取多个文件，可以使用正则表达式"*"，注意这里可以支持填写多个路径

要读取的文件路径，如果要读取多个文件，可以使用正则表达式"*"，注意这里可以支持填写多个路径。

当指定单个Hdfs文件，HdfsReader暂时只能使用单线程进行数据抽取。二期考虑在非压缩文件情况下针对单个File可以进行多线程并发读取。

当指定多个Hdfs文件，HdfsReader支持使用多线程进行数据抽取。线程并发数通过通道数指定。

当指定通配符，HdfsReader尝试遍历出多个文件信息。例如: 指定/*代表读取/目录下所有的文件，指定/bazhen/\*代表读取bazhen目录下游所有的文件。HdfsReader目前只支持"*"和"?"作为文件通配符。

**特别需要注意的是，DataX会将一个作业下同步的所有的文件视作同一张数据表。用户必须自己保证所有的File能够适配同一套schema信息。并且提供给DataX权限可读。**


## column

* 描述：读取字段列表，type指定源数据的类型，index指定当前列来自于文本第几列(以0开始)，value指定当前类型为常量，不从源头文件读取数据，而是根据value值自动生成对应的列。 <br />

  默认情况下，用户可以全部按照String类型读取数据，配置如下：

  ```json
	 ["*"]
  ```
  用户可以指定Column字段信息，配置如下：
  ```json5
   [{
    "type": "long",
    "index": 0    //从本地文件文本第一列获取int字段
   },
   {
    "type": "string",
    "value": "alibaba"  //HdfsReader内部生成alibaba的字符串字段作为当前字段
   }]
	```
	对于用户指定Column信息，type必须填写，index/value必须选择其一。
   例子：
   ```json
  [
    {
       "index": 0,
       "type": "long"
    },
    {
       "index": 1,
       "type": "boolean"
    },
    {
       "type": "string",
       "value": "hello"
    },
    {
       "index": 2,
       "type": "double"
     }
  ]
```	
	
