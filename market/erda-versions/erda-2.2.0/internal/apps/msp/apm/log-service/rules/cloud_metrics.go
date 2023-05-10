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

package rules

// var (
// 	cloudProducts = []string{"ali_waf_access", "ali_api_gateway_access"}
// 	cloudMetrics  = map[string]*pb.MetricMeta{
// 		"ali_waf_access": &pb.MetricMeta{
// 			Name: pb.NameDefine{
// 				Key:  "ali_waf_access",
// 				Name: "WAF Access",
// 			},
// 			Tags: map[string]*pb.TagDefine{
// 				"ua_os": &pb.TagDefine{
// 					Key:  "ua_os",
// 					Name: "UA OS",
// 				},
// 				"remote_addr": &pb.TagDefine{
// 					Key:  "remote_addr",
// 					Name: "Remote Addr",
// 				},
// 				"request_method": &pb.TagDefine{
// 					Key:  "request_method",
// 					Name: "Request Method",
// 				},
// 				"sls_log_store": &pb.TagDefine{
// 					Key:  "sls_log_store",
// 					Name: "SLS Log Store",
// 				},
// 				"ua_browser_type": &pb.TagDefine{
// 					Key:  "ua_browser_type",
// 					Name: "UA Browser Type",
// 				},
// 				"org_name": &pb.TagDefine{
// 					Key:  "org_name",
// 					Name: "Org Name",
// 				},
// 				"server_protocol": &pb.TagDefine{
// 					Key:  "server_protocol",
// 					Name: "Server Protocol",
// 				},
// 				"status": &pb.TagDefine{
// 					Key:  "status",
// 					Name: "Status",
// 				},
// 				"host": &pb.TagDefine{
// 					Key:  "host",
// 					Name: "Host",
// 				},
// 				"http_referer": &pb.TagDefine{
// 					Key:  "http_referer",
// 					Name: "HTTP Referer",
// 				},
// 				"https": &pb.TagDefine{
// 					Key:  "https",
// 					Name: "Https",
// 				},
// 				"upstream_addr": &pb.TagDefine{
// 					Key:  "upstream_addr",
// 					Name: "Upstream Addr",
// 				},
// 				"http_error": &pb.TagDefine{
// 					Key:  "http_error",
// 					Name: "HTTP Error",
// 				},
// 				"http_x_forwarded_for": &pb.TagDefine{
// 					Key:  "http_x_forwarded_for",
// 					Name: "HTTP X Forwarded For",
// 				},
// 				"product": &pb.TagDefine{
// 					Key:  "product",
// 					Name: "Product",
// 				},
// 				"real_client_ip": &pb.TagDefine{
// 					Key:  "real_client_ip",
// 					Name: "Real Client IP",
// 				},
// 				"sls_topic": &pb.TagDefine{
// 					Key:  "sls_topic",
// 					Name: "SLS Topic",
// 				},
// 				"ua_browser_family": &pb.TagDefine{
// 					Key:  "ua_browser_family",
// 					Name: "UA Browser Family",
// 				},
// 				"upstream_response_time": &pb.TagDefine{
// 					Key:  "upstream_response_time",
// 					Name: "Upstream Response Time",
// 				},
// 				"block_action": &pb.TagDefine{
// 					Key:  "block_action",
// 					Name: "Block Action",
// 				},
// 				"dice_org_name": &pb.TagDefine{
// 					Key:  "dice_org_name",
// 					Name: "Dice Org Name",
// 				},
// 				"request_path": &pb.TagDefine{
// 					Key:  "request_path",
// 					Name: "Request Path",
// 				},
// 				"request_traceid": &pb.TagDefine{
// 					Key:  "request_traceid",
// 					Name: "Request Traceid",
// 				},
// 				"dice_org_id": &pb.TagDefine{
// 					Key:  "dice_org_id",
// 					Name: "Dice Org ID",
// 				},
// 				"sls_source": &pb.TagDefine{
// 					Key:  "sls_source",
// 					Name: "SLS Source",
// 				},
// 				"status_type": &pb.TagDefine{
// 					Key:  "status_type",
// 					Name: "Status Type",
// 				},
// 				"wxbb_invalid_wua": &pb.TagDefine{
// 					Key:  "wxbb_invalid_wua",
// 					Name: "Wxbb Invalid Wua",
// 				},
// 				"http_user_agent": &pb.TagDefine{
// 					Key:  "http_user_agent",
// 					Name: "HTTP User Agent",
// 				},
// 				"sls_category": &pb.TagDefine{
// 					Key:  "sls_category",
// 					Name: "SLS Category",
// 				},
// 				"sls_project": &pb.TagDefine{
// 					Key:  "sls_project",
// 					Name: "SLS Project",
// 				},
// 				"ua_browser": &pb.TagDefine{
// 					Key:  "ua_browser",
// 					Name: "UA Browser",
// 				},
// 				"matched_host": &pb.TagDefine{
// 					Key:  "matched_host",
// 					Name: "Matched Host",
// 				},
// 				"origin": &pb.TagDefine{
// 					Key:  "origin",
// 					Name: "Origin",
// 				},
// 				"user_id": &pb.TagDefine{
// 					Key:  "user_id",
// 					Name: "User ID",
// 				},
// 				"ua_os_family": &pb.TagDefine{
// 					Key:  "ua_os_family",
// 					Name: "UA OS Family",
// 				},
// 				"wxbb_rule_id": &pb.TagDefine{
// 					Key:  "wxbb_rule_id",
// 					Name: "Wxbb Rule ID",
// 				},
// 				"wxbb_test": &pb.TagDefine{
// 					Key:  "wxbb_test",
// 					Name: "Wxbb Test",
// 				},
// 				"content_type": &pb.TagDefine{
// 					Key:  "content_type",
// 					Name: "Content Type",
// 				},
// 				"region": &pb.TagDefine{
// 					Key:  "region",
// 					Name: "Region",
// 				},
// 				"time": &pb.TagDefine{
// 					Key:  "time",
// 					Name: "Time",
// 				},
// 				"ua_device_type": &pb.TagDefine{
// 					Key:  "ua_device_type",
// 					Name: "UA Device Type",
// 				},
// 			},
// 			Fields: map[string]*pb.FieldDefine{
// 				"body_bytes_sent": &pb.FieldDefine{
// 					Key:  "body_bytes_sent",
// 					Type: "number",
// 					Name: "Body Bytes Sent",
// 					Unit: "",
// 				},
// 				"remote_port": &pb.FieldDefine{
// 					Key:  "remote_port",
// 					Type: "number",
// 					Name: "Remote Port",
// 					Unit: "",
// 				},
// 				"request_length": &pb.FieldDefine{
// 					Key:  "request_length",
// 					Type: "number",
// 					Name: "Request Length",
// 					Unit: "",
// 				},
// 				"request_time_msec": &pb.FieldDefine{
// 					Key:  "request_time_msec",
// 					Type: "number",
// 					Name: "Request Time Msec",
// 					Unit: "",
// 				},
// 				"status": &pb.FieldDefine{
// 					Key:  "status",
// 					Type: "number",
// 					Name: "Status",
// 					Unit: "",
// 				},
// 				"upstream_status": &pb.FieldDefine{
// 					Key:  "upstream_status",
// 					Type: "number",
// 					Name: "Upstream Status",
// 					Unit: "",
// 				},
// 				"count": &pb.FieldDefine{
// 					Key:  "count",
// 					Type: "number",
// 					Name: "Count",
// 					Unit: "",
// 				},
// 			},
// 		},
// 		"ali_api_gateway_access": &pb.MetricMeta{
// 			Name: pb.NameDefine{
// 				Key:  "ali_api_gateway_access",
// 				Name: "API Gateway Access",
// 			},
// 			Tags: map[string]*pb.TagDefine{
// 				"path": &pb.TagDefine{
// 					Key:  "path",
// 					Name: "Path",
// 				},
// 				"region": &pb.TagDefine{
// 					Key:  "region",
// 					Name: "Region",
// 				},
// 				"appName": &pb.TagDefine{
// 					Key:  "appName",
// 					Name: "App Name",
// 				},
// 				"consumerAppKey": &pb.TagDefine{
// 					Key:  "consumerAppKey",
// 					Name: "Consumer App Key",
// 				},
// 				"dice_org_name": &pb.TagDefine{
// 					Key:  "dice_org_name",
// 					Name: "Dice Org Name",
// 				},
// 				"httpMethod": &pb.TagDefine{
// 					Key:  "httpMethod",
// 					Name: "HTTP Method",
// 				},
// 				"providerAliUid": &pb.TagDefine{
// 					Key:  "providerAliUid",
// 					Name: "Provider Ali UID",
// 				},
// 				"requestHandleTime": &pb.TagDefine{
// 					Key:  "requestHandleTime",
// 					Name: "Request Handle Time",
// 				},
// 				"apiStageName": &pb.TagDefine{
// 					Key:  "apiStageName",
// 					Name: "API Stage Name",
// 				},
// 				"sls_project": &pb.TagDefine{
// 					Key:  "sls_project",
// 					Name: "SLS Project",
// 				},
// 				"responseHeaders": &pb.TagDefine{
// 					Key:  "responseHeaders",
// 					Name: "Response Headers",
// 				},
// 				"sls_category": &pb.TagDefine{
// 					Key:  "sls_category",
// 					Name: "SLS Category",
// 				},
// 				"exception": &pb.TagDefine{
// 					Key:  "exception",
// 					Name: "Exception",
// 				},
// 				"initialRequestId": &pb.TagDefine{
// 					Key:  "initialRequestId",
// 					Name: "Initial Request ID",
// 				},
// 				"sls_topic": &pb.TagDefine{
// 					Key:  "sls_topic",
// 					Name: "SLS Topic",
// 				},
// 				"statusCode": &pb.TagDefine{
// 					Key:  "statusCode",
// 					Name: "Status Code",
// 				},
// 				"statusCodeType": &pb.TagDefine{
// 					Key:  "statusCodeType",
// 					Name: "Status Code Type",
// 				},
// 				"apiGroupUid": &pb.TagDefine{
// 					Key:  "apiGroupUid",
// 					Name: "API Group UID",
// 				},
// 				"clientNonce": &pb.TagDefine{
// 					Key:  "clientNonce",
// 					Name: "Client Nonce",
// 				},
// 				"product": &pb.TagDefine{
// 					Key:  "product",
// 					Name: "Product",
// 				},
// 				"requestProtocol": &pb.TagDefine{
// 					Key:  "requestProtocol",
// 					Name: "Request Protocol",
// 				},
// 				"sls_source": &pb.TagDefine{
// 					Key:  "sls_source",
// 					Name: "SLS Source",
// 				},
// 				"http_error": &pb.TagDefine{
// 					Key:  "http_error",
// 					Name: "HTTP Error",
// 				},
// 				"domain": &pb.TagDefine{
// 					Key:  "domain",
// 					Name: "Domain",
// 				},
// 				"errorMessage": &pb.TagDefine{
// 					Key:  "errorMessage",
// 					Name: "Error Message",
// 				},
// 				"apiName": &pb.TagDefine{
// 					Key:  "apiName",
// 					Name: "API Name",
// 				},
// 				"appId": &pb.TagDefine{
// 					Key:  "appId",
// 					Name: "App ID",
// 				},
// 				"requestHeaders": &pb.TagDefine{
// 					Key:  "requestHeaders",
// 					Name: "Request Headers",
// 				},
// 				"apiGroupName": &pb.TagDefine{
// 					Key:  "apiGroupName",
// 					Name: "API Group Name",
// 				},
// 				"origin": &pb.TagDefine{
// 					Key:  "origin",
// 					Name: "Origin",
// 				},
// 				"requestQueryString": &pb.TagDefine{
// 					Key:  "requestQueryString",
// 					Name: "Request Query String",
// 				},
// 				"sls_log_store": &pb.TagDefine{
// 					Key:  "sls_log_store",
// 					Name: "SLS Log Store",
// 				},
// 				"apiStageUid": &pb.TagDefine{
// 					Key:  "apiStageUid",
// 					Name: "API Stage UID",
// 				},
// 				"dice_org_id": &pb.TagDefine{
// 					Key:  "dice_org_id",
// 					Name: "Dice Org ID",
// 				},
// 				"customTraceId": &pb.TagDefine{
// 					Key:  "customTraceId",
// 					Name: "Custom Trace ID",
// 				},
// 				"errorCode": &pb.TagDefine{
// 					Key:  "errorCode",
// 					Name: "Error Code",
// 				},
// 				"requestId": &pb.TagDefine{
// 					Key:  "requestId",
// 					Name: "Request ID",
// 				},
// 				"apiUid": &pb.TagDefine{
// 					Key:  "apiUid",
// 					Name: "API UID",
// 				},
// 				"clientIp": &pb.TagDefine{
// 					Key:  "clientIp",
// 					Name: "Client IP",
// 				},
// 				"requestBody": &pb.TagDefine{
// 					Key:  "requestBody",
// 					Name: "Request Body",
// 				},
// 				"responseBody": &pb.TagDefine{
// 					Key:  "responseBody",
// 					Name: "Response Body",
// 				},
// 				"instanceId": &pb.TagDefine{
// 					Key:  "instanceId",
// 					Name: "Instance ID",
// 				},
// 				"org_name": &pb.TagDefine{
// 					Key:  "org_name",
// 					Name: "Org Name",
// 				},
// 			},
// 			Fields: map[string]*pb.FieldDefine{
// 				"requestSize": &pb.FieldDefine{
// 					Key:  "requestSize",
// 					Type: "number",
// 					Name: "Request Size",
// 					Unit: "",
// 				},
// 				"responseSize": &pb.FieldDefine{
// 					Key:  "responseSize",
// 					Type: "number",
// 					Name: "Response Size",
// 					Unit: "",
// 				},
// 				"serviceLatency": &pb.FieldDefine{
// 					Key:  "serviceLatency",
// 					Type: "number",
// 					Name: "Service Latency",
// 					Unit: "",
// 				},
// 				"statusCode": &pb.FieldDefine{
// 					Key:  "statusCode",
// 					Type: "number",
// 					Name: "Status Code",
// 					Unit: "",
// 				},
// 				"totalLatency": &pb.FieldDefine{
// 					Key:  "totalLatency",
// 					Type: "number",
// 					Name: "Total Latency",
// 					Unit: "",
// 				},
// 				"count": &pb.FieldDefine{
// 					Key:  "count",
// 					Type: "number",
// 					Name: "Count",
// 					Unit: "",
// 				},
// 			},
// 		},
// 	}
// )

// func (a *Adapt) listCloudMetricNames() []*pb.NameDefine {
// 	var list []*pb.NameDefine
// 	for _, p := range cloudProducts {
// 		list = append(list, &cloudMetrics[p].Name)
// 	}
// 	return list
// }

// func (a *Adapt) getCloudMetricDefine(metric string) *pb.MetricMeta {
// 	return cloudMetrics[metric]
// }

// func (a *Adapt) listCloudMetricDefine() []*pb.MetricMeta {
// 	var list []*pb.MetricMeta
// 	for _, p := range cloudProducts {
// 		list = append(list, cloudMetrics[p])
// 	}
// 	return list
// }
