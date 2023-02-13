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

package dicehub

import (
	"context"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
	image "github.com/erda-project/erda/internal/apps/dop/dicehub/image/db"
)

type provider struct {
	Log     logs.Logger
	DB      *gorm.DB          `autowired:"mysql-client"`
	Router  httpserver.Router `autowired:"http-router"`
	ImageDB *image.ImageConfigDB
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.ImageDB = &image.ImageConfigDB{DB: p.DB}
	return nil
}

func (p *provider) Run(ctx context.Context) error { return Initialize(p) }

func init() {
	servicehub.Register("dicehub", &servicehub.Spec{
		Services: []string{"dicehub"},
		Creator:  func() servicehub.Provider { return &provider{} },
	})
}
