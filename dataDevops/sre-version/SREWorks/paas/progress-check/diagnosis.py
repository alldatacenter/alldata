from kubernetes import client, config
import time
import os
from oauthlib.oauth2 import LegacyApplicationClient
from requests_oauthlib import OAuth2Session
from datetime import datetime

CURRENT_PATH = os.path.dirname(os.path.abspath(__file__))
ENDPOINT = 'http://sreworks-appmanager'
# CLIENT_ID = os.getenv('APPMANAGER_CLIENT_ID')
CLIENT_ID = "superclient"
# CLIENT_SECRET = os.getenv('APPMANAGER_CLIENT_SECRET')
CLIENT_SECRET = "stLCjCPKbWmki65DsAj2jPoeBLPimpJa"
# USERNAME = os.getenv('APPMANAGER_ACCESS_ID')
USERNAME = "superuser"
# PASSWORD = os.getenv('APPMANAGER_ACCESS_SECRET')
PASSWORD = "yJfIYmjAiCL0ondV3kY7e5x6kVTpvC3h"

appSet = {'health','search','ocenter','aiops','app','upload','help','dataops','job','cluster','team','system','healing'}
jobSet = {'health','search','ocenter','aiops','app','upload','help','dataops','job','cluster','team','system','healing','core'}
core = {'template','swadmin','desktop'}
jobSum = 14

class AppManagerClient(object):

    def __init__(self, endpoint, client_id, client_secret, username, password):
        os.environ.setdefault('OAUTHLIB_INSECURE_TRANSPORT', '1')
        self._endpoint = endpoint
        self._client_id = client_id
        self._client_secret = client_secret
        self._username = username
        self._password = password
        self._token = self._fetch_token()

    @property
    def client(self):
        return OAuth2Session(self._client_id, token=self._token)

    def _fetch_token(self):
        """
        获取 appmanager access token
        """
        oauth = OAuth2Session(client=LegacyApplicationClient(client_id=CLIENT_ID))
        return oauth.fetch_token(
            token_url=os.path.join(ENDPOINT, 'oauth/token'),
            username=self._username,
            password=self._password,
            client_id=self._client_id,
            client_secret=self._client_secret
        )


class KubernetesTools(object):
    def __init__(self):
        self.core = client.CoreV1Api()
        self.storage = client.StorageV1Api()
        self.batch = client.BatchV1Api()

    def get_pvc(self, namespace):
        return self.core.list_namespaced_persistent_volume_claim(namespace).items

    def get_storage_class(self):
        return self.storage.list_storage_class().items

    def get_event(self, namespace, apiVersion=None, kind=None, metaname=None):
        events = []
        for item in self.core.list_namespaced_event(namespace).items:
            if apiVersion is not None and item.involved_object.apiVersion != apiVersion:
                continue
            if kind is not None and item.involved_object.kind != kind:
                continue
            if metaname is not None and item.involved_object.name != metaname:
                continue
            events.append(item)
        return events

    def get_init_time(self):
        cms = self.core.list_namespaced_config_map("sreworks")
        t = 0
        for cm in cms.items:
            if cm.metadata.name == "init-configmap":
                t = cm.metadata.creation_timestamp
                break
        t = t.strftime("%Y-%m-%d %H:%M:%S")
        t = datetime.strptime(t,"%Y-%m-%d %H:%M:%S")
        return t


class Diagnosis(object):
    def __init__(self, ktools: KubernetesTools):
        self.ktools = ktools
        self.pass_list = set()

    def execute(self):
        if "check_pvc" not in self.pass_list:
            message = self.check_pvc()
            if message is None:
                self.pass_list.add("check_pvc")
            else:
                return message

    def check_pvc(self):

        # 场景1. 校验pvc中storageclass是否存在
        pvcs = self.ktools.get_pvc("sreworks")
        storageclass_list = set()
        for pvc in pvcs:
            if pvc.spec.storage_class_name is not None:
                storageclass_list.add(pvc.spec.storage_class_name)

        check_storageclass_list = set()
        for sc in self.ktools.get_storage_class():
            if sc.metadata.name in storageclass_list:
                check_storageclass_list.add(sc.metadata.name)

        if len(storageclass_list - check_storageclass_list) > 0:
            return "%s not found in StorageClass, Please check helm install parameter --set global.storageClass=??  " % ','.join(
                storageclass_list - check_storageclass_list)

        # 场景2. 校验pvc功能是否可以正常使用
        # 查询pvc的状态和事件 -> 在状态不对的时候查事件
        error_pvc = {"name": None, "status": None}
        for pvc in pvcs:
            if pvc.status.phase != "Bound":
                error_pvc["name"] = pvc.metadata.name
                error_pvc["status"] = pvc.status.phase
                break

        if error_pvc['name'] is not None:
            events = self.ktools.get_event("sreworks", kind="PersistentVolumeClaim", metaname=error_pvc["name"])
            if len(events) > 0:
                return "PersistentVolumeClaim %s status:%s Event:%s" % (
                    error_pvc["name"], error_pvc['status'], events[0].message)
            else:
                return "PersistentVolumeClaim %s status:%s Event not found" % (error_pvc["name"], error_pvc['status'])

    def check_base(self):

        # 场景3. 检查底座软件运行是否正常 MySQL/MinIO

        return "123"

    # 场景3. 检查appmanager运行是否正常,初始化是否完成
    def check_appmanager(self):
        try:
            appmanagerClient = AppManagerClient(ENDPOINT, CLIENT_ID, CLIENT_SECRET, USERNAME, PASSWORD)
        except Exception:
            return None
        return appmanagerClient




def process_check(ktools: KubernetesTools, appmanagerClient: AppManagerClient, last_info) -> bool:
    completed_apps = set()
    completed_jobs = set()
    uncompleted_jobs = set()

    for job in ktools.batch.list_namespaced_job(namespace='sreworks').items:
        if not job.metadata.name.endswith("-init-job"):
            continue
        app_name = job.metadata.name.replace('-init-job', '').replace('sreworks-saas-', '').replace('sreworks-', '')
        if app_name == 'demoapp':
            continue
        if job.status.succeeded == 1:
            completed_apps.add(app_name)
        else:
            uncompleted_jobs.add(app_name)
    unknown_jobs = jobSet - completed_jobs - uncompleted_jobs
    if unknown_jobs:
        res = appmanagerClient.client.get(
            ENDPOINT + "/realtime/app-instances?stageId=pre,prod&optionKey=source&optionValue=swadmin&page=1&pageSize=100")
        appDatas = res.json()
        for v in appDatas['data']['items']:
            if v['status'] != 'PENDING' and v['status'] != 'ERROR':
                completed_apps.add(v['appId'])
        for j in unknown_jobs:
            if j in completed_apps:
                completed_jobs.add(j)
            else:
                flag = 1
                for c in core:
                    if c not in completed_apps:
                        flag = 0
                if flag:
                    completed_jobs.add(j)
                else:
                    uncompleted_jobs.add(j)

    message = last_info["diagnosis"].execute()

    stdout = ''
    stdout += 'Progress: %.2f%% \n' % (len(completed_jobs) / jobSum * 100.0)
    stdout += 'Installing: %s \n' % ' | '.join(uncompleted_jobs)
    stdout += 'Finished: %s \n' % ' | '.join(completed_jobs)
    if message is not None:
        stdout += 'Message: %s \n' % message

    if last_info["stdout"] != stdout:
        print("%sUseTime: %s\n" % (stdout, str(datetime.now() - last_info["start_time"]).split(".")[0]))
        last_info["stdout"] = stdout

    if jobSet == completed_jobs:
        return False

    return True


if __name__ == '__main__':
    # config.load_kube_config()  # 在服务器上使用该方法
    config.load_incluster_config()  # 在pod中用该方法
    ktools = KubernetesTools()
    diagnosis = Diagnosis(ktools)  # 诊断类
    appmanagerClient = diagnosis.check_appmanager()
    if not appmanagerClient:
        print('Waiting for appmanager initialization.')
    # 检测appmanager是否初始化结束
    while not appmanagerClient:
        time.sleep(20)
        appmanagerClient = diagnosis.check_appmanager()

    t=ktools.get_init_time()
    last_info = {"stdout": "", "start_time": t, "diagnosis": diagnosis}
    while process_check(ktools,appmanagerClient, last_info):
        time.sleep(10)
    print("SREWorks installation is complete!")
