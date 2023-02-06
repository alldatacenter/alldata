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

package report

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/pkg/errors"
)

type ReportClient struct {
	CFG        *config
	HttpClient *http.Client
}

type MetricReport interface {
	SetCFG(cfg *config)
	Send(in []*Metric) error
	CreateReportClient(addr, username, password string) *ReportClient
}

type NamedMetrics struct {
	Name    string
	Metrics Metrics
}

type Metrics []*Metric

func (c *ReportClient) SetCFG(cfg *config) {
	c.CFG = cfg
}

func (c *ReportClient) CreateReportClient(addr, username, password string) *ReportClient {
	return &ReportClient{
		CFG: &config{
			ReportConfig: ReportConfig{
				Collector: CollectorConfig{
					Addr:     addr,
					UserName: username,
					Password: password,
				},
			},
		}, HttpClient: new(http.Client),
	}
}

func (c *ReportClient) Send(in []*Metric) error {
	groups := c.group(in)
	for _, group := range groups {
		if len(group.Metrics) == 0 {
			continue
		}
		requestBuffer, err := c.serialize(group)
		if err != nil {
			continue
		}
		for i := 0; i < c.CFG.ReportConfig.Collector.Retry; i++ {
			if err = c.write(group.Name, requestBuffer); err == nil {
				break
			}
			fmt.Printf("%s E! Retry %d # report in to collector error %s \n", time.Now().Format("2006-01-02 15:04:05"), i, err.Error())
		}
	}
	return nil
}

func (c *ReportClient) serialize(group *NamedMetrics) (io.Reader, error) {
	requestContent, err := json.Marshal(map[string]interface{}{group.Name: group.Metrics})
	if err != nil {
		return nil, err
	}
	base64Content := make([]byte, base64.StdEncoding.EncodedLen(len(requestContent)))
	base64.StdEncoding.Encode(base64Content, requestContent)
	return CompressWithGzip(bytes.NewBuffer(base64Content))
}

func (c *ReportClient) group(in []*Metric) []*NamedMetrics {
	metrics := &NamedMetrics{
		Name:    "metrics",
		Metrics: make([]*Metric, 0),
	}
	trace := &NamedMetrics{
		Name:    "trace",
		Metrics: make([]*Metric, 0),
	}
	errorG := &NamedMetrics{
		Name:    "error",
		Metrics: make([]*Metric, 0),
	}
	for _, m := range in {
		switch m.Name {
		case "trace":
		case "span":
			trace.Metrics = append(trace.Metrics, m)
			break
		case "error":
			errorG.Metrics = append(errorG.Metrics, m)
			break
		default:
			metrics.Metrics = append(metrics.Metrics, m)
		}
	}
	return []*NamedMetrics{metrics, trace, errorG}
}

func (c *ReportClient) write(name string, requestBuffer io.Reader) error {
	req, err := http.NewRequest(http.MethodPost, c.formatRoute(name), requestBuffer)
	if err != nil {
		return err
	}
	req.Header.Set("Content-Encoding", "gzip")
	req.Header.Set("Custom-Content-Encoding", "base64")
	req.Header.Set("Content-Type", "application/json")
	if len(c.CFG.ReportConfig.Collector.UserName) > 0 {
		req.SetBasicAuth(c.CFG.ReportConfig.Collector.UserName, c.CFG.ReportConfig.Collector.Password)
	}
	resp, err := c.HttpClient.Do(req)
	if err != nil {
		return err
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		err = errors.Errorf("when writing to [%s] received status code: %d\n", c.formatRoute(name), resp.StatusCode)
	}
	defer func() {
		if err := resp.Body.Close(); err != nil {
			fmt.Printf("%s error! close response body error %s\n", time.Now().Format("2006-01-02 15:04:05"), err)
		}
	}()
	return err
}

func (c *ReportClient) formatRoute(name string) string {
	return fmt.Sprintf("http://%s/collect/%s", c.CFG.ReportConfig.Collector.Addr, name)
}
