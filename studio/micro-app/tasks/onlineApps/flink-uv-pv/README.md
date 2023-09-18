## 项目说明

## Kafka 配置

$KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server master:9092,node01:9092,node02:9092 --topic default

## ES 配置安装


## WebSocket 安装文档
1. 安装ws模块
    ```bash
    npm install ws
    ```
3. 进入resources/websocket路径中；

4. 执行如下命令，启动We bSocketServer：
    ```bash
    node index.js
    ```
5. 使用浏览器打开webSocket.html文件，确认浏览器中显示已经连接字段；