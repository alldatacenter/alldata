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

package personal_workbench

import (
	"embed"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	componentprotocol "github.com/erda-project/erda-infra/providers/component-protocol"
	"github.com/erda-project/erda-infra/providers/component-protocol/protocol"
	"github.com/erda-project/erda-infra/providers/i18n"
	menupb "github.com/erda-project/erda-proto-go/msp/menu/pb"
	projectpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/bundle"
	_ "github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/services/workbench"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

//go:embed component-protocol/scenarios
var scenarioFS embed.FS

//go:embed i18n
var i18nFS embed.FS

type Config struct {
	Debug bool `default:"false" env:"DEBUG" desc:"enable debug logging"`
}

type provider struct {
	Config Config

	Log              logs.Logger
	Protocol         componentprotocol.Interface
	CPTran           i18n.I18n                      `autowired:"i18n@personal-workbench"`
	Tran             i18n.Translator                `translator:"common"`
	TenantProjectSvc projectpb.ProjectServiceServer `autowired:"erda.msp.tenant.project.ProjectService"`
	MenuSvc          menupb.MenuServiceServer       `autowired:"erda.msp.menu.MenuService"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.Log.Info("init personal-workbench")
	bdl := bundle.New(
		bundle.WithHepa(),
		bundle.WithOrchestrator(),
		bundle.WithGittar(),
		bundle.WithDOP(),
		bundle.WithMSP(),
		bundle.WithPipeline(),
		bundle.WithMonitor(),
		bundle.WithCollector(),
		bundle.WithHTTPClient(httpclient.New(
			httpclient.WithTimeout(time.Second*15, time.Duration(conf.BundleTimeoutSecond())*time.Second), // bundle 默认 (time.Second, time.Second*3)
		)),
		bundle.WithKMS(),
		bundle.WithCoreServices(),
		bundle.WithHTTPClient(
			httpclient.New(
				httpclient.WithTimeout(time.Second, time.Second*90),
				httpclient.WithEnableAutoRetry(false),
			)),
	)
	if err := p.CPTran.RegisterFilesFromFS("i18n", i18nFS); err != nil {
		return err
	}
	p.Protocol.SetI18nTran(p.CPTran)
	wb := workbench.New(workbench.WithBundle(bdl), workbench.WithProjectSvc(p.TenantProjectSvc), workbench.WithMenuSvc(p.MenuSvc))
	p.Protocol.WithContextValue(types.Workbench, wb)
	p.Protocol.WithContextValue(types.GlobalCtxKeyBundle, bdl)
	protocol.MustRegisterProtocolsFromFS(scenarioFS)
	p.Log.Info("init component-protocol done")
	return nil
}

func init() {
	servicehub.Register("service.personal-workbench", &servicehub.Spec{
		Services:    []string{"personal-workbench"},
		Description: "erda personal workbench",
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
