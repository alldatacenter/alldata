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

package orgapis

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"strconv"

	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/httpserver"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
	api "github.com/erda-project/erda/pkg/common/httpapi"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (p *provider) checkOrgMetrics(ctx httpserver.Context) (string, error) {
	req := ctx.Request()
	idStr := api.OrgID(req)
	orgID, err := strconv.ParseUint(idStr, 10, 64)
	if err != nil {
		return "", fmt.Errorf("Org-ID is not number")
	}

	// 校验orgID
	for _, key := range []string{
		"filter_org_id", "filter_tags.org_id",
		"eq_org_id", "eq_tags.org_id",
		"filter_dice_org_id", "filter_tags.dice_org_id",
		"eq_dice_org_id", "eq_tags.dice_org_id",
	} {
		diceOrgID := req.URL.Query().Get(key)
		if diceOrgID != "" {
			if diceOrgID != idStr {
				return "", fmt.Errorf("orgID %s is not match", diceOrgID)
			}
			return idStr, nil
		}
	}

	// 校验orgName
	for _, key := range []string{
		"filter_org_name", "filter_tags.org_name",
		"eq_org_name", "eq_tags.org_name",
	} {
		orgName := req.URL.Query().Get(key)
		if orgName != "" {
			info, err := p.bundle.GetOrg(orgID)
			if err != nil {
				return "", err
			}
			if info == nil {
				return "", fmt.Errorf("not found org")
			}
			if orgName != info.Name {
				return "", fmt.Errorf("orgName %s is not match", orgName)
			}
			return idStr, nil
		}
	}

	// 校验集群
	for _, key := range []string{
		"filter_cluster_name", "filter_tags.cluster_name",
		"eq_cluster_name", "eq_tags.cluster_name",
		"in_cluster_name", // in_cluster_name 这个会有权限检查遗漏的问题，后面优化一下，要么去掉，要么 判断所有in_cluster_name的值 都通过权限校验
	} {
		clusterNames := req.URL.Query()[key]
		if len(clusterNames) > 0 {
			err := p.checkOrgIDByClusters(orgID, clusterNames)
			if err != nil {
				return "", err
			}
			return idStr, nil
		}
	}

	// 校验项目
	for _, key := range []string{
		"filter_project_id", "filter_tags.project_id",
		"eq_project_id", "eq_tags.project_id",
	} {
		projectID := req.URL.Query().Get(key)
		if len(projectID) > 0 {
			if id, err := strconv.ParseUint(projectID, 10, 64); err == nil {
				proj, err := p.bundle.GetProject(id)
				if err != nil || proj == nil {
					return "", fmt.Errorf("fail to get project info: %s", err)
				}
				if proj.OrgID != uint64(orgID) {
					return "", fmt.Errorf("not found project %d in org %d", id, orgID)
				}
				return idStr, nil
			}
		}
	}

	// 校验应用
	for _, key := range []string{
		"filter_application_id", "filter_tags.application_id",
		"eq_application_id", "eq_tags.application_id",
	} {
		appID := req.URL.Query().Get(key)
		if len(appID) > 0 {
			if id, err := strconv.ParseUint(appID, 10, 64); err == nil {
				app, err := p.bundle.GetApp(id)
				if err != nil || app == nil {
					return "", fmt.Errorf("fail to get application info: %s", err)
				}
				if app.OrgID != uint64(orgID) {
					return "", fmt.Errorf("not found application %d in org %d", id, orgID)
				}
				return idStr, nil
			}
		}
	}

	info, err := p.bundle.GetOrg(orgID)
	if err != nil {
		return "", err
	}
	if info == nil {
		return "", fmt.Errorf("not found org")
	}
	clusters, err := p.listClustersByOrg(orgID)
	if err != nil {
		return "", fmt.Errorf("fail to list cluster by org(%d)", orgID)
	}
	q := ctx.Request().URL.Query()
	q.Add("or_in_org_name", info.Name)
	for _, cluster := range clusters {
		q.Add("or_in_cluster_name", cluster)
	}
	ctx.Request().URL.RawQuery = q.Encode()
	return idStr, nil
}

// 根据集群校验企业ID
func (p *provider) checkOrgIDByClusters(orgID uint64, clusterNames []string) error {
	resp, err := p.cmdb.QueryAllOrgClusterRelation()
	if err != nil {
		return err
	}
	clustersMap := make(map[string]bool, len(resp))
	for _, item := range resp {
		if item.OrgID == orgID {
			clustersMap[item.ClusterName] = true
		}
	}
	for _, clusterName := range clusterNames {
		if !clustersMap[clusterName] {
			return fmt.Errorf("cluster %s is not match", clusterName)
		}
	}
	return nil
}

func (p *provider) listClustersByOrg(orgID uint64) ([]string, error) {
	ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := p.ClusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{OrgID: uint32(orgID)})
	if err != nil {
		return nil, err
	}
	var list []string
	for _, item := range resp.Data {
		list = append(list, item.Name)
	}
	return list, nil
}

func (p *provider) checkOrgByClusters(ctx httpserver.Context, clusters []*resourceCluster) error {
	idStr := api.OrgID(ctx.Request())
	orgID, err := strconv.ParseUint(idStr, 10, 64)
	if err != nil {
		msg := fmt.Sprint("Org-ID is not number")
		permission.Failure(ctx, msg)
		return errors.New(msg)
	}

	clusterNames := make([]string, 0, len(clusters))
	for _, cluster := range clusters {
		clusterNames = append(clusterNames, cluster.ClusterName)
	}
	if err := p.checkOrgIDByClusters(orgID, clusterNames); err != nil {
		permission.Failure(ctx, err.Error())
		return err
	}
	return nil
}

func (p *provider) getOrgIDNameFromBody(ctx httpserver.Context) (string, error) {
	req := ctx.Request()
	byts, err := ioutil.ReadAll(req.Body)
	if err != nil {
		return "", err
	}
	req.Body = ioutil.NopCloser(bytes.NewReader(byts))
	var body struct {
		OrgName string `json:"org_name"`
	}
	err = json.Unmarshal(byts, &body)
	if err != nil {
		return "", err
	}
	orgInfo, err := p.bundle.GetOrg(body.OrgName)
	if err != nil {
		return "", fmt.Errorf("fail to found org info: %s", err)
	}
	if orgInfo == nil {
		return "", fmt.Errorf("fail to found org info")
	}
	ctx.SetAttribute("Org-ID", int(orgInfo.ID))
	return strconv.Itoa(int(orgInfo.ID)), nil
}
