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

package mysql

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/orchestrator/addon/mysql/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type config struct {
}

// +provider
type provider struct {
	Cfg      *config
	Logger   logs.Logger
	Register transport.Register
	DB       *gorm.DB `autowired:"mysql-client"`
	Bdl      *bundle.Bundle

	addonMySQLService pb.AddonMySQLServiceServer
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.Bdl = bundle.New(bundle.WithKMS(), bundle.WithCoreServices())
	p.addonMySQLService = &mysqlService{
		logger: p.Logger,
		kms:    NewKMSWrapper(p.Bdl),
		perm:   p.Bdl,
		db: &dbclient.DBClient{
			DBEngine: &dbengine.DBEngine{
				DB: p.DB,
			},
		},
		encrypt: encryption.New(
			encryption.WithRSAScrypt(encryption.NewRSAScrypt(encryption.RSASecret{
				PublicKey:          conf.PublicKey(),
				PublicKeyDataType:  encryption.Base64,
				PrivateKey:         conf.PrivateKey(),
				PrivateKeyType:     encryption.PKCS1,
				PrivateKeyDataType: encryption.Base64,
			}))),
	}
	if p.Register != nil {
		pb.RegisterAddonMySQLServiceImp(p.Register, p.addonMySQLService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.orchestrator.addon.mysql.AddonMySQLService" || ctx.Type() == pb.AddonMySQLServiceServerType() || ctx.Type() == pb.AddonMySQLServiceHandlerType():
		return p.addonMySQLService
	}
	return p
}

func init() {
	servicehub.Register("erda.orchestrator.addon.mysql", &servicehub.Spec{
		Services: append(pb.ServiceNames()),
		Types:    pb.Types(),
		OptionalDependencies: []string{
			"service-register",
			"mysql",
		},
		Description: "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
