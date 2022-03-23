# 并行工作进程数
workers = 1
# 指定每个工作者的线程数
threads = 1
# 监听内网端口
bind = "0.0.0.0:9000"
# 设置守护进程,将进程交给supervisor管理
daemon = True
# 工作模式协程
worker_class = "gevent"
# 设置进程文件目录
pidfile = "./logs/gunicorn.pid"
# 设置访问日志和错误信息日志路径
accesslog = "./logs/gunicorn-access.log"
errorlog = "./logs/gunicorn-error.log"
# 设置日志记录级别
loglevel = "error"
