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

package notify

import (
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/mysql"
	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/messenger/notify/db"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
}

type provider struct {
	C             *config
	Register      transport.Register `autowired:"service-register" optional:"true"`
	notifyService *notifyService
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.notifyService = &notifyService{}
	p.notifyService.DB = db.New(ctx.Service("mysql").(mysql.Interface).DB())
	p.notifyService.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())
	if p.Register != nil {
		type NotifyService = pb.NotifyServiceServer
		pb.RegisterNotifyServiceImp(p.Register, p.notifyService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.messenger.notify.NotifyService" || ctx.Type() == pb.NotifyServiceServerType() || ctx.Type() == pb.NotifyServiceHandlerType():
		return p.notifyService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.messenger.notify", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
