from multiprocessing import cpu_count

# workers = cpu_count() * 2 + 1
workers = 2

# TODO: add app load entry and set proc title
bind = ['0:8888']
timeout = 60 * 2  # Nginx 2 minutes timeout

# TODO: test eventlet
# worker_class = "eventlet"
# pidfile = '~/tesla-faas/server.pid'
# proc_name = 'tesla-faas-server'

# access_log_format = '%(t)s %(p)s %({x-traceid}i)s %(D)sus [%(r)s] %(s)s %(b)sB'


def when_ready(server):
    server.log.info("Server is ready. Spawning workers")
    # resume_inter_changings()


def pre_fork(server, worker):
    server.log.info("Worker before spawned (pid: %s)", worker.pid)


def post_fork(server, worker):
    server.log.info("Worker after spawned (pid: %s)", worker.pid)
    # web.ctx.tesla = tesla


def pre_exec(server):
    server.log.info("Forked child, re-executing.")


def worker_abort(worker):
    worker.log.error("Worker(%s) aborted.", worker.pid)
