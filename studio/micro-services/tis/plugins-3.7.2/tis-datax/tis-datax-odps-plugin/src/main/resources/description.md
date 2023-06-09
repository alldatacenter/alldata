* 介绍： ODPSWriter插件用于实现往ODPS插入或者更新数据，主要提供给etl开发同学将业务数据导入odps，适合于TB,GB数量级的数据传输，如果需要传输PB量级的数据，请选择dt task工具 ;
* 实现原理：在底层实现上，ODPSWriter是通过DT Tunnel写入ODPS系统的，有关ODPS的更多技术细节请参看 ODPS主站 https://data.aliyun.com/product/odps 和ODPS产品文档 https://help.aliyun.com/product/27797.html
