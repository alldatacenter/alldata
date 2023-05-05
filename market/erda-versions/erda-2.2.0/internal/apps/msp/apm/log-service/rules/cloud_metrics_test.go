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

// import (
// 	"bytes"
// 	"encoding/json"
// 	"fmt"
// 	"strings"
// 	"unicode"
// )

// func printCloudMetricsDefine(text string) {
// 	var m pb.Metric
// 	err := json.Unmarshal([]byte(text), &m)
// 	if err != nil {
// 		fmt.Println(err)
// 		return
// 	}

// 	name: "Application Cache Error"
// 	tags:
// 		cluster_name:
// 			name: "Cluster Name"
// 	fields:
// 		elapsed_count:
// 			type: "number"
// 			name: "Elapsed Count"
// 			uint:

// 	out := &bytes.Buffer{}
// 	out.WriteString("&pb.MetricMeta{\n")
// 	out.WriteString("	Name: pb.NameDefine{\n")
// 	out.WriteString(`		Key:  "` + m.Name + `",` + "\n")
// 	out.WriteString(`		Name:  "` + getMetricName(m.Name) + `",` + "\n")
// 	out.WriteString("	},\n")
// 	out.WriteString("	Tags: map[string]*pb.TagDefine{\n")
// 	for t := range m.Tags {
// 		if strings.HasPrefix(t, "_") {
// 			continue
// 		}
// 		out.WriteString(`		"` + t + `": &pb.TagDefine{` + "\n")
// 		out.WriteString(`			Key:  "` + t + `",` + "\n")
// 		out.WriteString(`			Name:  "` + getFieldName(t) + `",` + "\n")
// 		out.WriteString("		},\n")
// 	}
// 	out.WriteString("	},\n")
// 	out.WriteString("	Fields: map[string]*pb.FieldDefine{\n")
// 	for f, v := range m.Fields {
// 		if strings.HasPrefix(f, "_") {
// 			continue
// 		}
// 		out.WriteString(`		"` + f + `": &pb.FieldDefine{` + "\n")
// 		out.WriteString(`			Key:  "` + f + `",` + "\n")
// 		out.WriteString(`			Type:  "` + getFieldType(v) + `",` + "\n")
// 		out.WriteString(`			Name:  "` + getFieldName(f) + `",` + "\n")
// 		out.WriteString(`			Unit:  "",` + "\n")
// 		out.WriteString("		},\n")
// 	}
// 	out.WriteString("	},\n")
// 	out.WriteString("}\n")
// 	fmt.Println(string(out.Bytes()))
// }

// func printCloudMetricsDefine(text string) {
// 	var m pb.Metric
// 	err := json.Unmarshal([]byte(text), &m)
// 	if err != nil {
// 		fmt.Println(err)
// 		return
// 	}
// 	out := &bytes.Buffer{}
// 	out.WriteString(m.Name + ".yml\n")
// 	out.WriteString("name: \"" + getMetricName(m.Name) + "\"\n")
// 	out.WriteString("tags:\n")
// 	for t := range m.Tags {
// 		if strings.HasPrefix(t, "_") {
// 			continue
// 		}
// 		out.WriteString("    " + t + ":\n")
// 		out.WriteString("        name: \"" + getFieldName(t) + "\"\n")
// 	}
// 	for f, v := range m.Fields {
// 		if strings.HasPrefix(f, "_") {
// 			continue
// 		}
// 		out.WriteString("    " + f + ":\n")
// 		out.WriteString("        name: \"" + getFieldName(f) + "\"\n")
// 		out.WriteString("        type: \"" + getFieldType(v) + "\"\n")
// 		out.WriteString("        unit:\n")
// 	}
// 	out.WriteString("\n")
// 	fmt.Println(string(out.Bytes()))
// }

// func getMetricName(name string) string {
// 	if strings.HasPrefix(name, "ali_") {
// 		name = name[len("ali_"):]
// 	}
// 	var parts []string
// 	for _, n := range strings.Split(name, "_") {
// 		if len(n) <= 0 {
// 			continue
// 		}
// 		parts = append(parts, ToTitle(n))
// 	}
// 	return strings.Join(parts, " ")
// }

// func ToTitle(name string) string {
// 	switch strings.ToLower(name) {
// 	case "api", "id", "ip", "uid", "http", "sls", "waf", "ua", "os":
// 		return strings.ToUpper(name)
// 	}
// 	return strings.Title(name)
// }

// func getFieldName(text string) string {
// 	var parts []string
// 	for _, name := range strings.Split(text, "_") {
// 		names := []rune(name)
// 		j, i, flag, l := 0, 0, 0, len(names)
// 		for ; i < l; i++ {
// 			c := names[i]
// 			if unicode.IsUpper(c) {
// 				if flag == 0 {
// 					flag = 2
// 					continue
// 				}
// 				if flag >= 2 {
// 					flag++
// 					continue
// 				}
// 				parts = append(parts, ToTitle(string(names[j:i])))
// 				j = i
// 				flag = 2
// 			} else {
// 				if flag > 2 {
// 					parts = append(parts, ToTitle(string(names[j:i-1])))
// 					j = i - 1
// 				}
// 				flag = 1
// 			}
// 		}
// 		if j < l {
// 			parts = append(parts, ToTitle(string(names[j:])))
// 		}
// 	}
// 	return strings.Join(parts, " ")
// }

// func getFieldType(v interface{}) string {
// 	switch v.(type) {
// 	case string:
// 		return "string"
// 	}
// 	return "number"
// }

// func Example_api_gateway_define_print() {
// 	printCloudMetricsDefine(`
// 	{
// 		"name":"ali_api_gateway_access",
// 		"timestamp":1595985120000000000,
// 		"tags":{
// 			"__receive_time__":"1595985128",
// 			"_meta":"true",
// 			"apiGroupName":"ibm_api_dev",
// 			"apiGroupUid":"2f2d6aba41b6476ca01a4bf03d9afb09",
// 			"apiName":"API_Sync_Push",
// 			"apiStageName":"RELEASE",
// 			"apiStageUid":"bbedaf503a654a349230ff16e6f12dbb",
// 			"apiUid":"7335f1cce41c4de3b6aea7530f98a538",
// 			"appId":"",
// 			"appName":"",
// 			"clientIp":"120.27.173.34",
// 			"clientNonce":"",
// 			"consumerAppKey":"",
// 			"customTraceId":"",
// 			"dice_org_id":"1,",
// 			"dice_org_name":"terminus",
// 			"domain":"ibm-api-dev.nonprd-emflbxm.mobil.com.cn",
// 			"errorCode":"OK",
// 			"errorMessage":"",
// 			"exception":"",
// 			"httpMethod":"POST",
// 			"initialRequestId":"",
// 			"instanceId":"api-shared-vpc-001",
// 			"org_name":"terminus",
// 			"origin":"sls",
// 			"path":"/task-impl-center/v1/ibm/datalake/ingestion/sync/push",
// 			"product":"apigateway",
// 			"providerAliUid":"1646165317123365",
// 			"region":"cn-shanghai",
// 			"requestBody":"",
// 			"requestHandleTime":"2020-07-29T01:12:00Z",
// 			"requestHeaders":"",
// 			"requestId":"42AF06B4-FF47-4F7E-A360-8C557D96701F",
// 			"requestProtocol":"HTTPS",
// 			"requestQueryString":"",
// 			"responseBody":"",
// 			"responseHeaders":"",
// 			"sls_category":"",
// 			"sls_log_store":"api-gateway",
// 			"sls_project":"api-gateway-1646165317123365-cn-shanghai",
// 			"sls_source":"",
// 			"sls_topic":"",
// 			"statusCode":"200",
// 			"statusCodeType":"2XX",
// 			"http_error":"true"
// 		},
// 		"fields":{
// 			"requestSize":1403,
// 			"responseSize":623,
// 			"serviceLatency":4,
// 			"statusCode":200,
// 			"totalLatency":5
// 		},
// 		"@timestamp":1595985120000
// 	}
// 	`)

// 	// Output:
// 	// TODO .
// }

// func Example_waf_define_print() {
// 	printCloudMetricsDefine(`
// 	{
// 		"name":"ali_waf_access",
// 		"timestamp":1595980800000000000,
// 		"tags":{
// 			"_meta":"true",
// 			"block_action":"",
// 			"content_type":"application/json",
// 			"dice_org_id":"1,",
// 			"dice_org_name":"terminus",
// 			"host":"eventbox.tpaas.mobil.com.cn",
// 			"http_referer":"-",
// 			"http_user_agent":"Java/1.8.0_242",
// 			"http_x_forwarded_for":"-",
// 			"https":"on",
// 			"matched_host":"*.tpaas.mobil.com.cn",
// 			"org_name":"terminus",
// 			"origin":"sls",
// 			"product":"waf",
// 			"real_client_ip":"47.103.124.194",
// 			"region":"cn",
// 			"remote_addr":"47.103.124.194",
// 			"request_method":"POST",
// 			"request_path":"/api/dice/eventbox/message/create",
// 			"request_traceid":"76b20f6915959808004198680e3711",
// 			"server_protocol":"HTTP/1.1",
// 			"sls_category":"",
// 			"sls_log_store":"waf-logstore",
// 			"sls_project":"waf-project-1646165317123365-cn-hangzhou",
// 			"sls_source":"waf_access_log",
// 			"sls_topic":"waf_access_log",
// 			"status":"200",
// 			"status_type":"2XX",
// 			"time":"2020-07-29T08:00:00+08:00",
// 			"ua_browser":"bot",
// 			"ua_browser_family":"robot/spider",
// 			"ua_browser_type":"robot",
// 			"ua_device_type":"unknown",
// 			"ua_os":"unknown",
// 			"ua_os_family":"unknown",
// 			"upstream_addr":"47.103.164.238:443",
// 			"upstream_response_time":"0.030",
// 			"user_id":"1646165317123365",
// 			"wxbb_invalid_wua":"",
// 			"wxbb_rule_id":"",
// 			"wxbb_test":"",
// 			"http_error":"true"
// 		},
// 		"fields":{
// 			"body_bytes_sent":4767,
// 			"remote_port":33559,
// 			"request_length":466,
// 			"request_time_msec":31,
// 			"status":200,
// 			"upstream_status":200
// 		},
// 		"@timestamp":1595980800000
// 	}
// 	`)

// 	// Output:
// 	// TODO .
// }
