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

package kubernetes

import (
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/k8sclient"
)

var k8sClients = make(map[string]*k8sclient.K8sClient, 0)

type Kubernetes struct{}

func New() *Kubernetes {
	return &Kubernetes{}
}

// getK8sClient Get or create kubernetes client by cluster name.
func (k *Kubernetes) getK8sClient(clusterName string) (*k8sclient.K8sClient, error) {
	if clusterName == "" {
		return nil, fmt.Errorf("empty cluster name")
	}

	if k8sClient, ok := k8sClients[clusterName]; ok {
		return k8sClient, nil
	}

	k8sClient, err := k8sclient.New(clusterName)
	if err != nil {
		errMsg := fmt.Errorf("cluster %s k8sclient create error, error: %+v", clusterName, err)
		logrus.Error(errMsg)
		return nil, errMsg
	}

	k8sClients[clusterName] = k8sClient

	return k8sClient, nil
}
