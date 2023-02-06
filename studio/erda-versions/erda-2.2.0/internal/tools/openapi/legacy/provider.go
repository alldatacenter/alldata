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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/base/version"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/types"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
)

type config struct {
	CP types.ComponentProtocolConfigs `file:"component-protocol"`
}

type provider struct {
	Cfg          *config
	TokenService tokenpb.TokenServiceServer `autowired:"erda.core.token.TokenService"`
}

func (p *provider) Run(ctx context.Context) error {
	logrus.Infof(version.String())
	logrus.Errorf("[alert] openapi instance start")
	conf.Load()
	srv, err := NewServer(p.TokenService)
	if err != nil {
		return err
	}
	types.CPConfigs = p.Cfg.CP
	return srv.ListenAndServe()
}

func init() {
	servicehub.Register("openapi", &servicehub.Spec{
		Services:   []string{"openapi"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
