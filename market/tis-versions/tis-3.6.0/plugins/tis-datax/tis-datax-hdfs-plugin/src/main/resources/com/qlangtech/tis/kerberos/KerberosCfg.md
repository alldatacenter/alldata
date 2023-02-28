## principal

[详细说明](https://docs.cloudera.com/documentation/enterprise/5-3-x/topics/cm_sg_principal_keytab.html)

Kerberos 下的用户可以称为 Principal，缩写可以是 pric（见于一些配置文件中），由三个部分组成，分别是 primary, instance 和 realm。Kerberos principal 用于使用 Kerberos 做为安全加固的系统中，来代表一个用户唯一的身份。primary 又称为用户 user component，可以是任意的字符串或者就是操作系统下的用户名等等。

然后接着的部分叫做 instance，是用来给某个角色的用户或者服务来创建 principal 的。一个 instance，会被 "/" 和 primary 分隔。最后一个部分是 realm，概念上很像 DNS 上的 domain 域名，可以用来定义一组相似的对象，也可以说 realm 定义了一组 principals。每一个 realm 可以有私有的配置，包括 KDC 的地址和加密的算法，都可以独立存在。有些大型公司通常会创建一个独立的 realm 来分发管理员的权限。

Kerberos 给 principal 指定 ticket 票据，让他们可以访问用 Kerberos 做安全加固的 Hadoop 服务。principal 可以形如: username/fully.qualified.domain.name@YOUR_REALM.COM。username 是指原型 Hadoop 服务的 Unix 账户，例如 hdfs 或者 mapred 之类的。

而对于个人用户（指那些需要访问集群，比如 Hive Client 或者 HDFS Client 连接的这些），username 也是指 Unix 账号，例如 Tony, runzhliu 之类。只包含 primary 的 principal 也是可以接受的，例如 runzhliu@YOUR_REALM.COM。


## keytabPath

Keytab 就是一个包含了（若干）principals 和一个加密了的 principal key的文件。一个 Keytab 文件每个 host 都是唯一的，因为 principal 的定义了包含了 hostname 的。这个文件可以用来认证，而不需要传递公开的密码，因为只要有这个 Keytab 就可以代表这个 principal 来操作 Hadoop 的服务。所以说 Keytab 是需要保管好的。

