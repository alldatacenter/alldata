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

package k8sclient

import (
	"os"
	"time"

	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/k8sclient/config"
	"github.com/erda-project/erda/pkg/k8sclient/scheme"
)

type K8sClient struct {
	// custom options
	timeout              *time.Duration
	priorityUseInCluster bool
	schemes              []func(scheme *runtime.Scheme) error

	// client for kubernetes
	ClientSet kubernetes.Interface
	CRClient  client.Client
}

// New new K8sClient with clusterName.
func New(clusterName string, ops ...Option) (*K8sClient, error) {
	var rc *rest.Config
	var err error
	var kc K8sClient
	for _, op := range ops {
		op(&kc)
	}

	inClusterName := os.Getenv(string(apistructs.DICE_CLUSTER_NAME))
	if inClusterName == clusterName && kc.priorityUseInCluster {
		rc, err = config.GetInClusterRestConfig()
		// if get config from /var/sericeaccount failed, try cluster-manager again
		if err != nil {
			rc, err = GetRestConfig(clusterName)
		}
	} else {
		rc, err = GetRestConfig(clusterName)
	}

	if err != nil {
		return nil, err
	}
	ops = append(ops, WithSchemes(scheme.LocalSchemeBuilder...))

	return NewForRestConfig(rc, ops...)
}

// NewWithTimeOut new k8sClient with timeout
func NewWithTimeOut(clusterName string, timeout time.Duration) (*K8sClient, error) {
	var rc *rest.Config
	var err error

	inClusterName := os.Getenv(string(apistructs.DICE_CLUSTER_NAME))
	if inClusterName == clusterName {
		rc, err = config.GetInClusterRestConfig()
	} else {
		rc, err = GetRestConfig(clusterName)
	}

	if err != nil {
		return nil, err
	}

	rc.Timeout = timeout

	return NewForRestConfig(rc, WithSchemes(scheme.LocalSchemeBuilder...))
}

// NewForRestConfig new K8sClient with rest.Config, you can register your custom runtime.Scheme.
func NewForRestConfig(c *rest.Config, ops ...Option) (*K8sClient, error) {
	var kc K8sClient
	var err error

	for _, op := range ops {
		op(&kc)
	}
	if kc.timeout != nil {
		c.Timeout = *kc.timeout
	}

	if kc.ClientSet, err = kubernetes.NewForConfig(c); err != nil {
		return nil, err
	}

	sc := runtime.NewScheme()
	schemeBuilder := &runtime.SchemeBuilder{}

	for _, s := range kc.schemes {
		schemeBuilder.Register(s)
	}

	if err = schemeBuilder.AddToScheme(sc); err != nil {
		return nil, err
	}

	if kc.CRClient, err = client.New(c, client.Options{Scheme: sc}); err != nil {
		return nil, err
	}

	return &kc, nil
}

type Option func(*K8sClient)

func WithTimeout(timeout time.Duration) Option {
	return func(k *K8sClient) {
		k.timeout = &timeout
	}
}

func WithSchemes(schemes ...func(scheme *runtime.Scheme) error) Option {
	return func(k *K8sClient) {
		k.schemes = schemes
	}
}

// WithPreferredToUseInClusterConfig set whether priority to use in cluster config
// if not set this option, we will get and use config set by cluster agent
func WithPreferredToUseInClusterConfig() Option {
	return func(k *K8sClient) {
		k.priorityUseInCluster = true
	}
}

// NewForInCluster New client for in cluster
func NewForInCluster(ops ...Option) (*K8sClient, error) {
	rc, err := config.GetInClusterRestConfig()
	if err != nil {
		return nil, err
	}
	return NewForRestConfig(rc, WithSchemes(scheme.LocalSchemeBuilder...))
}

// GetRestConfig get rest config with clusterName
func GetRestConfig(clusterName string) (*rest.Config, error) {
	b := bundle.New(bundle.WithClusterManager())

	ci, err := b.GetCluster(clusterName)
	if err != nil {
		return nil, err
	}

	rc, err := config.ParseManageConfig(clusterName, ci.ManageConfig)
	if err != nil {
		return nil, err
	}

	return rc, nil
}
