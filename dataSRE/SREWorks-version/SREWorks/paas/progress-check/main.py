from kubernetes.client import api_client, V1Job, BatchV1Api
from kubernetes.client.api import core_v1_api
from kubernetes import client, config
import time
from collections import defaultdict


class KubernetesTools(object):
    def __init__(self):
        self.core = client.CoreV1Api()
        self.batch = BatchV1Api()

    # 当前job下的所有pod
    def get_pods_by(self, job_name: str, ns: str):
        pods = self.core.list_namespaced_pod(namespace=ns, label_selector=f'job-name={job_name}')
        pods_list = []
        for pod in pods.items:
            pods_list.append(pod.metadata.name)
        return pods_list

    # def process(self, percent: int, jobs_finished, pods_list: list, current_job: str):
    #     print(f'Progress: {percent}%')
    #     print(f'Installing: {current_job}:')
    #     for pod in pods_list:
    #         print('          '+pod)
    #     print(f'Finished: {jobs_finished}')

    # 当前job失败的原因
    def get_error_pods(self, job_name: str, ns: str):
        pods = self.core.list_namespaced_pod(namespace=ns, label_selector=f'job-name={job_name}')
        error_reason = set()
        for pod in pods.items:
            for condition in pod.status.conditions:
                if condition.status == 'False':
                    error_reason.add(condition.reason)

        return error_reason


def process_check(ktools: KubernetesTools) -> bool:
    apps = set()
    completed_apps = set()

    for job in ktools.batch.list_namespaced_job(namespace='sreworks').items:
        if not job.metadata.name.endswith("-init-job"):
            continue
        app_name = job.metadata.name.replace('-init-job', '').replace('sreworks-saas-', '').replace('sreworks-', '')
        apps.add(app_name)
        if job.status.succeeded == 1:
            completed_apps.add(app_name)

    stdout = ''
    stdout += 'Progress: %s%% \n' % (len(completed_apps) / len(apps) * 100.0)
    stdout += 'Installing: %s \n' % ' | '.join(apps - completed_apps)
    stdout += 'Finished: %s \n' % ' | '.join(completed_apps)

    print(stdout)

    if apps == completed_apps:
        return False

    return True


if __name__ == '__main__':
    config.load_kube_config()
    ktools = KubernetesTools()

    while process_check(ktools):
        time.sleep(10)

    #
    #
    #
    # # 轮询（10s轮询一次）
    # job_finished = set()            # 已经完成的job集合
    # job_failed_reason = defaultdict(list)       # 运行错误的job和错误的原因
    # job_unfinished = set()      #还没运行完成的job
    # flag = 0        # 记录状态变化
    # while True:
    #     ns_list = ktools.core.list_namespace()
    #     for ns in ns_list.items:
    #         if ns.metadata.name in namespaces_list:
    #             job_list = ktools.batch.list_namespaced_job(namespace=ns.metadata.name)
    #             for job in job_list.items:
    #                 jobname = job.metadata.name
    #                 # 已经运行完毕的job不进行处理
    #                 if jobname in job_finished:
    #                     continue
    #                 try:
    #                     for condition in job.status.conditions:
    #                         # 出现新的运行完成的job
    #                         if condition.type == 'Complete':
    #                             percent += 5         #这个百分比还要再重新定义
    #                             # 将新完成的job加到集合中
    #                             job_finished.add(jobname)
    #                             # 将完成的job从未完成的集合中删除
    #                             job_unfinished.discard(jobname)
    #                             job_failed_reason.pop(jobname,None)
    #                             flag = 1
    #                         # 运行失败的job
    #                         else:
    #                             reason = list(ktools.get_error_pods(jobname, ns.metadata.name))
    #                             if job_failed_reason[jobname] == reason:
    #                                 continue
    #                             job_failed_reason[jobname] = reason
    #                             job_unfinished.discard(jobname)
    #                             flag = 1
    #                 # 还未运行完成的job
    #                 except TypeError:
    #                     if jobname in job_unfinished:
    #                         continue
    #                     job_unfinished.add(jobname)
    #                     job_failed_reason.pop(jobname, None)
    #                     flag = 1
    #
    #     # 此次轮询中如果有状态变化的话就输出一次进度
    #     if flag:
    #         print(f'Progress: {percent}%')
    #         print(f'Finished: {job_finished}')
    #         print('Message:')
    #         print(f'{job_unfinished} installation uncompleted. ')
    #         for key,value in job_failed_reason.items():
    #             print(f'{key} installation failed:   {value}')
    #         flag = 0
    #
    #     time.sleep(10)
