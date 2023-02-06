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

package eventbox

import (
	"context"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	eventpb "github.com/erda-project/erda-proto-go/core/messenger/eventbox/pb"
	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/internal/core/legacy"
	"github.com/erda-project/erda/internal/core/legacy/services/dingtalk/api/interfaces"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/input/http"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/monitor"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/register"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/webhook"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct{}

type provider struct {
	DingtalkApiClient interfaces.DingTalkApiClientFactory `autowired:"dingtalk.api"`
	Messenger         pb.NotifyServiceServer

	Register        transport.Register `autowired:"service-register" optional:"true"`
	C               *config
	eventBoxService *eventBoxService
	Perm            perm.Interface          `autowired:"permission"`
	CoreService     legacy.ExposedInterface `autowired:"core-services"`
}

func (p *provider) Run(ctx context.Context) error {
	return Initialize(p)
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.messenger.eventbox.EventBoxService" || ctx.Type() == eventpb.EventBoxServiceServerType() || ctx.Type() == eventpb.EventBoxServiceHandlerType():
		return p.eventBoxService
	}
	return p
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.eventBoxService = &eventBoxService{}
	httpi, err := http.New()
	if err != nil {
		logrus.Error("HttpInput init is failed err is ", err)
		return err
	}
	wh, err := webhook.NewWebHookHTTP()
	if err != nil {
		logrus.Error("Webhook init is failed err is ", err)
		return err
	}
	wh.CoreService = p.CoreService
	mon, err := monitor.NewMonitorHTTP()
	if err != nil {
		logrus.Error("Monitor init is failed err is ", err)
		return err
	}
	reg, err := register.New()
	if err != nil {
		logrus.Error("Register init is failed err is ", err)
		return err
	}
	p.eventBoxService.HttpI = httpi
	p.eventBoxService.WebHookHTTP = wh
	p.eventBoxService.MonitorHTTP = mon
	p.eventBoxService.RegisterHTTP = register.NewHTTP(reg)

	if p.Register != nil {
		type EventBoxService = eventpb.EventBoxServiceServer
		eventpb.RegisterEventBoxServiceImp(p.Register, p.eventBoxService, apis.Options())
	}
	return nil
}

func init() {
	servicehub.Register("erda.core.messenger.eventbox", &servicehub.Spec{
		Services:             eventpb.ServiceNames(),
		Creator:              func() servicehub.Provider { return &provider{} },
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		Types:                eventpb.Types(),
		ConfigFunc: func() interface{} {
			return config{}
		},
	})
}
