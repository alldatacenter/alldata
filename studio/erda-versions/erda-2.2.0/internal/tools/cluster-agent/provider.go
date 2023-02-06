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

package cluster_agent

import (
	"context"
	"os"
	"time"

	"github.com/rancher/remotedialer"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/cluster-agent/config"
	"github.com/erda-project/erda/internal/tools/cluster-agent/pkg/client"
	"github.com/erda-project/erda/internal/tools/cluster-agent/pkg/leaderelection"
	k8sclientconfig "github.com/erda-project/erda/pkg/k8sclient/config"
)

type provider struct {
	Cfg *config.Config // auto inject this field
}

func (p *provider) Init(ctx servicehub.Context) error {
	logrus.Infof("load configuration: %+v", p.Cfg)
	if p.Cfg.Debug {
		logrus.SetLevel(logrus.DebugLevel)
		remotedialer.PrintTunnelData = true
	}
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	c := client.New(client.WithConfig(p.Cfg))

	if !p.Cfg.LeaderElection {
		return c.Start(ctx)
	}
	rc, err := k8sclientconfig.GetInClusterRestConfig()
	if err != nil {
		return err
	}

	identity, err := leaderelection.GenIdentity()
	if err != nil {
		return err
	}

	logrus.Infof("instance identity: %s", identity)

	return leaderelection.Start(ctx, rc, leaderelection.Options{
		Identity:                   identity,
		LeaderElectionResourceLock: p.Cfg.LeasesResourceLockType,
		LeaderElectionNamespace:    p.Cfg.ErdaNamespace,
		LeaderElectionID:           p.Cfg.LeaderElectionID,
		LeaseDuration:              time.Duration(p.Cfg.LeaseDuration) * time.Second,
		RenewDeadline:              time.Duration(p.Cfg.RenewDeadline) * time.Second,
		RetryPeriod:                time.Duration(p.Cfg.RetryPeriod) * time.Second,
		OnStartedLeading: func(ctx context.Context) {
			if err := c.Start(ctx); err != nil {
				logrus.Errorf("failed to start cluster agent: %v", err)
			}
		},
		OnNewLeaderFun: func(newLeaderIdentity string) {
			logrus.Infof("%s became leader", newLeaderIdentity)
		},
		OnStoppedLeading: func() {
			logrus.Info("leader lost")
			os.Exit(0)
		},
	})
}

func init() {
	servicehub.Register("cluster-agent", &servicehub.Spec{
		Services:    []string{"cluster-agent"},
		Description: "cluster agent",
		ConfigFunc: func() interface{} {
			return &config.Config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
