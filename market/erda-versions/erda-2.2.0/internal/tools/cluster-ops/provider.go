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

package cluster_ops

import (
	"context"
	"os"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/cluster-ops/client"
	"github.com/erda-project/erda/internal/tools/cluster-ops/config"
)

type provider struct {
	Cfg *config.Config
}

func (p *provider) Init(ctx servicehub.Context) error {
	logrus.SetOutput(os.Stdout)

	if p.Cfg.Debug {
		logrus.SetLevel(logrus.DebugLevel)
	}
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	c := client.New(client.WithConfig(p.Cfg))
	return c.Execute()
}

func init() {
	servicehub.Register("cluster-ops", &servicehub.Spec{
		Services:    []string{"cluster-ops"},
		Description: "cluster ops",
		ConfigFunc: func() interface{} {
			return &config.Config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
