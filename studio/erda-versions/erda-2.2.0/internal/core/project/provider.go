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

package project

import (
	"os"
	"strings"

	"github.com/sirupsen/logrus"
	"gorm.io/gorm"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/project/pb"
	"github.com/erda-project/erda/internal/core/project/dao"
)

var (
	name = "erda.core.project"
	spec = &servicehub.Spec{
		Services:             pb.ServiceNames(),
		OptionalDependencies: []string{"service-register"},
		ConfigFunc: func() interface{} {
			return new(config)
		},
		Types: pb.Types(),
		Creator: func() servicehub.Provider {
			return new(provider)
		},
	}
)

func init() {
	servicehub.Register(name, spec)
}

// +provider
type provider struct {
	R   transport.Register `autowired:"service-register" required:"true"`
	DB  *gorm.DB           `autowired:"mysql-gorm.v2-client"`
	Cfg *config

	proj *project
	l    *logrus.Entry
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.initLogrus()
	p.initDB()
	p.proj = &project{l: p.l.WithField("handler", "Project")}

	if p.R != nil {
		p.l.Infoln("register Project")
		pb.RegisterProjectImp(p.R, p.proj)
	}

	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	if ctx.Service() == "erda.core.project.Project" ||
		ctx.Type() == pb.ProjectServerType() ||
		ctx.Type() == pb.ProjectHandlerType() {
		return p.proj
	}
	return p
}

func (p *provider) initLogrus() {
	switch strings.ToLower(p.Cfg.LogLevel) {
	case "panic":
		logrus.SetLevel(logrus.PanicLevel)
	case "fatal":
		logrus.SetLevel(logrus.FatalLevel)
	case "error":
		logrus.SetLevel(logrus.ErrorLevel)
	case "warn":
		logrus.SetLevel(logrus.WarnLevel)
	case "info":
		logrus.SetLevel(logrus.InfoLevel)
	case "debug":
		logrus.SetLevel(logrus.DebugLevel)
	case "trace":
		logrus.SetLevel(logrus.TraceLevel)
	default:
		logrus.SetLevel(logrus.InfoLevel)
	}
	p.l = logrus.WithField("provider", name)
}

func (p *provider) initDB() {
	switch strings.ToLower(p.Cfg.LogLevel) {
	case "debug", "trace":
		p.DB = p.DB.Debug()
	default:
		if os.Getenv("GORM_DEBUG") == "true" {
			p.DB = p.DB.Debug()
		}
	}
	dao.Init(p.DB)
}

type config struct {
	LogLevel string `json:"log-level" yaml:"log-level"`
}
