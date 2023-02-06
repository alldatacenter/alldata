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

package browser

import (
	"bytes"
	"errors"
	"fmt"
	"net/url"
	"strconv"
	"strings"
	"time"

	jsoniter "github.com/json-iterator/go"
	"github.com/varstr/uaparser"

	"github.com/erda-project/erda/internal/apps/msp/apm/browser/timing"
	metrics "github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

var (
	other = "其他"
	json  = jsoniter.ConfigCompatibleWithStandardLibrary
)

const defaultLocation = "局域网" // LAN

func (p *provider) invoke(key []byte, value []byte, topic *string, timestamp time.Time) (err error) {
	parts := strings.SplitN(string(value), ",", 6)
	if len(parts) < 5 {
		return errors.New("invalid analytics event")
	}
	metric := &metrics.Metric{
		Tags:   map[string]string{},
		Fields: map[string]interface{}{},
	}

	data, err := url.ParseQuery(parts[4])
	if err != nil {
		return nil
	}

	t, err := strconv.ParseInt(data.Get("date"), 10, 64)
	if err != nil {
		return fmt.Errorf("analytics event error : invalid date %v", err)
	}

	metric.Timestamp = t * int64(time.Millisecond)
	metric.Tags["_meta"] = "true"
	metric.Tags["_metric_scope"] = "micro_service"
	metric.Tags["_metric_scope_id"] = parts[0]
	metric.Tags["tk"] = parts[0]
	metric.Tags["cid"] = parts[1]
	metric.Tags["uid"] = parts[2]
	metric.Tags["ip"] = parts[3]
	if len(parts) > 5 {
		metric.Tags["ai"] = parts[5]
	}
	dh := data.Get("dh")
	if len(dh) > 0 {
		metric.Tags["host"] = dh
	}
	dp, err := url.Parse(data.Get("dp"))
	if err == nil {
		metric.Tags["doc_path"] = getPath(dp.Path)
	}
	name := data.Get("t")
	switch name {
	case "req":
	case "ajax":
	case "request":
		err = p.handleRequest(metric, data)
		break
	case "timing":
		err = p.handleTiming(metric, data)
		break
	case "error":
		err = p.handleError(metric, data)
		break
	case "device":
		err = p.handleDevice(metric, data)
		break
	case "browser":
		err = p.handleBrowser(metric, data)
		break
	case "document":
		err = p.handleDocument(metric, data)
		break
	case "script":
		err = p.handleScript(metric, data)
		break
	case "event":
		err = p.handleEvent(metric, data)
		break
	case "metric":
		err = p.handleCustomMetric(metric, data)
	default:
		return fmt.Errorf("analytics event error : unknown t=%s in data", name)
	}
	if err != nil {
		return err
	}
	return p.output.Write(metric)
}

func (p *provider) handleCustomMetric(metric *metrics.Metric, data url.Values) error {
	name := data.Get("n")
	name = strings.Replace(name, "-", "_", -1)
	name = strings.Replace(name, ".", "_", -1)
	name = strings.Replace(name, " ", "", -1)
	name = strings.Replace(name, "\t", "", -1)
	metric.Name = "ta_metric_" + name
	if tags, ok := data["tag"]; ok {
		for _, tag := range tags {
			idx := strings.Index(tag, "=")
			if idx <= 0 {
				continue
			}
			key := tag[0:idx]
			val := url.QueryEscape(tag[idx+1:])
			if len(key) <= 0 {
				continue
			}
			metric.Tags[key] = val
		}
	}
	if fields, ok := data["field"]; ok {
		for _, field := range fields {
			idx := strings.Index(field, "=")
			if idx <= 0 {
				continue
			}
			key := field[0:idx]
			val := url.QueryEscape(field[idx+1:])
			if len(key) <= 0 {
				continue
			}
			var value interface{} = val
			if len(val) > 0 {
				if val == "true" || val == "True" {
					value = true
				} else if val == "false" || val == "False" {
					value = false
				} else if strings.HasPrefix(val, "\"") && strings.HasSuffix(val, "\"") {
					text, err := strconv.Unquote(val) // "x\"xx" -> x"xx
					if err == nil {
						value = text
					}
				} else if strings.HasPrefix(val, "'") && strings.HasSuffix(val, "'") {
					value = val[1 : len(val)-1] // 直接移除前后的 单引号
				} else {
					last := len(val) - 1
					t := val[last]
					switch t {
					case 'i':
						i, err := strconv.ParseInt(val[:last], 10, 64)
						if err == nil {
							value = i
						}
					case 'b': // 主要用于 0、1 转换为 bool 的情况
						b, err := strconv.ParseBool(val[:last])
						if err == nil {
							value = b
						}
					case 'f':
						f, err := strconv.ParseFloat(val[:last], 64)
						if err == nil {
							value = f
						}
					default: // 没有特殊后缀的，尽量将 filed 转换为浮点数
						f, err := strconv.ParseFloat(val, 64)
						if err == nil {
							value = f
						}
					}
				}

			}
			metric.Fields[key] = value
		}
	}
	return nil
}

func (p *provider) handleEvent(metric *metrics.Metric, data url.Values) error {
	metric.Name = "ta_event"
	metric.Fields["count"] = 1
	if appendMobileInfoIfNeed(metric, data) {
		metric.Tags["en"] = data.Get("en")
		metric.Tags["ei"] = data.Get("ei")
	}
	metric.Tags["x"] = data.Get("x")
	metric.Tags["y"] = data.Get("y")
	metric.Tags["xp"] = data.Get("xp")
	metric.Fields["x"] = parseInt64(data.Get("x"), 0)
	metric.Fields["y"] = parseInt64(data.Get("y"), 0)
	return nil
}

func (p *provider) handleScript(metric *metrics.Metric, data url.Values) error {
	metric.Name = "ta_script"
	metric.Fields["count"] = 1
	if !appendMobileInfoIfNeed(metric, data) {
		ua := data.Get("ua")
		handleUA(ua, metric)
	}
	msg := data.Get("msg")
	if msg != "" {
		lines := strings.Split(msg, "\n")
		if len(lines) > 0 {
			metric.Tags["error"] = lines[0]
		}
	}
	metric.Tags["source"] = data.Get("source")
	metric.Tags["line_no"] = data.Get("lineno")
	metric.Tags["column_no"] = data.Get("colno")
	metric.Tags["message"] = data.Get("msg")
	return nil
}

func (p *provider) handleDocument(metric *metrics.Metric, data url.Values) error {
	metric.Name = "ta_document"
	metric.Fields["count"] = 1
	if !appendMobileInfoIfNeed(metric, data) {
		metric.Tags["ds"] = data.Get("ds")
		metric.Tags["dr"] = data.Get("dr")
		metric.Tags["de"] = data.Get("de")
		metric.Tags["dk"] = data.Get("dk")
		metric.Tags["dl"] = data.Get("dl")
	}
	metric.Tags["dt"] = data.Get("dt")
	metric.Tags["tp"] = data.Get("tp")
	metric.Tags["ck"] = data.Get("ck")
	metric.Tags["lc"] = data.Get("lc")
	return nil
}

func (p *provider) handleBrowser(metric *metrics.Metric, data url.Values) error {
	metric.Name = "ta_browser"
	metric.Fields["count"] = 1
	if appendMobileInfoIfNeed(metric, data) {
		metric.Tags["sr"] = "sr"
	} else {
		ua := data.Get("ua")
		handleUA(ua, metric)
		metric.Tags["ce"] = data.Get("ce")
		metric.Tags["vp"] = data.Get("vp")
		metric.Tags["ul"] = data.Get("ul")
		metric.Tags["sr"] = data.Get("sr")
		metric.Tags["sd"] = data.Get("sd")
		metric.Tags["fl"] = data.Get("fl")
	}
	return nil
}

func (p *provider) handleDevice(metric *metrics.Metric, data url.Values) error {
	metric.Name = "ta_device"
	metric.Fields["count"] = 1
	if appendMobileInfoIfNeed(metric, data) {
		metric.Tags["channel"] = data.Get("ch")
		jb := data.Get("jb")
		if jb == "1" {
			metric.Tags["jb"] = "true"
		} else {
			metric.Tags["jb"] = "false"
		}
		metric.Tags["cpu"] = data.Get("cpu")
		metric.Tags["sdk"] = data.Get("sdk")
		metric.Tags["sd"] = data.Get("sd")
		metric.Tags["mem"] = data.Get("mem")
		metric.Tags["rom"] = data.Get("rom")
	}
	metric.Tags["sr"] = data.Get("sr")
	metric.Tags["vid"] = data.Get("vid")
	return nil
}

func (p *provider) handleError(metric *metrics.Metric, data url.Values) error {
	metric.Name = "ta_error"
	metric.Fields["count"] = 1
	_ = appendMobileInfoIfNeed(metric, data)
	ua := data.Get("ua")
	handleUA(ua, metric)
	metric.Tags["source"] = data.Get("ers")
	metric.Tags["line_no"] = data.Get("erl")
	metric.Tags["column_no"] = data.Get("erc")
	metric.Tags["vid"] = data.Get("vid")
	metric.Tags["error"] = data.Get("erm")
	metric.Tags["stack_trace"] = data.Get("sta")

	return nil
}

func (p *provider) handleRequest(metric *metrics.Metric, data url.Values) error {
	metric.Name = "ta_req"
	_ = appendMobileInfoIfNeed(metric, data)
	metric.Fields["tt"] = parseInt64(data.Get("tt"), 0)
	metric.Fields["req"] = parseInt64(data.Get("req"), 0)
	metric.Fields["res"] = parseInt64(data.Get("res"), 0)
	statusCodeStr := data.Get("st")
	statusCode := parseInt64(statusCodeStr, 0)
	reqError := false
	if statusCode >= 400 {
		reqError = true
	}
	metric.Fields["errors"] = reqError
	metric.Fields["status"] = statusCode
	reqURL := data.Get("url")
	metric.Tags["url"] = reqURL
	metric.Tags["req_path"] = getPath(reqURL)
	metric.Tags["status_code"] = statusCodeStr
	metric.Tags["method"] = data.Get("me")
	return nil
}

func (p *provider) handleTiming(metric *metrics.Metric, data url.Values) error {
	metric.Name = "ta_timing"
	if appendMobileInfoIfNeed(metric, data) {
		metric.Fields["plt"] = parseInt64(data.Get("nt"), 0)
	} else {
		ua := data.Get("ua")
		handleUA(ua, metric)
		timingStr := data.Get("pt")
		var plt, act, dns, tcp, srt, net int64
		if timingStr == "" {
			nt := timing.ParseNavigationTiming(data.Get("nt"))
			plt = nt.LoadTime + nt.ReadyStart
			dns = nt.LookupDomainTime
			tcp = nt.ConnectTime
			srt = nt.RequestTime
			act = nt.AppCacheTime

			metric.Fields["rrt"] = nt.RedirectTime
			metric.Fields["put"] = nt.UnloadEventTime
			metric.Fields["rqt"] = nt.RequestTime - nt.ResponseTime
			metric.Fields["rpt"] = nt.ResponseTime
			metric.Fields["dit"] = nt.InitDomTreeTime
			metric.Fields["drt"] = nt.DomReadyTime
			metric.Fields["clt"] = nt.LoadEventTime
			metric.Fields["set"] = nt.ScriptExecuteTime
			metric.Fields["wst"] = nt.RedirectTime + nt.AppCacheTime + nt.LookupDomainTime + nt.ConnectTime + nt.RequestTime - nt.ResponseTime
			metric.Fields["fst"] = (nt.LoadTime + nt.ReadyStart) - (nt.InitDomTreeTime + nt.DomReadyTime + nt.LoadEventTime)
			metric.Fields["pct"] = (nt.LoadTime + nt.ReadyStart) - (nt.DomReadyTime + nt.LoadEventTime)
			metric.Fields["rct"] = (nt.LoadTime + nt.ReadyStart) - nt.LoadEventTime
		} else {
			pt := timing.ParsePerformanceTiming(timingStr)
			plt = pt.LoadEventEnd - pt.NavigationStart
			act = pt.DomainLookupStart - pt.FetchStart
			dns = pt.DomainLookupEnd - pt.DomainLookupStart
			tcp = pt.ConnectEnd - pt.ConnectStart
			srt = pt.ResponseStart - pt.RequestStart

			metric.Fields["put"] = pt.UnloadEventEnd - pt.UnloadEventStart
			metric.Fields["rrt"] = pt.RedirectEnd - pt.RedirectStart
			metric.Fields["rqt"] = pt.ResponseStart - pt.RequestStart
			metric.Fields["rqt"] = pt.ResponseEnd - pt.ResponseStart
			metric.Fields["dit"] = pt.DomInteractive - pt.ResponseEnd
			metric.Fields["drt"] = pt.DomComplete - pt.DomInteractive
			metric.Fields["clt"] = pt.LoadEventEnd - pt.LoadEventStart
			metric.Fields["set"] = pt.DomContentLoadedEventEnd - pt.DomContentLoadedEventStart
			metric.Fields["wst"] = pt.ResponseStart - pt.NavigationStart
			metric.Fields["fst"] = pt.FirstPaintTime
			metric.Fields["pct"] = (pt.LoadEventEnd - pt.FetchStart) + (pt.FetchStart - pt.NavigationStart) - ((pt.DomComplete - pt.DomInteractive) + (pt.LoadEventEnd - pt.LoadEventStart))
			metric.Fields["rct"] = (pt.LoadEventEnd - pt.FetchStart) + (pt.FetchStart - pt.NavigationStart) - (pt.LoadEventEnd - pt.LoadEventStart)
		}

		metric.Fields["plt"] = plt
		metric.Fields["act"] = act
		metric.Fields["dns"] = dns
		metric.Fields["tcp"] = tcp
		metric.Fields["srt"] = srt

		net = act + tcp + dns

		metric.Fields["net"] = net
		metric.Fields["prt"] = plt - srt - net

		rts, err := timing.ParseResourceTiming(data.Get("rt"))
		if err != nil {
			p.Log.Error(err)
		}
		if rts != nil {
			metric.Fields["rlt"] = rts.Timing()
			metric.Fields["rdc"] = rts.DNSCount()
		}
		if plt > 8000 {
			metric.Tags["slow"] = "true"
		}
	}
	ip := metric.Tags["ip"]
	if len(ip) > 0 && !strings.Contains(ip, ":") {
		if locationInfo, err := p.ipdb.Find(ip); err == nil {
			metric.Tags["country"] = locationInfo.Country
			metric.Tags["province"] = locationInfo.Region
			metric.Tags["city"] = locationInfo.City
		}
	}
	if len(metric.Tags["country"]) == 0 {
		metric.Tags["country"] = defaultLocation
	}
	if len(metric.Tags["province"]) == 0 {
		metric.Tags["province"] = defaultLocation
	}
	if len(metric.Tags["city"]) == 0 {
		metric.Tags["city"] = defaultLocation
	}

	for k, v := range metric.Fields {
		if val, ok := v.(int64); ok {
			if val < 0 {
				return fmt.Errorf("analytics event error : %s=%d is negative", k, val)
			}
		}
	}
	return nil
}

func appendMobileInfoIfNeed(metric *metrics.Metric, data url.Values) bool {
	ua := data.Get("ua")
	if isMobile(ua) {
		metric.Name = metric.Name + "_mobile"
		metric.Tags["type"] = "mobile"
		appendMobileInfo(metric, data)
		return true
	}
	metric.Tags["type"] = "browser"
	return false
}

func appendMobileInfo(metric *metrics.Metric, data url.Values) {
	metric.Tags["ns"] = data.Get("ns")
	metric.Tags["av"] = data.Get("av")
	metric.Tags["br"] = data.Get("br")
	metric.Tags["gps"] = data.Get("gps")
	metric.Tags["osv"] = data.Get("osv")
	os := data.Get("osn")
	if os == "" {
		metric.Tags["os"] = other
	} else {
		metric.Tags["os"] = os
	}
	device := data.Get("md")
	if device == "" {
		metric.Tags["device"] = other
	} else {
		metric.Tags["device"] = device
	}
}

func handleUA(ua string, metric *metrics.Metric) {
	if ua != "" {
		info := uaparser.Parse(ua)
		if info != nil {
			if info.Browser != nil {
				metric.Tags["browser"] = info.Browser.Name
				metric.Tags["browser_version"] = info.Browser.Version
			} else {
				metric.Tags["browser"] = other
				metric.Tags["browser_version"] = other
			}
			if info.OS != nil {
				metric.Tags["os"] = info.OS.Name
				metric.Tags["osv"] = info.OS.Version
			} else {
				metric.Tags["os"] = other
				metric.Tags["osv"] = other
			}
			if info.Device == nil {
				metric.Tags["device"] = other
			} else {
				metric.Tags["device"] = info.Device.Name
			}
		}
	}
}

func isMobile(ua string) bool {
	ua = strings.ToLower(ua)
	return ua == "ios" || ua == "android"
}

func getPath(s string) string {
	return replaceNumber(s)
}

func replaceNumber(path string) string {
	parts := strings.Split(path, "/")
	last := len(parts) - 1
	var buffer bytes.Buffer
	for i, item := range parts {
		if _, err := strconv.ParseInt(item, 10, 64); err == nil {
			buffer.WriteString("{number}")
		} else {
			buffer.WriteString(item)
		}
		if i != last {
			buffer.WriteString("/")
		}
	}
	return buffer.String()
}

func parseInt64(value string, def int64) int64 {
	if num, err := strconv.ParseInt(value, 10, 64); err == nil {
		return num
	}
	return def
}
