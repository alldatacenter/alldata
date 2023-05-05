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

package bundle

import (
	"fmt"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/numeral"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	cpuMetric    = "cpu"
	memMetric    = "mem"
	memoryMetric = "memory"
	diskMetric   = "disk"
	loadMetric   = "system"
)

var metrics = []string{cpuMetric, memMetric, diskMetric, loadMetric}

func (b *Bundle) GetHostMetricInfo(clusterName string) (map[string]*apistructs.HostMetric, error) {
	hostMetrics := make(map[string]*apistructs.HostMetric)
	for _, metric := range metrics {
		metricResp, err := b.getHostMetric(clusterName, metric)
		if err != nil {
			return nil, err
		}
		for k, v := range metricResp {
			_, ok := hostMetrics[k]
			if !ok {
				hostMetrics[k] = new(apistructs.HostMetric)
			}
			switch metric {
			case cpuMetric:
				hostMetrics[k].CPU = v
			case memMetric:
				hostMetrics[k].Memory = v
			case diskMetric:
				hostMetrics[k].Disk = v
			case loadMetric:
				hostMetrics[k].Load = v
			}
		}
	}
	return hostMetrics, nil
}

func (b *Bundle) getHostMetric(clusterName, metric string) (map[string]float64, error) {
	host, err := b.urls.Monitor()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var dataType string
	switch metric {
	case cpuMetric:
		dataType = "usage_active"
	case memMetric:
		dataType = "used_percent"
	case diskMetric:
		dataType = "used_percent"
	case loadMetric:
		dataType = "load5"
	}

	// 只支持毫秒
	now := time.Now()
	start := strconv.FormatInt(now.Add(-5*time.Minute).UnixNano()/int64(time.Millisecond), 10)
	end := strconv.FormatInt(now.UnixNano()/int64(time.Millisecond), 10)

	var metricResp apistructs.HostMetricResponse
	request := hc.Get(host).Path(strutil.Concat("/api/metrics/charts/", metric)).
		Header("Internal-Client", "bundle").
		Param("filter_cluster_name", clusterName).
		Param("group", "host_ip").
		Param("avg", dataType).
		Param("limit", "1000").
		Param("start", start).
		Param("end", end)
	if metric == cpuMetric {
		request = request.Param("filter_cpu", "cpu-total")
	}

	resp, err := request.Do().JSON(&metricResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !metricResp.Success {
		return nil, toAPIError(resp.StatusCode(), metricResp.Error)
	}

	dataMapKey := "avg." + dataType
	hostMetrics := make(map[string]float64, 0)
	for _, result := range metricResp.Data.Results {
		if result.Name == metric {
			for _, data := range result.Data {
				if value, ok := data[dataMapKey]; ok {
					hostMetrics[value.Tag] = numeral.Round(value.Data, 2)
				}
			}
		}
	}
	return hostMetrics, nil
}

func (b *Bundle) GetLog(orgName string, req apistructs.DashboardSpotLogRequest) (*apistructs.DashboardSpotLogData, error) {
	host, err := b.urls.Monitor()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	request := hc.Get(host).Path("/api/logs").
		Header("Internal-Client", "bundle").
		Header("org", orgName).
		Param("count", strconv.FormatInt(req.Count, 10)).
		Param("id", req.ID).
		Param("stream", string(req.Stream)).
		Param("source", string(req.Source)).
		Param("start", strconv.FormatInt(int64(req.Start), 10)).
		Param("end", strconv.FormatInt(int64(req.End), 10))

	if req.Debug {
		request = request.Param("debug", "true")
	}

	var logResp apistructs.DashboardSpotLogResponse

	httpResp, err := request.Do().JSON(&logResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if !httpResp.IsOK() || !logResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), logResp.Error)
	}
	return &logResp.Data, nil
}

// GetProjectMetric 项目资源汇总信息
func (b *Bundle) GetProjectMetric(paramValues url.Values) (map[string]interface{}, error) {
	host, err := b.urls.Monitor()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	path := "/api/metrics/container_top/histogram"

	logrus.Infof("request project resource metric host:%s, url: %v", host, path)
	var data map[string]interface{}
	r, err := hc.Get(host).
		Path(path).
		Params(paramValues).
		Do().
		JSON(&data)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() {
		logrus.Errorf("request project resource metric err: %v", data)
		return nil, toAPIError(r.StatusCode(), apistructs.ErrorResponse{Msg: "failed to request project resource metrics"})
	}
	return data, nil
}

// GetProjectMetric 项目资源汇总信息
func (b *Bundle) MetricsRouting(pathRouting, name string, paramValues url.Values) (interface{}, error) {
	host, err := b.urls.Monitor()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var path string
	if strings.Contains(pathRouting, "histogram") {
		path = fmt.Sprintf("/api/metrics/%s/histogram", name)
	} else {
		path = fmt.Sprintf("/api/metrics/%s", name)
	}

	logrus.Infof("request metric host:%s, url: %v", host, path)
	var data map[string]interface{}
	r, err := hc.Get(host).
		Path(path).
		Params(paramValues).
		Do().
		JSON(&data)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() {
		logrus.Errorf("request project resource metric err: %v", data)
		return nil, toAPIError(r.StatusCode(), apistructs.ErrorResponse{Msg: "failed to request metrics"})
	}
	return data["data"], nil
}
