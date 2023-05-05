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

package query

import (
	"github.com/erda-project/erda-infra/providers/i18n"
)

func getScopeTagsTree(scope string) map[string]interface{} {
	if scope == "micro_service" {
		return map[string]interface{}{
			"key":       "dice_application_id",
			"dimension": "app",
		}
	}
	// org
	return map[string]interface{}{
		"key":       "dice_project_id",
		"dimension": "project",
		"dynamic_children": map[string]interface{}{
			"key":       "dice_application_id",
			"dimension": "app",
		},
	}
}

// GetTagsTree .
func (p *provider) GetTagsTree(scope string, lang i18n.LanguageCodes) []map[string]interface{} {
	return []map[string]interface{}{
		{
			"tag": map[string]interface{}{
				"name":  p.t.Text(lang, "Dice"),
				"key":   "origin",
				"value": "dice",
			},
			"dynamic_children": getScopeTagsTree(scope),
		},
		{
			"tag": map[string]interface{}{
				"name":  p.t.Text(lang, "Aliyun"),
				"key":   "origin",
				"value": "sls",
			},
			"children": []map[string]interface{}{
				{
					"tag": map[string]interface{}{
						"name":  p.t.Text(lang, "WAF"),
						"key":   "product",
						"value": "waf",
					},
					"children": []map[string]interface{}{
						{
							"tag": map[string]interface{}{
								"name": "Matched Host",
								"key":  "matched_host",
							},
						}, {
							"tag": map[string]interface{}{
								"name": "Real Client IP",
								"key":  "real_client_ip",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Region",
								"key":  "region",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Ua Browser",
								"key":  "ua_browser",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "User ID",
								"key":  "user_id",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Block Action",
								"key":  "block_action",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Content Type",
								"key":  "content_type",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Ua Os",
								"key":  "ua_os",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Upstream Addr",
								"key":  "upstream_addr",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Wxbb Rule ID",
								"key":  "wxbb_rule_id",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "HTTP Referer",
								"key":  "http_referer",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Traceid",
								"key":  "request_traceid",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "SLS Category",
								"key":  "sls_category",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "HTTP User Agent",
								"key":  "http_user_agent",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "HTTP X Forwarded For",
								"key":  "http_x_forwarded_for",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Ua Browser Version",
								"key":  "ua_browser_version",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Ua Device Type",
								"key":  "ua_device_type",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Upstream Response Time",
								"key":  "upstream_response_time",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Host",
								"key":  "host",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Remote Port",
								"key":  "remote_port",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Time",
								"key":  "time",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Ua Browser Type",
								"key":  "ua_browser_type",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Wxbb Test",
								"key":  "wxbb_test",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "SLS Source",
								"key":  "sls_source",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Status",
								"key":  "status",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Ua Browser Family",
								"key":  "ua_browser_family",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Wxbb Invalid Wua",
								"key":  "wxbb_invalid_wua",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Body Bytes Sent",
								"key":  "body_bytes_sent",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Path",
								"key":  "request_path",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Time Msec",
								"key":  "request_time_msec",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Server Protocol",
								"key":  "server_protocol",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "SLS Project",
								"key":  "sls_project",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "SLS Topic",
								"key":  "sls_topic",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Ua Os Family",
								"key":  "ua_os_family",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "HTTP Cookie",
								"key":  "http_cookie",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Https",
								"key":  "https",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Remote Addr",
								"key":  "remote_addr",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Length",
								"key":  "request_length",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Method",
								"key":  "request_method",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "SLS Log Store",
								"key":  "sls_log_store",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Upstream Status",
								"key":  "upstream_status",
							},
						},
					},
				},
				{
					"tag": map[string]interface{}{
						"name":  p.t.Text(lang, "APIGateway"),
						"key":   "product",
						"value": "apigateway",
					},
					"children": []map[string]interface{}{
						{
							"tag": map[string]interface{}{
								"name": "Error Message",
								"key":  "errorMessage",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Exception",
								"key":  "exception",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "HTTP Method",
								"key":  "httpMethod",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Headers",
								"key":  "requestHeaders",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Response Body",
								"key":  "responseBody",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "API Group Name",
								"key":  "apiGroupName",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "API Stage Name",
								"key":  "apiStageName",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "SLS Topic",
								"key":  "sls_topic",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Body",
								"key":  "requestBody",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Client Nonce",
								"key":  "clientNonce",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Consumer App Key",
								"key":  "consumerAppKey",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Provider Ali UID",
								"key":  "providerAliUid",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Instance ID",
								"key":  "instanceId",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Handle Time",
								"key":  "requestHandleTime",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Size",
								"key":  "requestSize",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Custom Trace ID",
								"key":  "customTraceId",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Initial Request ID",
								"key":  "initialRequestId",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Protocol",
								"key":  "requestProtocol",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request Query String",
								"key":  "requestQueryString",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Response Size",
								"key":  "responseSize",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "SLS Source",
								"key":  "sls_source",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Status Code",
								"key":  "statusCode",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "API Group UID",
								"key":  "apiGroupUid",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "App ID",
								"key":  "appId",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Path",
								"key":  "path",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Total Latency",
								"key":  "totalLatency",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Response Headers",
								"key":  "responseHeaders",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "SLS Log Store",
								"key":  "sls_log_store",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "SLS Project",
								"key":  "sls_project",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "API Name",
								"key":  "apiName",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "API Stage UID",
								"key":  "apiStageUid",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Error Code",
								"key":  "errorCode",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Request ID",
								"key":  "requestId",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Service Latency",
								"key":  "serviceLatency",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "App Name",
								"key":  "appName",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Region",
								"key":  "region",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Domain",
								"key":  "domain",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "SLS Category",
								"key":  "sls_category",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "API UID",
								"key":  "apiUid",
							},
						},
						{
							"tag": map[string]interface{}{
								"name": "Client IP",
								"key":  "clientIp",
							},
						},
					},
				},
			},
		},
	}
}
