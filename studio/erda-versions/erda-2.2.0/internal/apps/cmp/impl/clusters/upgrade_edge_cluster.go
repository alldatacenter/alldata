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

package clusters

import (
	"context"
	"fmt"
	"os"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/golang-collections/collections/set"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/http/httputil"
)

type Clusters struct {
	db         *dbclient.DBClient
	bdl        *bundle.Bundle
	credential tokenpb.TokenServiceServer
	clusterSvc clusterpb.ClusterServiceServer
}

func New(db *dbclient.DBClient, bdl *bundle.Bundle, c tokenpb.TokenServiceServer, clusterSvc clusterpb.ClusterServiceServer) *Clusters {
	return &Clusters{db: db, bdl: bdl, credential: c, clusterSvc: clusterSvc}
}

// status:
//		1 -- in processing, jump to check log
//		2 -- do precheck
//		3 -- invalid, do not support (non k8s cluster, central cluster, higher version ecluster)
func (c *Clusters) UpgradeEdgeCluster(ctx context.Context, req apistructs.UpgradeEdgeClusterRequest, userid string, orgid string) (recordID uint64, status int, precheckHint string, err error) {
	records, err := getUpgradeRecords(c.db, req.ClusterName)
	if err != nil {
		errstr := fmt.Sprintf("failed to query record: %v", err)
		logrus.Errorf(errstr)
		return
	}
	if len(records) > 0 {
		if records[0].Status == dbclient.StatusTypeProcessing &&
			records[0].CreatedAt.After(time.Now().Add(-2*time.Hour)) {
			status = 1
			precheckHint = "The cluster is being upgraded, whether to jump to view the progress"
			recordID = records[0].ID
			return
		}
	}
	clusterInfo, err := c.bdl.QueryClusterInfo(req.ClusterName)
	if err != nil {
		errstr := fmt.Sprintf("failed to queryclusterinfo: %v, clusterinfo: %v", err, clusterInfo)
		logrus.Errorf(errstr)
		return
	}
	if !clusterInfo.IsK8S() {
		status = 3
		precheckHint = "Doesn't support upgrade cluster which not k8s type"
		return
	}
	if clusterInfo.Get(apistructs.DICE_IS_EDGE) != "true" {
		status = 3
		precheckHint = "Doesn't support upgrade central cluster"
		return
	}
	centralClusterInfo, err := c.bdl.QueryClusterInfo(os.Getenv("DICE_CLUSTER_NAME"))
	if err != nil {
		errStr := fmt.Sprintf("failed to queryclusterinfo: %v, clusterinfo: %v", err, clusterInfo)
		logrus.Errorf(errStr)
		return
	}

	edgeVersion := clusterInfo.Get(apistructs.DICE_VERSION)
	centralVersion := centralClusterInfo.Get(apistructs.DICE_VERSION)
	logrus.Infof("upgrade edge cluster [%v], edge cluster version [%v], central version [%v]",
		req.ClusterName, edgeVersion, centralVersion)

	if strings.Compare(edgeVersion, centralVersion) > 0 {
		status = 3
		precheckHint = fmt.Sprintf("Edge cluster version [%s] above the central cluster version[%s], don't need upgrade", edgeVersion, centralVersion)
		return
	}

	if req.PreCheck {
		status = 2
		precheckHint = fmt.Sprintf("Will upgrade from version [%s] to [%s], please confirm it", edgeVersion, centralVersion)
		return
	}

	// get cluster access key
	cak, err := c.GetOrCreateAccessKey(ctx, req.ClusterName)
	if err != nil {
		logrus.Errorf("get cluster access key failed, cluster: %s, error: %v", req.ClusterName, err)
		return
	}

	if cak.AccessKey == "" {
		err = fmt.Errorf("empty cluster access key, cluster: %s", req.ClusterName)
		logrus.Errorf(err.Error())
		return
	}

	yml := apistructs.PipelineYml{
		Version: "1.1",
		Stages: [][]*apistructs.PipelineYmlAction{{{
			Type:    "upgrade-edge-cluster",
			Version: "1.0",
			Params: map[string]interface{}{
				"dice_version":       centralClusterInfo.Get(apistructs.DICE_VERSION),
				"cluster_access_key": cak.AccessKey,
			},
		}}},
	}

	b, err := yaml.Marshal(yml)
	if err != nil {
		errstr := fmt.Sprintf("failed to marshal pipelineyml: %v", err)
		logrus.Errorf(errstr)
		return
	}

	dto, err := c.bdl.CreatePipeline(&apistructs.PipelineCreateRequestV2{
		PipelineYml: string(b),
		PipelineYmlName: fmt.Sprintf("ops-upgrade-edge-cluster-%s.yml",
			clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME)),
		ClusterName:    clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME),
		PipelineSource: apistructs.PipelineSourceOps,
		AutoRunAtOnce:  true,
	})
	if err != nil {
		errstr := fmt.Sprintf("failed to createpipeline: %v", err)
		logrus.Errorf(errstr)
		return
	}
	recordID, err = createRecord(c.db, dbclient.Record{
		RecordType:  dbclient.RecordTypeUpgradeEdgeCluster,
		UserID:      userid,
		OrgID:       orgid,
		ClusterName: req.ClusterName,
		Status:      dbclient.StatusTypeProcessing,
		Detail:      "",
		PipelineID:  dto.ID,
	})

	if err != nil {
		errstr := fmt.Sprintf("failed to create record: %v", err)
		logrus.Errorf(errstr)
		return
	}
	return
}

func getUpgradeRecords(db *dbclient.DBClient, cluster string) ([]dbclient.Record, error) {
	return db.RecordsReader().ByClusterNames(cluster).ByRecordTypes(dbclient.RecordTypeUpgradeEdgeCluster.String()).Do()
}

func (c *Clusters) BatchUpgradeEdgeCluster(ctx context.Context, req apistructs.BatchUpgradeEdgeClusterRequest, userid string) {
	for _, v := range req.Clusters {
		recordID, status, precheckHint, err := c.UpgradeEdgeCluster(ctx,
			apistructs.UpgradeEdgeClusterRequest{
				OrgID:       v.OrgID,
				ClusterName: v.ClusterName},
			userid,
			strconv.FormatUint(v.OrgID, 10))
		if err == nil && recordID != 0 {
			continue
		}
		orgIdStr := strconv.FormatUint(v.OrgID, 10)
		if err != nil {
			err = fmt.Errorf("update edge cluster failed, org id:%d, cluster:%s, error:%v", v.OrgID, v.ClusterName, err)
			logrus.Errorf(err.Error())
			_, er := c.db.RecordsWriter().Create(&dbclient.Record{
				RecordType:  dbclient.RecordTypeUpgradeEdgeCluster,
				UserID:      userid,
				OrgID:       orgIdStr,
				ClusterName: v.ClusterName,
				Status:      dbclient.StatusTypeFailed,
				Detail:      err.Error(),
				PipelineID:  0,
			})
			if er != nil {
				logrus.Errorf("failed to create record: %v", err)
			}
		}
		switch status {
		case 1:
			logrus.Warnf("cluster upgrade ignore, org [%d], cluster [%s] is in upgrading", v.OrgID, v.ClusterName)
		case 2:
			logrus.Warnf("cluster upgrade ignore, org [%d], cluster [%s] need precheck", v.OrgID, v.ClusterName)
		case 3:
			logrus.Warnf("cluster upgrade ignore, org [%d], cluster [%s], %s", v.OrgID, v.ClusterName, precheckHint)
		default:
			logrus.Errorf("cluster upgrade, org [%d], cluster [%s], invalid status code:%d", v.OrgID, v.ClusterName, status)
		}
	}
}

func (c *Clusters) GetOrgInfo(req *apistructs.OrgSearchRequest) (map[uint64]apistructs.OrgDTO, error) {
	orgs, err := c.bdl.ListOrgs(req, "")
	if err != nil {
		return nil, err
	}
	result := make(map[uint64]apistructs.OrgDTO, len(orgs.List))
	for _, v := range orgs.List {
		result[v.ID] = v
	}
	return result, nil
}

func (c *Clusters) ListClusters(ctx context.Context, req apistructs.OrgClusterInfoRequest) (result []apistructs.OrgClusterInfoBasicData, err error) {
	orgsInfo := make(map[uint64]apistructs.OrgDTO)
	result = make([]apistructs.OrgClusterInfoBasicData, 0)
	// get org info
	orgsInfo, err = c.GetOrgInfo(&apistructs.OrgSearchRequest{
		Q:        req.OrgName,
		PageNo:   1,
		PageSize: 100,
	})
	if err != nil {
		return nil, err
	}
	// get org cluster info
	if len(orgsInfo) == 0 {
		return result, nil
	}
	clusters := make([]*clusterpb.ClusterInfo, 0)
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	if req.OrgName != "" && len(orgsInfo) < 5 {
		var wg sync.WaitGroup
		for k := range orgsInfo {
			wg.Add(1)
			go func(orgid uint64) {
				defer func() {
					wg.Done()
				}()
				r, e := c.clusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{
					ClusterType: req.ClusterType,
					OrgID:       uint32(orgid),
				})
				if e != nil {
					err = e
					return
				}
				clusters = append(clusters, r.Data...)
			}(k)
		}
		wg.Wait()
	} else {
		var resp *clusterpb.ListClusterResponse
		resp, err = c.clusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{
			ClusterType: req.ClusterType,
		})
		clusters = resp.Data
	}

	if err != nil {
		return
	}
	cSet := set.New()
	for _, v := range clusters {
		// Filter clusters which duplicate related
		if cSet.Has(v.Name) {
			continue
		}
		cSet.Insert(v.Name)
		result = append(result,
			apistructs.OrgClusterInfoBasicData{
				ClusterName: v.Name,
				ClusterType: v.Type,
				Version:     "",
				CreateTime:  v.CreatedAt.AsTime().UTC().Format("2006-01-02T15:04:05Z"),
			})
	}
	sort.Sort(orgClusterInfoList(result))
	return result, nil
}

func (c *Clusters) UpdateClusterVersion(req []apistructs.OrgClusterInfoBasicData) error {
	var wg sync.WaitGroup
	if len(req) == 0 {
		return nil
	}
	wg.Add(len(req))
	for i, v := range req {
		val := v
		go func(oc *apistructs.OrgClusterInfoBasicData) {
			defer func() {
				wg.Done()
			}()
			cid, err := c.bdl.QueryClusterInfo(oc.ClusterName)
			if err != nil {
				logrus.Errorf("query cluster info failed, request:%+v, error:%v", val, err)
			}
			oc.Version = cid.Get(apistructs.DICE_VERSION)
			isEdgeCluster := cid.Get(apistructs.DICE_IS_EDGE)
			if isEdgeCluster == "false" {
				oc.IsCentralCluster = true
			} else {
				oc.IsCentralCluster = false
			}
		}(&req[i])
	}
	wg.Wait()
	return nil
}

func (c *Clusters) GetOrgClusterInfo(ctx context.Context, req apistructs.OrgClusterInfoRequest) (apistructs.OrgClusterInfoData, error) {
	var start, end int
	result := apistructs.OrgClusterInfoData{}
	clusters, err := c.ListClusters(ctx, req)
	if err != nil {
		return result, err
	}
	result.Total = len(clusters)

	end = req.PageNo * req.PageSize
	start = (req.PageNo - 1) * req.PageSize
	if start >= result.Total {
		return result, nil
	} else if end > result.Total {
		result.List = clusters[start:]
	} else {
		result.List = clusters[start:end]
	}
	_ = c.UpdateClusterVersion(result.List)
	return result, nil
}

type orgClusterInfoList []apistructs.OrgClusterInfoBasicData

func (o orgClusterInfoList) Len() int {
	return len(o)
}

func (o orgClusterInfoList) Less(i, j int) bool {
	return strings.Compare(o[i].OrgName, o[j].OrgName) < 0
}

func (o orgClusterInfoList) Swap(i, j int) {
	o[i], o[j] = o[j], o[i]
}
