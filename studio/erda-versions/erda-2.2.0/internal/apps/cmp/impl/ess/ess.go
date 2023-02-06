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

package ess

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	api "github.com/aliyun/alibaba-cloud-sdk-go/services/ess"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/impl/labels"
	"github.com/erda-project/erda/internal/apps/cmp/impl/mns"
	"github.com/erda-project/erda/internal/apps/cmp/impl/nodes"
	"github.com/erda-project/erda/pkg/crypto/encrypt"
	"github.com/erda-project/erda/pkg/dlock"
	"github.com/erda-project/erda/pkg/http/httputil"
)

const (
	EssGroupNameSuff          = "-dice-ess"
	EssScaleSimpleRuleSuff    = "-dice-auto-rule"
	EssScaleSchedulerRuleSuff = "-dice-scheduler-rule"
	EssScaleSchedulerTaskSuff = "-dice-scheduler-task"
	TimeLayout                = "2006-01-02T15:04Z"
	DetectInterval            = 30
	MaxLimit                  = 80
	MinLimit                  = 70
)

type Ess struct {
	bdl        *bundle.Bundle
	mns        *mns.Mns
	nodes      *nodes.Nodes
	labels     *labels.Labels
	Config     *Config
	ClusterSvc clusterpb.ClusterServiceServer
}

type Config struct {
	Region          string
	AccessKeyID     string
	AccessSecret    string
	client          *api.Client
	EssGroupID      string
	ScaleConfID     string
	EssScaleRule    string
	ScheduledTaskId string
	ScalingRuleId   string
	IsExist         bool
	ScalePipeLineID uint64
}

func New(bdl *bundle.Bundle, mns *mns.Mns, nodes *nodes.Nodes, labels *labels.Labels, clusterSvc clusterpb.ClusterServiceServer) *Ess {
	return &Ess{bdl: bdl, mns: mns, nodes: nodes, labels: labels, ClusterSvc: clusterSvc}
}

// Init Init auto scale
func (e *Ess) Init(req apistructs.BasicCloudConf, m *mns.Mns, n *nodes.Nodes) (*Ess, error) {
	accessKey := encrypt.AesDecrypt(req.AccessKeyId, apistructs.TerraformEcyKey)
	secretKey := encrypt.AesDecrypt(req.AccessKeySecret, apistructs.TerraformEcyKey)
	client, err := api.NewClientWithAccessKey(req.Region, accessKey, secretKey)
	if err != nil {
		return nil, errors.Wrap(err, "failed to new ess client with accessKey")
	}
	config := &Config{
		Region:       req.Region,
		AccessKeyID:  accessKey,
		AccessSecret: secretKey,
		client:       client,
	}
	instance := &Ess{
		Config: config,
		mns:    m,
		nodes:  n,
	}
	return instance, nil
}

// CreateAutoFlow Create ess with auto mode
func (e *Ess) CreateAutoFlow(clusterName string, vSwitchID string, ecsPassword string, sgID string) error {
	essScaleSimpleRuleName := clusterName + EssScaleSimpleRuleSuff
	err := e.CreateFlow(clusterName, vSwitchID, ecsPassword, sgID)
	if err != nil {
		return err
	}
	err = e.DeleteScaleRule(essScaleSimpleRuleName)
	if err != nil {
		return err
	}
	// Create ess simple scale rule
	err = e.CreateScaleRule(essScaleSimpleRuleName, 2)
	return err
}

// CreateSchedulerFlow Create ess with schedule mode
func (e *Ess) CreateSchedulerFlow(req apistructs.SchedulerScaleReq) error {
	essScaleSchedulerRuleName := req.ClusterName + EssScaleSchedulerRuleSuff
	essScaleSchedulerTask := req.ClusterName + EssScaleSchedulerTaskSuff
	err := e.CreateFlow(req.ClusterName, req.VSwitchID, req.EcsPassword, req.SgID)
	if err != nil {
		return err
	}

	err = e.DeleteScaleRule(essScaleSchedulerRuleName)
	if err != nil {
		return err
	}
	// Create ess autoscale rules with simple model.
	err = e.CreateScaleRule(essScaleSchedulerRuleName, req.Num)
	if err != nil {
		return err
	}
	if req.IsEdit {
		err = e.UpdateScheduledTasks(req.ScheduledTaskId, req.RecurrenceType, req.RecurrenceValue, req.LaunchTime)
		if err != nil {
			return err
		}
	} else {
		err = e.DeleteScheduledTasks(essScaleSchedulerTask)
		if err != nil {
			return err
		}
		// Create ess autoscale rules with schedule model.
		err = e.CreateScheduledTask(essScaleSchedulerTask, req.RecurrenceType, req.RecurrenceValue, req.LaunchTime)
		if err != nil {
			return err
		}
	}
	// Create autoscale rules with schedule model.
	pTime1, err := time.Parse(TimeLayout, req.LaunchTime)
	o, _ := time.ParseDuration(strconv.Itoa(req.ScaleDuration) + "h")
	t := pTime1.Add(o)
	id, err := e.nodes.DeleteEssNodesCron(apistructs.DeleteNodesCronRequest{
		DeleteNodesRequest: apistructs.DeleteNodesRequest{
			RmNodesRequest: apistructs.RmNodesRequest{
				ClusterName: req.ClusterName,
				OrgID:       uint64(req.OrgID),
				Password:    req.EcsPassword,
				Force:       false,
			},
			AccessKey:      req.AccessKeyId,
			SecretKey:      req.AccessKeySecret,
			Region:         req.Region,
			ScalingGroupId: e.Config.EssGroupID,
		},
		LaunchTime:      t.Format(TimeLayout),
		RecurrenceType:  req.RecurrenceType,
		RecurrenceValue: req.RecurrenceValue,
	}, apistructs.AutoScaleUserID, strconv.Itoa(req.OrgID))
	if err != nil {
		e.DeleteScheduledTasks(essScaleSchedulerTask)
		return err
	}
	e.Config.ScalePipeLineID = *id
	return nil
}

// CreateFlow Create elastic scale group, elastic scale configuration, elastic scale rule in order when start autoscale
func (e *Ess) CreateFlow(clusterName string, vSwitchID string, ecsPassword string, sgID string) error {
	var err error
	essGroupName := clusterName + EssGroupNameSuff
	// First, check elastic scale group whether exists
	flag, err := e.EnsureScaleGroupExist(essGroupName)
	if err != nil {
		return err
	}
	if flag {
		return nil
	}
	// Create elastic scale group
	err = e.CreateScaleGroup(essGroupName, vSwitchID)
	if err != nil {
		return err
	}
	// Create elastic scale configuration
	err = e.CreateScaleConf(ecsPassword, sgID)
	if err != nil {
		return err
	}
	// Create mns queue
	var r apistructs.MnsReq
	r.Region = e.Config.Region
	r.AccessKeyId = e.Config.AccessKeyID
	r.AccessKeySecret = e.Config.AccessSecret
	r.ClusterName = clusterName
	err = e.mns.InitConfig(r)
	if err != nil {
		return err
	}
	err = e.mns.CreateQueue()
	if err != nil {
		return err
	}
	mnsQueueName := strings.Join([]string{
		"acs",
		"ess",
		e.Config.Region,
		e.mns.Config.AccountID,
		"queue/" + e.mns.Config.QueueName,
	}, ":")
	// Related ess elastic scale group to mns queue
	err = e.CreateNotificationConfiguration(mnsQueueName)
	if err != nil {
		return err
	}
	// Enable elastic scale group
	err = e.EnableScalingGroup()
	return err
}

// EnsureScaleGroupExist Ensure elastic scale group whether exists
func (e *Ess) EnsureScaleGroupExist(essGroupName string) (bool, error) {

	request := api.CreateDescribeScalingGroupsRequest()
	request.Scheme = "https"

	request.ScalingGroupName1 = essGroupName

	response, err := e.Config.client.DescribeScalingGroups(request)
	if err != nil {
		logrus.Errorf("failed to get ess group: %v", err)
		return false, err
	}
	if response.TotalCount != 1 {
		return false, nil
	}
	e.Config.EssGroupID = response.ScalingGroups.ScalingGroup[0].ScalingGroupId
	e.Config.IsExist = true
	return true, nil
}

// EnsureScaleRuleExist Ensure elastic scale rule whether exists
func (e *Ess) EnsureScaleRuleExist(name string) (bool, error) {

	request := api.CreateDescribeScalingRulesRequest()
	request.Scheme = "https"

	request.ScalingGroupId = e.Config.EssGroupID
	request.ScalingRuleName1 = name

	response, err := e.Config.client.DescribeScalingRules(request)
	if err != nil {
		logrus.Errorf("failed to get ess group: %v", err)
		return false, err
	}
	if response.TotalCount != 1 {
		return false, nil
	}
	//ScalingRuleId
	e.Config.EssScaleRule = response.ScalingRules.ScalingRule[0].ScalingRuleAri
	e.Config.ScalingRuleId = response.ScalingRules.ScalingRule[0].ScalingRuleId
	return true, nil
}

// DeleteScaleRule Delete elastic scale rule
func (e *Ess) DeleteScaleRule(name string) error {
	f, err := e.EnsureScaleRuleExist(name)
	if err != nil {
		return err
	}
	if !f {
		return nil
	}
	request := api.CreateDeleteScalingRuleRequest()
	request.Scheme = "https"

	request.ScalingRuleId = e.Config.ScalingRuleId

	_, err = e.Config.client.DeleteScalingRule(request)
	if err != nil {
		logrus.Errorf("failed to delete scale rule: %v", err)
		return err
	}
	return nil
}

// EnsureScheduledTasks Ensure schedule task whether exists
func (e *Ess) EnsureScheduledTasks(name string) (bool, error) {

	request := api.CreateDescribeScheduledTasksRequest()
	request.Scheme = "https"

	request.ScheduledTaskName1 = name

	response, err := e.Config.client.DescribeScheduledTasks(request)
	if err != nil {
		logrus.Errorf("failed to get ess scheduler task: %v", err)
		return false, err
	}
	if response.TotalCount != 1 {
		return false, nil
	}
	//ScalingRuleId
	e.Config.ScheduledTaskId = response.ScheduledTasks.ScheduledTask[0].ScheduledTaskId
	return true, nil
}

//DeleteScaleRule Delete scale rule
func (e *Ess) DeleteScheduledTasks(name string) error {

	f, err := e.EnsureScheduledTasks(name)
	if err != nil {
		return err
	}
	if !f {
		return nil
	}
	request := api.CreateDeleteScheduledTaskRequest()
	request.Scheme = "https"

	request.ScheduledTaskId = e.Config.ScheduledTaskId

	_, err = e.Config.client.DeleteScheduledTask(request)
	if err != nil {
		logrus.Errorf("failed to delete scheduler task: %v", err)
		return err
	}
	return nil
}

//CreateScaleGroup Create scale group
func (e *Ess) CreateScaleGroup(essGroupName string, vSwitchID string) error {
	request := api.CreateCreateScalingGroupRequest()
	request.Scheme = "https"

	request.MinSize = "0"
	request.MaxSize = requests.NewInteger(1000)
	request.ScalingGroupName = essGroupName
	request.VSwitchId = vSwitchID

	response, err := e.Config.client.CreateScalingGroup(request)
	if err != nil {
		logrus.Errorf("failed to create ess group: %v", err)
		return err
	}
	e.Config.EssGroupID = response.ScalingGroupId
	return nil
}

// CreateScaleConf
func (e *Ess) CreateScaleConf(ecsPassword string, sgID string) error {
	request := api.CreateCreateScalingConfigurationRequest()
	request.Scheme = "https"

	request.ScalingGroupId = e.Config.EssGroupID
	request.ImageName = "centos_7_7_x64_20G_alibase_20200220.vhd"
	request.InstanceTypes = &[]string{
		"ecs.sn2ne.2xlarge",
		"ecs.sn2.xlarge",
	}
	//request.InstanceTypes = &[]string{
	//	"ecs.sn2ne.large",
	//	"ecs.sn1ne.large",
	//	"ecs.sn2ne.2xlarge",
	//}
	request.Cpu = requests.NewInteger(8)
	request.Memory = requests.NewInteger(32)
	request.SystemDiskCategory = "cloud_ssd"
	request.SystemDiskSize = requests.NewInteger(40)
	request.SecurityGroupId = sgID
	request.DataDisk = &[]api.CreateScalingConfigurationDataDisk{
		{
			Size:     "200",
			Category: "cloud_ssd",
		},
	}
	password := encrypt.AesDecrypt(ecsPassword, apistructs.TerraformEcyKey)
	request.Password = password

	resp, err := e.Config.client.CreateScalingConfiguration(request)
	if err != nil {
		logrus.Errorf("failed to create ess scale config: %v", err)
		return err
	}
	e.Config.ScaleConfID = resp.ScalingConfigurationId
	return nil
}

// CreateScaleRule Create simple scale rule
func (e *Ess) CreateScaleRule(essScaleSimpleRuleName string, num int) error {
	request := api.CreateCreateScalingRuleRequest()
	request.Scheme = "https"

	request.ScalingGroupId = e.Config.EssGroupID
	request.ScalingRuleName = essScaleSimpleRuleName
	request.AdjustmentType = "QuantityChangeInCapacity"
	request.AdjustmentValue = requests.NewInteger(num)
	request.ScalingRuleType = "SimpleScalingRule"

	resp, err := e.Config.client.CreateScalingRule(request)
	if err != nil {
		logrus.Errorf("failed to create ess scale rule: %v", err)
		return err
	}
	e.Config.EssScaleRule = resp.ScalingRuleAri
	return nil
}

// UpdateScheduledTasks Update schedule task
func (e *Ess) UpdateScheduledTasks(id string, recurrenceType string, recurrenceValue string, launchTime string) error {

	d, _ := time.ParseDuration(strconv.Itoa(300*24) + "h")
	pTime, err := time.Parse(TimeLayout, launchTime)
	d1 := pTime.Add(d)

	request := api.CreateModifyScheduledTaskRequest()
	request.Scheme = "https"

	request.ScheduledTaskId = id
	request.ScheduledAction = e.Config.EssScaleRule
	request.RecurrenceType = recurrenceType
	request.RecurrenceValue = recurrenceValue
	request.RecurrenceEndTime = d1.Format(TimeLayout)
	request.TaskEnabled = requests.NewBoolean(true)

	_, err = e.Config.client.ModifyScheduledTask(request)
	if err != nil {
		logrus.Errorf("failed to modify ess scheduler task: %v", err)
		return err
	}
	e.Config.ScheduledTaskId = id
	return nil
}

// CreateScheduledTask Create schedule task
func (e *Ess) CreateScheduledTask(name string, recurrenceType string, recurrenceValue string, launchTime string) error {

	d, _ := time.ParseDuration(strconv.Itoa(300*24) + "h")
	pTime, err := time.Parse(TimeLayout, launchTime)
	d1 := pTime.Add(d)

	request := api.CreateCreateScheduledTaskRequest()
	request.Scheme = "https"

	request.ScheduledTaskName = name
	request.ScheduledAction = e.Config.EssScaleRule
	request.LaunchTime = launchTime
	request.RecurrenceEndTime = d1.Format(TimeLayout)
	request.RecurrenceType = recurrenceType
	request.RecurrenceValue = recurrenceValue
	request.TaskEnabled = requests.NewBoolean(true)

	response, err := e.Config.client.CreateScheduledTask(request)
	if err != nil {
		logrus.Errorf("failed to create ess scheduler task: %v", err)
		return err
	}
	e.Config.ScheduledTaskId = response.ScheduledTaskId
	return nil
}

// ExecScaleRule Execute scale rule
func (e *Ess) ExecScaleRule(scaleRuleName string) error {
	request := api.CreateExecuteScalingRuleRequest()
	request.Scheme = "https"

	// request.ScalingRuleAri = "ari:acs:ess:cn-hangzhou:1356642369236709:scalingrule/asr-bp10owakduy0vvzwwe03"
	request.ScalingRuleAri = scaleRuleName

	_, err := e.Config.client.ExecuteScalingRule(request)
	if err != nil {
		logrus.Errorf("failed to execute ess scale rule: %v", err)
		return err
	}
	return nil
}

//CreateNotificationConfiguration Related configuration
func (e *Ess) CreateNotificationConfiguration(queueName string) error {
	request := api.CreateCreateNotificationConfigurationRequest()
	request.Scheme = "https"

	request.ScalingGroupId = e.Config.EssGroupID
	request.NotificationArn = queueName
	request.NotificationType = &[]string{
		"AUTOSCALING:SCALE_OUT_SUCCESS",
		"AUTOSCALING:SCALE_IN_SUCCESS",
		"AUTOSCALING:SCALE_OUT_ERROR",
		"AUTOSCALING:SCALE_IN_ERROR",
		"AUTOSCALING:SCALE_REJECT",
		"AUTOSCALING:SCALE_OUT_START",
		"AUTOSCALING:SCALE_IN_START",
		"AUTOSCALING:SCHEDULE_TASK_EXPIRING",
	}

	_, err := e.Config.client.CreateNotificationConfiguration(request)
	if err != nil {
		logrus.Errorf("failed to create ess scale notification: %v", err)
		return err
	}
	return nil
}

// EnableScalingGroup Enable scale group
func (e *Ess) EnableScalingGroup() error {

	request := api.CreateEnableScalingGroupRequest()
	request.Scheme = "https"

	request.ScalingGroupId = e.Config.EssGroupID
	request.ActiveScalingConfigurationId = e.Config.ScaleConfID

	_, err := e.Config.client.EnableScalingGroup(request)
	if err != nil {
		logrus.Errorf("failed enable scale group: %v", err)
		return err
	}
	return nil
}

func (e *Ess) AutoScale() {
	// Listen elastic scale
	go func() {
		for {
			ctx, cancel := context.WithCancel(context.Background())
			lock, err := dlock.New("/autoscale/auto", func() { cancel() })
			if err := lock.Lock(context.Background()); err != nil {
				logrus.Errorf("failed to lock: %v", err)
				continue
			}

			if err != nil {
				logrus.Errorf("failed to get dlock: %v", err)
				// try again
				continue
			}
			e.DetectResource(ctx)
			if err := lock.Unlock(); err != nil {
				logrus.Errorf("failed to unlock: %v", err)
			}
		}
	}()
}

// DetectResource Detect cluster resource
func (e *Ess) DetectResource(ctx context.Context) {
	ticker := time.NewTicker(time.Minute * DetectInterval)
	logrus.Errorf("begin to execute autoscale...")
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	for range ticker.C {
		logrus.Errorf("begin to execute autoscale in loop...")
		resp, err := e.ClusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{})
		if err != nil {
			logrus.Errorf("failed get to get cluster list")
			continue
		}
		clusters := resp.Data
		var totalCPU float64
		var totalMem int64
		var requestCPU float64
		var requestMem int64
		var offLineNode string
		var lockedNode string
		for _, cluster := range clusters {
			if cluster.OpsConfig != nil && cluster.OpsConfig.ScaleMode == "auto" {
				err := e.validateOpsConfig(cluster.OpsConfig)
				if err != nil {
					logrus.Errorf("cluster: %s, error: %v", cluster.Name, err)
					continue
				}
				resourceInfoData, err := e.bdl.ResourceInfo(cluster.Name, false)
				if err != nil {
					logrus.Errorf("failed get to get cluster resource info data: %v\n", err)
					continue
				}
				totalCPU = 0
				totalMem = 0
				requestCPU = 0
				requestMem = 0
				for k, v := range resourceInfoData.Nodes {
					// Check labels (dice/stateless-service and bigdata-) in production environment
					if (isExistInArray("dice/stateless-service", v.Labels) && isExistInArray("dice/workspace-prod", v.Labels)) || isExistInArray("dice/bigdata-job", v.Labels) {
						totalCPU += v.CPUAllocatable
						totalMem += v.MemAllocatable
						requestCPU += v.CPUReqsUsage
						requestMem += v.MemReqsUsage
					}
					// Label host which need offline
					if isExistInArray("dice/autoscale", v.Labels) {
						if isExistInArray("dice/locked", v.Labels) {
							lockedNode = k
						}
						offLineNode = k
					}
				}
				cpuUsage := requestCPU / totalCPU * 100
				memUsage := float64(requestMem) / float64(totalMem) * 100
				// Expansion when resource used greater than 80%
				if cpuUsage > MaxLimit || memUsage > MaxLimit {
					accountInfoReq := apistructs.BasicCloudConf{
						Region:          cluster.OpsConfig.Region,
						AccessKeyId:     cluster.OpsConfig.AccessKey,
						AccessKeySecret: cluster.OpsConfig.SecretKey,
					}
					as, err := e.Init(accountInfoReq, e.mns, e.nodes)
					if err != nil {
						logrus.Errorf("failed to init ess sdk: %v", err)
						continue
					}
					logrus.Errorf("ess sdk hava ready for autostale")
					err = as.ExecScaleRule(cluster.OpsConfig.EssScaleRule)
					if err != nil {
						logrus.Errorf("failed to execute scale rule: %v", err)
					}
				}
				// Reduce capacity when resource used less than 70%
				if cpuUsage < MinLimit && memUsage < MinLimit {
					if lockedNode != "" {
						id, err := e.mns.GetInstancesIDByPrivateIp(apistructs.EcsInfoReq{
							BasicCloudConf: apistructs.BasicCloudConf{
								AccessKeyId:     cluster.OpsConfig.AccessKey,
								AccessKeySecret: cluster.OpsConfig.SecretKey,
								Region:          cluster.OpsConfig.Region,
							},
							PrivateIPs: []string{lockedNode},
						})
						if err != nil {
							logrus.Errorf("failed get id by private ip: %v", err)
							continue
						}
						logrus.Errorf("execute offline action from ess on id: %v\n", id)
						req := apistructs.DeleteNodesRequest{
							RmNodesRequest: apistructs.RmNodesRequest{
								ClusterName: cluster.Name,
								OrgID:       uint64(cluster.OrgID),
								Hosts:       []string{lockedNode},
								Password:    cluster.OpsConfig.EcsPassword,
								Force:       false,
							},
							AccessKey:      cluster.OpsConfig.AccessKey,
							SecretKey:      cluster.OpsConfig.SecretKey,
							Region:         cluster.OpsConfig.Region,
							ScalingGroupId: cluster.OpsConfig.EssGroupID,
							InstanceIDs:    id,
						}
						_, err = e.nodes.DeleteEssNodes(req, apistructs.AutoScaleUserID, strconv.Itoa(int(cluster.OrgID)))
						if err != nil {
							logrus.Errorf("failed delete node from ess: %v", err)
						}
					}
					if offLineNode != "" {
						_, err = e.labels.UpdateLabels(apistructs.UpdateLabelsRequest{
							ClusterName: cluster.Name,
							OrgID:       uint64(cluster.OrgID),
							Hosts:       []string{offLineNode},
							Labels:      []string{"locked"},
						}, apistructs.AutoScaleUserID)
						if err != nil {
							logrus.Errorf("failed to label the offline node %s: %v", offLineNode, err)
						}
					}
				}
			}
		}
	}
	<-ctx.Done()
}

func (e *Ess) validateOpsConfig(opsConf *clusterpb.OpsConfig) error {
	if opsConf == nil {
		err := fmt.Errorf("empty ops config")
		logrus.Error(err.Error())
		return err
	}

	if e.isEmpty(opsConf.AccessKey) || e.isEmpty(opsConf.SecretKey) || e.isEmpty(opsConf.Region) {
		err := fmt.Errorf("invalid ops config")
		return err
	}
	return nil
}

func (e Ess) isEmpty(str string) bool {
	return strings.Replace(str, " ", "", -1) == ""
}

func isExistInArray(value string, array []string) bool {
	for _, v := range array {
		if v == value {
			return true
		}
	}
	return false
}
