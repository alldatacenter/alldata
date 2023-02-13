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

package authentication

import (
	"context"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
)

type config struct {
	SyncInterval    time.Duration `file:"sync_interval" default:"2m" desc:"sync access key info from remote"`
	ExpiredDuration time.Duration `file:"expired_duration" default:"10m" desc:"the max duration of request spent in the network"`
}

// +provider
type provider struct {
	Cfg          *config
	Log          logs.Logger
	TokenService tokenpb.TokenServiceServer `autowired:"erda.core.token.TokenService"`

	accessKeyValidator *accessKeyValidator
}

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	p.accessKeyValidator = &accessKeyValidator{
		TokenService: p.TokenService,
		collection:   AccessItemCollection{},
	}
	ctx.AddTask(p.InitAKItemTask)
	ctx.AddTask(p.SyncAKItemTask)
	return nil
}

func (p *provider) InitAKItemTask(ctx context.Context) error {
	if err := p.accessKeyValidator.syncFullAccessKeys(ctx); err != nil {
		p.Log.Errorf("InitAKItem Task failed. err: %s", err)
	}
	return nil
}

// +SyncAKItemTask
func (p *provider) SyncAKItemTask(ctx context.Context) error {
	p.Log.Info("SyncAKItemTask is running...")
	tick := time.NewTicker(p.Cfg.SyncInterval)
	defer tick.Stop()
	for {
		select {
		case <-tick.C:
			if err := p.accessKeyValidator.syncFullAccessKeys(ctx); err != nil {
				p.Log.Errorf("Sync accessKeys task failed. err: %s", err)
			}
		case <-ctx.Done():
			return nil
		}
	}
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.oap.collector.authentication.Validator":
		return p.accessKeyValidator
	}
	return p
}

func init() {
	servicehub.Register("erda.oap.collector.authentication", &servicehub.Spec{
		Services: []string{
			"erda.oap.collector.authentication.Validator",
		},
		Description: "here is description of erda.oap.collector.authentication",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
