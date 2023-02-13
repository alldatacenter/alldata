#!/usr/bin/env bash

set -x

rm -rf /Users/jinghua.yjh/git/tkg-one/tkg-one-start/src/main/resources/mybatis/ConfigMapper.xml
rm -rf /Users/jinghua.yjh/git/tkg-one/tkg-one-start/src/main/resources/mybatis/ConsumerMapper.xml
rm -rf /Users/jinghua.yjh/git/tkg-one/tkg-one-start/src/main/resources/mybatis/ConsumerHistoryMapper.xml
rm -rf /Users/jinghua.yjh/git/tkg-one/tkg-one-start/src/main/resources/mybatis/ConsumerNodeMapper.xml
rm -rf /Users/jinghua.yjh/git/tkg-one/tkg-one-start/src/main/resources/mybatis/ChatopsHistoryMapper.xml
rm -rf /Users/jinghua.yjh/git/tkg-one/tkg-one-start/src/main/resources/mybatis/PrestoMemoryViewSnapshotMapper.xml
rm -rf /Users/jinghua.yjh/git/tkg-one/tkg-one-start/src/main/resources/mybatis/PrestoNodeMapper.xml

cd tkg-one-start
mvn mybatis-generator:generate

rm -rf /Users/jinghua.yjh/git/tkg-one/tkg-one-start/src/main/resources/tmp
