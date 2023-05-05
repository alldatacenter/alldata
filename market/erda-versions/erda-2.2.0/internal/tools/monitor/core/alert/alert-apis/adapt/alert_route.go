// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package adapt

import (
	"fmt"
	"net/url"
	"strings"

	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

type routeFunc func(params map[string]interface{}) string
type routeParamFunc func(params map[string]interface{}) string

func newRoute(format string, paramFuncs ...routeParamFunc) routeFunc {
	return func(params map[string]interface{}) string {
		var routeParams []interface{}
		for _, paramFunc := range paramFuncs {
			routeParams = append(routeParams, paramFunc(params))
		}
		return fmt.Sprintf(format, routeParams...)
	}
}

const (
	routeFormatHostMetric   = "/dataCenter/alarm/report/%s/%s?category=alert&x_filter_host_ip=%s&x_timestamp=%s"
	routeFormatHostDetail   = "/dataCenter/alarm/report/%s/machine-list/%s"
	routeFormatRuntime      = "/dop/projects/%s/deploy/list/%s/%s/runtime/%s"
	routeFormatTopology     = "/microService/%s/%s/%s/topology/%s?appId=%s"
	routeFormatErrorDetail  = "/microService/%s/%s/%s/monitor/%s/error/error-detail/%s"
	routeFormatStatusList   = "/microService/%s/%s/%s/monitor/%s/status"
	routeFormatStatusDetail = "/microService/%s/%s/%s/monitor/%s/status/%s"
	routeFormatBI           = "/microService/%s/%s/%s/monitor/%s/bi/%s"
	routeFormatSI           = "/microService/%s/%s/%s/topology/%s/%s/%s/%s/si/%s"

	routeFormatProcess     = "/microService/%s/%s/%s/monitor/%s/service-analysis/%s/%s/%s/process"
	routeFormatTransaction = "/microService/%s/%s/%s/monitor/%s/service-analysis/%s/%s/%s/transaction?type=%s"

	routeFormatOrgAddon = "/dataCenter/alarm/middleware-chart?addon_id=%s&cluster_name=%s&timestamp=%s"
	routeFormatOrgPod   = "/dataCenter/alarm/pod?pod_name=%s&cluster_name=%s&timestamp=%s"

	routeOrgAddonId          = "org_addon"
	routeOrgPodId            = "org_pod"
	routeMachineCpuId        = "machine_cpu"
	routeMachineLoadId       = "machine_load"
	routeMachineDiskId       = "machine_disk"
	routeMachineDiskIOId     = "machine_disk_io"
	routeMachineCrashId      = "machine_crash"
	routeMachineDetailId     = "machine_detail"
	routeWorkbenchRuntimeId  = "workbench_runtime"
	routeMicroTopologyId     = "micro_topology"
	routeMicroErrorDetailId  = "micro_error_detail"
	routeMicroStatusListId   = "micro_status_list"
	routeMicroStatusDetailId = "micro_status_detail"
	routeMicroBiAjaxId       = "micro_bi_ajax"
	routeMicroBiPositionId   = "micro_bi_position"
	routeMicroBiDomainId     = "micro_bi_domain"
	routeMicroBiScriptId     = "micro_bi_script"
	routeMicroBiPageId       = "micro_bi_page"
	routeMicroSiJvmId        = "micro_jvm"
	routeMicroSiNodeJsId     = "micro_nodejs"
	routeMicroSiWebId        = "micro_si_web"
	routeMicroSiRPCId        = "micro_si_rpc"
	routeMicroSiDbId         = "micro_si_db"
	routeMicroSiCacheId      = "micro_si_cache"
)

var routeMap = map[string]routeFunc{
	routeMachineCpuId: newRoute(
		routeFormatHostMetric,
		routeParamConvert("cluster_name"),
		routeParamOrigin("cpu"),
		routeParamConvert("host_ip"),
		routeParamConvert("timestamp_unix"),
	),
	routeMachineLoadId: newRoute(
		routeFormatHostMetric,
		routeParamConvert("cluster_name"),
		routeParamOrigin("load"),
		routeParamConvert("host_ip"),
		routeParamConvert("timestamp_unix"),
	),
	routeMachineDiskId: newRoute(
		routeFormatHostMetric,
		routeParamConvert("cluster_name"),
		routeParamOrigin("disk"),
		routeParamConvert("host_ip"),
		routeParamConvert("timestamp_unix"),
	),
	routeMachineDiskIOId: newRoute(
		routeFormatHostMetric,
		routeParamConvert("cluster_name"),
		routeParamOrigin("diskio"),
		routeParamConvert("host_ip"),
		routeParamConvert("timestamp_unix"),
	),
	routeMachineCrashId: newRoute(
		routeFormatHostMetric,
		routeParamConvert("cluster_name"),
		routeParamOrigin("crash"),
		routeParamConvert("host_ip"),
		routeParamConvert("timestamp_unix"),
	),
	routeMachineDetailId: newRoute(
		routeFormatHostDetail,
		routeParamConvert("cluster_name"),
		routeParamConvert("host_ip"),
	),
	routeWorkbenchRuntimeId: newRoute(
		routeFormatRuntime,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("application_id"),
		routeParamConvert("runtime_id"),
	),
	routeMicroTopologyId: newRoute(
		routeFormatTopology,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamConvert("application_id"),
	),
	routeMicroErrorDetailId: newRoute(
		routeFormatErrorDetail,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamConvert("error_id"),
	),
	routeMicroStatusListId: newRoute(
		routeFormatStatusList,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
	),
	routeMicroStatusDetailId: newRoute(
		routeFormatStatusDetail,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamConvert("metric_id"),
	),
	routeMicroBiAjaxId: newRoute(
		routeFormatBI,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamOrigin("ajax"),
	),
	routeMicroBiPositionId: newRoute(
		routeFormatBI,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamOrigin("position"),
	),
	routeMicroBiDomainId: newRoute(
		routeFormatBI,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamOrigin("domain"),
	),
	routeMicroBiScriptId: newRoute(
		routeFormatBI,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamOrigin("script"),
	),
	routeMicroBiPageId: newRoute(
		routeFormatBI,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamOrigin("page"),
	),
	routeMicroSiJvmId: newRoute(
		routeFormatProcess,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamConvert("application_id"),
		routeParamsConvert("application_id", "runtime_name", "service_name"),
		routeParamConvert("service_name"),
	),
	routeMicroSiNodeJsId: newRoute(
		routeFormatProcess,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamConvert("application_id"),
		routeParamsConvert("application_id", "runtime_name", "service_name"),
		routeParamConvert("service_name"),
	),
	routeMicroSiWebId: newRoute(
		routeFormatTransaction,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamConvert("application_id"),
		routeParamsConvert("application_id", "runtime_name", "service_name"),
		routeParamConvert("service_name"),
		routeParamOrigin("web"),
	),
	routeMicroSiRPCId: newRoute(
		routeFormatTransaction,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamConvert("application_id"),
		routeParamsConvert("application_id", "runtime_name", "service_name"),
		routeParamConvert("service_name"),
		routeParamOrigin("rpc"),
	),
	routeMicroSiDbId: newRoute(
		routeFormatTransaction,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamConvert("application_id"),
		routeParamsConvert("application_id", "runtime_name", "service_name"),
		routeParamConvert("service_name"),
		routeParamOrigin("db"),
	),
	routeMicroSiCacheId: newRoute(
		routeFormatTransaction,
		routeParamConvert("project_id"),
		routeParamConvert("workspace"),
		routeParamConvert("tenant_group"),
		routeParamConvert("terminus_key"),
		routeParamConvert("application_id"),
		routeParamsConvert("application_id", "runtime_name", "service_name"),
		routeParamConvert("service_name"),
		routeParamOrigin("cache"),
	),
	routeOrgAddonId: newRoute(
		routeFormatOrgAddon,
		routeParamConvert("addon_id"),
		routeParamConvert("cluster_name"),
		routeParamConvert("timestamp_unix"),
	),
	routeOrgPodId: newRoute(
		routeFormatOrgPod,
		routeParamConvert("pod_name"),
		routeParamConvert("cluster_name"),
		routeParamConvert("timestamp_unix"),
	),
}

// get key
func routeParamOrigin(key string) routeParamFunc {
	return func(params map[string]interface{}) string {
		return key
	}
}

// Get the value corresponding to the key in the parameter, otherwise get the template key
func routeParamConvert(key string) routeParamFunc {
	return func(params map[string]interface{}) string {
		value, ok := utils.GetMapValueString(params, key)
		if !ok {
			value = "{{" + key + "}}"
		} else {
			// url coding
			value = url.QueryEscape(value)
		}
		return value
	}
}

func routeParamsConvert(keys ...string) routeParamFunc {
	return func(params map[string]interface{}) string {
		serviceId := make([]string, 0)
		for _, key := range keys {
			value, ok := utils.GetMapValueString(params, key)
			if !ok {
				value = "{{" + key + "}}"
			} else {
				value = url.QueryEscape(value)
			}
			serviceId = append(serviceId, value)
		}
		return strings.Join(serviceId, "_")
	}
}

// transform alert url
func convertAlertURL(domain, orgName, routeID string, params map[string]interface{}) string {
	route, ok := routeMap[routeID]
	if !ok {
		return ""
	}
	return domain + "/" + orgName + route(params)
}

// convert custom market url
func convertDashboardURL(domain, orgName, path, dashboardID string, groups []string) string {
	u := domain + "/" + orgName + path + "/" + dashboardID
	conn := "?"
	for _, group := range groups {
		u += conn + group + "={{" + group + "}}"
		conn = "&"
	}
	u += conn + "timestamp={{timestamp}}"
	return u
}

// transform record url
func convertRecordURL(domain, orgName, path string) string {
	return domain + "/" + orgName + path
}
