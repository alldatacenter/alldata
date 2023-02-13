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

package clientgo

import (
	"github.com/erda-project/erda/pkg/clientgo/customclient"
	"github.com/erda-project/erda/pkg/clientgo/kubernetes"
)

type ClientSet struct {
	K8sClient    *kubernetes.Clientset
	CustomClient *customclient.Clientset
}

func New(addr string) (*ClientSet, error) {
	var cs ClientSet
	var err error
	cs.K8sClient, err = kubernetes.NewKubernetesClientSet(addr)
	if err != nil {
		return nil, err
	}
	cs.CustomClient, err = customclient.NewCustomClientSet(addr)
	if err != nil {
		return nil, err
	}
	return &cs, nil
}
