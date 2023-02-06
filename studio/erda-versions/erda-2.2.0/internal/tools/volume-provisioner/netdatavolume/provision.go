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

package netdatavolume

import (
	"context"
	"fmt"
	"os/exec"
	"sync"

	"github.com/sirupsen/logrus"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"sigs.k8s.io/sig-storage-lib-external-provisioner/v6/controller"

	"github.com/erda-project/erda/pkg/strutil"
)

type netDataVolumeProvisioner struct {
	client     kubernetes.Interface
	restClient rest.Interface
	config     *rest.Config
}

func NewNetDataVolumeProvisioner(config *rest.Config, client kubernetes.Interface) *netDataVolumeProvisioner {
	return &netDataVolumeProvisioner{
		client:     client,
		restClient: client.CoreV1().RESTClient(),
		config:     config,
	}
}

func (p *netDataVolumeProvisioner) Provision(ctx context.Context, options controller.ProvisionOptions) (*v1.PersistentVolume, controller.ProvisioningState, error) {
	logrus.Infof("Start provisioning netdata volume: pv: %v", options.PVName)
	volPathOnHost, err := volumeRealPath(&options, options.PVName)
	if err != nil {
		return nil, controller.ProvisioningFinished, err
	}
	volPath, err := volumePath(&options, options.PVName)
	if err != nil {
		return nil, controller.ProvisioningFinished, err
	}
	mkdirCommand := exec.Command("/bin/sh", "-c", fmt.Sprintf("mkdir -p %s", volPath))
	if _, err := mkdirCommand.Output(); err != nil {
		return nil, controller.ProvisioningFinished, fmt.Errorf("Failed to mkdir: %v, err: %v", volPath, err)
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
									// Every node is satisfied
									Key:      "kubernetes.io/hostname",
									Operator: v1.NodeSelectorOpNotIn,
									Values:   []string{"127.0.0.1"},
								},
							},
						},
					},
				},
			},
		},
	}, controller.ProvisioningFinished, nil
}

// volumePath volumepath in kagent container
func volumePath(options *controller.ProvisionOptions, pvName string) (string, error) {
	mountPath, err := findNetdataMountedPath(options)
	if err != nil {
		return "", err
	}
	return strutil.JoinPath(mountPath, "netdatavolume", pvName), nil
}

// volumeRealPath volumepath in host
func volumeRealPath(options *controller.ProvisionOptions, pvName string) (string, error) {
	mountPath, err := findNetdataMountedPath(options)
	if err != nil {
		return "", err
	}
	return strutil.JoinPath("/", strutil.TrimPrefixes(mountPath, "/hostfs"), "netdatavolume", pvName), nil
}

var (
	netdataMountedPath     string
	netdataMountedPathErr  error
	netdataMountedPathLock sync.Once
)

// findNetdataMountedPath find out which directory nfs/glusterfs mounted
// return e.g. /hostfs/netdata, because we mount / to /hostfs in container
func findNetdataMountedPath(options *controller.ProvisionOptions) (string, error) {
	if options.StorageClass.Parameters != nil && options.StorageClass.Parameters["hostpath"] != "" {
		return strutil.JoinPath("/hostfs", options.StorageClass.Parameters["hostpath"]), nil
	}
	netdataMountedPathLock.Do(func() {
		mountPoint, err := DiscoverMountPoint()
		if err != nil {
			netdataMountedPathErr = err
		}
		netdataMountedPath = mountPoint
	})
	return netdataMountedPath, netdataMountedPathErr
}
