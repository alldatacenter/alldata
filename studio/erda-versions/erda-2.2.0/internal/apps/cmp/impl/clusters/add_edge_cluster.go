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
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskresult"
	"github.com/erda-project/erda/pkg/envconf"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

func (c *Clusters) AddClusters(ctx context.Context, req apistructs.CloudClusterRequest, userid string) (uint64, error) {
	var recordID uint64
	// load central cluster env
	err := envconf.Load(&req)

	logrus.Debugf("add cluster request:%+v", req)

	if err != nil {
		errstr := fmt.Sprint("failed to load DICE_CLUSTER_NAME")
		logrus.Errorf(errstr)
		return recordID, err
	}

	// check DICE_CLUSTER_NAME
	if isEmpty(req.CentralClusterName) {
		errstr := fmt.Sprint("DICE_CLUSTER_NAME is empty")
		logrus.Errorf(errstr)
		return recordID, errors.New(errstr)
	}

	if isEmpty(req.DisplayName) {
		req.DisplayName = req.ClusterName
	}

	clusterInfo, err := c.bdl.QueryClusterInfo(req.CentralClusterName)
	if err != nil {
		errstr := fmt.Sprintf("failed to query clusterinfo: %v, err: %v", clusterInfo, err)
		logrus.Errorf(errstr)
		return recordID, err
	}

	// build create cluster pipeline
	if req.CollectorURL == "" {
		// protocol is http,https in gts dice-cluster-info (key: DICE_PROTOCOL), manual is http or https;
		if strings.Contains(req.CentralDiceProtocol, "https") {
			req.CentralDiceProtocol = "https"
		} else {
			req.CentralDiceProtocol = "http"
		}
		req.CollectorURL = fmt.Sprintf("%s://collector.%s", req.CentralDiceProtocol, req.CentralRootDomain)
	}
	if req.OpenAPI == "" {
		req.OpenAPI = fmt.Sprintf("%s://openapi.%s", req.CentralDiceProtocol, req.CentralRootDomain)
	}
	if req.ClusterDialer == "" {
		req.ClusterDialer = fmt.Sprintf("%s://cluster-manager.%s", req.CentralDiceProtocol, req.CentralRootDomain)
	}

	// check DICE_ROOT_DOMAIN
	if isEmpty(clusterInfo.Get(apistructs.DICE_ROOT_DOMAIN)) {
		errstr := fmt.Sprint("DICE_ROOT_DOMAIN is empty")
		logrus.Errorf(errstr)
		return recordID, errors.New(errstr)
	}

	if isEmpty(req.Terraform) {
		req.Terraform = "apply"
	}

	if !stringInSlice(string(req.CloudVendor), apistructs.CloudVendorSlice) {
		errstr := fmt.Sprintf("cloud vendor:%v is not valid", req.CloudVendor)
		logrus.Errorf(errstr)
		return recordID, errors.New(errstr)
	}

	strs := strings.Split(string(req.CloudVendor), "-")
	req.CloudVendorName, req.CloudBasicRsc = strs[0], strs[1]

	// import cluster
	ic := apistructs.ImportCluster{
		CredentialType: "proxy",
		ClusterName:    req.ClusterName,
		DisplayName:    req.DisplayName,
		OrgID:          req.OrgID,
		ScheduleConfig: apistructs.ClusterSchedConfig{
			CPUSubscribeRatio: "1",
		},
		ClusterType:    "k8s",
		WildcardDomain: req.RootDomain,
	}
	err = c.importCluster(ctx, userid, &ic)
	if err != nil {
		logrus.Errorf("import cluster failed, request: %v, error: %v", req, err)
		return recordID, err
	}

	// get cluster access key
	cak, err := c.GetOrCreateAccessKey(ctx, req.ClusterName)
	if err != nil {
		logrus.Errorf("get cluster access key failed, cluster: %s, error: %v", req.ClusterName, err)
		return recordID, err
	}
	req.ClusterAccessKey = cak.AccessKey

	logrus.Debugf("cloud request: %v", req)

	var yml apistructs.PipelineYml
	var recordType dbclient.RecordType
	if req.CloudVendor == apistructs.CloudVendorAliEcs || req.InstallerIp != "" {
		// only alicloud-ecs need DockerCIDR
		if isEmpty(req.DockerCIDR) && req.CloudVendor == apistructs.CloudVendorAliEcs {
			errstr := "DockerCIDR is empty"
			logrus.Errorf(errstr)
			return recordID, err
		}
		if isEmpty(req.DockerBip) && req.CloudVendor == apistructs.CloudVendorAliEcs {
			strs := strings.Split(req.DockerCIDR, "/")
			ip := strs[0]
			mask := strs[1]
			bip := strings.Split(ip, ".")
			bip[3] = "1"
			dockerBip := strings.Join(bip, ".")
			req.DockerBip = strings.Join([]string{dockerBip, mask}, "/")
		}
		recordType = getRecordType(req.CloudVendor)
		yml = buildEcsPipeline(req)
	} else if req.CloudVendor == apistructs.CloudVendorAliAck { // TODO remove
		recordType = dbclient.RecordTypeAddAliACKECluster
		yml = buildCSPipeline(req)
	} else if req.CloudVendor == apistructs.CloudVendorAliCS {
		recordType = dbclient.RecordTypeAddAliCSECluster
		yml = buildCSPipeline(req)
	} else if req.CloudVendor == apistructs.CloudVendorAliCSManaged {
		recordType = dbclient.RecordTypeAddAliCSManagedCluster
		yml = buildCSPipeline(req)
	} else {
		errstr := fmt.Sprintf("cloud vendor:%v is not valid", req.CloudVendor)
		logrus.Errorf(errstr)
		return recordID, errors.New(errstr)
	}

	b, err := yaml.Marshal(yml)
	if err != nil {
		errstr := fmt.Sprintf("failed to marshal pipeline yml: %v", err)
		logrus.Errorf(errstr)
		return recordID, err
	}
	logrus.Infof("add edge cluster yaml: %v", string(b))

	dto, err := c.bdl.CreatePipeline(&apistructs.PipelineCreateRequestV2{
		PipelineYml:     string(b),
		PipelineYmlName: fmt.Sprintf("ops-add-cluster-%s.yml", req.ClusterName),
		ClusterName:     clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME),
		PipelineSource:  apistructs.PipelineSourceOps,
		AutoRunAtOnce:   true,
	})
	if err != nil {
		errstr := fmt.Sprintf("failed to create pipeline: %v", err)
		logrus.Errorf(errstr)
		return recordID, err
	}

	recordID, err = c.db.RecordsWriter().Create(&dbclient.Record{
		RecordType:  recordType,
		UserID:      userid,
		OrgID:       strconv.FormatUint(req.OrgID, 10),
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

func (c *Clusters) MonitorCloudCluster() (abort bool, err error) {
	var dto *apistructs.PipelineDetailDTO
	reader := c.db.RecordsReader()
	clusterTypes := []string{
		dbclient.RecordTypeAddAliACKECluster.String(),
		dbclient.RecordTypeAddAliCSECluster.String(),
		dbclient.RecordTypeAddAliCSManagedCluster.String(),
		dbclient.RecordTypeAddAliECSECluster.String(),
	}
	// interval: two hour
	interval := 2 * 60 * 60
	status := []string{dbclient.StatusTypeSuccess.String(), dbclient.StatusTypeProcessing.String()}
	reader.ByCreateTime(interval).ByStatuses(status...).ByRecordTypes(clusterTypes...).PageNum(0).PageSize(500)

	records, err := reader.Do()
	if err != nil {
		logrus.Errorf("get create cluster record failed, error: %v", err)
		return
	}
	logrus.Infof("get %d add edge cluster records", len(records))
	ctx := context.Background()
	for _, record := range records {
		if record.PipelineID == 0 {
			err = fmt.Errorf("invalid pipeline id")
			_ = c.processFailedPipeline(ctx, record, err)
			continue
		}
		dto, err = c.bdl.GetPipeline(record.PipelineID)
		if err != nil && strutil.Contains(err.Error(), "not found") {
			err = fmt.Errorf("not found pipeline: %d", record.PipelineID)
			_ = c.processFailedPipeline(ctx, record, err)
			continue
		}
		if dto == nil {
			err = fmt.Errorf("empty pipeline content")
			_ = c.processFailedPipeline(ctx, record, err)
			continue
		}
		if len(dto.PipelineStages) == 0 {
			err = fmt.Errorf("empty pipeline stages, pipelineid: %d", record.PipelineID)
			_ = c.processFailedPipeline(ctx, record, err)
			continue
		}
		for _, stage := range dto.PipelineStages {
			if len(stage.PipelineTasks) == 0 {
				err = fmt.Errorf("empty task in pipeline stage")
				_ = c.processFailedPipeline(ctx, record, err)
				break
			}
			for _, task := range stage.PipelineTasks {
				if task.Status.IsFailedStatus() {
					if len(task.Result.Errors) != 0 {
						err = fmt.Errorf("%s", task.Result.Errors[0].Msg)
					} else {
						err = fmt.Errorf("run pipeline failed")
					}
					_ = c.processFailedPipeline(ctx, record, err)
					break
				}
				if !task.Status.IsSuccessStatus() {
					break
				}
				if task.Status.IsSuccessStatus() && task.Name == "diceInstall" {
					_ = c.processSuccessPipeline(task.Result.Result, record)
				}
			}
		}
	}

	return false, nil
}

func (c *Clusters) processFailedPipeline(ctx context.Context, record dbclient.Record, error error) error {
	record.Status = dbclient.StatusTypeFailed
	record.Detail = error.Error()
	err := c.db.RecordsWriter().Update(record)
	if err != nil {
		logrus.Errorf("update add edge cluster to failed status failed, cluster:%s, pipeline:%v, err:%v", record.ClusterName, record.PipelineID, err)
		return err
	}
	orgID, err := strconv.Atoi(record.OrgID)
	if err != nil {
		return err
	}
	req := apistructs.OfflineEdgeClusterRequest{
		OrgID:       uint64(orgID),
		ClusterName: record.ClusterName,
	}
	// if create cluster failed, delete the init record
	_, _, err = c.OfflineEdgeCluster(ctx, req, record.UserID, record.OrgID)
	if err != nil {
		return err
	}
	return nil
}

func (c *Clusters) processSuccessPipeline(pTaskResult taskresult.Result, record dbclient.Record) error {
	req := &clusterpb.CreateClusterRequest{}
	// get cluster info from pipeline result
	for _, m := range pTaskResult.Metadata {
		if m.Name == "cluster_info" {
			cluster := []byte(m.Value)
			err := json.Unmarshal(cluster, &req)
			if err != nil {
				logrus.Errorf("unmarshal create cluster request failed, error: %v", err)
				return err
			}
			break
		}
	}

	// get cluster info
	ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	resp, err := c.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: record.ClusterName})
	if err != nil {
		logrus.Errorf("get cluster info failed, cluster:%s,  error: %v", record.ClusterName, err)
		return err
	}

	clusterInfo := resp.Data
	// update cluster info
	ur := &clusterpb.UpdateClusterRequest{
		Name:            clusterInfo.Name,
		DisplayName:     clusterInfo.DisplayName,
		Type:            clusterInfo.Type,
		CloudVendor:     req.CloudVendor,
		WildcardDomain:  clusterInfo.WildcardDomain,
		SchedulerConfig: clusterInfo.SchedConfig,
		OpsConfig:       req.OpsConfig,
	}
	_, err = c.clusterSvc.UpdateCluster(ctx, ur)
	if err != nil {
		logrus.Errorf("update cluster info failed, cluster:%s,  error: %v", record.ClusterName, err)
		return err
	}
	// update record
	record.Status = dbclient.StatusTypeSuccessed
	err = c.db.RecordsWriter().Update(record)
	if err != nil {
		logrus.Errorf("update record failed, error: %v", err)
		return err
	}
	return nil
}

func isEmpty(str string) bool {
	return strings.Replace(str, " ", "", -1) == ""
}

func stringInSlice(a string, list []string) bool {
	for _, b := range list {
		if b == a {
			return true
		}
	}
	return false
}

func getRecordType(vendor apistructs.CloudVendor) dbclient.RecordType {
	switch vendor {
	case apistructs.CloudVendorAliAck:
		return dbclient.RecordTypeAddAliCSECluster
	case apistructs.CloudVendorAliCS:
		return dbclient.RecordTypeAddAliCSECluster
	case apistructs.CloudVendorAliCSManaged:
		return dbclient.RecordTypeAddAliCSManagedCluster
	default:
		return dbclient.RecordTypeAddAliECSECluster
	}
}

func buildEcsPipeline(req apistructs.CloudClusterRequest) apistructs.PipelineYml {
	yml := apistructs.PipelineYml{
		Version: "1.1",
		Stages: [][]*apistructs.PipelineYmlAction{{{
			Type:    "cluster-resource",
			Version: "1.0",
			Alias:   string(apistructs.NodePhasePlan),
			Params: map[string]interface{}{
				// base fields which create cluster
				"cluster_access_key": req.ClusterAccessKey,

				"org_name":       req.OrgName,
				"dice_version":   req.DiceVersion,
				"cluster_name":   req.ClusterName,
				"root_domain":    req.RootDomain,
				"enable_https":   req.EnableHttps,
				"cluster_size":   req.ClusterSize,
				"nameservers":    req.Nameservers,
				"collector_url":  req.CollectorURL,
				"open_api":       req.OpenAPI,
				"cluster_dialer": req.ClusterDialer,

				// fields which create cluster on vpc
				"cloud_vendor":     req.CloudVendor,
				"region":           req.Region,
				"cluster_type":     req.ClusterType,
				"cluster_spec":     req.ClusterSpec,
				"charge_type":      req.ChargeType,
				"charge_period":    req.ChargePeriod,
				"ak":               req.AccessKey,
				"sk":               req.SecretKey,
				"vpc_id":           req.VpcID,
				"vpc_cidr":         req.VpcCIDR,
				"vswitch_id":       req.VSwitchID,
				"vswitch_cidr":     req.VSwitchCIDR,
				"nat_gateway_id":   req.NatGatewayID,
				"forward_table_id": req.ForwardTableID,
				"snat_table_id":    req.SnatTableID,

				// container service fields
				"service_cidr": req.ServiceCIDR,
				"pod_cidr":     req.PodCIDR,
				"docker_cidr":  req.DockerCIDR,
				"docker_bip":   req.DockerBip,
				"docker_root":  req.DockerRoot,
				"exec_root":    req.ExecRoot,

				// jump server field
				"installer_ip": req.InstallerIp,
				"user":         req.User,
				"password":     req.Password,
				"port":         req.Port,

				// host ip and disk info
				"host_ips": req.HostIps,
				"device":   req.Device,

				// nas/glusterfs fields
				"nas_domain":    req.NasDomain,
				"nas_path":      req.NasPath,
				"glusterfs_ips": req.GlusterfsIps,

				"terraform": "plan",
			},
		}}, {{
			Type:    "cluster-resource",
			Version: "1.0",
			Alias:   string(apistructs.NodePhaseBuyNode),
			Params: map[string]interface{}{

				"cluster_access_key": req.ClusterAccessKey,

				"org_name":       req.OrgName,
				"dice_version":   req.DiceVersion,
				"cluster_name":   req.ClusterName,
				"root_domain":    req.RootDomain,
				"enable_https":   req.EnableHttps,
				"cluster_size":   req.ClusterSize,
				"nameservers":    req.Nameservers,
				"collector_url":  req.CollectorURL,
				"open_api":       req.OpenAPI,
				"cluster_dialer": req.ClusterDialer,

				"cloud_vendor":     req.CloudVendor,
				"region":           req.Region,
				"cluster_type":     req.ClusterType,
				"cluster_spec":     req.ClusterSpec,
				"charge_type":      req.ChargeType,
				"charge_period":    req.ChargePeriod,
				"ak":               req.AccessKey,
				"sk":               req.SecretKey,
				"vpc_id":           req.VpcID,
				"vpc_cidr":         req.VpcCIDR,
				"vswitch_id":       req.VSwitchID,
				"vswitch_cidr":     req.VSwitchCIDR,
				"nat_gateway_id":   req.NatGatewayID,
				"forward_table_id": req.ForwardTableID,
				"snat_table_id":    req.SnatTableID,

				"service_cidr": req.ServiceCIDR,
				"pod_cidr":     req.PodCIDR,
				"docker_cidr":  req.DockerCIDR,
				"docker_bip":   req.DockerBip,
				"docker_root":  req.DockerRoot,
				"exec_root":    req.ExecRoot,

				"installer_ip": req.InstallerIp,
				"user":         req.User,
				"password":     req.Password,
				"port":         req.Port,

				"host_ips": req.HostIps,
				"device":   req.Device,

				"nas_domain":    req.NasDomain,
				"nas_path":      req.NasPath,
				"glusterfs_ips": req.GlusterfsIps,

				"terraform": req.Terraform,
				// "terraform": "plan",
			},
		}}, {{
			Type:    "dice-install",
			Version: "1.0",
			Alias:   string(apistructs.NodePhaseInstall),
			Params: map[string]interface{}{
				"dice_version":  req.DiceVersion,
				"cluster_name":  req.ClusterName,
				"display_name":  req.DisplayName,
				"root_domain":   req.RootDomain,
				"org_id":        req.OrgID,
				"cloud_vendor":  req.CloudVendor,
				"region":        req.Region,
				"charge_type":   req.ChargeType,
				"charge_period": req.ChargePeriod,
				"ak":            req.AccessKey,
				"sk":            req.SecretKey,
			},
		}},
		},
	}
	return yml
}

func buildCSPipeline(req apistructs.CloudClusterRequest) apistructs.PipelineYml {
	managed := false
	worker_number := 0 // TODO: Get param from frontend
	if req.CloudVendor == apistructs.CloudVendorAliCSManaged {
		managed = true
		worker_number = 2
	}

	yml := apistructs.PipelineYml{
		Version: "1.1",
		Stages: [][]*apistructs.PipelineYmlAction{{{
			Type:    "cs-kubernetes",
			Version: "1.0",
			Alias:   string(apistructs.NodePhasePlan),
			Params: map[string]interface{}{
				"org_name":     req.OrgName,
				"root_domain":  req.RootDomain,
				"enable_https": req.EnableHttps,

				"cluster_access_key": req.ClusterAccessKey,

				"cluster_name":      req.ClusterName,
				"access_key_id":     req.AccessKey,
				"access_key_secret": req.SecretKey,
				"region":            req.Region,
				"charge_type":       req.ChargeType,
				"charge_period":     req.ChargePeriod,
				"vpc_id":            req.VpcID,
				"vswitch_id":        req.VSwitchID,
				"vpc_subnet":        req.VpcCIDR,
				"vswitch_subnet":    req.VSwitchCIDR,
				"nat_gateway_id":    req.NatGatewayID,
				"forward_table_id":  req.ForwardTableID,
				"snat_table_id":     req.SnatTableID,
				"container_subnet":  req.PodCIDR,
				"vip_subnet":        req.ServiceCIDR,
				"worker_number":     worker_number,
				"ecs_instance_type": req.EcsInstType,
				"k8s_version":       req.K8sVersion,
				"managed":           managed,

				"collector_url":     req.CollectorURL,
				"openapi_url":       req.OpenAPI,
				"cluster_dialer":    req.ClusterDialer,
				"terraform_command": "plan",
			},
		}}, {{
			Type:    "cs-kubernetes",
			Version: "1.0",
			Alias:   string(apistructs.NodePhaseBuyNode),
			Params: map[string]interface{}{
				"org_name":     req.OrgName,
				"root_domain":  req.RootDomain,
				"enable_https": req.EnableHttps,

				"cluster_access_key": req.ClusterAccessKey,

				"cluster_name":      req.ClusterName,
				"access_key_id":     req.AccessKey,
				"access_key_secret": req.SecretKey,
				"region":            req.Region,
				"charge_type":       req.ChargeType,
				"charge_period":     req.ChargePeriod,
				"vpc_subnet":        req.VpcCIDR,
				"vpc_id":            req.VpcID,
				"vswitch_id":        req.VSwitchID,
				"vswitch_subnet":    req.VSwitchCIDR,
				"nat_gateway_id":    req.NatGatewayID,
				"forward_table_id":  req.ForwardTableID,
				"snat_table_id":     req.SnatTableID,
				"container_subnet":  req.PodCIDR,
				"vip_subnet":        req.ServiceCIDR,
				"worker_number":     worker_number,
				"ecs_instance_type": req.EcsInstType,
				"k8s_version":       req.K8sVersion,
				"managed":           managed,

				"collector_url":     req.CollectorURL,
				"openapi_url":       req.OpenAPI,
				"cluster_dialer":    req.ClusterDialer,
				"terraform_command": req.Terraform,
				// "terraform_command": "plan",
			},
		}}, {{
			Type:    "dice-install",
			Version: "1.0",
			Alias:   string(apistructs.NodePhaseInstall),
			Params: map[string]interface{}{
				"dice_version":  req.DiceVersion,
				"cluster_name":  req.ClusterName,
				"display_name":  req.DisplayName,
				"root_domain":   req.RootDomain,
				"org_id":        req.OrgID,
				"cloud_vendor":  req.CloudVendor,
				"region":        req.Region,
				"charge_type":   req.ChargeType,
				"charge_period": req.ChargePeriod,
				"ak":            req.AccessKey,
				"sk":            req.SecretKey,
			},
		}},
		},
	}
	return yml
}
