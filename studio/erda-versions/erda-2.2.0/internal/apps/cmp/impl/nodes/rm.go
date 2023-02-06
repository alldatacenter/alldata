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

package nodes

import (
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/crypto/encrypt"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/strutil"
)

func (n *Nodes) RmNodes(req apistructs.RmNodesRequest, userid string, orgid string) (uint64, error) {
	var recordID uint64
	clusterInfo, err := n.bdl.QueryClusterInfo(req.ClusterName)
	if err != nil {
		errstr := fmt.Sprintf("failed to queryclusterinfo: %v, clusterinfo: %v", err, clusterInfo)
		logrus.Errorf(errstr)
		return recordID, err
	}
	if !clusterInfo.IsK8S() {
		errstr := fmt.Sprintf("unsupported cluster type, cluster name: %s, cluster type: %s",
			clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME), clusterInfo.MustGet(apistructs.DICE_CLUSTER_TYPE))
		logrus.Errorf(errstr)
		err := errors.New(errstr)
		return recordID, err
	}
	force := ""
	if req.Force {
		force = "true"
	}
	yml := apistructs.PipelineYml{
		Version: "1.1",
		Stages: [][]*apistructs.PipelineYmlAction{{{
			Type:    "rm-nodes",
			Version: "1.0",
			Params: map[string]interface{}{
				"hosts":    strutil.Join(req.Hosts, ",", true),
				"password": req.Password,
				"force":    force,
				"cluster":  req.ClusterName,
			},
		}}},
	}
	b, err := yaml.Marshal(yml)
	if err != nil {
		errstr := fmt.Sprintf("failed to marshal pipelineyml: %v", err)
		logrus.Errorf(errstr)
		return recordID, err
	}
	dto, err := n.bdl.CreatePipeline(&apistructs.PipelineCreateRequestV2{
		PipelineYml: string(b),
		PipelineYmlName: fmt.Sprintf("ops-rm-nodes-%s-%s.yml",
			clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME),
			uuid.UUID()[:12]),
		ClusterName:    clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME),
		PipelineSource: apistructs.PipelineSourceOps,
		AutoRunAtOnce:  true,
	})
	if err != nil {
		errstr := fmt.Sprintf("failed to createpipeline: %v", err)
		logrus.Errorf(errstr)
		return recordID, err
	}
	// curl -XPOST 'monitor.default:7096/api/resources/hosts/actions/offline' -d '{"clusterName":"$ACTION_CLUSTER", "hostIPs":["$host"]}'
	type offlineStruct struct {
		ClusterName string   `json:"clusterName"`
		HostIPs     []string `json:"hostIPs"`
	}

	// resp, err := httpclient.New().Post(os.Getenv("MONITOR_ADDR")).
	// 	Path("/api/resources/hosts/actions/offline").
	// 	JSONBody(offlineStruct{
	// 		ClusterName: req.ClusterName,
	// 		HostIPs:     req.Hosts,
	// 	}).Do().DiscardBody()
	// if err != nil || !resp.IsOK() {
	// 	logrus.Errorf("call monitor offline api failed: %+v, %+v", err, resp)
	// }
	recordID, err = n.db.RecordsWriter().Create(&dbclient.Record{
		RecordType:  dbclient.RecordTypeRmNodes,
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
		return recordID, err
	}
	return recordID, nil
}

// ess remove nodes & delete nodes in cloud resource
func (n *Nodes) DeleteEssNodes(req apistructs.DeleteNodesRequest, userid string, orgid string) (uint64, error) {
	var recordID uint64
	clusterInfo, err := n.bdl.QueryClusterInfo(req.ClusterName)
	if err != nil {
		errstr := fmt.Sprintf("failed to queryclusterinfo: %v, clusterinfo: %v", err, clusterInfo)
		logrus.Errorf(errstr)
		return recordID, err
	}
	if !clusterInfo.IsK8S() {
		errstr := fmt.Sprintf("unsupported cluster type, cluster name: %s, cluster type: %s",
			clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME), clusterInfo.MustGet(apistructs.DICE_CLUSTER_TYPE))
		logrus.Errorf(errstr)
		err := errors.New(errstr)
		return recordID, err
	}
	// default is not force
	force := ""
	if req.Force {
		force = "true"
	}
	var stages [][]*apistructs.PipelineYmlAction
	if req.ForceDelete {
		stages = [][]*apistructs.PipelineYmlAction{{{
			Type:    "delete-ess-nodes",
			Version: "1.0",
			Alias:   apistructs.NodePhaseDeleteNodes.String(),
			Params: map[string]interface{}{
				"ak":               req.AccessKey,
				"sk":               req.SecretKey,
				"region":           req.Region,
				"scaling_group_id": req.ScalingGroupId,
				"instance_ids":     req.InstanceIDs,
			},
		}},
		}
	} else {
		stages = [][]*apistructs.PipelineYmlAction{{{
			Type:    "rm-nodes",
			Version: "1.0",
			Alias:   apistructs.NodePhaseRmNodes.String(),
			Params: map[string]interface{}{
				"hosts":    strutil.Join(req.Hosts, ",", true),
				"password": req.Password,
				"force":    force,
				"cluster":  req.ClusterName,
			},
		}}, {{
			Type:    "delete-ess-nodes",
			Version: "1.0",
			Alias:   apistructs.NodePhaseDeleteNodes.String(),
			Params: map[string]interface{}{
				"ak":               req.AccessKey,
				"sk":               req.SecretKey,
				"region":           req.Region,
				"scaling_group_id": req.ScalingGroupId,
				"instance_ids":     req.InstanceIDs,
			},
		}},
		}
	}

	yml := apistructs.PipelineYml{
		Version: "1.1",
		Stages:  stages,
	}
	b, err := yaml.Marshal(yml)
	if err != nil {
		errstr := fmt.Sprintf("failed to marshal pipelineyml: %v", err)
		logrus.Errorf(errstr)
		return recordID, err
	}
	dto, err := n.bdl.CreatePipeline(&apistructs.PipelineCreateRequestV2{
		PipelineYml:     string(b),
		PipelineYmlName: fmt.Sprintf("ops-delete-ess-nodes-%s.yml", clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME)),
		ClusterName:     clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME),
		PipelineSource:  apistructs.PipelineSourceOps,
		AutoRunAtOnce:   true,
	})
	if err != nil {
		errstr := fmt.Sprintf("failed to createpipeline: %v", err)
		logrus.Errorf(errstr)
		return recordID, err
	}

	var d apistructs.NodesRecordDetail
	d.Hosts = req.Hosts
	d.InstanceIDs = strings.Split(req.InstanceIDs, ",")
	detail, err := json.Marshal(d)
	if err != nil {
		logrus.Errorf("failed to marshal delete nodes detail, request: %v, error: %v", d, err)
		return recordID, err
	}

	recordID, err = n.db.RecordsWriter().Create(&dbclient.Record{
		RecordType:  dbclient.RecordTypeDeleteEssNodes,
		UserID:      userid,
		OrgID:       orgid,
		ClusterName: req.ClusterName,
		Status:      dbclient.StatusTypeProcessing,
		Detail:      string(detail),
		PipelineID:  dto.ID,
	})
	if err != nil {
		errstr := fmt.Sprintf("failed to create record: %v", err)
		logrus.Errorf(errstr)
		return recordID, err
	}
	return recordID, nil
}

// ess remove nodes & delete nodes in cron mode
func (n *Nodes) DeleteEssNodesCron(req apistructs.DeleteNodesCronRequest, userid string, orgid string) (*uint64, error) {
	// check cluster info
	clusterInfo, err := n.bdl.QueryClusterInfo(req.ClusterName)
	if err != nil {
		errstr := fmt.Sprintf("failed to queryclusterinfo: %v, clusterinfo: %v", err, clusterInfo)
		logrus.Errorf(errstr)
		return nil, err
	}
	if !clusterInfo.IsK8S() {
		errstr := fmt.Sprintf("unsupported cluster type, cluster name: %s, cluster type: %s",
			clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME), clusterInfo.MustGet(apistructs.DICE_CLUSTER_TYPE))
		logrus.Errorf(errstr)
		err := errors.New(errstr)
		return nil, err
	}

	// create cron expression
	cronMap := make(map[string]string)
	layout := "2006-01-02T15:04Z"
	launchTime, err := time.Parse(layout, req.LaunchTime)

	o1, _ := time.ParseDuration("8h")
	t1 := launchTime.Add(o1)

	if err != nil {
		err := fmt.Errorf("parse launch time failed, request: %v, error: %v", req.LaunchTime, err)
		logrus.Error(err)
		return nil, err
	}
	cronMap["minute"] = strconv.Itoa(t1.Minute())
	cronMap["hour"] = strconv.Itoa(t1.Hour())
	cronMap["day"] = "*"
	cronMap["month"] = "*"
	cronMap["week"] = "*"
	if req.RecurrenceType == "Daily" {
		cronMap["day"] = fmt.Sprintf("*/%s", req.RecurrenceValue)
	} else if req.RecurrenceType == "Monthly" {
		cronMap["day"] = req.RecurrenceValue
	} else if req.RecurrenceType == "Weekly" {
		cronMap["week"] = req.RecurrenceValue
	} else {
		err := fmt.Errorf("unsupport recurrence type, cluster name:%s, type : %s", req.ClusterName, req.RecurrenceType)
		logrus.Error(err)
		return nil, err
	}
	cronExpr := strings.Join([]string{cronMap["minute"], cronMap["hour"], cronMap["day"], cronMap["month"], cronMap["week"]}, " ")
	logrus.Infof("create cron expression, cluster name: %s, cron expression: %s", req.ClusterName, cronExpr)

	req.AccessKey = encrypt.AesDecrypt(req.AccessKey, apistructs.TerraformEcyKey)
	req.SecretKey = encrypt.AesDecrypt(req.SecretKey, apistructs.TerraformEcyKey)
	req.Password = encrypt.AesDecrypt(req.Password, apistructs.TerraformEcyKey)
	// default is not force
	force := ""
	if req.Force {
		force = "true"
	}
	yml := apistructs.PipelineYml{
		Version: "1.1",
		Cron:    cronExpr,
		Stages: [][]*apistructs.PipelineYmlAction{{{
			Type:    "ess-info",
			Version: "1.0",
			Alias:   string(apistructs.NodePhaseEssInfo),
			Params: map[string]interface{}{
				"ak":               req.AccessKey,
				"sk":               req.SecretKey,
				"region":           req.Region,
				"scaling_group_id": req.ScalingGroupId,
			},
		}}, {{
			Type:    "rm-nodes",
			Version: "1.0",
			Alias:   string(apistructs.NodePhaseRmNodes),
			Params: map[string]interface{}{
				"hosts_file": fmt.Sprintf("${%s}/hosts", string(apistructs.NodePhaseEssInfo)),
				"password":   req.Password,
				"force":      force,
				"cluster":    req.ClusterName,
			},
		}}, {{
			Type:    "delete-ess-nodes",
			Version: "1.0",
			Alias:   string(apistructs.NodePhaseDeleteNodes),
			Params: map[string]interface{}{
				"ak":                req.AccessKey,
				"sk":                req.SecretKey,
				"region":            req.Region,
				"scaling_group_id":  req.ScalingGroupId,
				"is_cron":           true,
				"instance_ids_file": fmt.Sprintf("${%s}/instance_ids", string(apistructs.NodePhaseEssInfo)),
			},
		}},
		},
	}
	b, err := yaml.Marshal(yml)
	if err != nil {
		errstr := fmt.Sprintf("failed to marshal pipelineyml: %v", err)
		logrus.Errorf(errstr)
		return nil, err
	}
	dto, err := n.bdl.CreatePipeline(&apistructs.PipelineCreateRequestV2{
		PipelineYml:     string(b),
		PipelineYmlName: fmt.Sprintf("%s-%s.yml", apistructs.DeleteEssNodesCronPrefix, clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME)),
		ClusterName:     clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME),
		PipelineSource:  apistructs.PipelineSourceOps,
		AutoStartCron:   true,
		CronStartFrom:   &launchTime,
	})

	if err != nil {
		errstr := fmt.Sprintf("failed to createpipeline: %v", err)
		logrus.Errorf(errstr)
		return nil, err
	}

	return dto.CronID, nil
}
