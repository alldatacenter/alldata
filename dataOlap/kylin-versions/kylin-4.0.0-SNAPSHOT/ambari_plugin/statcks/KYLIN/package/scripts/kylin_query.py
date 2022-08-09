import pwd
import grp
from resource_management import *


class KylinQuery(Script):
    def install(self, env):
        import params
        env.set_params(params)
        # create kylin directories
        Directory([params.kylin_install_dir, params.kylin_log_dir, params.kylin_pid_dir],
                  mode=0755,
                  cd_access='a',
                  create_parents=True
                  )
        # download kylin-4.0.0.tar.gz
        Execute('wget {0} -O kylin-4.0.0.tar.gz'.format(params.kylin_download))
        # Install kylin
        Execute('tar -zxvf kylin-4.0.0.tar.gz -C {0}'.format(params.kylin_install_dir))
        # Remove kylin installation file
        Execute('rm -rf kylin-4.0.0.tar.gz')
        # Ensure all files owned by kylin user:group
        cmd = format("chown -R hdfs:hdfs {kylin_install_dir}")
        Execute(cmd)
        # Initialize environment variables
        File(format("{tmp_dir}/kylin_env.rc"),
             content=Template("env.rc.j2"),
             owner='hdfs',
             group='hdfs',
             mode=0o700
             )

    def configure(self, env):
        import params
        env.set_params(params)
        kylin_properties = InlineTemplate(params.kylin_properties)
        File(format("{kylin_install_dir}/conf/kylin.properties"),
             owner='hdfs',
             group='hdfs',
             content=kylin_properties)
        Execute(format("chown -R hdfs:hdfs {kylin_log_dir} {kylin_pid_dir}"))
        cmd = format("sh {kylin_install_dir}/bin/check-env.sh")
        Execute(cmd, user="hdfs")
        Execute("hadoop fs -mkdir -p /kylin/kylin_metadata", user="hdfs")
        Execute("hadoop fs -chmod -R 777 /kylin/kylin_metadata", user="hdfs")

    def start(self, env):
        import params
        env.set_params(params)
        self.configure(env)
        cmd = format(
            ". {tmp_dir}/kylin_env.rc;{kylin_install_dir}/bin/kylin.sh start;cp -rf {kylin_install_dir}/pid {kylin_pid_file}")
        Execute(cmd, user='hdfs')

    def stop(self, env):
        import params
        env.set_params(params)
        cmd = format("{kylin_install_dir}/bin/kylin.sh stop")
        Execute(cmd, user='hdfs')
        File(params.kylin_pid_file,
             action="delete",
             owner='hdfs'
             )

    def restart(self, env):
        self.stop(env)
        self.start(env)

    def status(self, env):
        check_process_status('/var/run/kylin/kylin.pid')


if __name__ == "__main__":
    KylinQuery().execute()
