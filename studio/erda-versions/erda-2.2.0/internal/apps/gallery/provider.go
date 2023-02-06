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

package gallery

import (
	"os"

	"github.com/sirupsen/logrus"
	"gorm.io/gorm"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/apps/gallery/pb"
	"github.com/erda-project/erda/internal/apps/gallery/cache"
	"github.com/erda-project/erda/internal/apps/gallery/dao"
	"github.com/erda-project/erda/internal/apps/gallery/handler"
	"github.com/erda-project/erda/pkg/common/apis"
)

var (
	name = "erda.apps.gallery"
	spec = servicehub.Spec{
		Define:               nil,
		Services:             pb.ServiceNames(),
		Dependencies:         nil,
		OptionalDependencies: []string{"service-register"},
		DependenciesFunc:     nil,
		Summary:              "gallery service",
		Description:          "gallery service",
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
	servicehub.Register(name, &spec)
}

// +provider
type provider struct {
	R transport.Register `autowired:"service-register" required:"true"`

	// providers clients
	C    *cache.Cache    `autowired:"erda.apps.gallery.easy-memory-cache-client"`
	D    *gorm.DB        `autowired:"mysql-gorm.v2-client"`
	Tran i18n.Translator `translator:"gallery"`

	Cfg *config

	l *logrus.Entry
}

func (p *provider) Init(ctx servicehub.Context) error {
	logrus.SetLevel(p.Cfg.GetLogLevel())
	p.l = logrus.WithField("provider", name)
	p.l.Infoln("Init")
	if p.Cfg.GetLogLevel() > logrus.InfoLevel || os.Getenv("GORM_DEBUG") == "true" {
		p.D = p.D.Debug()
	}
	dao.Init(p.D)
	if p.R != nil {
		p.l.Infoln("register GalleryServer")
		h := &handler.GalleryHandler{C: p.C, L: p.l.WithField("handler", "GalleryHandler"), Tran: p.Tran}
		pb.RegisterGalleryImp(p.R, h, apis.Options())
	}

	return nil
}

type config struct {
	LogLevel logrus.Level `json:"log-level" yaml:"log-level"`
}

func (c config) GetLogLevel() logrus.Level {
	if c.LogLevel <= logrus.InfoLevel {
		return logrus.InfoLevel
	}
	return c.LogLevel
}
