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

package grabber

import (
	"encoding/json"
	"fmt"
	"reflect"
	"strings"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/services/cms"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/internal/tools/monitor/extensions/cloud/aliyun/metrics/cloudcat/api"
	gl "github.com/erda-project/erda/internal/tools/monitor/extensions/cloud/aliyun/metrics/cloudcat/globals"
)

// var (
// 	ErrDifferentMetaMetrics = errors.New("metaMetrics has been changed")
// )

type Grabber struct {
	Name        string
	Namespace   string
	index       int // the index in Scheduler.grabbers
	orgId       string
	done        chan struct{}
	pipe        chan []*api.Metric
	interval    time.Duration // 采集时间间隔
	metaMetrics []cms.Resource
}

func New(namespace string, interval time.Duration, orgId string, index int) (g *Grabber, err error) {
	name, done := "", make(chan struct{})
	switch namespace {
	case "": // cms itself
		name = "cms"
	case "waf":
		name = "waf"
	default:
		s := strings.Split(namespace, "_")
		if len(s) < 2 || s[0] != "acs" {
			return nil, errors.Errorf("invalid namespace %s\n", namespace)
		}
		name = s[1]
	}
	g = &Grabber{
		Name:      name,
		Namespace: namespace,
		interval:  interval,
		index:     index,
		orgId:     orgId,
		done:      done,
	}
	_, err = g.loadMetaMetrics()
	return
}

func (g *Grabber) String() string {
	return fmt.Sprintf("%dth grabber<%s> of <%s>", g.index, g.Name, g.orgId)
}

func (g *Grabber) Subscribe(pipe chan []*api.Metric) {
	g.pipe = pipe
}

func (g *Grabber) Gather() {
	for {
		gl.Log.Infof("grabber %s start to get data...", g)
		for _, item := range g.metaMetrics {
			go func(meta cms.Resource) {
				data, err := api.GetDescribeMetricLast(g.orgId, meta.Namespace, meta.MetricName)
				if err != nil {
					switch err {
					case api.ErrEmptyResults:
						// ignore
					default:
						gl.Log.Infof("gather failed of %s. err=%s name=%s metricName=%s", g, err, g.Name, meta.MetricName)
						return
					}
				}
				if len(data) == 0 {
					return
				}
				metric := g.toMetrics(data, meta)
				if metric != nil {
					g.pipe <- metric
				}
			}(item)
		}

		select {
		case <-g.done:
			return
		case <-time.After(g.interval):
		}
	}
}

func (g *Grabber) toMetrics(batch []string, meta cms.Resource) []*api.Metric {
	res := make([]*api.Metric, 0, len(batch))
	for _, dp := range batch {
		res = append(res, g.extractDataPoints(dp, meta)...)
	}
	return res
}

func (g *Grabber) extractDataPoints(dp string, meta cms.Resource) (res []*api.Metric) {
	defer func() {
		if err := recover(); err != nil {
			gl.Log.Errorf("extract failed. err=%s", err)
			res = nil
		}
		return
	}()
	var all []map[string]interface{}
	err := json.Unmarshal([]byte(dp), &all)
	if err != nil {
		gl.Log.Errorf("extract datapoints %s faild. err=%s", dp, err)
		return nil
	}

	leftTags := func(source map[string]interface{}, excludes []string) {
		excludes = append(excludes, "timestamp")
		for _, item := range excludes {
			delete(source, item)
		}
	}

	res = make([]*api.Metric, 0, len(all))
	for i := 0; i < len(all); i++ {
		data := all[i]
		m := &api.Metric{
			Name:      "aliyun" + "_" + g.Name,
			Timestamp: uint64(data["timestamp"].(float64)) * 1e6,
			Tags:      map[string]string{},
			Fields:    map[string]interface{}{},
		}

		switch {
		case strings.Contains(meta.Statistics, "Average"):
			m.Fields[meta.MetricName] = data["Average"]
		case strings.Contains(meta.Statistics, "Value"):
			m.Fields[meta.MetricName] = data["Value"]
		case strings.Contains(meta.Statistics, "Maximum"):
			m.Fields[meta.MetricName] = data["Maximum"]
		case strings.Contains(meta.Statistics, "Minimum"):
			m.Fields[meta.MetricName] = data["Minimum"]
		default:
			if len(meta.Statistics) == 0 {
				continue
			}
			// logrus.Infof("Uncovered Statistics %s; data %+v", meta.Statistics, data)
			idx := strings.Index(meta.Statistics, ",")
			if idx > 0 {
				m.Fields[meta.MetricName] = data[meta.Statistics[:idx]]
			} else {
				m.Fields[meta.MetricName] = data[meta.Statistics]
			}
		}
		// left fields as tags
		leftTags(data, strings.Split(meta.Statistics, ","))
		for k, v := range data {
			m.Tags[k] = fmt.Sprintf("%v", v)
		}
		res = append(res, m)
	}
	return res
}

func (g *Grabber) loadMetaMetrics() (same bool, err error) {
	data, err := api.ListMetricMeta(g.orgId, g.Namespace)
	if err != nil {
		return false, err
	}

	same = reflect.DeepEqual(g.metaMetrics, data)
	g.metaMetrics = data
	return
}

// 1h, 2h, 4h, 8h, 16h
func (g *Grabber) Sync(notify chan int) {
	growing := 1
	for {
		select {
		case <-g.done:
			return
		case <-time.After(time.Duration(growing) * time.Hour):
		}

		same, err := g.loadMetaMetrics()
		if !same {
			notify <- g.index // send index to scheduler
		}
		if err != nil {
			gl.Log.Errorf("loadMetaMetrics error. err=%s grabber=%s", err, g.Name)
			continue
		}
		if growing < 16 {
			growing = growing * 2
		}
	}
}

func (g *Grabber) Close() error {
	close(g.done)
	return nil
}
