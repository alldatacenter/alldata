## hostAliases
 启动Pod时会在容器内的hosts文件中添加所输入的内容，例子：
 ``` yaml
  - ip: "127.0.0.1"
    hostnames:
      - "foo.local"
      - "bar.local"
 ```