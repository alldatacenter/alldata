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

package edge

import (
	"context"
	"fmt"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/ecp/dbclient"
)

var (
	// config-orgID-configSetName   e.g. edgecfg-000-democofnigset
	defaultConfigSetFormat = "edgecfg-%s-%s"
)

// ListConfigSet List configSet by orgID or clusterID.
func (e *Edge) ListConfigSet(ctx context.Context, param *apistructs.EdgeConfigSetListPageRequest) (int, *[]apistructs.EdgeConfigSetInfo, error) {
	total, configSets, err := e.db.ListEdgeConfigSet(param)
	if err != nil {
		return 0, nil, err
	}

	configSetInfos := make([]apistructs.EdgeConfigSetInfo, 0, len(*configSets))

	for _, configSet := range *configSets {
		clusterInfo, err := e.getClusterInfo(ctx, configSet.ClusterID)
		if err != nil {
			return 0, nil, err
		}
		configSetInfos = append(configSetInfos, *convertToEdgeConfigSetInfo(configSet, clusterInfo.Name))
	}
	return total, &configSetInfos, nil

}

// GetConfigSet Get configSet by configSetID.
func (e *Edge) GetConfigSet(ctx context.Context, configSetID int64) (*apistructs.EdgeConfigSetInfo, error) {
	var (
		configSetInfo *apistructs.EdgeConfigSetInfo
	)

	configSet, err := e.db.GetEdgeConfigSet(configSetID)
	if err != nil {
		return nil, err
	}

	clusterInfo, err := e.getClusterInfo(ctx, configSet.ClusterID)
	if err != nil {
		return nil, err
	}

	configSetInfo = convertToEdgeConfigSetInfo(*configSet, clusterInfo.Name)

	return configSetInfo, nil
}

// CreateConfigSet Create configSet namespaces in specified cluster.
func (e *Edge) CreateConfigSet(ctx context.Context, req *apistructs.EdgeConfigSetCreateRequest) (uint64, error) {
	var (
		nsAPIVersion = "v1"
		nsKind       = "Namespace"
	)

	configSet := &dbclient.EdgeConfigSet{
		OrgID:       req.OrgID,
		ClusterID:   req.ClusterID,
		Name:        req.Name,
		DisplayName: req.DisplayName,
		Description: req.Description,
	}

	namespace := &corev1.Namespace{
		TypeMeta: metav1.TypeMeta{
			APIVersion: nsAPIVersion,
			Kind:       nsKind,
		},
	}

	clusterInfo, err := e.getClusterInfo(ctx, req.ClusterID)
	if err != nil {
		return 0, err
	}

	namespace.Name, err = e.getConfigSetName(configSet.OrgID, configSet.Name)
	if err != nil {
		return 0, err
	}

	if err = e.k8s.CreateNamespace(clusterInfo.Name, namespace); err != nil {
		return 0, err
	}

	if err = e.db.CreateEdgeConfigSet(configSet); err != nil {
		deleteErr := e.k8s.DeleteNamespace(clusterInfo.Name, namespace.Name)
		if deleteErr != nil {
			return 0, deleteErr
		}
		return 0, err
	}

	return configSet.ID, err

}

// DeleteConfigSet Delete configSet by configSetID in specified cluster.
func (e *Edge) DeleteConfigSet(ctx context.Context, configSetID int64) error {
	configSet, err := e.db.GetEdgeConfigSet(configSetID)
	if err != nil {
		return fmt.Errorf("failed to get edge configset, configSetID: %d, err: (%v)", configSetID, err)
	}

	// If configSet depend on application, can't delete it.
	edgeApps, err := e.db.GetEdgeAppByConfigSet(configSet.Name, configSet.ClusterID)
	if err != nil {
		return fmt.Errorf("failed to get releated app")
	}
	if len(*edgeApps) > 0 {
		relatedApps := make([]string, 0)
		for _, relatedApp := range *edgeApps {
			relatedApps = append(relatedApps, relatedApp.Name)
		}
		return fmt.Errorf("application %s  have related this configset", fmt.Sprint(relatedApps))
	}

	if err = e.db.DeleteEdgeConfigSet(configSetID); err != nil {
		return fmt.Errorf("failed to delete edge configset, (%v)", err)
	}

	clusterInfo, err := e.getClusterInfo(ctx, configSet.ClusterID)
	if err != nil {
		return err
	}

	if err = e.db.DeleteEdgeCfgSetItemByCfgID(configSetID); err != nil {
		return err
	}

	nsName, err := e.getConfigSetName(configSet.OrgID, configSet.Name)
	if err != nil {
		return err
	}

	err = e.k8s.DeleteNamespace(clusterInfo.Name, nsName)

	if err != nil {

		return err
	}

	return nil
}

// UpdateConfigSet Update configSet
func (e *Edge) UpdateConfigSet(configSetID int64, req *apistructs.EdgeConfigSetUpdateRequest) error {

	configSet, err := e.db.GetEdgeConfigSet(configSetID)
	if err != nil {
		return fmt.Errorf("failed to get edge configset,configSetID: %d, (%v)", configSetID, err)
	}

	configSet.DisplayName = req.DisplayName
	configSet.Description = req.Description

	err = e.db.UpdateEdgeConfigSet(configSet)
	if err != nil {
		return err
	}
	return nil
}

// getConfigSetName Generate configSet name by orgID and configSet name.
func (e *Edge) getConfigSetName(orgID int64, configSetName string) (string, error) {
	return fmt.Sprintf(defaultConfigSetFormat, fmt.Sprintf("%03d", orgID), configSetName), nil
}

// convertToEdgeConfigSetInfo Convert type EdgeConfigSet to type EdgeConfigSetInfo.
func convertToEdgeConfigSetInfo(configSet dbclient.EdgeConfigSet, clusterName string) *apistructs.EdgeConfigSetInfo {
	return &apistructs.EdgeConfigSetInfo{
		ID:          int64(configSet.ID),
		OrgID:       configSet.OrgID,
		Name:        configSet.Name,
		ClusterName: clusterName,
		DisplayName: configSet.DisplayName,
		ClusterID:   configSet.ClusterID,
		Description: configSet.Description,
		CreatedAt:   configSet.CreatedAt,
		UpdatedAt:   configSet.UpdatedAt,
	}
}
