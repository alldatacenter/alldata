import os
import base64
from time import sleep
from resource_management import *
from resource_management.core.logger import Logger
from resource_management.libraries.functions import check_process_status


class NginxMaster(Script):
    def install(self, env):
        import params
        env.set_params(params)
        # create nginx directories
        Directory([params.nginx_install_dir, params.nginx_log_dir, params.nginx_pid_dir],
                  mode=0755,
                  cd_access='a',
                  create_parents=True
                  )
        # download nginx-1.8.1.tar.gz
        Execute('wget {0} -O nginx-1.8.1.tar.gz'.format(params.nginx_download))
        # Install nginx
        Execute('tar -zxvf nginx-1.8.1.tar.gz -C {0}'.format(params.nginx_install_dir))
        # Remove nginx installation file
        Execute('rm -rf nginx-1.8.1.tar.gz')

    def configure(self, env):
        import params
        env.set_params(params)
        File(format("{nginx_install_dir}/conf/nginx.conf"), content=InlineTemplate(params.nginx_conf))
        Execute(format("chown -R root:root {nginx_log_dir} {nginx_pid_dir}"))

    def start(self, env):
        import params
        env.set_params(params)
        self.configure(env)
        cmd = format("cd {nginx_install_dir}; sbin/nginx")
        Execute(cmd)

    def stop(self, env):
        Execute("pkill -9 nginx")

    def restart(self, env):
        import params
        env.set_params(params)
        self.configure(env)
        cmd = format("cd {nginx_install_dir}; sbin/nginx -s reload")
        Execute(cmd)

    def status(self, env):
        check_process_status('/var/run/nginx/nginx.pid')


if __name__ == "__main__":
    NginxMaster().execute()
