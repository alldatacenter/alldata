### Docker image for building TubeMQ C++ SDK Client
##### Pull Image
```
docker pull inlong/tubemq-cpp
```

#### Build TubeMQ C++ SDK Client
```
docker run -it --net=host -v REPLACE_BY_CPP_SOURCE_DIR_PATH:/tubemq-cpp/  inlong/tubemq-cpp /bin/bash
sh build_linux.sh
cd release/
sh release_linux.sh
```