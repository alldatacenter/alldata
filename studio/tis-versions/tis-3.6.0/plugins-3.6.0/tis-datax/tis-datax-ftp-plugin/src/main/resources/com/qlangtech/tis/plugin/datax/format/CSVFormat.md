## csvReaderConfig

 描述：读取CSV类型文件参数配置，Map类型。读取CSV类型文件使用的CsvReader进行读取，会有很多配置，不配置则使用默认值。
 
 ```json
{ "safetySwitch": false,  
  "skipEmptyRecords": false,       
  "useTextQualifier": false} 
 ```
 所有配置项及默认值,配置时 csvReaderConfig 的map中请严格按照以下字段名字进行配置：
 ```java
 boolean caseSensitive = true;
 char textQualifier = 34;
 boolean trimWhitespace = true;
 boolean useTextQualifier = true;//是否使用csv转义字符
 char delimiter = 44;//分隔符
 char recordDelimiter = 0;
 char comment = 35;
 boolean useComments = false;
 int escapeMode = 1;
 boolean safetySwitch = true;//单列长度是否限制100000字符
 boolean skipEmptyRecords = true;//是否跳过空行
 boolean captureRawRecord = true;
 ```
