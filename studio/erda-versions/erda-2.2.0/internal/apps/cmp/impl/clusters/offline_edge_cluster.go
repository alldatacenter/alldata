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
	"errors"
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/http/httputil"
)

type precheckResp struct {
	Success bool `json:"success"`
	Data    bool `json:"data"`
}
type cmdbDeleteClusterResp struct {
	Success bool   `json:"success"`
	Data    string `json:"data"`
}

func (c *Clusters) OfflineEdgeCluster(ctx context.Context, req apistructs.OfflineEdgeClusterRequest, userid string, orgid string) (recordID uint64, preCheckHint string, err error) {
	var (
		fakecluster bool
	)

	clusterInfo, er := c.bdl.QueryClusterInfo(req.ClusterName)
	if er != nil {
		fakecluster = true
	} else {
		isEdgeCluster := clusterInfo.Get(apistructs.DICE_IS_EDGE)
		if isEdgeCluster != "true" {
			errstr := fmt.Sprintf("unsupport to offline non-edge cluster, clustername: %s", clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME))
			logrus.Errorf(errstr)
			err = errors.New(errstr)
			return
		}
	}
	status := dbclient.StatusTypeSuccess
	detail := ""
	if !fakecluster && !req.Force {
		// project cluster refer check
		var projReferred bool
		projReferred, err = c.bdl.ProjectClusterReferred(userid, orgid, req.ClusterName)
		if err != nil {
			status = dbclient.StatusTypeFailed
			logrus.Errorf("check project cluster refer info failed, orgid: %s, cluster_name: %s", orgid, req.ClusterName)
			return
		}

		if projReferred {
			status = dbclient.StatusTypeFailed
			detail = "An existing project is using the cluster and cannot offline this cluster if force offline is not set."
			// pre-check failed
			if req.PreCheck {
				preCheckHint = detail
				return
			}
		}

		// runtime cluster refer check
		if status == dbclient.StatusTypeSuccess {
			var referenceResp *apistructs.ResourceReferenceData
			var rReferred bool
			referenceResp, err = c.bdl.FindClusterResource(req.ClusterName, orgid)
			if err != nil {
				status = dbclient.StatusTypeFailed
				logrus.Errorf("check runtime cluster refer info failed, orgid: %s, cluster_name: %s", orgid, req.ClusterName)
				return
			}

			if referenceResp.AddonReference > 0 || referenceResp.ServiceReference > 0 {
				rReferred = true
			}

			if rReferred {
				status = dbclient.StatusTypeFailed
				detail = "There are Runtimes (Addons) in the cluster, cannot offline this cluster if force offline is not set."
				// pre-check failed
				if req.PreCheck {
					preCheckHint = detail
					return
				}
			}
		}
	}

	if req.PreCheck {
		logrus.Infof("edge cluster offline line pre check done")
		return
	}

	// Offline cluster by call cmd /api/clusters/<clusterName>
	if status == dbclient.StatusTypeSuccess {
		if _, err = c.bdl.DereferenceCluster(req.OrgID, req.ClusterName, userid, req.Force); err != nil {
			return
		}

		var relations []apistructs.OrgClusterRelationDTO
		relations, err = c.bdl.ListOrgClusterRelation(userid, req.ClusterName)
		if err != nil {
			logrus.Errorf("list org cluster relation failed, cluster: %s, error: %v", req.ClusterName, err)
			return
		}

		if len(relations) == 0 {
			ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
			_, err = c.clusterSvc.DeleteCluster(ctx, &clusterpb.DeleteClusterRequest{ClusterName: req.ClusterName})
			if err != nil {
				errstr := fmt.Sprintf("failed to delete cluster %s : %v", req.ClusterName, err)
				logrus.Errorf(errstr)
				err = errors.New(errstr)
				return
			}

			// Delete accessKey
			if err = c.DeleteAccessKey(req.ClusterName); err != nil {
				errStr := fmt.Sprintf("failed to delete cluster access key, cluster: %v, err: %v", req.ClusterName, err)
				logrus.Error(errStr)
				return
			}
		}
	}

	if req.OrgID <= 0 {
		return
	}

	recordID, err = createRecord(c.db, dbclient.Record{
		RecordType:  dbclient.RecordTypeOfflineEdgeCluster,
		UserID:      userid,
		OrgID:       orgid,
		ClusterName: req.ClusterName,
		Status:      status,
		Detail:      detail,
	})

	if err != nil {
		// ignore record update error
		logrus.Errorf("update cluster delete record failed, cluster: %s, error: %v", req.ClusterName, err)
	}

	logrus.Infof("detail: %v", detail)
	return
}

func createRecord(db *dbclient.DBClient, record dbclient.Record) (recordID uint64, err error) {
	return db.RecordsWriter().Create(&record)
}

func (c *Clusters) BatchOfflineEdgeCluster(ctx context.Context, req apistructs.BatchOfflineEdgeClusterRequest, userid string) error {
	for _, cluster := range req.Clusters {
		req := apistructs.OfflineEdgeClusterRequest{
			OrgID:       0,
			ClusterName: cluster,
			Force:       true,
		}
		_, _, err := c.OfflineEdgeCluster(ctx, req, "onlyYou", strconv.Itoa(int(req.OrgID)))
		if err != nil {
			err := fmt.Errorf("cluster offline failed, cluster: %s, error: %v", cluster, err)
			logrus.Errorf(err.Error())
			return err
		}
	}
	return nil
}
