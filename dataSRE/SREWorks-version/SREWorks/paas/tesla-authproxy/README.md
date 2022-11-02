# 系统介绍
- Tesla权限代理服务主要实现了不同的账户登录和权限控制逻辑切换，对上层业务屏蔽了内部和专有云不同环境下的账户登录
和权限控制的差异化，让上层业务能快速接入并满足各个环境的权限安全体系要求。
- Tesla权限代理服务内部对接BUC+ACL，专有云对接AAS+OAM。

# 启动方式
- 本机启动对内环境代码
```jshelllanguage
mvn clean install -Dmaven.test.skip=true -pl tesla-authproxy-start pandora-boot:run -Dmaven.test.skip=true -Dspring.profiles.active=private-local
```
- 本机启动专有云环境代码
```jshelllanguage
find . -type d -name target | xargs rm -rf && mvn -f pom_private.xml clean install -Dmaven.test.skip=true && mvn -f pom_private.xml -pl tesla-authproxy-start spring-boot:run -Dmaven.test.skip=true -Dspring.profiles.active=private-local
```
 
