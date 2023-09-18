## connStrategy

客户端连接Spark服务端可选择以下连接方式之一：

* [Amazon EC2](https://github.com/amplab/spark-ec2): scripts that let you launch a cluster on EC2 in about 5 minutes
* [Standalone Deploy Mode](https://spark.apache.org/docs/2.4.4/spark-standalone.html): launch a standalone cluster quickly without a third-party cluster manager
* [Mesos](https://spark.apache.org/docs/2.4.4/running-on-mesos.html): deploy a private cluster using Apache Mesos
* [YARN](https://spark.apache.org/docs/2.4.4/running-on-yarn.html): deploy Spark on top of Hadoop NextGen (YARN)
* [Kubernetes](https://spark.apache.org/docs/2.4.4/running-on-kubernetes.html#cluster-mode): deploy Spark on top of Kubernetes

例如，选择**Standalone Deploy Mode**模式模式，可设置：`spark://192.168.28.201:7077`
