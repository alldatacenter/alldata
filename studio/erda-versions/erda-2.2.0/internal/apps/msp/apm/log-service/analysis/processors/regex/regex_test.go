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

package regex

import (
	"encoding/json"
	"testing"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/log-service/analysis/processors"
)

func Test_Process_With_ValidParams_Should_Success(t *testing.T) {
	var (
		metricName = "test_metric"
	)
	cfg, _ := json.Marshal(map[string]interface{}{
		"pattern": "(\\d+)",
		"keys": []*pb.FieldDefine{
			{
				Key:  "ip",
				Type: "string",
			},
		},
		"appendTags": map[string]string{
			"append_tag_1": "value_1",
		},
		"replaceKey": map[string]string{
			"level": "_level",
		},
	})
	p, _ := New(metricName, cfg)

	metric, fields, appendTags, replaceKey, err := p.(processors.Processor).Process("123")
	if metric != metricName {
		t.Errorf("Process error, expect metricName: %s, but got %s", metricName, metric)
	}
	if len(fields) == 0 {
		t.Errorf("fields should not empty")
	}
	if len(appendTags) == 0 {
		t.Errorf("appendTags should not empty")
	}
	if len(replaceKey) == 0 {
		t.Errorf("replaceKey should not empty")
	}
	metric, fields, appendTags, replaceKey, err = p.Process("abc")
	if err != ErrNotMatch {
		t.Errorf("should miss match")
	}

}

func Test_Process_With_InvalidLenOfKeys_Should_Fail(t *testing.T) {
	var (
		metricName = "test_metric"
	)
	cfg, _ := json.Marshal(map[string]interface{}{
		"pattern": "(\\d+)",
		"keys": []*pb.FieldDefine{
			{
				Key:  "ip",
				Type: "string",
			},
			{
				Key:  "extra",
				Type: "string",
			},
		},
		"appendTags": map[string]string{
			"append_tag_1": "value_1",
		},
	})
	p, _ := New(metricName, cfg)

	_, _, _, _, err := p.(processors.Processor).Process("abc")
	if err != ErrNotMatch {
		t.Errorf("should miss match")
	}
}

func Test_Process_With_InvalidTypeOfKeys_Should_Fail(t *testing.T) {
	var (
		metricName = "test_metric"
	)
	cfg, _ := json.Marshal(map[string]interface{}{
		"pattern": "(\\S+)",
		"keys": []*pb.FieldDefine{
			{
				Key:  "ip",
				Type: "int",
			},
		},
		"appendTags": map[string]string{
			"append_tag_1": "value_1",
		},
	})
	p, _ := New(metricName, cfg)

	_, _, _, _, err := p.(processors.Processor).Process("abc")
	if err != ErrNotMatch {
		t.Errorf("should miss match")
	}
}

//
// import (
// 	"encoding/json"
// 	"fmt"
// 	"reflect"
//
// )
//
// func testAndPrint(keys []*pb.FieldDefine, metric, pattern, content string) {
// 	cfg, _ := json.Marshal(map[string]interface{}{
// 		"pattern": pattern,
// 		"keys":    keys,
// 	})
// 	p, err := New(metric, cfg)
// 	if err != nil {
// 		fmt.Println(err)
// 		return
// 	}
// 	name, fields, err := p.Process(content)
// 	if err != nil {
// 		fmt.Println(err)
// 		return
// 	}
// 	fmt.Println("name: ", name)
// 	for _, key := range keys {
// 		val := fields[key.Key]
// 		typ := reflect.TypeOf(val)
// 		fmt.Printf("%s (%s) = %v\n", key.Key, typ.Kind(), val)
// 	}
// }
//
// func ExampleProcessor() {
// 	keys := []*pb.FieldDefine{
// 		&pb.FieldDefine{
// 			Key:  "ip",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "time",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "method",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "url",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "request_time",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "request_length",
// 			Type: "number",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "status",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "length",
// 			Type: "number",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "ref_url",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "browser",
// 			Type: "string",
// 		},
// 	}
// 	testAndPrint(
// 		keys,
// 		"metric_name",
// 		"([\\d\\.]+) \\S+ \\S+ \\[(\\S+) \\S+\\] \"(\\w+) ([^\\\"]*)\" ([\\d\\.]+) (\\d+) (\\d+) (\\d+|-) \"([^\\\"]*)\" \"([^\\\"]*)\"",
// 		"10.200.0.101 - - [10/Aug/2017:14:57:51 +0800] \"POST /PutData?Category=YunOsAccountOpLog&AccessKeyId=abcdef&Date=Fri%2C%2028%20Jun%202013%2006%3A53%3A30%20GMT&Topic=raw&Signature=defg HTTP/1.1\" 0.024 18204 200 37 \"-\" \"aliyun-sdk-java\"",
// 	)
//
// 	// Output:
// 	// name:  metric_name
// 	// ip (string) = 10.200.0.101
// 	// time (string) = 10/Aug/2017:14:57:51
// 	// method (string) = POST
// 	// url (string) = /PutData?Category=YunOsAccountOpLog&AccessKeyId=abcdef&Date=Fri%2C%2028%20Jun%202013%2006%3A53%3A30%20GMT&Topic=raw&Signature=defg HTTP/1.1
// 	// request_time (string) = 0.024
// 	// request_length (float64) = 18204
// 	// status (string) = 200
// 	// length (float64) = 37
// 	// ref_url (string) = -
// 	// browser (string) = aliyun-sdk-java
// }
//
// func ExampleProcessor_nginx() {
// 	keys := []*pb.FieldDefine{
// 		&pb.FieldDefine{
// 			Key:  "userIp",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "scheme",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "host",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "requestLine",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "status",
// 			Type: "number",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "latency",
// 			Type: "number",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "requestSize",
// 			Type: "number",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "responseSize",
// 			Type: "number",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "referer",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "ua",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "upstreamAddr",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "upstreamStatus",
// 			Type: "number",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "upstreamLatency",
// 			Type: "number",
// 		},
// 	}
// 	testAndPrint(
// 		keys,
// 		"metric_name",
// 		`^[^\t]*\t([^\t,]*)[^\t]*\t[^\t]*\t([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)\t([^\t]*)\t[^\t]*\t[^\t]*\t[^\t]*\t([^\t]*)\t([^\t,]*)[^\t]*\t([^\t,]*)[^\t]*\t.*`,
// 		"[21/Jul/2020:15:39:24 +0800]\t127.0.0.1, 42.120.75.141\t10.118.183.0\thttp\tterminus-org.dev.terminus.io\tGET /api/deployments/actions/list-pending-approval?pageSize=15&type=BUILD&pageNo=1 HTTP/1.1\t200\t4.608\t1307\t2309\thttp://terminus-org.dev.terminus.io/workBench/approval/my-approve/pending?operator[]=1000019&pageNo=1&type=BUILD\tMozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Safari/537.36\t-\t-\t-\t10.96.239.163:9529\t200\t4.608\t20571\t1",
// 	)
//
// 	// Output:
// 	// name:  metric_name
// 	// userIp (string) = 127.0.0.1
// 	// scheme (string) = http
// 	// host (string) = terminus-org.dev.terminus.io
// 	// requestLine (string) = GET /api/deployments/actions/list-pending-approval?pageSize=15&type=BUILD&pageNo=1 HTTP/1.1
// 	// status (float64) = 200
// 	// latency (float64) = 4.608
// 	// requestSize (float64) = 1307
// 	// responseSize (float64) = 2309
// 	// referer (string) = http://terminus-org.dev.terminus.io/workBench/approval/my-approve/pending?operator[]=1000019&pageNo=1&type=BUILD
// 	// ua (string) = Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Safari/537.36
// 	// upstreamAddr (string) = 10.96.239.163:9529
// 	// upstreamStatus (float64) = 200
// 	// upstreamLatency (float64) = 4.608
// }
//
// func ExampleProcessor_tomcat() {
// 	keys := []*pb.FieldDefine{
// 		&pb.FieldDefine{
// 			Key:  "remoteIP",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "time",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "method",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "path",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "version",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "httpCode",
// 			Type: "number",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "group1",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "group2",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "ua",
// 			Type: "string",
// 		},
// 	}
// 	testAndPrint(
// 		keys,
// 		"metric_name",
// 		`^(?P<remoteIP>\S+) \S+ \S+ \[(?P<time>[\w:\/]+\s[+\-]\d{4})\] "(?P<method>\S+)\s?(?P<path>\S+)?\s?(?P<version>\S+)?" (?P<httpCode>\d{3}|-) (\d+|-)\s?"?([^"]*)"?\s?"?(?P<client>[^"]*)?"?$`,
// 		`11.11.11.11 - - [25/Jan/2000:14:00:01 +0100] "GET /1986.js HTTP/1.1" 200 932 "-" "Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.9.1.7) Gecko/20091221 Firefox/3.5.7 GTB6"`,
// 	)
//
// 	// Output:
// 	// name:  metric_name
// 	// remoteIP (string) = 11.11.11.11
// 	// time (string) = 25/Jan/2000:14:00:01 +0100
// 	// method (string) = GET
// 	// path (string) = /1986.js
// 	// version (string) = HTTP/1.1
// 	// httpCode (float64) = 200
// 	// group1 (string) = 932
// 	// group2 (string) = -
// 	// ua (string) = Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.9.1.7) Gecko/20091221 Firefox/3.5.7 GTB6
// }
//
// func ExampleProcessor_t() {
// 	// tmall-fe
// 	keys := []*pb.FieldDefine{
// 		&pb.FieldDefine{
// 			Key:  "time",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "level",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "method",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "url",
// 			Type: "string",
// 		},
// 		&pb.FieldDefine{
// 			Key:  "status",
// 			Type: "number",
// 		},
// 	}
// 	testAndPrint(
// 		keys,
// 		"metric_name",
// 		`^\[([^\]]+)\] (\S+) \[[^\]]+\] Proxy request \[([^:]+):([^\]]+)\] .* \[(\d+)\].+$`,
// 		"[Sun Jul 26 2020 12:41:01 GMT+0800 (China Standard Time)] WARNING [Middleware proxy] Proxy request [GET:/api/tick?count=5] get error status [404] ",
// 	)
//
// 	// Output:
// 	// name:  metric_name
// 	// time (string) = Sun Jul 26 2020 12:41:01 GMT+0800 (China Standard Time)
// 	// level (string) = WARNING
// 	// method (string) = GET
// 	// url (string) = /api/tick?count=5
// 	// status (float64) = 404
// }
