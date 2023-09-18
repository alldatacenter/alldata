为Alibaba DataX OOS reader、writer插件提供基于UI的开箱即用的插件实现

* OSSReader提供了读取OSS数据存储的能力。在底层实现上，OSSReader使用OSS官方Java SDK获取OSS数据，并转换为DataX传输协议传递给Writer。[详细](https://github.com/alibaba/DataX/blob/master/ossreader/doc/ossreader.md)
* OSSWriter提供了向OSS写入类CSV格式的一个或者多个表文件。[详细](https://github.com/alibaba/DataX/blob/master/osswriter/doc/osswriter.md)