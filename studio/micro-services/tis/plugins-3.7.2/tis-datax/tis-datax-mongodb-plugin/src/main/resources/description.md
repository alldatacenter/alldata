* MongoDB DataSource 插件封装了MongoDB作为数据源的插件，可以向TIS导入MongoDB的数据表进行后续处理
* MongoDBReader 封装alibaba DataX reader，插件利用 MongoDB 的java客户端MongoClient进行MongoDB的读操作。最新版本的Mongo已经将DB锁的粒度从DB级别降低到document级别，配合上MongoDB强大的索引功能，基本可以达到高性能的读取MongoDB的需求。[详细](https://github.com/alibaba/DataX/blob/master/mongodbreader/doc/mongodbreader.md)
* MongoDBWriter 封装alibaba DataX writer，插件利用 MongoDB 的java客户端MongoClient进行MongoDB的写操作。最新版本的Mongo已经将DB锁的粒度从DB级别降低到document级别，配合上MongoDB强大的索引功能，基本可以满足数据源向MongoDB写入数据的需求，针对数据更新的需求，通过配置业务主键的方式也可以实现[详细](https://github.com/alibaba/DataX/blob/master/mongodbwriter/doc/mongodbwriter.md)
