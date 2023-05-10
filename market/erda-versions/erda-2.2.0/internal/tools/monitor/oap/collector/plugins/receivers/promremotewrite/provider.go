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

package promremotewrite

import (
	"fmt"
	"math"
	"net/http"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/labstack/echo"
	pmodel "github.com/prometheus/common/model"
	"github.com/prometheus/prometheus/prompb"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
)

var providerName = plugins.WithPrefixReceiver("prometheus-remote-write")

type config struct {
}

// +provider
type provider struct {
	Cfg    *config
	Log    logs.Logger
	Router httpserver.Router `autowired:"http-router"`

	consumerFunc model.ObservableDataConsumerFunc
}

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	p.Router.POST("/api/v1/prometheus-remote-write", p.prwHandler)
	return nil
}

func (p *provider) prwHandler(ctx echo.Context) error {
	if p.consumerFunc == nil {
		return ctx.NoContent(http.StatusOK)
	}
	req := ctx.Request()
	buf, err := lib.ReadBody(req)
	if err != nil {
		return ctx.String(http.StatusInternalServerError, fmt.Sprintf("read body err: %s", err))
	}

	var wr prompb.WriteRequest
	err = proto.Unmarshal(buf, &wr)
	if err != nil {
		return ctx.String(http.StatusInternalServerError, fmt.Sprintf("unmarshal body err: %s", err))
	}

	now := time.Now() // receive time
	for _, ts := range wr.Timeseries {
		tags := map[string]string{}
		for _, l := range ts.Labels {
			tags[l.Name] = l.Value
		}
		metricName := tags[pmodel.MetricNameLabel]
		if metricName == "" {
			return fmt.Errorf("%q not found in tags or empty", pmodel.MetricNameLabel)
		}
		delete(tags, pmodel.MetricNameLabel)

		// set pmodel.JobLabel as  name
		job := tags[pmodel.JobLabel]
		if job == "" {
			return fmt.Errorf("%q not found in tags or empty", pmodel.JobLabel)
		}
		delete(tags, pmodel.JobLabel)

		for _, s := range ts.Samples {
			fields := make(map[string]interface{})
			if math.IsNaN(s.Value) {
				continue
			}
			fields[metricName] = s.Value

			// converting to metric
			t := now
			if s.Timestamp > 0 {
				t = time.Unix(0, s.Timestamp*1000000)
			}
			m := metric.Metric{
				Name:      job,
				Timestamp: t.UnixNano(),
				Tags:      tags,
				Fields:    fields,
			}
			p.consumerFunc(&m)
		}
	}

	return ctx.NoContent(http.StatusOK)
}

func (p *provider) RegisterConsumer(consumer model.ObservableDataConsumerFunc) {
	p.consumerFunc = consumer
}

func (p *provider) ComponentConfig() interface{} {
	return p.Cfg
}

func init() {
	servicehub.Register(providerName, &servicehub.Spec{
		Services: []string{
			providerName,
		},
		Description: "here is description of prometheus-remote-write",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
