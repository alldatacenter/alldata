// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import path from 'path-browserify';
import { filter, get, isEmpty, isFunction, mapValues, pickBy, throttle } from 'lodash';
import { qs } from './query-string';
import { DOC_ORG_INTRO, DOC_PROJECT_INTRO } from 'common/constants';
import routeInfoStore from 'core/stores/route';
import { getConfig } from 'core/config';

export function resolvePath(goPath: string) {
  return path.resolve(window.location.pathname, goPath);
}

const changeBrowserHistory = throttle(
  (action, _path) => {
    const history = getConfig('history');
    action === 'replace' ? history.replace(_path) : history.push(_path);
  },
  1000,
  { trailing: false },
);

export interface IOptions {
  [pathName: string]: any;
  append?: boolean;
  replace?: boolean;
  forbidRepeat?: boolean;
  jumpOut?: boolean;
}

/**
 * 路由跳转
 * @param pathStr 跳转路径
 * @param {string} options.replace 替换当前history记录
 * @param {boolean} options.forbidRepeat 为true时1s内禁止再次点击
 * @param {boolean} options.jumpOut 为true时新开窗口
 * @param {object} options.rest 使用pages时的参数对象
 */

export const goTo = (pathStr: string, options?: IOptions) => {
  const { replace = false, forbidRepeat = false, jumpOut = false, query, ...rest } = (options as IOptions) || {};
  let _path = '';

  if (/^(http|https):\/\//.test(pathStr)) {
    // 外链
    if (jumpOut) {
      window.open(pathStr);
    } else {
      window.location.href = pathStr;
    }
    return;
  } else if (pathStr.startsWith(goTo.pagePrefix)) {
    const orgName = get(location.pathname.split('/'), '[1]') || '-';
    const [urlParams, urlQuery] = routeInfoStore.getState((s) => [s.params, s.query]);
    const pathParams = { orgName, ...urlParams, ...urlQuery, ...rest };
    const curPath = goTo.pagePathMap[pathStr.replace(goTo.pagePrefix, '')];
    // 缺少参数
    if (curPath === undefined) {
      return;
    }
    _path = isFunction(curPath) ? curPath(pathParams) : curPath;
  } else {
    _path = resolvePath(pathStr);
  }
  if (query && !isEmpty(query)) {
    _path += `?${qs.stringify(query)}`;
  }

  if (jumpOut) {
    window.open(_path);
    return;
  }

  const action = replace ? 'replace' : 'push';

  if (forbidRepeat) {
    changeBrowserHistory(action, _path);
  } else {
    const history = getConfig('history');
    action === 'replace' ? history.replace(_path) : history.push(_path);
  }
};

const pathFormat = (url: string) => (params: object) => {
  const necessaryArg: string[] = [];
  const [_path, _query] = url.split('?');
  // query不算必须的路由参数
  const newPath = _path.replace(/\{(\w+)\}/g, (_, key) => {
    necessaryArg.push(key);
    return params[key];
  });
  const lostArg = filter(necessaryArg, (arg: string) => params[arg] === undefined);
  if (lostArg.length) {
    // eslint-disable-next-line no-console
    console.error('Jump missing parameters：', lostArg, `in url: ${url}`);
    return undefined;
  }
  if (!_query) {
    return newPath;
  }
  let newQuery = _query.replace(/\{(\w+)\}/g, (_, key) => {
    return params[key] !== undefined && params[key] !== null ? params[key] : '';
  });
  // 移除空的query参数
  newQuery = qs.stringify(pickBy(qs.parse(newQuery), (v: any) => v !== ''));
  return newQuery ? [newPath, newQuery].join('?') : newPath;
};
goTo.pagePrefix = '__dice__'; // 防止goTo传入同名参数

export enum pages {
  noAuth = '/{orgName}/noAuth',
  perm = '/{orgName}/perm?scope={scope}',
  inviteToOrg = '/{orgName}/inviteToOrg',
  freshMan = '/{orgName}/freshMan',
  notFound = '/{orgName}/notFound',

  // dop
  orgRoot = '/{orgName}',
  orgList = '/{orgName}/org-list',
  dopRoot = '/{orgName}/dop/projects',
  dopApps = '/{orgName}/dop/apps',
  dopService = '/{orgName}/dop/service',
  dopApprove = '/{orgName}/dop/approval',
  dopApprovePending = '/{orgName}/dop/approval/my-approve/pending',
  dopMyInitiate = '/{orgName}/dop/approval/my-initiate',
  dopMyInitiateWait = '/{orgName}/dop/approval/my-initiate/WaitApprove',
  dopPublisher = '/{orgName}/dop/publisher',
  dopPublicProjects = '/{orgName}/dop/public-projects',

  // project
  project = '/{orgName}/dop/projects/{projectId}',
  projectSetting = '/{orgName}/dop/projects/{projectId}/setting',
  projectLabel = '/{orgName}/dop/projects/{projectId}/setting?tabKey=projectLabel',
  projectNotifyGroup = '/{orgName}/dop/projects/{projectId}/setting?tabKey=notifyGroup',
  projectService = '/{orgName}/dop/projects/{projectId}/service',
  testPlanDetail = '/{orgName}/dop/projects/{projectId}/manual/testPlan/{testPlanID}?caseId={caseId}&testSetID={testSetID}',
  projectApps = '/{orgName}/dop/projects/{projectId}/apps',
  projectAllIssue = '/{orgName}/dop/projects/{projectId}/issues/all',
  projectIssue = '/{orgName}/dop/projects/{projectId}/issues/{type}',
  projectIssueDetail = '/{orgName}/dop/projects/{projectId}/issues/{type}?id={id}&type={type}',
  projectIssueRoot = '/{orgName}/dop/projects/{projectId}/issues',
  projectMeasureDashboard = '/{orgName}/dop/projects/{projectId}/measure/bug',
  projectMeasureTask = '/{orgName}/dop/projects/{projectId}/measure/task',
  projectMeasure = '/{orgName}/dop/projects/{projectId}/measure',
  projectManualTestRoot = '/{orgName}/dop/projects/{projectId}/manual',
  projectAutoTestRoot = '/{orgName}/dop/projects/{projectId}/auto',
  projectManualTestCase = '/{orgName}/dop/projects/{projectId}/manual/testCase',
  projectAutoTestCase = '/{orgName}/dop/projects/{projectId}/auto/testCase',
  projectTestCodeCoverage = '/{orgName}/dop/projects/{projectId}/statistics/code-coverage',
  projectTestDashboard = '/{orgName}/dop/projects/{projectId}/statistics/test-dashboard',
  projectTestStatisticsRoot = '/{orgName}/dop/projects/{projectId}/statistics',
  projectDataSource = '/{orgName}/dop/projects/{projectId}/auto/data-source',
  projectManualTestPlane = '/{orgName}/dop/projects/{projectId}/manual/testPlan',
  projectManualTestEnv = '/{orgName}/dop/projects/{projectId}/manual/testEnv',
  projectDashboard = '/{orgName}/dop/projects/{projectId}/dashboard',
  projectResource = '/{orgName}/dop/projects/{projectId}/resource',
  projectTicket = '/{orgName}/dop/projects/{projectId}/ticket',
  projectTestReport = '/{orgName}/dop/projects/{projectId}/test-report',
  projectTestReportCreate = '/{orgName}/dop/projects/{projectId}/test-report/create',
  projectMemberManagement = '/{orgName}/dop/projects/{projectId}/setting?tabKey=projectMember',

  // app
  app = '/{orgName}/dop/projects/{projectId}/apps/{appId}',
  repo = '/{orgName}/dop/projects/{projectId}/apps/{appId}/repo',
  appMr = '/{orgName}/dop/projects/{projectId}/apps/{appId}/repo/mr/open/{mrId}',
  pipelineRoot = '/{orgName}/dop/projects/{projectId}/apps/{appId}/pipeline',
  appApiDesign = '/{orgName}/dop/projects/{projectId}/apps/{appId}/apiDesign',
  repoBackup = '/{orgName}/dop/projects/{projectId}/apps/{appId}/repo/backup',
  commit = '/{orgName}/dop/projects/{projectId}/apps/{appId}/repo/commit/{commitId}',
  branches = '/{orgName}/dop/projects/{projectId}/apps/{appId}/repo/branches',
  tags = '/{orgName}/dop/projects/{projectId}/apps/{appId}/repo/tags',
  commits = '/{orgName}/dop/projects/{projectId}/apps/{appId}/repo/commits/{branch}/{path}',
  pipeline = '/{orgName}/dop/projects/{projectId}/apps/{appId}/pipeline?caseId={caseId}&pipelineID={pipelineID}',
  dataTask = '/{orgName}/dop/projects/{projectId}/apps/{appId}/dataTask/{pipelineID}',
  dataTaskRoot = '/{orgName}/dop/projects/{projectId}/apps/{appId}/dataTask',
  deploy = '/{orgName}/dop/projects/{projectId}/apps/{appId}/deploy',
  qaTicket = '/{orgName}/dop/projects/{projectId}/apps/{appId}/ticket/open?type={type}',
  release = '/{orgName}/dop/projects/{projectId}/apps/{appId}/release?q={q}',
  runtimeDetail = '/{orgName}/dop/projects/{projectId}/apps/{appId}/deploy/runtimes/{runtimeId}/overview?serviceName={serviceName}&jumpFrom={jumpFrom}',
  runtimeDetailRoot = '/{orgName}/dop/projects/{projectId}/apps/{appId}/deploy/runtimes/{runtimeId}/overview',
  appDataModel = '/{orgName}/dop/projects/{projectId}/apps/{appId}/dataModel',
  appDataMarket = '/{orgName}/dop/projects/{projectId}/apps/{appId}/dataMarket',
  appCodeQuality = '/{orgName}/dop/projects/{projectId}/apps/{appId}/test',
  appCodeQualityReports = '/{orgName}/dop/projects/{projectId}/apps/{appId}/test/quality',
  appCodeQualityIssueOpen = '/{orgName}/dop/projects/{projectId}/apps/{appId}/ticket/open',
  appCodeQualityIssue = '/{orgName}/dop/projects/{projectId}/apps/{appId}/ticket',
  appSetting = '/{orgName}/dop/projects/{projectId}/apps/{appId}/setting',

  // 中间件平台首页 /addonPlatform/addonsManage 去掉了。详情页保留，暂时挂在DevOps平台下面。
  addonPlatformMysqlAccount = '/{orgName}/dop/addonsManage/{projectId}/{instanceId}/mysql-settings/account',
  addonPlatformMysqlConsumer = '/{orgName}/dop/addonsManage/{projectId}/{instanceId}/mysql-settings/consumer',
  addonPlatformOverview = '/{orgName}/dop/addonsManage/{projectId}/{instanceId}/overview',
  logAnalyticConsole = '/{orgName}/dop/addonsManage/{projectId}/{instanceId}/log-analytics?appName={appName}',
  jvmProfiler = '/{orgName}/dop/addonsManage/{projectId}/{instanceId}/jvm-profiler',
  appSetting_config = '/{orgName}/dop/projects/{projectId}/apps/{appId}/setting?tabKey=appConfig',
  buildDetailConfig = '/{orgName}/dop/projects/{projectId}/config/apps/{appId}/runtimes/{branch}/{env}',
  mspRoot = '/{orgName}/msp/mspManage',
  mspProjects = '/{orgName}/msp/projects',
  mspRootOverview = '/{orgName}/msp/overview',
  mspOverviewRoot = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}',
  mspOverview = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}?appId={appId}&runtimeId={runtimeId}',
  mspApiStrategy = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/gateway/api-package/{packageId}/detail/api-policies/safety-policy',
  mspProjectNotifyGroup = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/alarm-management/{terminusKey}/notify-group',
  mspTopology = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/topology/{terminusKey}?appId={appId}',
  monitorAPIOverview = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/gateway/apis/api-monitor?appId={appId}&runtimeId={runtimeId}',
  microTraceSearch = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/monitor/{terminusKey}/trace/search?appId={appId}&start={start}&end={end}&status={status}&mode={mode}&quick={quick}',
  microTrace = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/monitor/{terminusKey}/trace',
  microTraceDetail = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/monitor/{terminusKey}/trace/trace-detail/{traceId}',
  cmpRoot = '/{orgName}/cmp/overview',

  createProject = '/{orgName}/orgCenter/projects/createProject',
  cmpClusters = '/{orgName}/cmp/clusters',
  cmpClusterState = '/{orgName}/cmp/clusters/{clusterName}/state',
  cmpResourceRank = '/{orgName}/cmp/resource-rank',
  cmpResourceProjectRank = '/{orgName}/cmp/resource-rank/project',
  cmpResourceOwnerRank = '/{orgName}/cmp/resource-rank/owner',
  publisherContent = '/{orgName}/dop/publisher/{type}/{publisherItemId}',
  iterationDetail = '/{orgName}/dop/projects/{projectId}/issues/iteration/{iterationId}/{issueType}',
  taskList = '/{orgName}/dop/projects/{projectId}/issues/all?type=task',
  bugList = '/{orgName}/dop/projects/{projectId}/issues/all?type=bug',
  issueDetail = '/{orgName}/dop/projects/{projectId}/issues/{issueType}?id={issueId}&iterationID={iterationId}&type={issueType}',
  ticketDetail = '/{orgName}/dop/projects/{projectId}/ticket?id={issueId}&pageNo=1',
  backlog = '/{orgName}/dop/projects/{projectId}/issues/backlog?id={issueId}&issueType={issueType}',
  project_test_autoTestPlanDetail = '/{orgName}/dop/projects/{projectId}/auto/testPlan/{id}',
  project_test_spaceDetail_apis = '/{orgName}/dop/projects/{projectId}/auto/testCase/{id}/apis',
  project_test_spaceDetail_scenes = '/{orgName}/dop/projects/{projectId}/auto/testCase/{id}/scenes',

  // 微服务
  apiStrategy = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/gateway/api-package/{packageId}/detail/api-policies/safety-policy?apiId={apiId}',
  apiManageQuery = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/gateway/apis?redirectApp={redirectApp}&redirectService={redirectService}&redirectRuntimeId={redirectRuntimeId}',
  apiManage = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/gateway/apis',

  // 微服务日志分析规则
  ms_addLogAnalyzeRule = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/log/{addonId}/rule/add?source={source}',

  // 网关“入口流量管理”
  getwayDetail = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/gateway/api-package/{packageId}/detail',
  gatewayList = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/gateway/api-package?domain={domain}',

  // 企业中心告警数据报表
  alarmReport = '/{orgName}/cmp/alarm/report/{clusterName}/{chartUniqId}?category={category}&x_filter_host_ip={ip}&x_timestamp={timestamp}',

  // 企业中心自定义大盘
  orgCustomDashboard = '/{orgName}/cmp/customDashboard',
  orgAddCustomDashboard = '/{orgName}/cmp/customDashboard/add',
  orgCustomDashboardDetail = '/{orgName}/cmp/customDashboard/{customDashboardId}',

  // 微服务监控自定义大盘
  microServiceCustomDashboard = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/analysis/{terminusKey}/custom-dashboard',
  microServiceAddCustomDashboard = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/analysis/{terminusKey}/custom-dashboard/add',
  microServiceCustomDashboardDetail = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/analysis/{terminusKey}/custom-dashboard/{customDashboardId}',

  // 微服务-总览服务列表
  mspServiceAnalyze = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/synopsis/{terminusKey}/service-list/{applicationId}/{serviceId}/{serviceName}',

  // 微服务-监控中心-服务监控
  mspServiceMonitor = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/monitor/{terminusKey}/service-analysis/{applicationId}/{serviceId}/{serviceName}',

  // 微服务-具体事务分析页
  mspServiceTransaction = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/monitor/{terminusKey}/service-analysis/{applicationId}/{serviceId}/{serviceName}/transaction',

  mspGatewayIngress = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/synopsis/{terminusKey}/topology/gateway-ingress',

  mspExternalInsight = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/synopsis/{terminusKey}/topology/ei/{hostName}/affairs',

  mspServiceProcess = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/synopsis/{terminusKey}/service-analysis/{applicationId}/{serviceId}/{serviceName}/process',

  // 服务分析页-追踪详情
  mspServiceTraceDetail = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/monitor/{terminusKey}/service-analysis/{applicationId}/{serviceId}/{serviceName}/transaction/trace-detail/{traceId}',

  // 微服务-接入配置页
  mspConfigurationPage = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/environment/{terminusKey}/configuration',

  // 链路调试
  mspTraceDebug = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/monitor/{terminusKey}/trace/debug',

  // 企业日志分析规则
  addLogAnalyzeRule = '/{orgName}/cmp/log/rule/add?source={source}',

  // 企业告警记录详情
  orgAlarmRecordDetail = '/{orgName}/cmp/alarm/record/{id}',

  // 微服务告警记录详情
  microServiceAlarmRecordDetail = '/{orgName}/msp/{projectId}/{env}/{tenantGroup}/alarm-management/{terminusKey}/alarm-record/{id}',

  // 云资源管理
  cloudSource = '/{orgName}/cmp/cloudSource',
  cmpDomain = '/{orgName}/cmp/domain',
  cmpServices = '/{orgName}/cmp/services',
  cmpAddon = '/{orgName}/cmp/addon',
  cmpJobs = '/{orgName}/cmp/jobs',
  cmpReport = '/{orgName}/cmp/report',
  cmpAlarm = '/{orgName}/cmp/alarm',
  cmpAlarmStatistics = '/{orgName}/cmp/alarm/statistics',
  cmpAlarmRecord = '/{orgName}/cmp/alarm/record',
  cmpAlarmStrategy = '/{orgName}/cmp/alarm/strategy',
  cmpAlarmCustom = '/{orgName}/cmp/alarm/custom',
  cmpLog = '/{orgName}/cmp/log',
  cmpLogQuery = '/{orgName}/cmp/log/query',
  cmpLogRule = '/{orgName}/cmp/log/rule',
  cloudSourceEcs = '/{orgName}/cmp/cloudSource/ecs',
  cloudSourceVpc = '/{orgName}/cmp/cloudSource/vpc',
  cloudSourceOss = '/{orgName}/cmp/cloudSource/oss',
  cloudSourceRds = '/{orgName}/cmp/cloudSource/rds',
  cloudSourceMq = '/{orgName}/cmp/cloudSource/mq',
  cloudSourceRedis = '/{orgName}/cmp/cloudSource/redis',
  cloudAccounts = '/{orgName}/cmp/cloudSource/accounts',

  cmpClustersDetail = '/{orgName}/cmp/clusters/{clusterName}/detail',
  cmpClustersContainer = '/{orgName}/cmp/container/{clusterName}',
  cmpClustersNodes = '/{orgName}/cmp/container/{clusterName}/nodes',
  cmpClustersNodeDetail = '/{orgName}/cmp/container/{clusterName}/nodes/{nodeId}/detail',
  cmpClustersPods = '/{orgName}/cmp/container/{clusterName}/pods',
  cmpClustersPodDetail = '/{orgName}/cmp/container/{clusterName}/pods/{podId}/detail',
  cmpClustersWorkload = '/{orgName}/cmp/container/{clusterName}/workload',
  cmpClustersWorkloadDetail = '/{orgName}/cmp/container/{clusterName}/workload/{workloadId}/detail',
  cmpClustersEventLog = '/{orgName}/cmp/container/{clusterName}/event-log',

  // orgCenter
  orgCenterRoot = '/{orgName}/orgCenter/projects',
  orgCenterMarket = '/{orgName}/orgCenter/market',
  orgCenterPublisherSetting = '/{orgName}/orgCenter/market/publisher/setting',
  orgCenterCertificate = '/{orgName}/orgCenter/market/publisher/certificate',
  orgCenterApproval = '/{orgName}/orgCenter/approval',
  orgCenterApprovalUndone = '/{orgName}/orgCenter/approval/undone',
  orgCenterAnnouncement = '/{orgName}/orgCenter/setting/detail?tabKey=announcement',
  orgCenterSafety = '/{orgName}/orgCenter/safety',

  cmpNotifyGroup = '/{orgName}/orgCenter/setting/detail?tabKey=notifyGroup',
  cmpSetting = '/{orgName}/orgCenter/setting/detail',

  // Api 管理平台
  apiManageRoot = '/{orgName}/dop/apiManage/api-market/mine',
  apiManageMarket = '/{orgName}/dop/apiManage/api-market',
  apiDesign = '/{orgName}/dop/apiManage/api-design',
  apiAccessManage = '/{orgName}/dop/apiManage/access-manage',
  apiAccessManageDetail = '/{orgName}/dop/apiManage/access-manage/detail/{accessID}',
  apiMyVisit = '/{orgName}/dop/apiManage/client',
  apiManageAssetVersions = '/{orgName}/dop/apiManage/api-market/{scope}/{assetID}',
  apiManageAssetDetail = '/{orgName}/dop/apiManage/api-market/{scope}/{assetID}/{versionID}',

  // 市场
  market = '/{orgName}/market/download/{publishItemId}',

  // api仓库
  apiDocs = '/{orgName}/dop/projects/{projectId}/apps/{appId}/repo/tree/{branchName}/.dice/apidocs/{docName}',

  // 边缘计算平台
  ecpApp = '/{orgName}/ecp/application',
  ecpAppSiteManage = '/{orgName}/ecp/application/{id}',
  ecpAppSiteIpManage = '/{orgName}/ecp/application/{id}/{siteName}',
  ecpResource = '/{orgName}/ecp/resource',
  ecpSiteMachine = '/{orgName}/ecp/resource/{id}',
  ecpSetting = '/{orgName}/ecp/setting',
  ecpSettingDetail = '/{orgName}/ecp/setting/{id}',

  // sysAdmin
  sysAdmin = '/{orgName}/sysAdmin',

  dataAppEntry = '/{orgName}/fdp/__cluster__/__workspace__/data-govern-platform/data-source',
}

goTo.pages = {
  ...pages,
  // doc path
  'doc-project-intro': DOC_PROJECT_INTRO,
  'doc-org-intro': DOC_ORG_INTRO,
};
goTo.pagePathMap = {};
goTo.resolve = {} as {
  [k in keyof typeof pages]: (params?: Obj, prependOrigin?: boolean) => string;
};
mapValues(pages, (v, k) => {
  goTo.pages[k] = `${goTo.pagePrefix}${k}`;
  goTo.pagePathMap[k] = v.match(/\{.+\}/) ? pathFormat(v) : v;
  goTo.resolve[k] = (params?: Obj, prependOrigin?: boolean) => {
    const orgName = get(location.pathname.split('/'), '[1]') || '-';
    const [urlParams, urlQuery] = routeInfoStore.getState((s) => [s.params, s.query]);
    const pathParams = { orgName, ...urlParams, ...urlQuery, ...params };
    const prefix = prependOrigin ? window.location.origin : '';
    const pagePath = pathFormat(v)(pathParams);
    return prefix + pagePath;
  };
});
