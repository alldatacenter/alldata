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

package components

import (
	"embed"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	componentprotocol "github.com/erda-project/erda-infra/providers/component-protocol"
	"github.com/erda-project/erda-infra/providers/component-protocol/protocol"
	"github.com/erda-project/erda-infra/providers/i18n"
	messenger "github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/common"

	_ "github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-detail"
	_ "github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-list"
	_ "github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-overview"
	_ "github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-notify-detail"
	_ "github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-notify-list"
)

//go:embed scenarios
var scenarioFS embed.FS

type config struct {
}

type provider struct {
	Cfg *config
	Log logs.Logger

	Protocol componentprotocol.Interface
	CPTran   i18n.I18n `autowired:"i18n"`

	MonitorAlertService monitorpb.AlertServiceServer `autowired:"erda.core.monitor.alert.AlertService"`
	Metric              metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`

	bdl       *bundle.Bundle
	Messenger messenger.NotifyServiceServer `autowired:"erda.core.messenger.notify.NotifyService"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.Protocol.SetI18nTran(p.CPTran)
	p.Protocol.WithContextValue(common.ContextKeyServiceMonitorMetricService, p.Metric)
	p.Protocol.WithContextValue(common.ContextKeyServiceMonitorAlertService, p.MonitorAlertService)
	p.Protocol.WithContextValue(common.ContextKeyServiceMessengerService, p.Messenger)
	p.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())
	p.Protocol.WithContextValue(common.ContextKeyCoreServicesUrl, p.bdl)
	protocol.MustRegisterProtocolsFromFS(scenarioFS)
	return nil
}

func init() {
	servicehub.Register("msp-alert-components", &servicehub.Spec{
		Services:    []string{"msp-alert-components"},
		Description: "msp-alert-components",
		ConfigFunc:  func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
