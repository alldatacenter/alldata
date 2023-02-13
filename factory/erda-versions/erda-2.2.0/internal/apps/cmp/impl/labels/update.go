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

package labels

import (
	"encoding/json"
	"errors"
	"fmt"
	"regexp"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

type Labels struct {
	db  *dbclient.DBClient
	bdl *bundle.Bundle
}

func New(db *dbclient.DBClient, bdl *bundle.Bundle) *Labels {
	return &Labels{
		db:  db,
		bdl: bdl,
	}
}

func (l *Labels) UpdateLabels(req apistructs.UpdateLabelsRequest, userid string) (recordID uint64, err error) {
	clusterInfo, err := l.bdl.QueryClusterInfo(req.ClusterName)
	if err != nil {
		errstr := fmt.Sprintf("failed to QueryClusterInfo, clustername: %s, err: %v", req.ClusterName, err)
		logrus.Errorf(errstr)
		err = errors.New(errstr)
		return
	}
	if clusterInfo.IsDCOS() {
		_, err = soldierUpdateLabels(clusterInfo, req)
	} else if clusterInfo.IsK8S() || clusterInfo.IsEDAS() {
		err = kubernetesUpdateLabels(clusterInfo, req)
	} else {
		errstr := fmt.Sprintf("updatelabels: unsupported clustertype: %v", req)
		logrus.Error(errstr)
		err = errors.New(errstr)
		return
	}
	status := dbclient.StatusTypeSuccess
	if err != nil {
		status = dbclient.StatusTypeFailed
	}
	type Detail struct {
		apistructs.UpdateLabelsRequest
		Message string `json:"message"`
	}
	var detailStruct Detail
	detailStruct.UpdateLabelsRequest = req
	detailStruct.Message = ""
	if err != nil {
		detailStruct.Message = err.Error()
	}
	detail, err := json.Marshal(detailStruct)
	if err != nil {
		return
	}
	recordID, err = l.db.RecordsWriter().Create(&dbclient.Record{
		RecordType:  dbclient.RecordTypeSetLabels,
		UserID:      userid,
		OrgID:       strconv.FormatUint(req.OrgID, 10),
		ClusterName: req.ClusterName,
		Status:      status,
		Detail:      string(detail),
	})
	return
}

type updateLabelsRequest struct {
	Hosts []string `json:"hosts"`
	Tag   string   `json:"tag"`
	Sync  bool     `json:"sync"`
}

func soldierUpdateLabels(clusterInfo apistructs.ClusterInfoData, req apistructs.UpdateLabelsRequest) (string, error) {
	var res struct {
		Success bool
		Data    string
		Err     struct {
			Code string
			Msg  string
		}
	}

	var req_ updateLabelsRequest
	req_.Hosts = req.Hosts
	req_.Sync = true
	var existStatefulService, existStatelessService bool
	for _, l := range req.Labels {
		if l == "stateful-service" {
			existStatefulService = true
		} else if l == "stateless-service" {
			existStatelessService = true
		}
	}
	if existStatefulService {
		req.Labels = append(req.Labels, "service-stateful")
	}
	if existStatelessService {
		req.Labels = append(req.Labels, "service-stateless")
	}
	req_.Tag = strutil.Join(req.Labels, ",")
	u := discover.Soldier()
	if clusterInfo.MustGet(apistructs.DICE_IS_EDGE) == "true" {
		u = clusterInfo.MustGetPublicURL("soldier")
	}
	r, err := httpclient.New(httpclient.WithCompleteRedirect()).
		Post(u).
		Path("/api/nodes/tag").
		JSONBody(req_).
		Do().JSON(&res)
	if err != nil {
		return "", err
	}
	if !r.IsOK() {
		return "", fmt.Errorf("call soldier failed, cluster name: %s, status code: %d",
			clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME), r.StatusCode())
	}
	if !res.Success {
		return "", fmt.Errorf("call soldier failed, cluster name: %s, err code: %s, err msg: %s",
			clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME), res.Err.Code, res.Err.Msg)
	}
	if res.Data != "" {
		if regexp.MustCompile(`^tag-\d+\.log$`).MatchString(res.Data) {
			logrus.Infof("cluster name: %s, update labels: %s",
				clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME), res.Data)
			return res.Data, nil
		} else {
			return "", errors.New(res.Data)
		}
	}
	return "", nil
}

func kubernetesUpdateLabels(clusterInfo apistructs.ClusterInfoData, req apistructs.UpdateLabelsRequest) error {
	var res struct {
		Success bool
		Err     struct {
			Code string
			Msg  string
		}
	}
	tag := make(map[string]string)
	for _, l := range req.Labels {
		if strutil.HasPrefixes(l, "dice/") {
			tag[l] = "true"
		} else {
			tag["dice/"+l] = "true"
		}
	}
	for k, v := range req.LabelsWithValue {
		if strutil.HasPrefixes(k, "dice/") {
			tag[k] = v
		} else {
			tag["dice/"+k] = v
		}
	}
	r, err := httpclient.New(httpclient.WithCompleteRedirect()).
		Post(discover.Orchestrator()).
		Path("/api/nodelabels").
		JSONBody(map[string]interface{}{
			"clustername": clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME),
			"clustertype": clusterInfo.MustGet(apistructs.DICE_CLUSTER_TYPE),
			"hosts":       req.Hosts,
			"tag":         tag,
		}).
		Do().JSON(&res)
	if err != nil {
		return err
	}
	if !r.IsOK() {
		return fmt.Errorf("call scheduler failed, cluster name: %s, status code: %d",
			clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME), r.StatusCode())
	}
	if !res.Success {
		return fmt.Errorf("call scheduler failed, cluster name: %s, err code: %s, err msg: %s",
			clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME), res.Err.Code, res.Err.Msg)
	}
	return nil
}
