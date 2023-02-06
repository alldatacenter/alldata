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

package image

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/dicehub/image/pb"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/image/db"
)

type config struct {
}

// +provider
type provider struct {
	Cfg          *config
	Log          logs.Logger
	Register     transport.Register `autowired:"service-register" required:"true"`
	DB           *gorm.DB           `autowired:"mysql-client"`
	imageService *imageService
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.imageService = &imageService{
		p:  p,
		db: &db.ImageConfigDB{DB: p.DB},
	}
	if p.Register != nil {
		pb.RegisterImageServiceImp(p.Register, p.imageService)
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.dicehub.image.ImageService" || ctx.Type() == pb.ImageServiceServerType() || ctx.Type() == pb.ImageServiceHandlerType():
		return p.imageService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.dicehub.image", &servicehub.Spec{
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
