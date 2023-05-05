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

package sysconf

func (sc Sysconf) DependsOn(k string) map[string]string {
	m := map[string]map[string]string{
		"cmdb":           {"CMDB_ADDR": sc.Cluster.Host("cmdb") + ":9093"},
		"status":         {"STATUS_ADDR": sc.Cluster.Host("status") + ":7098"},
		"headless":       {"HEADLESS_ADDR": sc.Cluster.Host("headless") + ":9222"},
		"monitor":        {"MONITOR_ADDR": sc.Cluster.Host("monitor") + ":7096"},
		"addon-platform": {"ADDON_PLATFORM_ADDR": sc.Cluster.Host("addon-platform") + ":8080"},
		"dicehub":        {"DICEHUB_ADDR": sc.Cluster.Host("dicehub") + ":10000"},
		"eventbox":       {"EVENTBOX_ADDR": sc.Cluster.Host("eventbox") + ":9528"},
		"gittar": {
			"GITTAR_ADDR":        sc.Cluster.Host("gittar") + ":5566",
			"GITTAR_PUBLIC_ADDR": sc.Platform.Domain("gittar"),
			"GITTAR_PUBLIC_URL":  sc.Platform.PublicURL("gittar"),
		},
		"dashboard": {"DASHBOARD_ADDR": sc.Cluster.Host("dashboard") + ":7081"},
		"uc": {
			"UC_ADDR":        sc.Cluster.Host("uc") + ":8080",
			"UC_PUBLIC_ADDR": sc.Platform.Domain("uc"),
			"UC_PUBLIC_URL":  sc.Platform.PublicURL("uc"),
		},
		"officer":  {"OFFICER_ADDR": sc.Cluster.Host("officer") + ":9029"},
		"pipeline": {"PIPELINE_ADDR": sc.Cluster.Host("pipeline") + ":3081"},
		"collector": {
			"COLLECTOR_ADDR":        sc.Cluster.Host("collector") + ":7076",
			"COLLECTOR_PUBLIC_ADDR": sc.Platform.Domain("collector"),
			"COLLECTOR_PUBLIC_URL":  sc.Platform.PublicURL("collector"),
		},
		"orchestrator": {"ORCHESTRATOR_ADDR": sc.Cluster.Host("orchestrator") + ":8081"},
		"openapi": {
			"OPENAPI_ADDR":        sc.Cluster.Host("openapi") + ":9529",
			"OPENAPI_PUBLIC_ADDR": sc.Platform.Domain("openapi"),
			"OPENAPI_PUBLIC_URL":  sc.Platform.PublicURL("openapi"),
		},
		"ops": {
			"OPS_ADDR": sc.Cluster.Host("ops") + ":9027",
		},
		"scheduler": {"SCHEDULER_ADDR": sc.Cluster.Host("scheduler") + ":9091"},
		"sonar": {
			"SONAR_ADDR":        sc.Cluster.Host("sonar") + ":9000",
			"SONAR_PUBLIC_ADDR": sc.Platform.Domain("sonar"),
			"SONAR_PUBLIC_URL":  sc.Platform.PublicURL("sonar"),
		},
		"ui": {
			"UI_ADDR":        sc.Cluster.Host("ui") + ":80",
			"UI_PUBLIC_ADDR": sc.Platform.Domain("ui"),
			"UI_PUBLIC_URL":  sc.Platform.PublicURL("ui"),
		},
		"nexus":                       {"NEXUS_ADDR": sc.Cluster.Host("nexus") + ":8081"},
		"hepa":                        {"HEPA_ADDR": sc.Cluster.Host("hepa") + ":8080"},
		"netportal":                   {"NETPORTAL_ADDR": sc.Cluster.Host("netportal") + ""},
		"gittar-adaptor":              {"GITTAR_ADAPTOR_ADDR": sc.Cluster.Host("gittar-adaptor") + ":1086"},
		"qa":                          {"QA_ADDR": sc.Cluster.Host("qa") + ":3033"},
		"tmc":                         {"TMC_ADDR": sc.Cluster.Host("tmc") + ":8050"},
		"pmp-backend":                 {"PMP_BACKEND_ADDR": sc.Cluster.Host("pmp-backend") + ":5080"},
		"dl":                          {"DL_ADDR": sc.Cluster.Host("dl") + ":8080"},
		"fdp":                         {"FDP_ADDR": sc.Cluster.Host("fdp") + ":8080"},
		"pandora":                     {"PANDORA_ADDR": sc.Cluster.Host("pandora") + ":8050"},
		"analyzer-metrics":            {"ANALYZER_METRICS_ADDR": sc.Cluster.Host("analyzer-metrics") + ":8081"},
		"analyzer-starter":            {"ANALYZER_STARTER_ADDR": sc.Cluster.Host("analyzer-starter") + ":8081"},
		"analyzer-alert":              {"ANALYZER_ALERT_ADDR": sc.Cluster.Host("analyzer-alert") + ":8081"},
		"analyzer-error-insight":      {"ANALYZER_ERROR_INSIGHT_ADDR": sc.Cluster.Host("analyzer-error-insight") + ":8081"},
		"alerting-compute-jobmanager": {"ALERTING_COMPUTE_JOBMANAGER_ADDR": sc.Cluster.Host("alerting-compute-jobmanager") + ":8081"},
		"alerting-notice-jobmanager":  {"ALERTING_NOTICE_JOBMANAGER_ADDR": sc.Cluster.Host("alerting-notice-jobmanager") + ":8081"},
		"error-insight-jobmanager":    {"ERROR_INSIGHT_JOBMANAGER_ADDR": sc.Cluster.Host("error-insight-jobmanager") + ":8081"},
		"alert-run-jobmanager":        {"ALERT_RUN_JOBMANAGER_ADDR": sc.Cluster.Host("alert-run-jobmanager") + ":8081"},
		"log-insight-jobmanager":      {"LOG_INSIGHT_JOBMANAGER_ADDR": sc.Cluster.Host("log-insight-jobmanager") + ":8081"},
		"trace-insight-jobmanager":    {"TRACE_INSIGHT_JOBMANAGER_ADDR": sc.Cluster.Host("trace-insight-jobmanager") + ":8081"},
		"soldier": {
			"SOLDIER_ADDR":        sc.Cluster.Host("soldier") + ":9028",
			"SOLDIER_PUBLIC_ADDR": sc.Platform.Domain("soldier"),
			"SOLDIER_PUBLIC_URL":  sc.Platform.PublicURL("soldier"),
		},
	}
	if sc.MainPlatform != nil {
		m["openapi"]["OPENAPI_PUBLIC_ADDR"] = sc.MainPlatform.Domain("openapi")
		m["openapi"]["OPENAPI_PUBLIC_URL"] = sc.MainPlatform.PublicURL("openapi")
		m["collector"]["COLLECTOR_PUBLIC_ADDR"] = sc.MainPlatform.Domain("collector")
		m["collector"]["COLLECTOR_PUBLIC_URL"] = sc.MainPlatform.PublicURL("collector")
	}
	return m[k]
}
