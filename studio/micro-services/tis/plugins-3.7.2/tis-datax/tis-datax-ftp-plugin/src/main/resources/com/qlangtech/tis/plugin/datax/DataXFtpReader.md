## path
  
  远程FTP文件系统的路径信息，注意这里可以支持填写多个路径。
  
  当指定单个远程FTP文件，FtpReader暂时只能使用单线程进行数据抽取。二期考虑在非压缩文件情况下针对单个File可以进行多线程并发读取。
  
  当指定多个远程FTP文件，FtpReader支持使用多线程进行数据抽取。线程并发数通过通道数指定。
  
  当指定通配符，FtpReader尝试遍历出多个文件信息。例如: 指定`/*`代表读取/目录下所有的文件，指定`/bazhen/*`代表读取bazhen目录下游所有的文件。FtpReader目前只支持*作为文件通配符。
  
  特别需要注意的是，DataX会将一个作业下同步的所有Text File视作同一张数据表。用户必须自己保证所有的File能够适配同一套schema信息。读取文件用户必须保证为类CSV格式，并且提供给DataX权限可读。
  
  特别需要注意的是，如果Path指定的路径下没有符合匹配的文件抽取，DataX将报错
  
## column

 描述：读取字段列表，type指定源数据的类型，index指定当前列来自于文本第几列(以0开始)，value指定当前类型为常量，不从源头文件读取数据，而是根据value值自动生成对应的列。
 
 用户可以指定Column字段信息，配置如下： 
  ```json
   {
     "type": "long" , "index": 0
   }
  ```
  从远程FTP文件文本第一列获取int字段
  ```json
  {
       "type": "long" , "value": "alibaba"
  }
  ``` 
  从FtpReader内部生成`alibaba`的字符串字段作为当前字段

  >> 对于用户指定Column信息，type必须填写，index/value必须选择其一
  
  例子:
  ```json5
  [
   { "index": 0,   "type": "long"  },
   { "index": 1,   "type": "boolean" },
   { "index": 2,   "type": "double" },
   { "index": 3,   "type": "string" },
   { "index": 4,   "type": "date",  "format": "yyyy.MM.dd" },
   { "type": "string", "value": "alibaba"  //从FtpReader内部生成alibaba的字符串字段作为当前字段 
   }
  ]
  ```


## nullFormat

 描述：文本文件中无法使用标准字符串定义null(空指针)，DataX提供nullFormat定义哪些字符串可以表示为null。
 例如如果用户配置: nullFormat:"\N"，那么如果源头数据是"\N"，DataX视作null字段。默认值：\N
 
## protocol

SFTP 和 FTP 非常相似，都支持批量传输（一次传输多个文件），文件夹 / 目录导航，文件移动，文件夹 / 目录创建，文件删除等。但还是存在着差异，SFTP 和 FTP 之间的区别：

* 链接方式不同

    FTP 使用 TCP 端口 21 上的控制连接建立连接。而 SFTP 是在客户端和服务器之间通过 SSH 协议 (TCP 端口 22) 建立的安全连接来传输文件。

* 安全性不同

    SFTP 使用加密传输认证信息和传输的数据，所以使用 SFTP 相对于 FTP 是非常安全。

* 效率不同

    SFTP 这种传输方式使用了加密解密技术，所以传输效率比普通的 FTP 要低得多。

* 使用的协议不同

    FTP 使用 TCP / IP 协议。而，SFTP 是 SSH 协议的一部分，它是一种远程登录信息。

* 安全通道

    FTP 不提供任何安全通道来在主机之间传输文件；而 SFTP 协议提供了一个安全通道，用于在网络上的主机之间传输文件。
  
