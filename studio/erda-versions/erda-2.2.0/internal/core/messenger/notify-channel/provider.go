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

package notify_channel

import (
	"os"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/messenger/notifychannel/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/messenger/notify-channel/db"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/ucauth"
)

type config struct{}

type provider struct {
	Cfg                 *config
	Log                 logs.Logger
	Register            transport.Register
	uc                  *ucauth.UCClient
	notifyChanelService *notifyChannelService
	bdl                 *bundle.Bundle
	DB                  *gorm.DB        `autowired:"mysql-client"`
	I18n                i18n.Translator `autowired:"i18n" translator:"cs-i18n"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithKMS(), bundle.WithCoreServices())
	ucClientId := os.Getenv("UC_CLIENT_ID")
	ucClientSecret := os.Getenv("UC_CLIENT_SECRET")
	p.uc = ucauth.NewUCClient(discover.UC(), ucClientId, ucClientSecret)
	if conf.OryEnabled() {
		ucDB, err := dao.NewDBClient()
		if err != nil {
			return err
		}
		oryKratosProvateAddr := os.Getenv("ORY_KRATOS_ADMIN_ADDR")
		p.uc = ucauth.NewUCClient(oryKratosProvateAddr, conf.OryCompatibleClientID(), conf.OryCompatibleClientSecret())
		p.uc.SetDBClient(ucDB.DB)
	}
	p.notifyChanelService = &notifyChannelService{
		p:               p,
		NotifyChannelDB: &db.NotifyChannelDB{DB: p.DB},
	}
	if p.Register != nil {
		pb.RegisterNotifyChannelServiceImp(p.Register, p.notifyChanelService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.messenger.notifychannel.NotifyChannelService" || ctx.Type() == pb.NotifyChannelServiceServerType() || ctx.Type() == pb.NotifyChannelServiceHandlerType():
		return p.notifyChanelService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.messenger.notifychannel", &servicehub.Spec{
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
