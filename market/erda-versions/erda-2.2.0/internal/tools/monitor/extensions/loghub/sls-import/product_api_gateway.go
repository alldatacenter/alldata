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

package slsimport

import (
	"bytes"
	"net/http"
	"strconv"
	"time"

	sls "github.com/aliyun/aliyun-log-go-sdk"

	"github.com/erda-project/erda-infra/providers/elasticsearch"
	logs "github.com/erda-project/erda/internal/tools/monitor/core/log"
	metrics "github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

// apiGroupUid	API分组ID
// apiGroupName	API分组名称
// apiUid	API ID
// apiName	API名称
// apiStageUid	API环境ID
// apiStageName	API环境名称
// httpMethod	调用的HTTP方法
// path	请求的路径
// domain	调用的域名
// statusCode	HTTP的状态码
// errorMessage	错误信息
// appId	调用者应用ID
// appName	调用者应用名称
// clientIp	调用者客户端的IP地址
// exception	后端返回的具体错误信息
// providerAliUid	API提供者的帐户ID
// region	地域，例如：cn-hangzhou
// requestHandleTime	请求时间，格林威治时间
// requestId	请求ID，全局唯一
// requestSize	请求大小，单位：字节
// responseSize	返回数据大小，单位：字节
// serviceLatency	后端延迟时间，单位：毫秒

func (c *Consumer) apiGatewayProcess(shardID int, groups *sls.LogGroupList) string {
	buf := &bytes.Buffer{}
	for _, group := range groups.LogGroups {
		for _, log := range group.Logs {
			timestamp := int64(log.GetTime()) * int64(time.Second)
			// 导入日志
			logTags := c.getTagsByContents(group, log.Contents, "customTraceId")
			logTags["product"] = "apigateway"
			c.outputs.es.Write(&elasticsearch.Document{
				Index: c.getIndex(c.project, timestamp),
				Data: &logs.Log{
					ID:        c.id,
					Source:    "sls",
					Stream:    "stdout",
					Offset:    (int64(shardID) << 32) + int64(log.GetTime()),
					Content:   getAPIGatewayContent(buf, log.Contents),
					Timestamp: timestamp,
					Tags:      logTags,
				},
			})

			tags := c.newMetricTags(group, "apigateway")
			metric, err := getAPIGatewayMetrics(timestamp, tags, log.Contents)
			if err == nil {
				c.outputs.kafka.Write(metric)
			}
		}
	}
	return ""
}

// getAPIGatewayMetrics .
func getAPIGatewayMetrics(timestamp int64, tags map[string]string, contents []*sls.LogContent) (*metrics.Metric, error) {
	fields := make(map[string]interface{})
	fields["count"] = 1
	for _, kv := range contents {
		switch kv.GetKey() {
		case "statusCode":
			if len(kv.GetValue()) > 0 {
				status, typ, err := parseHTTPStatus(kv.GetValue())
				if err != nil {
					return nil, err
				}
				fields[kv.GetKey()] = status
				if status >= http.StatusBadRequest {
					tags["http_error"] = "true"
				}
				tags["statusCodeType"] = typ
				tags[kv.GetKey()] = kv.GetValue()
			}
		case "requestSize", "responseSize":
			if len(kv.GetValue()) > 0 {
				val, err := strconv.ParseInt(kv.GetValue(), 10, 64)
				if err != nil {
					return nil, err
				}
				fields[kv.GetKey()] = val
			}
		case "serviceLatency", "totalLatency":
			if len(kv.GetValue()) > 0 {
				val, err := strconv.ParseFloat(kv.GetValue(), 64)
				if err != nil {
					return nil, err
				}
				fields[kv.GetKey()] = val
			}
		default:
			tags[kv.GetKey()] = kv.GetValue()
		}
	}
	return &metrics.Metric{
		Name:      "ali_api_gateway_access",
		Timestamp: timestamp,
		Tags:      tags,
		Fields:    fields,
	}, nil
}

func getAPIGatewayContent(buf *bytes.Buffer, contents []*sls.LogContent) string {
	buf.Reset()
	var time, clientIP, domain, method, requestPath, status, latency string
	for _, content := range contents {
		switch content.GetKey() {
		case "requestHandleTime":
			time = content.GetValue()
		case "clientIp":
			clientIP = content.GetValue()
		case "httpMethod":
			method = content.GetValue()
		case "domain":
			domain = content.GetValue()
		case "path":
			requestPath = content.GetValue()
		case "statusCode":
			status = content.GetValue()
		case "serviceLatency":
			latency = content.GetValue()
		}
	}
	buf.WriteString("[")
	buf.WriteString(time)
	buf.WriteString("] ")
	buf.WriteString(clientIP)
	buf.WriteString(" -> ")
	buf.WriteString(method)
	buf.WriteString(" ")
	buf.WriteString(domain)
	buf.WriteString(" ")
	buf.WriteString(requestPath)
	buf.WriteString(" ")
	buf.WriteString(status)
	buf.WriteString(" latency:")
	buf.WriteString(latency)
	buf.WriteString("ms")
	content := buf.Bytes()
	return string(content)
}
