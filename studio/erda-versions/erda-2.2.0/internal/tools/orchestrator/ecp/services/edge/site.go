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
	"os"
	"time"

	"github.com/sirupsen/logrus"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/ecp/dbclient"
	"github.com/erda-project/erda/pkg/clientgo/apis/openyurt/v1alpha1"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

// ListSite List edge site paging.
func (e *Edge) ListSite(ctx context.Context, param *apistructs.EdgeSiteListPageRequest) (int, *[]apistructs.EdgeSiteInfo, error) {
	var clusterIDs []int64

	total, sites, err := e.db.ListEdgeSite(param)
	if err != nil {
		return 0, nil, err
	}

	if param.ClusterID > 0 {
		clusterIDs = []int64{param.ClusterID}
	} else {
		clusterIDs = e.getSitesClusterIDs(sites)
	}

	clusterNodePools, err := e.getNodePoolsByClusters(ctx, clusterIDs)
	if err != nil {
		return 0, nil, err
	}

	siteInfos := make([]apistructs.EdgeSiteInfo, 0, len(*sites))

	for i := range *sites {
		nodePoolMap := clusterNodePools[(*sites)[i].ClusterID]
		if nodePool, ok := nodePoolMap[(*sites)[i].Name]; ok {
			clusterInfo, err := e.getClusterInfo(ctx, (*sites)[i].ClusterID)
			if err != nil {
				return 0, nil, err
			}
			item := convertToEdgeSiteInfo(&(*sites)[i], getFormatNodeNum(*nodePool), clusterInfo.Name)
			siteInfos = append(siteInfos, *item)
		} else {
			return 0, nil, fmt.Errorf("node pool %s not found in cluster(id: %d)", (*sites)[i].Name, (*sites)[i].ClusterID)
		}
	}

	return total, &siteInfos, nil
}

// GetEdgeSite get edge site with site id
func (e *Edge) GetEdgeSite(ctx context.Context, edgeSiteID int64) (*apistructs.EdgeSiteInfo, error) {
	var (
		siteInfo *apistructs.EdgeSiteInfo
	)

	site, err := e.db.GetEdgeSite(edgeSiteID)
	if err != nil {
		return nil, err
	}

	clusterNodePool, err := e.getNodePoolsByCluster(ctx, site.ClusterID)
	if err != nil {
		return nil, err
	}

	clusterInfo, err := e.getClusterInfo(ctx, site.ClusterID)
	if err != nil {
		return nil, err
	}

	siteInfo = convertToEdgeSiteInfo(site, getFormatNodeNum(*clusterNodePool[site.Name]), clusterInfo.Name)

	return siteInfo, nil
}

// CreateSite Create edge site.
func (e *Edge) CreateSite(ctx context.Context, req *apistructs.EdgeSiteCreateRequest) (uint64, error) {
	var (
		// DefaultLabelKey: [clusterID].[orgName]
		// s.g. terminus-dev.terminus
		defaultLabelKey = "%s.%s"
		// Labels: s.g. beijing.terminus-dev.terminus: beijing
		labels             = make(map[string]string, 0)
		nodePoolAPIVersion = "apps.openyurt.io/v1alpha1"
		nodePoolKind       = "NodePool"
		// NodePoolType: Edge, Cloud
		edgeNodePoolType v1alpha1.NodePoolType = "Edge"
	)

	edgeSite := &dbclient.EdgeSite{
		OrgID:       req.OrgID,
		Name:        req.Name,
		DisplayName: req.DisplayName,
		Description: req.Description,
		Logo:        req.Logo,
		ClusterID:   req.ClusterID,
		Status:      req.Status,
	}

	// TODO: Field support: Selector, Taints
	// Create node pool first, insert to database if create succeed.
	nodePool := &v1alpha1.NodePool{
		TypeMeta: metav1.TypeMeta{
			APIVersion: nodePoolAPIVersion,
			Kind:       nodePoolKind,
		},
		ObjectMeta: metav1.ObjectMeta{
			Name: req.Name,
		},
		Spec: v1alpha1.NodePoolSpec{
			Type:        edgeNodePoolType,
			Selector:    nil,
			Labels:      nil,
			Annotations: nil,
			Taints:      nil,
		},
	}

	clusterInfo, err := e.getClusterInfo(ctx, req.ClusterID)
	if err != nil {
		return 0, err
	}

	orgInfo, err := e.getOrgInfo(req.OrgID)
	if err != nil {
		return 0, err
	}

	labels[fmt.Sprintf(defaultLabelKey, clusterInfo.Name, orgInfo.Name)] = req.Name
	nodePool.Spec.Labels = labels
	nodePool.Spec.Annotations = labels

	if err = e.k8s.CreateNodePool(clusterInfo.Name, nodePool); err != nil {
		return 0, err
	}

	if err = e.db.CreateEdgeSite(edgeSite); err != nil {
		deleteErr := e.k8s.DeleteNodePool(clusterInfo.Name, req.Name)
		if deleteErr != nil {
			return 0, deleteErr
		}
		return 0, err
	}

	return edgeSite.ID, nil
}

// UpdateSite Update edge site.
func (e *Edge) UpdateSite(edgeSiteID int64, req *apistructs.EdgeSiteUpdateRequest) error {

	edgeSite, err := e.db.GetEdgeSite(edgeSiteID)
	if err != nil {
		return err
	}

	edgeSite.DisplayName = req.DisplayName
	edgeSite.Description = req.Description
	edgeSite.Logo = req.Logo
	edgeSite.Status = req.Status

	if err = e.db.UpdateEdgeSite(edgeSite); err != nil {
		return err
	}

	return nil
}

// DeleteSite Delete edge site.
func (e *Edge) DeleteSite(ctx context.Context, edgeSiteID int64) error {
	edgeSite, err := e.db.GetEdgeSite(edgeSiteID)
	if err != nil || edgeSite == nil {
		return fmt.Errorf("failed to get edgesite, (%v)", err)
	}

	clusterInfo, err := e.getClusterInfo(ctx, edgeSite.ClusterID)
	if err != nil {
		return fmt.Errorf("get cluster (id: %d) error: %v", edgeSite.ClusterID, err)
	}

	np, err := e.k8s.GetNodePool(clusterInfo.Name, edgeSite.Name)
	if err != nil {
		return fmt.Errorf("get nodepool %s in cluster %s error: %v", edgeSite.Name, clusterInfo.Name, err)
	}

	if len(np.Status.Nodes) > 0 {
		return fmt.Errorf("can't delete noempty nodepool, offline it first %s", fmt.Sprint(np.Status.Nodes))
	}

	// If have application deployed in this site, can't delete it.
	edgeSites, err := e.db.GetEdgeAppsBySiteName(edgeSite.Name, edgeSite.ClusterID)
	if err != nil {
		return fmt.Errorf("failed to get releated edge app error: %v", err)
	}

	if len(*edgeSites) != 0 {
		return fmt.Errorf("failed to delete site, "+
			"site %s have releated edge app, please delete it first", edgeSite.Name)
	}

	// If there are some configSet items bind to this site, delete items together.
	cfgItem, err := e.db.GetEdgeConfigSetItemsBySiteID(edgeSiteID)
	if err != nil {
		return fmt.Errorf("failed to get releated configset item in site: %s", edgeSite.Name)
	}

	rmItemError := ""

	for _, item := range *cfgItem {
		if err = e.DeleteConfigSetItem(ctx, int64(item.ID)); err != nil {
			rmItemError += err.Error()
		}
	}

	if err = e.db.DeleteEdgeSite(edgeSiteID); err != nil {
		return fmt.Errorf("failed to delete edgesite, (%v)", err)
	}

	err = e.k8s.DeleteNodePool(clusterInfo.Name, edgeSite.Name)

	if err != nil {
		return err
	}

	if rmItemError != "" {
		return fmt.Errorf("delete releated config item error: %v, please contact administrator", rmItemError)
	}

	return nil
}

// GetInitSiteShell Get edge site init shell.
func (e *Edge) GetInitSiteShell(ctx context.Context, edgeSiteID int64) (map[string][]string, error) {
	var (
		result           = make(map[string][]string)
		initNodeShell    = make([]string, 0)
		shellDownTmpl    = "wget %s/%s && chmod 755 %s"
		shellExeTmpl     = "./%s install -c %s -p %s -s %s -t %s"
		sysNamespace     = "kube-system"
		defaultNamespace = "default"
		diceClusterCM    = "dice-cluster-info"
		secName          = "bootstrap-token-teedge"
		tokenKey         = "token-secret"
		shellNameTmpl    = "initnode_%s"
		tokenTmpl        = "teedge.%s"
	)
	edgeSite, err := e.db.GetEdgeSite(edgeSiteID)
	if err != nil {
		return nil, fmt.Errorf("failed to get edgesite, (%v)", err)
	}

	clusterInfo, err := e.getClusterInfo(ctx, edgeSite.ClusterID)
	if err != nil {
		return nil, err
	}

	teEdgeData, err := e.k8s.GetSecret(clusterInfo.Name, sysNamespace, secName)
	if err != nil {
		return nil, err
	}

	clusterInfoCM, err := e.k8s.GetConfigMap(clusterInfo.Name, defaultNamespace, diceClusterCM)
	if err != nil {
		return nil, fmt.Errorf("get dice cluster info error: %v", err)
	}

	cloudAddr := clusterInfoCM.Data["EDGE_CLOUD_ADDR"]
	cloudPort := clusterInfoCM.Data["EDGE_CLOUD_PORT"]
	toolAddr := clusterInfoCM.Data["EDGE_TOOL_ADDR"]

	shellName := fmt.Sprintf(shellNameTmpl, clusterInfo.Name)
	token := fmt.Sprintf(tokenTmpl, string(teEdgeData.Data[tokenKey]))

	initNodeShell = append(initNodeShell, fmt.Sprintf(shellDownTmpl, toolAddr, shellName, shellName))
	initNodeShell = append(initNodeShell, fmt.Sprintf(shellExeTmpl, shellName, cloudAddr, cloudPort, edgeSite.Name, token))

	result[edgeSite.Name] = initNodeShell

	return result, nil
}

// OfflineEdgeHost Offline edge host and clean monitor data.
func (e *Edge) OfflineEdgeHost(ctx context.Context, edgeSiteID int64, siteIP string) error {
	var (
		nodeFormatter  = "edgenode-%s-%s"
		appAddonType   = "addon"
		appNsFormatter = "edgeapp-%s"
		stsLabels      = "statefulset.kubernetes.io/pod-name"
		offlineTag     = "dice/offline"
		cleanDelayTime = 30 * time.Second
	)

	edgeSite, err := e.db.GetEdgeSite(edgeSiteID)
	if err != nil {
		return fmt.Errorf("get edge site (id: %d) error: %v", edgeSiteID, err)
	}

	clusterInfo, err := e.getClusterInfo(ctx, edgeSite.ClusterID)
	if err != nil {
		return fmt.Errorf("get cluster (id: %d) info error: %v", edgeSite.ClusterID, err)
	}

	edgeApps, err := e.db.ListEdgeAppBySiteName(edgeSite.OrgID, edgeSite.ClusterID, edgeSite.Name)
	if err != nil {
		return fmt.Errorf("get reference apps in site %s eror: %v", edgeSite.Name, err)
	}

	tipsStsNames := make([]string, 0)
	tipsDeployNames := make([]string, 0)

	for _, edgeApp := range *edgeApps {
		// Only mysql-edge is sts type in this version.
		if edgeApp.Type != appAddonType {
			tipsDeployNames = append(tipsDeployNames, edgeApp.Name)
			continue
		}

		nsName := fmt.Sprintf(appNsFormatter, edgeApp.Name)
		podList, err := e.k8s.ListPod(clusterInfo.Name, nsName)
		if err != nil {
			return fmt.Errorf("list pod in namespaces %s error: %v", nsName, err)
		}

		for _, po := range podList.Items {
			if _, ok := po.Labels[stsLabels]; !ok {
				continue
			}
			//if po.Status.Phase == v1.PodRunning && po.Status.HostIP == siteIP {
			if po.Status.HostIP == siteIP {
				tipsStsNames = append(tipsStsNames, edgeApp.Name)
			}
		}
	}

	if len(tipsStsNames) != 0 {
		return fmt.Errorf("there are statefulset app %s in site: %s", fmt.Sprint(tipsStsNames), edgeSite.Name)
	}

	if len(tipsDeployNames) != 0 {
		np, err := e.k8s.GetNodePool(clusterInfo.Name, edgeSite.Name)
		if err != nil {
			return fmt.Errorf("get node pool error: %v", err)
		}
		if len(np.Status.Nodes) <= 1 {
			return fmt.Errorf("there are applications %s on the last host", fmt.Sprint(tipsDeployNames))
		}
	}

	nodeName := fmt.Sprintf(nodeFormatter, edgeSite.Name, siteIP)

	node, err := e.k8s.GetNode(clusterInfo.Name, nodeName)
	if err != nil {
		return err
	}

	node.Labels[offlineTag] = "true"

	delete(node.Labels, v1alpha1.LabelDesiredNodePool)

	if err = e.k8s.UpdateNode(clusterInfo.Name, node); err != nil {
		return fmt.Errorf("update node labels error: %v", err)
	}

	go func() {
		ticker := time.NewTimer(cleanDelayTime)
		select {
		case <-ticker.C:
			if err = e.k8s.DeleteNode(clusterInfo.Name, nodeName); err != nil {
				logrus.Errorf("delete node %s eror: %v", nodeName, err)
				return
			}

			// delete monitor data.
			if err = cleanOfflineData(clusterInfo.Name, siteIP); err != nil {
				logrus.Errorf("clean monitor data %s error: %v", siteIP, err)
			}
			break
		}
		ticker.Stop()
		logrus.Infof("offline edge host %s in cluster %s succes", siteIP, clusterInfo.Name)
		return
	}()
	return nil
}

// getNodePoolsByClusters Get node pools by clusters.
func (e *Edge) getNodePoolsByClusters(ctx context.Context, clusterIDs []int64) (map[int64]NodePools, error) {
	var (
		clustersNodePools = make(map[int64]NodePools, 0)
	)
	for _, clusterID := range clusterIDs {
		nodePools, err := e.getNodePoolsByCluster(ctx, clusterID)
		if err != nil {
			return clustersNodePools, fmt.Errorf("get node pool (cluster: %v) error, %v", clusterID, err)
		}
		clustersNodePools[clusterID] = nodePools
	}
	return clustersNodePools, nil
}

// getNodePoolsByCluster Get all node pool by clusterID.
func (e *Edge) getNodePoolsByCluster(ctx context.Context, clusterID int64) (NodePools, error) {
	var (
		nodePoolList *v1alpha1.NodePoolList
		nodePools    = make(NodePools, 0)
	)

	clusterInfo, err := e.getClusterInfo(ctx, clusterID)
	if err != nil {
		return nil, err
	}

	nodePoolList, err = e.k8s.ListNodePool(clusterInfo.Name)
	if err != nil {
		return nodePools, err
	}

	return convertNPListToMap(nodePoolList), nil
}

// getSitesClusterIDs Get clusters by sites.
func (e *Edge) getSitesClusterIDs(sites *[]dbclient.EdgeSite) []int64 {
	var (
		tmpMap     = make(map[int64]struct{}, 0)
		clusterIDs = make([]int64, 0, len(*sites))
	)

	if sites == nil {
		return clusterIDs
	}

	for _, item := range *sites {
		if _, ok := tmpMap[item.ClusterID]; !ok {
			tmpMap[item.ClusterID] = struct{}{}
			clusterIDs = append(clusterIDs, item.ClusterID)
		}
	}

	return clusterIDs
}

// convertToEdgeSiteInfo Convert type EdgeSite to type EdgeSiteInfo.
func convertToEdgeSiteInfo(edgeSite *dbclient.EdgeSite, nodeCount, clusterName string) *apistructs.EdgeSiteInfo {
	return &apistructs.EdgeSiteInfo{
		ID:          int64(edgeSite.ID),
		OrgID:       edgeSite.OrgID,
		Name:        edgeSite.Name,
		DisplayName: edgeSite.DisplayName,
		Description: edgeSite.Description,
		Logo:        edgeSite.Logo,
		ClusterID:   edgeSite.ClusterID,
		ClusterName: clusterName,
		Status:      edgeSite.Status,
		NodeCount:   nodeCount,
		CreatedAt:   edgeSite.CreatedAt,
		UpdatedAt:   edgeSite.UpdatedAt,
	}
}

// convertNPListToMap Convert type NodePoolList to map[string]NodePool.
func convertNPListToMap(nodePoolList *v1alpha1.NodePoolList) map[string]*v1alpha1.NodePool {
	var (
		res = make(map[string]*v1alpha1.NodePool, 0)
	)

	if nodePoolList == nil || len(nodePoolList.Items) == 0 {
		logrus.Errorf("can't convert type NodePoolList to Map, NodePoolList is nil.")
		return res
	}
	for index := range nodePoolList.Items {
		res[nodePoolList.Items[index].Name] = &nodePoolList.Items[index]
	}

	return res
}

// getFormatNodeNum format node num.
func getFormatNodeNum(nodePool v1alpha1.NodePool) string {
	return fmt.Sprintf(siteNodeCountFormat, nodePool.Status.ReadyNodeNum, nodePool.Status.ReadyNodeNum+nodePool.Status.UnreadyNodeNum)
}

// cleanOfflineData clean offline host data
func cleanOfflineData(clusterName, hostIP string) error {
	const (
		reqUrl     = "/api/resources/hosts/actions/offline"
		monitorEnv = "MONITOR_ADDR"
	)

	resp, err := httpclient.New().Post(os.Getenv(monitorEnv)).
		Path(reqUrl).
		Header("Content-Type", "application/json").
		JSONBody(struct {
			ClusterName string   `json:"clusterName"`
			HostIPs     []string `json:"hostIPs"`
		}{
			ClusterName: clusterName,
			HostIPs:     []string{hostIP},
		}).Do().DiscardBody()

	if err != nil || !resp.IsOK() {
		logrus.Errorf("call monitor offline api failed: %+v, %+v", err, resp)
		return err
	}

	return nil
}
