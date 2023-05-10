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

package legacy

import (
	"context"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/i18n"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/providers/errorbox"
	"github.com/erda-project/erda/internal/core/legacy/services/permission"
	"github.com/erda-project/erda/pkg/oauth2"
)

type provider struct {
	Tran          i18n.Translator           `translator:"i18n"`
	Router        httpserver.Router         `autowired:"http-router"`
	ErrorBoxSvc   *errorbox.ErrorBoxService `autowired:"erda.core.services.errorbox.ErrorBoxService"`
	ResourceTrans i18n.Translator           `translator:"resource-trans"`
	oauth2server  *oauth2.OAuth2Server
	perm          *permission.Permission
	DB            *gorm.DB                   `autowired:"mysql-client"`
	TokenService  tokenpb.TokenServiceServer `autowired:"erda.core.token.TokenService"`
}

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.oauth2server = oauth2.NewOAuth2Server()
	pm := permission.New(
		permission.WithDBClient(&dao.DBClient{p.DB}),
	)
	p.perm = pm

	router := p.Router
	router.Any("/oauth2/token", p.oauth2server.Token)
	router.Any("/oauth2/invalidate_token", p.oauth2server.InvalidateToken)
	router.Any("/oauth2/validate_token", p.oauth2server.ValidateToken)
	return nil
}

func (p *provider) Run(ctx context.Context) error { return p.Initialize() }

func init() {
	servicehub.Register("core-services", &servicehub.Spec{
		Services: []string{"core-services"},
		Creator:  func() servicehub.Provider { return &provider{} },
	})
}
