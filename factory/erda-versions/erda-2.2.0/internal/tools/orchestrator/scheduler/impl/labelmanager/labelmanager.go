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

package labelmanager

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster/clusterutil"
	"github.com/erda-project/erda/pkg/strutil"
)

// LabelManager interface of LabelManagerImpl
type LabelManager interface {
	// List available labels.
	// map key: label name
	// map value: Is this label a prefix?
	//
	// Although different types of
	// clusters (dcos&k8s) have different names on the compute
	// nodes, the exposed labels are uniform names.
	List() map[string]bool

	// Not yet implemented
	SetNodeLabel(cluster Cluster, hosts []string, tags map[string]string) error

	// Not yet implemented
	// GetNodeLabel() map[string]string
}

// LabelManagerImpl implementation of LabelManager
type LabelManagerImpl struct{}

type Cluster struct {
	// e.g. terminus-y
	ClusterName string
	// One of the following options:
	// ServiceKindMarathon
	// ServiceKindK8S
	// JobKindMetronome
	// JobKindK8S
	ClusterType string
	// This can be ignored for k8s cluster
	SoldierURL string
}

// NewLabelManager create LabelManager
func NewLabelManager() LabelManager {
	return &LabelManagerImpl{}
}

func (s *LabelManagerImpl) List() map[string]bool {
	return map[string]bool{
		"locked":            false,
		"platform":          false,
		"pack-job":          false,
		"bigdata-job":       false,
		"job":               false,
		"stateful-service":  false,
		"stateless-service": false,
		"workspace-dev":     false,
		"workspace-test":    false,
		"workspace-staging": false,
		"workspace-prod":    false,
		"org-":              true,
		"location-":         true,
	}
}

var (
	NotFoundExecutor = errors.New("Not found Executor")
)

func (s *LabelManagerImpl) SetNodeLabel(cluster Cluster, hosts []string, tags map[string]string) error {
	executorType := clusterutil.ServiceKindMarathon
	switch strutil.ToLower(cluster.ClusterType) {
	case "dcos":
		executorType = clusterutil.ServiceKindMarathon
	case apistructs.K8S, "kubernetes":
		// executorType = clusterutil.ServiceKindK8S
		// TODO: Now even for k8s, executorname is MARATHONFORXXX
		executorType = clusterutil.ServiceKindMarathon
	case "edas":
		executorType = clusterutil.EdasKindInK8s
	}
	executorName := clusterutil.GenerateExecutorByCluster(cluster.ClusterName, executorType)
	executor, err := executor.GetManager().Get(executortypes.Name(executorName))
	if err != nil {
		return errors.Wrap(NotFoundExecutor, executorName)
	}
	if err := executor.SetNodeLabels(executortypes.NodeLabelSetting{cluster.SoldierURL}, hosts, tags); err != nil {
		return err
	}
	return nil
}
