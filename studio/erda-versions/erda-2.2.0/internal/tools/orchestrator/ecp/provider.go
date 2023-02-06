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

package ecp

import (
	"context"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
)

// Conf Define the configuration
type Conf struct {
	Debug bool `env:"DEBUG" default:"false"`
}

type provider struct {
	Cfg        *Conf
	ClusterSvc clusterpb.ClusterServiceServer `autowired:"erda.core.clustermanager.cluster.ClusterService"`
}

// Run
func (p *provider) Run(ctx context.Context) error {
	if p.Cfg.Debug {
		logrus.SetLevel(logrus.DebugLevel)
	}
	return nil
}

func init() {
	servicehub.Register("ecp", &servicehub.Spec{
		Services:    []string{"ecp"},
		Description: "Core components of edge computing platform.",
		ConfigFunc:  func() interface{} { return &Conf{} },
		Creator:     func() servicehub.Provider { return &provider{} },
	})
}
