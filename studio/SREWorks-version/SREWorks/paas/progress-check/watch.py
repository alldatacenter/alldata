from kubernetes.client import api_client,V1Job,BatchV1Api
from kubernetes.watch import Watch
from kubernetes.client.api import core_v1_api
from kubernetes import client,config
import time
from collections import defaultdict


class KubernetesTools(object):
    def __init__(self):
        self.core = client.CoreV1Api()
        self.batch = BatchV1Api()



if __name__ == '__main__':
    config.load_kube_config()
    ktools = KubernetesTools()

    apps = set()
    completed_apps = set()

    watcher = Watch()
    
    # 观察init-job是否完成，并且诊断相关的问题，全部完全则进度90%
    # 诊断采用枚举的方式，按照错误码来进行分类
    
    for event in watcher.stream(
            ktools.batch.list_namespaced_job,
            namespace='sreworks'):
        job = event['object']
        if not job.metadata.name.endswith("-init-job"):
            continue
        app_name = job.metadata.name.replace('-init-job', '').replace('sreworks-saas-', '').replace('sreworks-', '')
        apps.add(app_name)
        if job.status.succeeded == 1:
            completed_apps.add(app_name)

        stdout = ''
        stdout += 'Progress: %s%% \n' % (len(completed_apps) / len(apps) * 90.0)
        stdout += 'Installing: %s \n' % ' | '.join(apps - completed_apps)
        stdout += 'Finished: %s \n' % ' | '.join(completed_apps)

        print(stdout)

        if apps == completed_apps:
            break

    # 全部init-job完成之后，检查所有出厂服务pod状态是否可用。



