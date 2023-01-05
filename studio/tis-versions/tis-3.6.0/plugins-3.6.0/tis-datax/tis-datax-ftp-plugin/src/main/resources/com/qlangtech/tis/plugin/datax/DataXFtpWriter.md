## writeMode

 FtpWriter写入前数据清理处理模式： 
 
 1. **truncate**:  写入前清理目录下一fileName前缀的所有文件。
 
 2. **append**: 写入前不做任何处理，DataX FtpWriter直接使用filename写入，并保证文件名不冲突。
  
 3. **nonConflict**: 如果目录下有fileName前缀的文件，直接报错。
 
## fileFormat

 文件写出的格式，包括[csv](http://zh.wikipedia.org/wiki/%E9%80%97%E5%8F%B7%E5%88%86%E9%9A%94%E5%80%BC) 和**text**两种，**csv**是严格的**csv**格式，如果待写数据包括列分隔符，则会按照**csv**的转义语法转义，转义符号为双引号。**text**格式是用列分隔符简单分割待写数据，对于待写数据包括列分隔符情况下不做转义。
 

    

