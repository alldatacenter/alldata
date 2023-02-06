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

// Package volumeprovisioner is a persistent volume claim provisioner for kubernetes.
package volumeprovisioner

import (
	"context"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
)

// config The definition of volume-provisioner config
type config struct {
	// ProvisionerNamespace Consistent with the namespace of the volume-provisioner daemonSet,
	// Usually injected directly from metadata.namespace
	ProvisionerNamespace string `env:"PROVISIONER_NAMESPACE" default:"default"`
	// LocalProvisionerName Name of local volume provisioner
	LocalProvisionerName string `env:"LOCAL_PROVISIONER_NAME" default:"dice/local-volume"`
	// NetProvisionerName Name of netData volume provisioner
	NetProvisionerName string `env:"NET_PROVISIONER_NAME" default:"dice/netdata-volume"`
	// LocalMatchLabel Match label for execute on specified pods
	LocalMatchLabel string `env:"LOCAL_MATCH_LABEL" default:"app=volume-provisioner"`
	// ModeEdge Used for edge computing,
	ModeEdge bool `env:"EDGE_MODE" default:"false"`
	// NodeName Used for edge computing, directory creation action on the specified edge nodeSite
	NodeName string `env:"NODE_NAME" default:""`
}

type provider struct {
	Cfg *config
}

// TODO: refactor
// Init Initialize the provider to run
func (p *provider) Init(ctx servicehub.Context) error { return nil }

// Run Run the provider
func (p *provider) Run(ctx context.Context) error {
	logrus.Info("volumeProvisioner provider is running...")
	return initialize(p.Cfg, ctx)
}

// Close Close the provider
func (p *provider) Close() error {
	logrus.Info("volumeProvisioner provider is closing...")
	return nil
}

func init() {
	servicehub.Register("volume-provisioner", &servicehub.Spec{
		Services:    []string{"volume-provisioner"},
		Description: "This is a Persistent Volume Claim (PVC) provisioner for Kubernetes.",
		ConfigFunc:  func() interface{} { return &config{} },
		Creator:     func() servicehub.Provider { return &provider{} },
	})
}
