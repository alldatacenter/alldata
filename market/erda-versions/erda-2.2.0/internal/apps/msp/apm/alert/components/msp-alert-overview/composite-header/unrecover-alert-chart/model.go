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

package unrecover_alert_chart

import (
	"context"
	"encoding/json"

	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-overview/common"
)

type SimpleChart struct {
	Type                string                       `json:"type"`
	Data                Data                         `json:"data"`
	Metric              metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService" json:"-"`
	MonitorAlertService monitorpb.AlertServiceServer `autowired:"erda.core.monitor.alert.AlertService" json:"-"`

	sdk      *cptype.SDK
	inParams common.InParams
}

type Data struct {
	Main         string `json:"main"`
	Sub          string `json:"sub"`
	CompareText  string `json:"compareText"`
	CompareValue string `json:"compareValue"`
	Chart        Chart  `json:"chart"`
}

type Chart struct {
	XAxis  []string     `json:"xAxis"`
	Series []SeriesData `json:"series"`
}

type SeriesData struct {
	Name string `json:"name"`
	Data []int  `json:"data"`
}

func (s *SimpleChart) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(s)
	if err != nil {
		return err
	}
	return json.Unmarshal(b, &c)
}

func (s *SimpleChart) InitFromProtocol(ctx context.Context, c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	err = json.Unmarshal(b, s)
	if err != nil {
		return err
	}

	s.Metric = common.GetMonitorMetricServiceFromContext(ctx)
	s.MonitorAlertService = common.GetMonitorAlertServiceFromContext(ctx)
	s.sdk = cputil.SDK(ctx)
	var inParams common.InParams
	err = mapstructure.Decode(s.sdk.InParams, &inParams)
	if err != nil {
		return err
	}
	s.inParams = inParams
	return nil
}
