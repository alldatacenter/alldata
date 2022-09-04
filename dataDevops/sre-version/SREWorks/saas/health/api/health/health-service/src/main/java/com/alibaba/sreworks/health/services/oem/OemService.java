package com.alibaba.sreworks.health.services.oem;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.definition.DefinitionService;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.common.exception.CommonDefinitionExistException;
import com.alibaba.sreworks.health.common.exception.ParamException;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionCreateReq;
import com.alibaba.sreworks.health.operator.AppOperator;
import com.alibaba.sreworks.health.operator.DwOperator;
import com.alibaba.sreworks.health.operator.JobMasterOperator;
import com.alibaba.sreworks.health.operator.PmdbOperator;
import com.hubspot.jinjava.Jinjava;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置应用健康数据Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 15:55
 */
@Slf4j
@Service
public class OemService {
    private String APP_POD_EVENT_JOB_NAME_FORMAT = "oem_%s_pod_event_collect";
    private String APP_POD_EVENT_JOB_CONFIG_TEMPLATE = "{\"id\":95,\"gmtCreate\":1646306448766,\"gmtModified\":1646306448766,\"creator\":\"\",\"operator\":\"\",\"appId\":\"\",\"name\":\"oem_{{appName}}_pod_event_collect\",\"alias\":\"{{appName}}应用POD事件明细采集作业(内置)\",\"tags\":[\"{{appName}}\",\"pod\",\"event\"],\"description\":null,\"options\":null,\"triggerType\":\"cron\",\"triggerConf\":{\"cronExpression\":\"20 0/5 * * * ? *\",\"enabled\":true},\"scheduleType\":\"serial\",\"scheduleConf\":{\"taskIdList\":[{\"id\":109,\"gmtCreate\":1646306448757,\"gmtModified\":1646306448757,\"creator\":\"\",\"operator\":\"\",\"appId\":\"\",\"name\":\"oem_{{appName}}_pod_event\",\"alias\":\"{{appName}}应用POD事件明细(内置)\",\"execTimeout\":300,\"execType\":\"python\",\"execContent\":\"# coding: utf-8\\n\\nimport datetime\\nimport json\\nimport time\\nimport requests\\n\\nheaders = {}\\n\\npage_size = 1000\\none_millisecond = 1000\\nfive_minutes_millisecond = 300000\\none_hour_minutes_millisecond = 3600000\\n\\nhost = {\\n    \\\"dataset\\\": \\\"http://prod-dataops-dataset.sreworks-dataops.svc.cluster.local:80\\\",\\n    \\\"health\\\": \\\"http://prod-health-health.sreworks.svc.cluster.local:80\\\",\\n    \\\"app\\\": \\\"http://prod-app-app.sreworks.svc.cluster.local:80\\\"\\n}\\n\\ndemo_app_id = \\\"{{appId}}\\\"\\ndemo_app_name = \\\"{{appName}}\\\"\\nevent_definition_name = \\\"{{podEventDefinitionName}}\\\"\\n\\n\\ndef convert_utc_to_local_dt(gmt_ts):\\n    print(datetime.datetime.fromtimestamp(gmt_ts, tzinfo=datetime.timezone.utc))\\n\\n\\ndef get_time_range(ts, delta_ts, forward_gap=0):\\n    \\\"\\\"\\\"\\n    时间范围\\n    :param ts:\\n    :param delta_ts:\\n    :param forward_gap: 默认前推一个delta\\n    :return:\\n    \\\"\\\"\\\"\\n    ts_integer = int(ts)\\n    delta_ts_integer = int(delta_ts)\\n\\n    end_ts = ts_integer - ts_integer % delta_ts_integer\\n    start_ts = end_ts - delta_ts_integer\\n\\n    delta_forward_ts_integer = delta_ts_integer * forward_gap\\n\\n    return start_ts - delta_forward_ts_integer, end_ts - delta_forward_ts_integer\\n\\n\\ndef get_pod_events():\\n    endpoint = host[\\\"dataset\\\"] + \\\"/interface/pod_event\\\"\\n    start_timestamp, end_timestamp = get_time_range(time.time() * one_millisecond, five_minutes_millisecond)\\n    basic_url = f'''{endpoint}?sTimestamp={start_timestamp}&eTimestamp={end_timestamp}&pageSize={page_size}'''\\n\\n    page_num = 1\\n    datas = []\\n    while True:\\n        url = basic_url + \\\"&pageNum=\\\" + str(page_num)\\n        r = requests.get(url, headers=headers)\\n        if r.status_code != 200:\\n            break\\n\\n        ret = r.json().get(\\\"data\\\", None)\\n        if ret and ret.get(\\\"datas\\\"):\\n            datas.extend(ret.get(\\\"datas\\\"))\\n            _total_num = int(ret.get(\\\"totalNum\\\"))\\n            _page_size = int(ret.get(\\\"pageSize\\\"))\\n            _page_num = int(ret.get(\\\"pageNum\\\"))\\n            if _page_size > _total_num:\\n                break\\n            else:\\n                page_num = _page_num + 1\\n        else:\\n            break\\n\\n    return datas\\n\\n\\ndef get_app_pods(app_id, app_name):\\n    endpoint = host[\\\"dataset\\\"] + \\\"/interface/app_pod\\\"\\n    end_timestamp = int(time.time() * one_millisecond)\\n    start_timestamp = end_timestamp - one_hour_minutes_millisecond\\n    basic_url = f'''{endpoint}?sTimestamp={start_timestamp}&eTimestamp={end_timestamp}&appId={app_id}&pageSize={page_size}'''\\n\\n    page_num = 1\\n    datas = []\\n    while True:\\n        url = basic_url + \\\"&pageNum=\\\" + str(page_num)\\n        r = requests.get(url, headers=headers)\\n        if r.status_code != 200:\\n            break\\n\\n        ret = r.json().get(\\\"data\\\", None)\\n        if ret and ret.get(\\\"datas\\\"):\\n            datas.extend(ret.get(\\\"datas\\\"))\\n            _total_num = int(ret.get(\\\"totalNum\\\"))\\n            _page_size = int(ret.get(\\\"pageSize\\\"))\\n            _page_num = int(ret.get(\\\"pageNum\\\"))\\n            if _page_size > _total_num:\\n                break\\n            else:\\n                page_num = _page_num + 1\\n        else:\\n            break\\n\\n    app_pods = {}\\n    for data in datas:\\n        key = data[\\\"namespace\\\"] + \\\"#\\\" + data[\\\"podName\\\"]\\n        # data['podName'] = data['podName']\\n        data['appId'] = app_id\\n        data['appName'] = app_name\\n        data['appComponentName'] = data['appComponentName']\\n        data['appInstanceId'] = data['appInstanceId']\\n        data['appComponentInstanceId'] = data['appInstanceId']\\n        app_pods[key] = data\\n    return app_pods\\n\\n\\ndef get_app_event_definition(app_id, definition_name):\\n    endpoint = host[\\\"health\\\"] + \\\"/definition/getDefinitions?category=event&appId=\\\" + app_id + \\\"&name=\\\" + definition_name\\n    r = requests.get(endpoint, headers=headers)\\n    app_event_definitions = r.json().get(\\\"data\\\", None)\\n    # 仅取一个应用定义\\n    if app_event_definitions:\\n        return app_event_definitions[0]\\n    else:\\n        return None\\n\\n\\ndef push_event_instance(def_id, event_instances):\\n    endpoint = host[\\\"health\\\"] + \\\"/event_instance/pushEvents\\\"\\n    url = f'''{endpoint}?defId={def_id}'''\\n\\n    r = requests.post(url, headers=headers, json=event_instances)\\n    if r.status_code != 200:\\n        print(r.json())\\n\\n\\ndef sync(app_id, app_name, definition_name):\\n    app_event_definition = get_app_event_definition(app_id, definition_name)\\n    if app_event_definition:\\n        app_pods = get_app_pods(app_id, app_name)\\n        events = get_pod_events()\\n\\n        app_component_event_instances = {}\\n        if app_pods and events:\\n            for event in events:\\n                key = event[\\\"namespace\\\"] + \\\"#\\\" + event[\\\"podName\\\"]\\n                pod = app_pods.get(key, None)\\n                if pod is not None:\\n                    event[\\\"occurTs\\\"] = event[\\\"gmtOccur\\\"]\\n                    event[\\\"timestamp\\\"] = int(time.time() * one_millisecond)\\n                    event['appId'] = pod['appId']\\n                    event['appName'] = pod['appName']\\n                    event['appComponentName'] = pod['appComponentName']\\n                    event['appInstanceId'] = pod['appInstanceId']\\n                    event['appComponentInstanceId'] = pod['appInstanceId']\\n                    event['content'] = '[' + event['type'] + ']' + event['message']\\n                    event['type'] = \\\"POD事件\\\"\\n                    event['source'] = \\\"POD事件采集\\\"\\n\\n                    key = pod['appId'] + \\\"#\\\" + pod['appComponentName']\\n                    if key in app_component_event_instances:\\n                        app_component_event_instances[key].append(event)\\n                    else:\\n                        app_component_event_instances[key] = [event]\\n\\n        results = []\\n        for app_component, event_instances in app_component_event_instances.items():\\n            push_event_instance(app_event_definition['id'], event_instances)\\n            results.extend(event_instances)\\n        return results\\n\\n\\nprint(json.dumps(sync(demo_app_id, demo_app_name, event_definition_name)))\\n\",\"execRetryTimes\":0,\"execRetryInterval\":0,\"varConf\":{},\"sceneType\":\"collect\",\"sceneConf\":{\"isPushQueue\":\"false\",\"syncDw\":\"true\",\"id\":{{eventInstanceModelId}},\"type\":\"model\",\"layer\":\"{{eventInstanceModelLayer}}\"}}]},\"sceneType\":\"collect\",\"sceneConf\":{},\"varConf\":{},\"notifyConf\":null,\"eventConf\":[]}";

    private String APP_RISK_JOB_NAME_FORMAT = "oem_%s_pod_status_inspection";
    private String APP_RISK_JOB_CONFIG_TEMPLATE = "{\"id\":100,\"gmtCreate\":1646658291473,\"gmtModified\":1646658301348,\"creator\":\"999999999\",\"operator\":\"999999999\",\"appId\":\"\",\"name\":\"oem_{{appName}}_pod_status_inspection\",\"alias\":\"{{appName}}应用PDO状态巡检作业(内置)\",\"tags\":[\"pod_status\",\"{{appName}}\"],\"description\":\"{{appName}}应用PDO状态巡检作业\",\"options\":null,\"triggerType\":\"cron\",\"triggerConf\":{\"cronExpression\":\"2 * * * * ? *\",\"enabled\":true},\"scheduleType\":\"serial\",\"scheduleConf\":{\"taskIdList\":[{\"id\":156,\"gmtCreate\":1646658041535,\"gmtModified\":1646659825821,\"creator\":\"999999999\",\"operator\":\"999999999\",\"appId\":\"\",\"name\":\"oem_{{appName}}_pod_status\",\"alias\":\"{{appName}}应用POD状态(内置)\",\"execTimeout\":300,\"execType\":\"python\",\"execContent\":\"# coding: utf-8\\nimport json\\nimport time\\nimport requests\\n\\nheaders = {}\\nhost = {\\n    \\\"dataset\\\": \\\"http://prod-dataops-dataset.sreworks-dataops.svc.cluster.local:80\\\"\\n}\\n\\n\\ndemo_app_id = \\\"{{appId}}\\\"\\n\\npage_size = 1000\\none_second_millisecond = 1000\\none_minute_millisecond = 60000\\n\\n\\ndef _do_get_app_pod_status(endpoint, start_timestamp, end_timestamp, app_id):\\n    basic_url = f'''{endpoint}?sTimestamp={start_timestamp}&eTimestamp={end_timestamp}&appId={app_id}&pageSize={page_size}'''\\n    page_num = 1\\n    datas = []\\n    while True:\\n        url = basic_url + \\\"&pageNum=\\\" + str(page_num)\\n        r = requests.get(url, headers=headers)\\n        if r.status_code != 200:\\n            break\\n\\n        ret = r.json().get(\\\"data\\\", None)\\n        if ret and ret.get(\\\"datas\\\"):\\n            datas.extend(ret.get(\\\"datas\\\"))\\n            _total_num = int(ret.get(\\\"totalNum\\\"))\\n            _page_size = int(ret.get(\\\"pageSize\\\"))\\n            _page_num = int(ret.get(\\\"pageNum\\\"))\\n            if _page_size > _total_num:\\n                break\\n            else:\\n                page_num = _page_num + 1\\n        else:\\n            break\\n\\n    return datas\\n\\n\\ndef risk(app_id):\\n    now_millisecond = int(time.time()) * one_second_millisecond\\n    current_minute_timestamp = now_millisecond - now_millisecond % one_minute_millisecond\\n    end_timestamp = current_minute_timestamp - one_minute_millisecond\\n    start_timestamp = end_timestamp - one_minute_millisecond\\n\\n    endpoint_pod_status = host[\\\"dataset\\\"] + \\\"/interface/app_pod_status\\\"\\n    app_pod_status_list = _do_get_app_pod_status(endpoint_pod_status, start_timestamp, end_timestamp, app_id)\\n\\n    risk_instances = []\\n    app_instance_ids = set([])\\n    for app_pod_status in app_pod_status_list:\\n        app_instance_id = app_pod_status[\\\"appInstanceId\\\"]\\n        ready_str = app_pod_status[\\\"podReady\\\"].lower()\\n        if ready_str != \\\"true\\\":\\n            if app_instance_id in app_instance_ids:\\n                continue\\n            else:\\n                risk_instance = {\\n                    \\\"appInstanceId\\\": app_instance_id,\\n                    \\\"appComponentInstanceId\\\": app_pod_status[\\\"appComponentInstanceId\\\"],\\n                    \\\"gmt_occur\\\": start_timestamp,\\n                    \\\"source\\\": \\\"POD状态作业巡检\\\",\\n                    \\\"content\\\": app_pod_status[\\\"podName\\\"] + \\\" POD状态未就绪\\\"\\n                }\\n                risk_instances.append(risk_instance)\\n                app_instance_ids.add(app_instance_id)\\n\\n    return risk_instances\\n\\n\\nprint(json.dumps(risk(demo_app_id)))\\n\",\"execRetryTimes\":0,\"execRetryInterval\":0,\"varConf\":{},\"sceneType\":\"risk\",\"sceneConf\":{\"modelId\":{{riskDefinitionId}}}}]},\"sceneType\":\"risk\",\"sceneConf\":{},\"varConf\":{},\"notifyConf\":null,\"eventConf\":[]}";

    private String APP_INCIDENT_JOB_NAME_FORMAT = "oem_%s_pod_status_diagnosis";
    private String APP_INCIDENT_JOB_CONFIG_TEMPLATE = "{\"id\":115,\"gmtCreate\":1646719784182,\"gmtModified\":1646719888577,\"creator\":\"999999999\",\"operator\":\"999999999\",\"appId\":\"\",\"name\":\"oem_{{appName}}_pod_status_diagnosis\",\"alias\":\"{{appName}}应用POD状态异常诊断作业(内置)\",\"tags\":[\"pod_status\",\"diagnosis\",\"{{appName}}\"],\"description\":\"{{appName}}应用POD状态异常诊断作业\",\"options\":null,\"triggerType\":null,\"triggerConf\":null,\"scheduleType\":\"serial\",\"scheduleConf\":{\"taskIdList\":[{\"id\":217,\"gmtCreate\":1646719878089,\"gmtModified\":1646721491187,\"creator\":\"999999999\",\"operator\":\"999999999\",\"appId\":\"\",\"name\":\"oem_{{appName}}_pod_status_unready\",\"alias\":\"{{appName}}应用POD状态异常(内置)\",\"execTimeout\":600,\"execType\":\"python\",\"execContent\":\"# coding: utf-8\\n\\nimport os\\nimport json\\nimport time\\nimport random\\nimport requests\\n\\nheaders = {}\\nhost = {\\n    \\\"dataset\\\": \\\"http://prod-dataops-dataset.sreworks-dataops.svc.cluster.local:80\\\"\\n}\\n\\ndemo_app_id = \\\"{{appId}}\\\"\\n\\npage_size = 1000\\none_second_millisecond = 1000\\none_minute_millisecond = 60000\\n\\n\\n# 五种状态\\n# WAITING 等待自愈状态\\n# RUNNING 自愈中\\n# FAILURE 自愈失败\\n# SUCCESS 自愈成功\\n# CANCEL 自愈任务取消\\n\\n\\ndef _do_get_app_pod_status(endpoint, start_timestamp, end_timestamp, app_id, app_instance_id):\\n    basic_url = f'''{endpoint}?sTimestamp={start_timestamp}&eTimestamp={end_timestamp}&appId={app_id}&appInstanceId={app_instance_id}&pageSize={page_size}'''\\n    page_num = 1\\n    datas = []\\n    while True:\\n        url = basic_url + \\\"&pageNum=\\\" + str(page_num)\\n        r = requests.get(url, headers=headers)\\n        if r.status_code != 200:\\n            break\\n\\n        ret = r.json().get(\\\"data\\\", None)\\n        if ret and ret.get(\\\"datas\\\"):\\n            datas.extend(ret.get(\\\"datas\\\"))\\n            _total_num = int(ret.get(\\\"totalNum\\\"))\\n            _page_size = int(ret.get(\\\"pageSize\\\"))\\n            _page_num = int(ret.get(\\\"pageNum\\\"))\\n            if _page_size > _total_num:\\n                break\\n            else:\\n                page_num = _page_num + 1\\n        else:\\n            break\\n\\n    return datas\\n\\n\\ndef build_self_healing_instance(app_id):\\n    incident_instance = json.loads(open(os.getenv(\\\"varConfPath\\\")).read())\\n\\n    # 用户按需实现自愈逻辑\\n    random_int = random.randint(150, 300)\\n    time.sleep(random_int)\\n\\n    now_millisecond = int(time.time()) * one_second_millisecond\\n    current_minute_timestamp = now_millisecond - now_millisecond % one_minute_millisecond\\n    end_timestamp = current_minute_timestamp - one_minute_millisecond\\n    start_timestamp = end_timestamp - one_minute_millisecond\\n\\n    endpoint_pod_status = host[\\\"dataset\\\"] + \\\"/interface/app_pod_status\\\"\\n    app_instance_id = incident_instance[\\\"appInstanceId\\\"]\\n    app_pod_status_list = _do_get_app_pod_status(endpoint_pod_status, start_timestamp, end_timestamp, app_id, app_instance_id)\\n\\n    unready_pods = []\\n    for app_pod in app_pod_status_list:\\n        ready_str = app_pod[\\\"podReady\\\"].lower()\\n        if ready_str != \\\"true\\\":\\n            unready_pods.append(app_pod)\\n\\n    if len(unready_pods) < 1:\\n        incident_instance[\\\"selfHealingStatus\\\"] = \\\"SUCCESS\\\"\\n    else:\\n        incident_instance[\\\"selfHealingStatus\\\"] = \\\"FAILURE\\\"\\n        unready_pod_cnt = len(unready_pods)\\n        unready_pods_str = \\\"\\\"\\n        for unready_pod in unready_pods:\\n            unready_pods_str += \\\",\\\" + unready_pod[\\\"podName\\\"]\\n        incident_instance[\\\"cause\\\"] = f'''应用POD自愈失败,异常POD实例数[{unready_pod_cnt}],列表[{unready_pods_str}]'''\\n    return incident_instance\\n\\n\\ndef incident(app_id):\\n    current_instance = build_self_healing_instance(app_id)\\n    result = {\\n        \\\"currentIncident\\\": current_instance\\n    }\\n    open(os.getenv(\\\"varConfPath\\\"), 'w').write(json.dumps(result))\\n    print(json.dumps(result))\\n\\n\\nincident(demo_app_id)\\n\",\"execRetryTimes\":0,\"execRetryInterval\":0,\"varConf\":{},\"sceneType\":\"incident\",\"sceneConf\":{}}]},\"sceneType\":\"incident\",\"sceneConf\":{},\"varConf\":{},\"notifyConf\":null,\"eventConf\":[{\"source\":\"INCIDENT\",\"type\":\"KAFKA\",\"config\":{\"server\":\"sreworks-kafka.sreworks:9092\",\"topics\":[\"sreworks-health-incident-{{incidentDefinitionId}}\"]}}]}";

    private String ALERT_METRIC_NAME = "app_unready_pod_cnt";
    private String APP_ALERT_JOB_NAME_FORMAT = "oem_%s_unready_pod_cnt_alert_analysis";
    private String APP_ALERT_JOB_CONFIG_TEMPLATE = "{\"id\":118,\"gmtCreate\":1646726422024,\"gmtModified\":1646726604546,\"creator\":\"999999999\",\"operator\":\"999999999\",\"appId\":\"\",\"name\":\"oem_{{appName}}_unready_pod_cnt_alert_analysis\",\"alias\":\"{{appName}}应用unready状态PDO数量告警分析作业(内置)\",\"tags\":[\"{{appName}}\",\"unready_pod_cnt\"],\"description\":null,\"options\":null,\"triggerType\":null,\"triggerConf\":null,\"scheduleType\":\"serial\",\"scheduleConf\":{\"taskIdList\":[{\"id\":222,\"gmtCreate\":1646726593174,\"gmtModified\":1646727049225,\"creator\":\"999999999\",\"operator\":\"999999999\",\"appId\":\"\",\"name\":\"oem_{{appName}}_unready_pod_cnt_alert\",\"alias\":\"{{appName}}应用unready状态POD数量告警(内置)\",\"execTimeout\":300,\"execType\":\"python\",\"execContent\":\"# coding: utf-8\\n\\nimport os\\nimport json\\n\\n\\ndef build_new_instance():\\n    incident_instance = json.loads(open(os.getenv(\\\"varConfPath\\\")).read())\\n    if incident_instance[\\\"level\\\"].upper() == \\\"CRITICAL\\\":\\n        new_incident_instance = {\\n            \\\"appInstanceId\\\": incident_instance.get(\\\"appInstanceId\\\"),\\n            \\\"appComponentInstanceId\\\": incident_instance.get(\\\"appComponentInstanceId\\\"),\\n            \\\"cause\\\": \\\"异常状态POD数量超过[CRITICAL]告警等级容忍阈值,详情:\\\" + incident_instance.get(\\\"content\\\", \\\"\\\")\\n        }\\n        return new_incident_instance\\n\\n\\ndef incident():\\n    new_incident_instance = build_new_instance()\\n    if new_incident_instance:\\n        # 自愈依赖新的异常实例\\n        result = {\\n            \\\"nextIncident\\\": new_incident_instance\\n        }\\n        open(os.getenv(\\\"varConfPath\\\"), 'w').write(json.dumps(result))\\n        return result\\n    else:\\n        return {}\\n\\n\\nprint(json.dumps(incident()))\\n\",\"execRetryTimes\":0,\"execRetryInterval\":0,\"varConf\":{},\"sceneType\":\"alert\",\"sceneConf\":{\"modelId\":{{incidentDefinitionId}}}}]},\"sceneType\":\"alert\",\"sceneConf\":{},\"varConf\":{},\"notifyConf\":null,\"eventConf\":[{\"source\":\"ALERT\",\"type\":\"KAFKA\",\"config\":{\"server\":\"sreworks-kafka.sreworks:9092\",\"topics\":[\"sreworks-health-alert-{{alertDefinitionId}}\"]}}]}";

    @Autowired
    DefinitionService definitionService;

    @Autowired
    JobMasterOperator jobMasterOperator;

    @Autowired
    DwOperator dwOperator;

    @Autowired
    PmdbOperator pmdbOperator;

    @Autowired
    AppOperator appOperator;

    public void open(String appId) throws Exception {
        JSONObject app = getAppById(appId);
        String appName = app.getString("name");

        JSONArray metrics = pmdbOperator.getMetrics(ALERT_METRIC_NAME, appId, appName);
        if (CollectionUtils.isEmpty(metrics)) {
            log.warn(String.format("指标[%s]不存在,告警定义关联指标失败,请先开启内置指标", ALERT_METRIC_NAME));
            throw new ParamException(String.format("指标[%s]不存在,告警定义关联指标失败,请先开启内置指标", ALERT_METRIC_NAME));
        }

        DefinitionCreateReq eventDefinitionCreateReq = loadDefinitionConfig(Constant.EVENT, appId, appName);
        if (eventDefinitionCreateReq != null) {
            openOemPodEvent(eventDefinitionCreateReq);
        }

        DefinitionCreateReq riskDefinitionCreateReq = loadDefinitionConfig(Constant.RISK, appId, appName);
        if (riskDefinitionCreateReq != null) {
            openOemRisk(riskDefinitionCreateReq);
        }

        DefinitionCreateReq incidentDefinitionCreateReq = loadDefinitionConfig(Constant.INCIDENT, appId, appName);
        Integer incidentDefinitionId = null;
        if (incidentDefinitionCreateReq != null) {
            incidentDefinitionId = openOemIncident(incidentDefinitionCreateReq);
        }

        DefinitionCreateReq failureDefinitionCreateReq = loadDefinitionConfig(Constant.FAILURE, appId, appName);
        if (failureDefinitionCreateReq != null && incidentDefinitionId != null) {
            failureDefinitionCreateReq.getExConfig().setRefIncidentDefId(incidentDefinitionId);
            openOemFailure(failureDefinitionCreateReq);
        }

        DefinitionCreateReq alertDefinitionCreateReq = loadDefinitionConfig(Constant.ALERT, appId, appName);
        if (alertDefinitionCreateReq != null && incidentDefinitionId != null) {
            alertDefinitionCreateReq.getExConfig().setMetricId(metrics.getObject(0, JSONObject.class).getInteger("id"));
            openOemAlert(alertDefinitionCreateReq, incidentDefinitionId);
        }
    }

    public boolean state(String appId) throws Exception {
        JSONObject app = getAppById(appId);
        String appName = app.getString("name");

        boolean eventState = stateCronJob(String.format(APP_POD_EVENT_JOB_NAME_FORMAT, appName));
        boolean riskState = stateCronJob(String.format(APP_RISK_JOB_NAME_FORMAT, appName));

        return eventState && riskState;
    }

    public void close(String appId) throws Exception {
        JSONObject app = getAppById(appId);
        String appName = app.getString("name");

        closeCronJob(String.format(APP_POD_EVENT_JOB_NAME_FORMAT, appName));
        closeCronJob(String.format(APP_RISK_JOB_NAME_FORMAT, appName));
    }

    private void openOemPodEvent(DefinitionCreateReq eventDefinitionCreateReq) throws Exception {
        try {
            definitionService.addDefinition(eventDefinitionCreateReq);
        } catch (CommonDefinitionExistException ex) {
            log.warn(String.format("定义已经存在:%s", ex.getMessage()));
        }

        String jobName = String.format(APP_POD_EVENT_JOB_NAME_FORMAT, eventDefinitionCreateReq.getAppName());
        JSONObject job = jobMasterOperator.getJobByName(jobName);
        // 创建新作业
        if (CollectionUtils.isEmpty(job)) {
            JSONObject dwModel = dwOperator.getDWModel("EVENT_INSTANCE");
            if (CollectionUtils.isEmpty(dwModel)) {
                throw new ParamException("事件实例[EVENT_INSTANCE]模型不存在,请先创建数仓模型");
            }

            Jinjava jinjava = new Jinjava();
            Map<String, Object> params = new HashMap<>();
            params.put("appId", eventDefinitionCreateReq.getAppId());
            params.put("appName", eventDefinitionCreateReq.getAppName());
            params.put("podEventDefinitionName", eventDefinitionCreateReq.getName());
            params.put("eventInstanceModelId", dwModel.getInteger("id"));
            params.put("eventInstanceModelLayer", dwModel.getString("layer"));
            String appPodEventJobConfig = jinjava.render(APP_POD_EVENT_JOB_CONFIG_TEMPLATE, params);
            JSONObject jobConfig = JSONObject.parseObject(appPodEventJobConfig);

            jobMasterOperator.createJob(Collections.singletonList(jobConfig));
            job = jobMasterOperator.getJobByName(jobName);
        }

        // 启动作业
        jobMasterOperator.toggleCronJob(job.getLong("id"), true);
    }

    private void openOemRisk(DefinitionCreateReq riskDefinitionCreateReq) throws Exception {
        Integer riskDefinitionId = null;
        try {
            riskDefinitionId = definitionService.addDefinition(riskDefinitionCreateReq);
        } catch (CommonDefinitionExistException ex) {
            log.warn(String.format("定义已经存在:%s", ex.getMessage()));
        }

        String jobName = String.format(APP_RISK_JOB_NAME_FORMAT, riskDefinitionCreateReq.getAppName());
        JSONObject job = jobMasterOperator.getJobByName(jobName);
        // 创建新作业
        if (CollectionUtils.isEmpty(job)) {
            if (riskDefinitionId == null) {
                List<JSONObject> existDefinitions = definitionService.getDefinitions(riskDefinitionCreateReq.getAppId(),
                        null, riskDefinitionCreateReq.getName(), Constant.RISK);
                riskDefinitionId = existDefinitions.get(0).getInteger("id");
            }

            Jinjava jinjava = new Jinjava();
            Map<String, Object> params = new HashMap<>();
            params.put("appId", riskDefinitionCreateReq.getAppId());
            params.put("appName", riskDefinitionCreateReq.getAppName());
            params.put("riskDefinitionId", riskDefinitionId);
            String appPodEventJobConfig = jinjava.render(APP_RISK_JOB_CONFIG_TEMPLATE, params);
            JSONObject jobConfig = JSONObject.parseObject(appPodEventJobConfig);

            jobMasterOperator.createJob(Collections.singletonList(jobConfig));
            job = jobMasterOperator.getJobByName(jobName);
        }

        // 启动作业
        jobMasterOperator.toggleCronJob(job.getLong("id"), true);
    }

    private Integer openOemIncident(DefinitionCreateReq incidentDefinitionCreateReq) throws Exception {
        Integer incidentDefinitionId;
        try {
            incidentDefinitionId = definitionService.addDefinition(incidentDefinitionCreateReq);
        } catch (CommonDefinitionExistException ex) {
            log.warn(String.format("定义已经存在:%s", ex.getMessage()));
            List<JSONObject> existDefinitions = definitionService.getDefinitions(incidentDefinitionCreateReq.getAppId(),
                    null, incidentDefinitionCreateReq.getName(), Constant.INCIDENT);
            incidentDefinitionId = existDefinitions.get(0).getInteger("id");
        }

        String jobName = String.format(APP_INCIDENT_JOB_NAME_FORMAT, incidentDefinitionCreateReq.getAppName());
        JSONObject job = jobMasterOperator.getJobByName(jobName);
        // 创建新作业
        if (CollectionUtils.isEmpty(job)) {
            Jinjava jinjava = new Jinjava();
            Map<String, Object> params = new HashMap<>();
            params.put("appId", incidentDefinitionCreateReq.getAppId());
            params.put("appName", incidentDefinitionCreateReq.getAppName());
            params.put("incidentDefinitionId", incidentDefinitionId);
            String appPodEventJobConfig = jinjava.render(APP_INCIDENT_JOB_CONFIG_TEMPLATE, params);
            JSONObject jobConfig = JSONObject.parseObject(appPodEventJobConfig);

            jobMasterOperator.createJob(Collections.singletonList(jobConfig));
        }

        return incidentDefinitionId;
    }

    private void openOemFailure(DefinitionCreateReq failureDefinitionCreateReq) throws Exception {
        try {
            definitionService.addDefinition(failureDefinitionCreateReq);
        } catch (CommonDefinitionExistException ex) {
            log.warn(String.format("定义已经存在:%s", ex.getMessage()));
        }
    }

    private void openOemAlert(DefinitionCreateReq alertDefinitionCreateReq, Integer incidentDefinitionId) throws Exception {
        Integer subAlertDefinitionId;
        try {
            subAlertDefinitionId = definitionService.addDefinition(alertDefinitionCreateReq);
        } catch (CommonDefinitionExistException ex) {
            log.warn(String.format("定义已经存在:%s", ex.getMessage()));
            List<JSONObject> existDefinitions = definitionService.getDefinitions(alertDefinitionCreateReq.getAppId(),
                    null, alertDefinitionCreateReq.getName(), Constant.ALERT);
            subAlertDefinitionId = existDefinitions.get(0).getInteger("id");
        }

        String jobName = String.format(APP_ALERT_JOB_NAME_FORMAT, alertDefinitionCreateReq.getAppName());
        JSONObject job = jobMasterOperator.getJobByName(jobName);
        // 创建新作业
        if (CollectionUtils.isEmpty(job)) {
            Jinjava jinjava = new Jinjava();
            Map<String, Object> params = new HashMap<>();
            params.put("appId", alertDefinitionCreateReq.getAppId());
            params.put("appName", alertDefinitionCreateReq.getAppName());
            params.put("alertDefinitionId", subAlertDefinitionId);
            params.put("incidentDefinitionId", incidentDefinitionId);
            String appPodEventJobConfig = jinjava.render(APP_ALERT_JOB_CONFIG_TEMPLATE, params);
            JSONObject jobConfig = JSONObject.parseObject(appPodEventJobConfig);

            jobMasterOperator.createJob(Collections.singletonList(jobConfig));
        }
    }

    private DefinitionCreateReq loadDefinitionConfig(String category, String appId, String appName) {
        try {
            ClassPathResource classPathResource = new ClassPathResource("oem/" + category + "/definition_format.json");
            DefinitionCreateReq definitionCreateReq = JSONObject.parseObject(classPathResource.getInputStream(), DefinitionCreateReq.class);
            definitionCreateReq.setAppId(appId);
            definitionCreateReq.setAppName(appName);

            Jinjava jinjava = new Jinjava();
            Map<String, Object> params = new HashMap<>();
            params.put("appName", appName);
            String req = jinjava.render(JSONObject.toJSONString(definitionCreateReq), params);
            return JSONObject.parseObject(req, DefinitionCreateReq.class);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    private boolean stateCronJob(String jobName) throws Exception {
        JSONObject job = jobMasterOperator.getJobByName(jobName);
        if (!CollectionUtils.isEmpty(job)) {
            return job.getJSONObject("triggerConf").getBoolean("enabled");
        }
        return false;
    }

    private void closeCronJob(String jobName) throws Exception {
        JSONObject job = jobMasterOperator.getJobByName(jobName);
        if (!CollectionUtils.isEmpty(job)) {
            // 仅停止作业
            jobMasterOperator.toggleCronJob(job.getLong("id"), false);
        }
    }

    private JSONObject getAppById(String appId)  throws Exception {
        JSONObject app = appOperator.getAppById(appId);
        if (CollectionUtils.isEmpty(app)) {
            throw new ParamException("应用定义查询失败");
        }
        return app;
    }
}
