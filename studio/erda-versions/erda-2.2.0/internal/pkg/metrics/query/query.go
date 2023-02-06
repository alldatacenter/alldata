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
	"errors"
	"io/ioutil"
	"net/http"
	"net/url"
	"strings"
	"time"
)

var (
	metricApiV1 = "api/metrics"
)

type MetricQuery interface {
	QueryMetric(req *MetricQueryRequest) (*MetricQueryResponse, error)

	SetTimeout(duration time.Duration)
}

type queryClient struct {
	endpoint string
	timeout  time.Duration

	httpClient *http.Client
}

func (client *queryClient) SetTimeout(duration time.Duration) {
	client.httpClient.Timeout = duration
}

func (client *queryClient) QueryMetric(req *MetricQueryRequest) (*MetricQueryResponse, error) {
	response := &MetricQueryResponse{}
	err := client.doAction(req, response)
	return response, err
}

func (client *queryClient) doAction(req *MetricQueryRequest, resp *MetricQueryResponse) error {
	if req.diagram == "histogram" && req.point == 0 {
		return errors.New("point must be limit when diagram is histogram")
	}
	// 1. marshal QueryMetricRequest to http.Request
	// 2. wait for response
	// 3. unmarshal http.Response to QueryMetricResponse
	path := []string{metricApiV1, req.scope}
	if req.diagram != "" {
		path = append(path, req.diagram)
	}
	dst, err := url.Parse(client.endpoint)
	if err != nil {
		return err
	}
	dst.Path = strings.Join(path, "/")
	dst.RawQuery = req.ConstructParam().Encode()

	request, err := http.NewRequest("GET", dst.String(), nil)
	if err != nil {
		return err
	}
	response, err := client.httpClient.Do(request)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	return marshalResponse(response, resp)
}

func marshalResponse(response *http.Response, resp *MetricQueryResponse) error {
	body, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return err
	}
	resp.StatusCode = response.StatusCode
	if resp.StatusCode != http.StatusOK {
		serverError := NewServerError(body)
		return serverError
	}
	resp.Body = body
	return nil
}
