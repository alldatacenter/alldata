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

package localvolume

import (
	"context"
	"errors"
	"fmt"
	"sync"

	"github.com/sirupsen/logrus"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"sigs.k8s.io/sig-storage-lib-external-provisioner/v6/controller"

	"github.com/erda-project/erda/internal/tools/volume-provisioner/exec"
	"github.com/erda-project/erda/pkg/strutil"
)

type localVolumeProvisioner struct {
	client      kubernetes.Interface
	restClient  rest.Interface
	csConfig    *rest.Config
	lvpConfig   *Config
	cmdExecutor *exec.CmdExecutor
}

type Config struct {
	ModeEdge   bool
	MatchLabel string
	NodeName   string
	Namespace  string
}

func NewLocalVolumeProvisioner(lvpConfig *Config, csConfig *rest.Config, client kubernetes.Interface) *localVolumeProvisioner {
	return &localVolumeProvisioner{
		lvpConfig:   lvpConfig,
		client:      client,
		restClient:  client.CoreV1().RESTClient(),
		csConfig:    csConfig,
		cmdExecutor: exec.NewCmdExecutor(csConfig, client, lvpConfig.Namespace),
	}
}

func (p *localVolumeProvisioner) Provision(ctx context.Context, options controller.ProvisionOptions) (*v1.PersistentVolume, controller.ProvisioningState, error) {
	logrus.Infof("Start provisioning local volume: options: %v", options)

	if options.SelectedNode == nil {
		err := errors.New("not provide selectedNode in provisionOptions")
		logrus.Error(err)
		return nil, controller.ProvisioningFinished, err
	}

	volPathOnHost, err := volumeRealPath(&options, options.PVName)
	if err != nil {
		return nil, controller.ProvisioningFinished, err
	}

	volPath, err := volumePath(&options, options.PVName)
	if err != nil {
		return nil, controller.ProvisioningFinished, err
	}

	if p.lvpConfig.ModeEdge {
		if p.lvpConfig.NodeName != options.SelectedNode.Name {
			err = fmt.Errorf("cant't match create request, want: %s, request: %s", p.lvpConfig.NodeName, options.SelectedNode.Name)
			return nil, controller.ProvisioningFinished, err
		}
		if err = p.cmdExecutor.OnLocal(fmt.Sprintf("mkdir -p %s", volPath)); err != nil {
			logrus.Errorf("node %s mkdir %s error: %v", p.lvpConfig.NodeName, volPath, err)
			return nil, controller.ProvisioningFinished, err
		}
	} else {
		nodeSelector := fmt.Sprintf("kubernetes.io/hostname=%s", options.SelectedNode.Name)
		if err := p.cmdExecutor.OnNodesPods(fmt.Sprintf("mkdir -p %s", volPath),
			metav1.ListOptions{
				LabelSelector: nodeSelector,
			}, metav1.ListOptions{
				LabelSelector: p.lvpConfig.MatchLabel,
			}); err != nil {
			return nil, controller.ProvisioningFinished, err
		}
	}

	return &v1.PersistentVolume{
		ObjectMeta: metav1.ObjectMeta{
			Name: options.PVName,
		},
		Spec: v1.PersistentVolumeSpec{
			PersistentVolumeReclaimPolicy: v1.PersistentVolumeReclaimDelete,
			AccessModes:                   options.PVC.Spec.AccessModes,
			Capacity: v1.ResourceList{
				v1.ResourceName(v1.ResourceStorage): options.PVC.Spec.Resources.Requests[v1.ResourceName(v1.ResourceStorage)],
			},
			PersistentVolumeSource: v1.PersistentVolumeSource{
				Local: &v1.LocalVolumeSource{
					Path: volPathOnHost,
				},
			},
			NodeAffinity: &v1.VolumeNodeAffinity{
				Required: &v1.NodeSelector{
					NodeSelectorTerms: []v1.NodeSelectorTerm{
						{
							MatchExpressions: []v1.NodeSelectorRequirement{
								{
									Key:      "kubernetes.io/hostname",
									Operator: v1.NodeSelectorOpIn,
									Values:   []string{options.SelectedNode.Name},
								},
							},
						},
					},
				},
			},
		},
	}, controller.ProvisioningFinished, nil
}

var (
	hostPathOnce                    sync.Once
	hostPathErr                     error
	hostPathVolumePrefixInContainer string // = "/hostfs/data/localvolume"
)

func volumePath(options *controller.ProvisionOptions, pvName string) (string, error) {
	mountPath, err := findLocalVolumeMountedPath(options)
	if err != nil {
		return "", err
	}
	return strutil.JoinPath(mountPath, "localvolume", pvName), nil
}

func volumeRealPath(options *controller.ProvisionOptions, pvName string) (string, error) {
	mountPath, err := findLocalVolumeMountedPath(options)
	if err != nil {
		return "", err
	}
	return strutil.JoinPath("/", strutil.TrimPrefixes(mountPath, "/hostfs"), "localvolume", pvName), nil
}

func findLocalVolumeMountedPath(options *controller.ProvisionOptions) (string, error) {
	if options.StorageClass.Parameters != nil && options.StorageClass.Parameters["hostpath"] != "" {
		return strutil.JoinPath("/hostfs", options.StorageClass.Parameters["hostpath"]), nil
	}
	hostPathOnce.Do(func() {
		mountpoint, err := DiscoverMountPoint()
		if err != nil {
			hostPathErr = err
		}
		hostPathVolumePrefixInContainer = mountpoint
	})
	return hostPathVolumePrefixInContainer, hostPathErr
}
