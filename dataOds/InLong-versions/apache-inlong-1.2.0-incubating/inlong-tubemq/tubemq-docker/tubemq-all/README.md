#### tubemq-all docker image
TubeMQ standalone is available for development and experience.

##### Pull Image
```
docker pull inlong/tubemq-all:1.2.0-incubating
```

##### Start Standalone Container
```
docker run -p 8080:8080 -p 8715:8715 -p 8123:8123 -p 2181:2181 --name tubemq -d inlong/tubemq-all:1.2.0-incubating
```
this command will start zookeeper/master/broker service in one container.
#### Add Topic
If the container is running, you can access http://127.0.0.1:8080 to see the web GUI, and you can reference to the **Add Topic** part of [user guide](https://inlong.apache.org/docs/next/modules/tubemq/quick_start#2-quick-start)