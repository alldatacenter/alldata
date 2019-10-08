# 启动教程：
# 1、数据库
导入mall.sql
# 2、启动es
安装elasticsearch6.5.1，windows启动：双击elasticsearch.bat
# 3、后端服务：
启动mall-portal、mall-admin、mall-search；
# 4、前端服务：
3.1 修改mall-admin-web目录下的config/dev.env.js： BASE_API: '"http://localhost:8080"'
3.2 cd mall-admin-web目录 执行 npm run dev
# 5、登录 
账号密码：admin/123456
