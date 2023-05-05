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

package k8sspark

import (
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/logic"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/types"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/k8sclient"
)

var Kind = types.Kind(spec.PipelineTaskExecutorKindK8sSpark)

type K8sSpark struct {
	*types.K8sExecutor
	name        types.Name
	client      *k8sclient.K8sClient
	clusterName string
	cluster     apistructs.ClusterInfo
	errWrapper  *logic.ErrorWrapper
}

func New(name types.Name, clusterName string, cluster apistructs.ClusterInfo) (*K8sSpark, error) {
	// we could operate normal resources (job, pod, deploy,pvc,pv,crd and so on) by default config permissions(injected by kubernetes, /var/run/secrets/kubernetes.io/serviceaccount)
	// so WithPreferredToUseInClusterConfig it's enough for pipeline and orchestrator
	client, err := k8sclient.New(clusterName, k8sclient.WithTimeout(time.Duration(conf.K8SExecutorMaxInitializationSec())*time.Second), k8sclient.WithPreferredToUseInClusterConfig())
	if err != nil {
		return nil, err
	}
	k8sSpark := &K8sSpark{
		name:        name,
		clusterName: clusterName,
		client:      client,
		cluster:     cluster,
		errWrapper:  logic.NewErrorWrapper(name.String()),
	}
	k8sSpark.K8sExecutor = types.NewK8sExecutor(k8sSpark)
	return k8sSpark, nil
}

func (k *K8sSpark) Kind() types.Kind {
	return Kind
}

func (k *K8sSpark) Name() types.Name {
	return k.name
}
