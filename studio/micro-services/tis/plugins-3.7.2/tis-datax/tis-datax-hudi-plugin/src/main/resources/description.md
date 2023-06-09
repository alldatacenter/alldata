封装[Apache Hudi](https://hudi.apache.org/)，为用户提供一站式、开箱即用的千表入湖的解决方案

功能：

本组件整合Hudi提供的[DeltaStreamer](https://hudi.apache.org/docs/hoodie_deltastreamer#deltastreamer)功能，通过TIS生成成DeltaStreamer需要的所有Hudi表摄入所需要的
所有配置信息（Avro schemas、从数据源抽取数据的DataX配置，Hudi表分区信息及 [Key Generation](https://hudi.apache.org/docs/key_generation)配置）

配合TIS提供的各种Source Connnector组件，快速实现各种数据源`批量`入湖





