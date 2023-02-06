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

package http

import (
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/apis"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/plugins"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/plugins/http/triggering"
)

type config struct {
	Headers http.Header `file:"headers"`
	Trace   struct {
		Enable bool `file:"enable" default:"true"`
		Rate   int  `file:"rate" default:"50"`
	} `file:"trace"`
}

// +provider
type provider struct {
	Cfg *config
	Log logs.Logger
}

func (p *provider) Init(ctx servicehub.Context) error { return nil }

func (p *provider) Validate(c *pb.Checker) error {
	urlstr := c.Config["url"].GetStringValue()
	if len(urlstr) <= 0 {
		return fmt.Errorf("url must not be empdy")
	}
	_, err := url.Parse(urlstr)
	if err != nil {
		return fmt.Errorf("invalid url: %s", err)
	}
	return nil
}

func (p *provider) New(c *pb.Checker) (plugins.Handler, error) {
	// method
	method := c.Config["method"].GetStringValue()
	if len(method) <= 0 {
		method = http.MethodGet
	}

	// url
	urlstr := c.Config["url"].GetStringValue()
	if len(urlstr) <= 0 {
		return nil, fmt.Errorf("url must not be empdy")
	}
	_, err := url.Parse(urlstr)
	if err != nil {
		return nil, fmt.Errorf("invalid url: %s", err)
	}

	// headers
	headers := http.Header{}
	headersStruct := c.Config["headers"].GetStructValue()
	headersStr, err := json.Marshal(headersStruct)
	if err != nil {
		return nil, fmt.Errorf("invalid headers: %s", err)
	}
	var hs map[string]string
	err = json.Unmarshal(headersStr, &hs)
	if err != nil {
		return nil, fmt.Errorf("invalid url: %s", err)
	}
	for k := range hs {
		headers.Add(k, hs[k])
	}

	// timeout
	var timeout time.Duration
	if val := c.Config["timeout"]; len(val.GetStringValue()) > 0 {
		timeout, err = time.ParseDuration(val.GetStringValue())
		if err != nil {
			return nil, fmt.Errorf("invalid timeout: %s", err)
		}
	}

	if timeout <= 0 {
		timeout = 5 * time.Second
	}

	// trace
	var trace bool
	if val := c.Config["trace"]; len(val.GetStringValue()) > 0 {
		trace, err = strconv.ParseBool(val.GetStringValue())
		if err != nil {
			return nil, fmt.Errorf("invalid trace: %s", err)
		}
	}

	// body
	content := ""
	bodyStruct := c.Config["body"].GetStructValue()
	if bodyStruct != nil {
		content = bodyStruct.Fields["content"].GetStringValue()
	}

	// retry
	retryCount := int64(c.Config["retry"].GetNumberValue())

	// interval
	interval := int64(c.Config["interval"].GetNumberValue())

	// triggering
	var triggers []*pb.Condition
	triggeringStruct := c.Config["triggering"].GetListValue()
	if triggeringStruct != nil {
		triggerBytes, err := json.Marshal(triggeringStruct)
		if err != nil {
			return nil, err
		}
		err = json.Unmarshal(triggerBytes, &triggers)
		if err != nil {
			return nil, err
		}
	}

	return &httpHandler{
		p:          p,
		tags:       c.Tags,
		method:     method,
		url:        urlstr,
		headers:    headers,
		body:       content,
		retry:      retryCount,
		triggering: triggers,
		interval:   interval,
		timeout:    timeout,
		trace:      trace,
	}, nil
}

type httpHandler struct {
	p          *provider
	tags       map[string]string
	method     string
	url        string
	headers    http.Header
	body       string
	retry      int64
	interval   int64
	triggering []*pb.Condition
	timeout    time.Duration
	trace      bool
}

func (h httpHandler) Do(ctx plugins.Context) error {
	select {
	case <-ctx.Done():
		return nil
	default:
	}

	tags, fields := make(map[string]string), make(map[string]interface{})
	for k, v := range h.tags {
		tags[k] = v
	}
	headers, err := json.Marshal(h.headers)
	if err != nil {
		return nil
	}

	tags["url"] = h.url
	tags["method"] = h.method
	tags["body"] = h.body
	tags["headers"] = string(headers)
	tags["retry_spec"] = strconv.FormatInt(h.retry, 10)
	var triggersBytes []byte
	if h.triggering != nil {
		triggersBytes, err = json.Marshal(h.triggering)
	}
	tags["interval"] = strconv.FormatInt(h.interval, 10)
	tags["triggering"] = string(triggersBytes)

	maybeCallCount := h.retry + 1
	for i := 0; i < int(maybeCallCount); i++ {

		fields["retry"] = i

		req, err := http.NewRequestWithContext(ctx, h.method, h.url, strings.NewReader(h.body))
		req.Header = h.headers

		if err != nil {
			fields["latency"] = 0
			checkerStatusMetric("2", apis.StatusRED, 601, tags, fields)
			continue
		}

		// create client with timeout
		client := &http.Client{
			Timeout: h.timeout,
			Transport: &http.Transport{
				Dial: func(netw, addr string) (net.Conn, error) {
					c, err := net.DialTimeout(netw, addr, h.timeout*3)
					if err != nil {
						return nil, err
					}
					c.SetDeadline(time.Now().Add(h.timeout))
					return c, nil
				},
			},
		}

		// do request
		start := time.Now()
		resp, err := client.Do(req)
		if err != nil {
			h.p.Log.Errorf("failed to do request for httpHandler (tags: %v)", tags)
			fields["latency"] = 0
			checkerStatusMetric("2", apis.StatusRED, 601, tags, fields)
			continue
		}

		latency := time.Now().Sub(start).Milliseconds()
		fields["latency"] = latency
		// strategy of triggering condition
		triggers := h.triggering
		resultStatus := true
		if resp != nil {
			for _, condition := range triggers {
				t := triggering.New(condition)
				resultStatus = t.Executor(resp) && resultStatus
			}
		}
		checkerStatusHandler(resultStatus, fields, tags, resp)

		if resultStatus {
			break
		}
	}

	err = ctx.Report(&plugins.Metric{
		Name:   "status_page",
		Tags:   tags,
		Fields: fields,
	})
	if err != nil {
		_ = fmt.Errorf("report metric failed. err: %s", err)
	}
	return nil
}

func checkerStatusHandler(resultStatus bool, fields map[string]interface{}, tags map[string]string, resp *http.Response) {
	if !resultStatus {
		checkerStatusMetric("2", apis.StatusRED, resp.StatusCode, tags, fields)
	} else {
		checkerStatusMetric("1", apis.StatusGreen, resp.StatusCode, tags, fields)
	}
	fields["triggering_status"] = resultStatus
}

func checkerStatusMetric(status, statusName string, statusCode int, tags map[string]string, fields map[string]interface{}) {
	fields["code"] = statusCode
	message := http.StatusText(statusCode)
	if message == "" {
		message = "Checker Request Failed"
	}
	fields["message"] = message
	tags["status"] = status
	tags["status_name"] = statusName
}

func init() {
	servicehub.Register("erda.msp.apm.checker.task.plugins.http", &servicehub.Spec{
		Services:   []string{"erda.msp.apm.checker.task.plugins.http"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
