1. 安装node,参考文档：https://nodejs.org/en/ 

2. 安装ws模块，ws：是nodejs的一个WebSocket库，可以用来创建服务。 https://github.com/websockets/ws

    ```bash
    npm install ws
    ```
3. 进入到/src/main/resources/websocket路径中；

4. 执行如下命令，启动WebSocketServer：
    ```bash
    node index.js
    ```
5. 使用浏览器打开webSocket.html文件，确认浏览器中显示已经连接字段；