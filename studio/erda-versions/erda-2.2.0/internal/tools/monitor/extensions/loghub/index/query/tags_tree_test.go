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

// func printLogTags(text string) {
// 	var tags map[string]string
// 	err := json.Unmarshal([]byte(text), &tags)
// 	if err != nil {
// 		fmt.Println(err)
// 		return
// 	}
// 	out := &bytes.Buffer{}
// 	for t := range tags {
// 		if strings.HasPrefix(t, "_") {
// 			continue
// 		}
// 		out.WriteString("map[string]interface{}{\n")
// 		out.WriteString(`	"tag": map[string]interface{}{` + "\n")
// 		out.WriteString(`		"name": "` + getFieldName(t) + `",` + "\n")
// 		out.WriteString(`		"key": "` + t + `",` + "\n")
// 		out.WriteString("	},\n")
// 		out.WriteString("},\n")
// 	}
// 	fmt.Println(string(out.Bytes()))
// }
//
// func Example_api_gateway_log_tags_print() {
// 	printLogTags(`{
// 	"__receive_time__": "1595725552",
// 	"_meta": "true",
// 	"apiGroupName": "ali_portal_prod",
// 	"apiGroupUid": "b79c6e9599674868a02758ea27604f0a",
// 	"apiName": "ali_portal_auth_apis_prod",
// 	"apiStageName": "RELEASE",
// 	"apiStageUid": "e4a749d7f08645408f2d5f30399a6c4e",
// 	"apiUid": "0544d387a1c349309f986492394c6e78",
// 	"appId": "110666723",
// 	"appName": "ali_portal_prod",
// 	"clientIp": "118.178.15.106",
// 	"clientNonce": "",
// 	"consumerAppKey": "203828444",
// 	"customTraceId": "",
// 	"dice_org_id": "1,",
// 	"dice_org_name": "terminus",
// 	"domain": "portal-gateway-prod.emflbxm.mobil.com.cn",
// 	"errorCode": "OK",
// 	"errorMessage": "",
// 	"exception": "",
// 	"httpMethod": "POST",
// 	"initialRequestId": "",
// 	"instanceId": "apigateway-cn-m7r1nul81002",
// 	"origin": "sls",
// 	"path": "/api/user/portal/sku/paging",
// 	"product": "apigateway",
// 	"providerAliUid": "1646165317123365",
// 	"region": "cn-shanghai",
// 	"requestBody": "",
// 	"requestHandleTime": "2020-07-26T01:05:41Z",
// 	"requestHeaders": "",
// 	"requestId": "C2AFA9F3-A817-4274-B975-BDC1E201449F",
// 	"requestProtocol": "HTTPS",
// 	"requestQueryString": "",
// 	"requestSize": "1487",
// 	"responseBody": "",
// 	"responseHeaders": "",
// 	"responseSize": "21023",
// 	"serviceLatency": "36",
// 	"sls_category": "",
// 	"sls_log_store": "api-gateway",
// 	"sls_project": "api-gateway-1646165317123365-cn-shanghai",
// 	"sls_source": "",
// 	"sls_topic": "",
// 	"statusCode": "200",
// 	"totalLatency": "43"
// 	}`)
//
// 	// Output:
// 	// TODO .
// }
//
// func Example_waf_log_tags_print() {
// 	printLogTags(`{
// 		"_meta": "true",
// 		"block_action": "",
// 		"body_bytes_sent": "20452",
// 		"content_type": "application/json;charset=UTF-8",
// 		"dice_org_id": "1,",
// 		"dice_org_name": "terminus",
// 		"host": "www.rewards.mobil.com.cn",
// 		"http_cookie": "acw_tc=76b20f4815956345630006275e7faadfb1ac40b6fc7f511ed99b4e7743ed95",
// 		"http_referer": "https://www.rewards.mobil.com.cn/Home/Mine",
// 		"http_user_agent": "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Mobile Safari/537.36",
// 		"http_x_forwarded_for": "-",
// 		"https": "on",
// 		"matched_host": "www.rewards.mobil.com.cn",
// 		"origin": "sls",
// 		"product": "waf",
// 		"real_client_ip": "113.90.232.206",
// 		"region": "cn",
// 		"remote_addr": "113.90.232.206",
// 		"remote_port": "38728",
// 		"request_length": "848",
// 		"request_method": "POST",
// 		"request_path": "/api/user/portal/sku/paging",
// 		"request_time_msec": "200",
// 		"request_traceid": "781bad0615956403408504374e7496",
// 		"server_protocol": "HTTP/1.1",
// 		"sls_category": "",
// 		"sls_log_store": "waf-logstore",
// 		"sls_project": "waf-project-1646165317123365-cn-hangzhou",
// 		"sls_source": "waf_access_log",
// 		"sls_topic": "waf_access_log",
// 		"status": "200",
// 		"time": "2020-07-25T09:25:41+08:00",
// 		"ua_browser": "chrome_mobile",
// 		"ua_browser_family": "chrome",
// 		"ua_browser_type": "mobile_browser",
// 		"ua_browser_version": "70.0.3538.25",
// 		"ua_device_type": "mobile",
// 		"ua_os": "android6",
// 		"ua_os_family": "android",
// 		"upstream_addr": "47.101.19.191:443",
// 		"upstream_response_time": "0.199",
// 		"upstream_status": "200",
// 		"user_id": "1646165317123365",
// 		"wxbb_invalid_wua": "",
// 		"wxbb_rule_id": "",
// 		"wxbb_test": ""
// 	}`)
//
// 	// Output:
// 	// TODO .
// }
