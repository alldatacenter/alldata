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
	"k8s.io/apimachinery/pkg/version"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"sigs.k8s.io/sig-storage-lib-external-provisioner/v6/controller"

	"github.com/erda-project/erda/internal/tools/volume-provisioner/localvolume"
	"github.com/erda-project/erda/internal/tools/volume-provisioner/netdatavolume"
)

func initLocalVolumeProvisioner(config *config, csConfig *rest.Config, client kubernetes.Interface, version *version.Info) {
	var pc *controller.ProvisionController

	logrus.Infof("Creating localvolumeProvisioner...")

	lvpConfig := &localvolume.Config{
		ModeEdge:   config.ModeEdge,
		MatchLabel: config.LocalMatchLabel,
		NodeName:   config.NodeName,
		Namespace:  config.ProvisionerNamespace,
	}

	lvp := localvolume.NewLocalVolumeProvisioner(lvpConfig, csConfig, client)

	if config.ModeEdge {
		pc = controller.NewProvisionController(client, config.LocalProvisionerName, lvp, version.GitVersion,
			controller.LeaderElection(false))
	} else {
		pc = controller.NewProvisionController(client, config.LocalProvisionerName, lvp, version.GitVersion)
	}

	go pc.Run(context.Background())
}

func initNetDataVolumeProvisioner(config *config, csConfig *rest.Config, client kubernetes.Interface, version *version.Info) {
	logrus.Infof("Creating netdatavolumeProvisioner...")

	nvp := netdatavolume.NewNetDataVolumeProvisioner(csConfig, client)
	pc := controller.NewProvisionController(client, config.NetProvisionerName, nvp, version.GitVersion)

	go pc.Run(context.Background())
}

func initialize(config *config, ctx context.Context) error {

	csConfig, err := rest.InClusterConfig()
	if err != nil {
		logrus.Errorf("Failed to create config: %v", err)
		return err
	}

	csConfig.QPS = 100
	csConfig.Burst = 100

	cs, err := kubernetes.NewForConfig(csConfig)
	if err != nil {
		logrus.Errorf("Failed to create client: %v", err)
		return err
	}
	serverVersion, err := cs.Discovery().ServerVersion()
	if err != nil {
		logrus.Errorf("Failed to get server version: %v", err)
		return err
	}

	if !config.ModeEdge {
		initNetDataVolumeProvisioner(config, csConfig, cs, serverVersion)
	}
	initLocalVolumeProvisioner(config, csConfig, cs, serverVersion)

	select {
	case <-ctx.Done():
	}
	return nil
}
