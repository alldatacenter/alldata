启动教程：
1、安装elasticsearch6.5.1，windows启动：双击elasticsearch.bat
2、后端服务：
	启动mall-portal、mall-admin、mall-search；
3、前端服务：
	修改mall-admin-web目录下的config/dev.env.js：
	BASE_API: '"http://localhost:8080"'
	cd mall-admin-web目录
	执行npm run dev
4、账号密码：admin/123456
