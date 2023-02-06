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

// __topic__	日志主题，固定为waf_access_log。
// acl_action	WAF精准访问控制规则行为，例如pass、drop、captcha等。
// 当值为空或短划线（-）时，也表示pass，即放行。

// acl_blocks	是否被精准访问控制规则拦截，其中：
// 1表示拦截。
// 其他值均表示通过。
// antibot	触发的爬虫风险管理防护策略类型，包括：
// ratelimit：频次控制
// sdk：APP端增强防护
// algorithm：算法模型
// intelligence：爬虫情报
// acl：精准访问控制
// blacklist：黑名单
// antibot_action	爬虫风险管理防护策略执行的操作，包括：
// challenge：嵌入JS进行验证
// drop：拦截
// report：记录
// captcha：滑块验证
// block_action	触发拦截的WAF防护类型，包括：
// tmd：CC攻击防护
// waf：Web应用攻击防护
// acl：精准访问控制
// geo：地区封禁
// antifraud：数据风控
// antibot：防爬封禁
// body_bytes_sent	发送给客户端的HTTP Body的字节数。
// cc_action	CC防护策略行为，例如none、challenge、pass、close、captcha、wait、login、n等。
// cc_blocks	是否被CC防护功能拦截， 其中：
// 1表示拦截。
// 其他值均表示通过。
// cc_phase	触发的CC防护策略，例如seccookie、server_ip_blacklist、static_whitelist、 server_header_blacklist、server_cookie_blacklist、server_args_blacklist、qps_overmax等。
// content_type	访问请求内容类型。
// host	源网站。
// http_cookie	访问来源客户端Cookie信息。
// http_referer	访问请求的来源URL信息。如果无来源URL信息，则显示短划线（-）。
// http_user_agent	请求头中的User Agent字段，一般包含来源客户端浏览器标识、操作系统标识等信息。
// http_x_forwarded_for	访问请求头部中带有的XFF头信息，用于识别通过HTTP代理或负载均衡方式连接到Web服务器的客户端最原始的IP地址。
// https	是否为HTTPS请求，其中：
// true：该请求是HTTPS请求。
// false：该请求是HTTP请求。
// matched_host	匹配到的源站，可能是泛域名。如果未匹配，则显示为短划线（-）。
// querystring	请求中的查询字符串。
// real_client_ip	访问客户的真实IP地址。如果获取不到，则显示为短划线（-）。
// region	WAF实例所属地域。
// remote_addr	请求连接的客户端IP地址。
// remote_port	请求连接的客户端端口号。
// request_length	请求长度，单位为字节。
// request_method	访问请求的HTTP请求方法。
// request_path	请求的相对路径（不包含查询字符串）。
// request_time_msec	访问请求时间，单位为毫秒。
// request_traceid	WAF记录的访问请求唯一ID标识。
// server_protocol	源站服务器响应的协议及版本号。
// status	WAF返回给客户端的HTTP响应状态信息。
// time	请求的发生时间。
// ua_browser	请求来源的浏览器信息。
// ua_browser_family	请求来源的浏览器系列。
// ua_browser_type	请求来源的浏览器类型。
// ua_browser_version	请求来源的浏览器版本。
// ua_device_type	请求来源客户端的设备类型。
// ua_os	请求来源客户端的操作系统信息。
// ua_os_family	请求来源客户端所属操作系统系列。
// upstream_addr	WAF使用的回源地址列表，格式为IP:Port，多个地址用逗号（,）分隔。
// upstream_ip	请求所对应的源站IP地址。例如，WAF回源到ECS，则该参数返回源站ECS的IP地址。
// upstream_response_time	源站响应WAF请求的时间，单位秒。如果返回短划线（-），表示响应超时。
// upstream_status	源站返回给WAF的响应状态。如果返回短划线（-），表示没有响应，例如该请求被WAF拦截或源站响应超时。
// user_id	阿里云账号ID。
// waf_action	Web攻击防护策略行为，其中：
// block表示拦截。
// bypass或其它值均表示放行。
// web_attack_type	Web攻击类型，例如xss、code_exec、webshell、sqli、lfilei、rfilei、other等。
// waf_rule_id	匹配的WAF的相关规则ID。
// cc_rule_id	CC攻击规则拦截ID。

func (c *Consumer) wafProcess(shardID int, groups *sls.LogGroupList) string {
	buf := &bytes.Buffer{}
	for _, group := range groups.LogGroups {
		for _, log := range group.Logs {
			timestamp := int64(log.GetTime()) * int64(time.Second)
			// 导入日志
			logTags := c.getTagsByContents(group, log.Contents, "request_traceid")
			logTags["product"] = "waf"
			c.outputs.es.Write(&elasticsearch.Document{
				Index: c.getIndex(c.project, timestamp),
				Data: &logs.Log{
					ID:        c.id,
					Source:    "sls",
					Stream:    "stdout",
					Offset:    (int64(shardID) << 32) + int64(log.GetTime()),
					Content:   getWAFContent(buf, log.Contents),
					Timestamp: timestamp,
					Tags:      logTags,
				},
			})

			tags := c.newMetricTags(group, "waf")
			// 导入指标
			m, err := getWAFMetrics(timestamp, tags, log.Contents)
			if err == nil {
				c.outputs.kafka.Write(m)
			}
		}
	}
	return ""
}

// getWafMetrics .
func getWAFMetrics(timestamp int64, tags map[string]string, contents []*sls.LogContent) (*metrics.Metric, error) {
	fields := make(map[string]interface{})
	fields["count"] = 1
	for _, kv := range contents {
		switch kv.GetKey() {
		case "status":
			if len(kv.GetValue()) > 0 {
				status, typ, err := parseHTTPStatus(kv.GetValue())
				if err != nil {
					return nil, err
				}
				fields[kv.GetKey()] = status
				if status >= http.StatusBadRequest {
					tags["http_error"] = "true"
				}
				tags["status_type"] = typ
				tags[kv.GetKey()] = kv.GetValue()
			}
		case "body_bytes_sent", "remote_port", "upstream_status":
			if len(kv.GetValue()) > 0 {
				val, err := strconv.ParseInt(kv.GetValue(), 10, 64)
				if err != nil {
					return nil, err
				}
				fields[kv.GetKey()] = val
			}
		case "request_time_msec", "request_length":
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
		Name:      "ali_waf_access",
		Timestamp: timestamp,
		Tags:      tags,
		Fields:    fields,
	}, nil
}

func getWAFContent(buf *bytes.Buffer, contents []*sls.LogContent) string {
	buf.Reset()
	var time, clientIP, host, method, requestPath, status, reqTime, respTime string
	for _, content := range contents {
		switch content.GetKey() {
		case "time":
			time = content.GetValue()
		case "real_client_ip":
			clientIP = content.GetValue()
		case "request_method":
			method = content.GetValue()
		case "host":
			host = content.GetValue()
		case "request_path":
			requestPath = content.GetValue()
		case "status":
			status = content.GetValue()
		case "request_time_msec":
			reqTime = content.GetValue()
		case "upstream_response_time":
			respTime = content.GetValue()
		}
	}
	buf.WriteString("[")
	buf.WriteString(time)
	buf.WriteString("] ")
	buf.WriteString(clientIP)
	buf.WriteString(" -> ")
	buf.WriteString(method)
	buf.WriteString(" ")
	buf.WriteString(host)
	buf.WriteString(" ")
	buf.WriteString(requestPath)
	buf.WriteString(" ")
	buf.WriteString(status)
	buf.WriteString(" ")
	buf.WriteString("req:")
	buf.WriteString(reqTime)
	buf.WriteString("ms")
	buf.WriteString(" ")
	buf.WriteString("resp:")
	buf.WriteString(respTime)
	buf.WriteString("ms")
	content := buf.Bytes()
	return string(content)
}
