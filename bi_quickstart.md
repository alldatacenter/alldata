#### （BI模块配置流程）

##### 根据官方部署文档，部署项目https://github.com/alldatacenter/alldata/blob/master/install.md

###### 部署可能会遇到的问题：

1. 在修改完配置文件后应该在alldata根目录进行打包，而不是单独模块打包，这样有些模块，无法打包成功

2. 切记认真查阅部署文档

   ##### 部署成功之后，点击BI模块点击修改操作进入修改界面，alldata-metadata-service模块会报错

  ![image](https://github.com/LMR-up/alldata/assets/80820139/d7957d60-3d5b-490b-aa48-413c696bb277)


   

   该模块爆的应该是数据库的错误，其实主要是因为该项目提供的数据库只有studio,缺少了foodmart2和robot数据库

   1：我们只需要新建这两个数据库并且把数据导入即可以下为数据库连接，https://gitee.com/zrxjava/srt-data/tree/master/db可以自己下载导入，我会把数据库文件放在项目根目录

   2：在数据库中新建完这两个数据库之后，主要查看studio数据库中metadata_source表中的db_schema字段注意该字段的数据，端口和用户名，密码是否和foodmart2库和robot库相匹配，并且注意数据库的访问权限

   ##### 数据库补充完毕，就可以愉快的使用bi功能了
   ![image](https://github.com/LMR-up/alldata/assets/80820139/a4033dd8-8b7f-4995-adfd-6863565c8388)

![image](https://github.com/LMR-up/alldata/assets/80820139/18b097c8-f957-43b6-bdfb-f902a6eede45)
