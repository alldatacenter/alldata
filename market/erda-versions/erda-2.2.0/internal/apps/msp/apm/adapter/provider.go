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

package adapter

import (
	"io/ioutil"

	"gopkg.in/yaml.v2"

	logs "github.com/erda-project/erda-infra/base/logs"
	servicehub "github.com/erda-project/erda-infra/base/servicehub"
	transport "github.com/erda-project/erda-infra/pkg/transport"
	pb "github.com/erda-project/erda-proto-go/msp/apm/adapter/pb"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
	CollectorUrl string   `file:"collector_url"`
	LibraryFiles []string `file:"libraryFiles"`
	ConfigFiles  []string `file:"configFiles"`
}

// +provider
type provider struct {
	Cfg            *config
	Log            logs.Logger
	Register       transport.Register
	adapterService *adapterService
	libraries      []*InstrumentationLibrary
	templates      map[string]*InstrumentationLibraryTemplate
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.libraries = make([]*InstrumentationLibrary, 0)
	p.templates = make(map[string]*InstrumentationLibraryTemplate)
	for _, file := range p.Cfg.LibraryFiles {
		f, err := ioutil.ReadFile(file)
		if err != nil {
			return err
		}
		libs := make([]*InstrumentationLibrary, 0)
		err = yaml.Unmarshal(f, &libs)
		if err != nil {
			return err
		}
		p.libraries = append(p.libraries, libs...)
	}
	for _, file := range p.Cfg.ConfigFiles {
		f, err := ioutil.ReadFile(file)
		if err != nil {
			return err
		}
		templates := make([]*InstrumentationLibraryTemplate, 0)
		err = yaml.Unmarshal(f, &templates)
		if err != nil {
			return err
		}
		for _, template := range templates {
			p.templates[template.InstrumentationLibrary] = template
		}
	}
	if p.Register != nil {
		p.adapterService = &adapterService{p}
		pb.RegisterInstrumentationLibraryServiceImp(p.Register, p.adapterService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.apm.adapter.AdapterService" || ctx.Type() == pb.InstrumentationLibraryServiceServerType() || ctx.Type() == pb.InstrumentationLibraryServiceHandlerType():
		return p.adapterService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.apm.adapter", &servicehub.Spec{
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
