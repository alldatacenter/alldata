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

package mns

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	_ "net/http/pprof"
	"strconv"
	"strings"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/services/sts"
	ali_mns "github.com/aliyun/aliyun-mns-go-sdk"
	"github.com/golang-collections/collections/set"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/internal/apps/cmp/impl/nodes"
	"github.com/erda-project/erda/pkg/crypto/encrypt"
	"github.com/erda-project/erda/pkg/dlock"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/retry"
)

type ScalingType string

func (s ScalingType) Sting() string {
	return string(s)
}

const (
	AsgMnsQueuePrefix             = "asg-mns"
	AutoScaleLabel                = "autoscale"
	ScalingOutSuccess ScalingType = "AUTOSCALING:SCALE_OUT_SUCCESS"
	ScalingInSuccess  ScalingType = "AUTOSCALING:SCALE_IN_SUCCESS"
)

type Mns struct {
	nodes      *nodes.Nodes
	db         *dbclient.DBClient
	bdl        *bundle.Bundle
	js         jsonstore.JsonStore
	Config     *Config
	ClusterSvc clusterpb.ClusterServiceServer
}

type Config struct {
	client          ali_mns.MNSClient
	queue           ali_mns.AliMNSQueue
	QueueName       string
	endpoint        string
	accessKey       string
	accessKeySecret string
	cluster         string
	region          string
	AccountID       string
	BathSize        int32
}

func New(db *dbclient.DBClient, bdl *bundle.Bundle, n *nodes.Nodes, js jsonstore.JsonStore, clusterSvc clusterpb.ClusterServiceServer) *Mns {
	return &Mns{db: db, bdl: bdl, nodes: n, js: js, ClusterSvc: clusterSvc}
}

func (m *Mns) GetAccountID(req apistructs.BasicCloudConf) (string, error) {
	client, err := sts.NewClientWithAccessKey(req.Region, req.AccessKeyId, req.AccessKeySecret)
	if err != nil {
		return "", err
	}

	request := sts.CreateGetCallerIdentityRequest()
	request.Scheme = "https"
	response, err := client.GetCallerIdentity(request)
	if err != nil {
		return "", err
	}
	if !response.BaseResponse.IsSuccess() {
		errStr := fmt.Sprintf("get account info response status code: %d", response.BaseResponse.GetHttpStatus())
		return "", errors.New(errStr)
	}
	if m.isEmpty(response.AccountId) {
		return "", errors.New("empty account id")
	}
	return response.AccountId, nil
}

func (m *Mns) InitConfig(req apistructs.MnsReq) error {
	if m.isEmpty(req.AccountId) {
		accountInfoReq := apistructs.BasicCloudConf{Region: req.Region, AccessKeyId: req.AccessKeyId, AccessKeySecret: req.AccessKeySecret}
		var err error
		accountId, err := m.GetAccountID(accountInfoReq)
		if err != nil {
			logrus.Errorf("cluster: %s, get accout id error: %v", req.ClusterName, err)
			return err
		}
		req.AccountId = accountId
	}

	endpoint := fmt.Sprintf("http://%s.mns.%s.aliyuncs.com/", req.AccountId, req.Region)
	logrus.Debugf("cluster: %s, mns get endpoint: %s", req.ClusterName, endpoint)

	client := ali_mns.NewAliMNSClient(endpoint, req.AccessKeyId, req.AccessKeySecret)
	queueName := strings.Join([]string{AsgMnsQueuePrefix, req.ClusterName}, "-")
	queue := ali_mns.NewMNSQueue(queueName, client)
	var c Config
	c.AccountID = req.AccountId
	c.client = client
	c.endpoint = endpoint
	c.accessKey = req.AccessKeyId
	c.accessKeySecret = req.AccessKeySecret
	c.cluster = req.ClusterName
	c.region = req.Region
	c.QueueName = queueName
	c.queue = queue
	c.BathSize = 4
	m.Config = &c

	return nil
}

func (m *Mns) CreateQueue() error {
	queueManager := ali_mns.NewMNSQueueManager(m.Config.client)
	// message live time in queue is 12h == 43200s
	err := queueManager.CreateQueue(m.Config.QueueName, 0, 65536, 43200, 30, 0, 3)

	if err != nil && !ali_mns.ERR_MNS_QUEUE_ALREADY_EXIST_AND_HAVE_SAME_ATTR.IsEqual(err) {
		logrus.Errorf("create queue failed, cluster: %s, error: %v", m.Config.cluster, err)
		return err
	}
	return nil
}

func (m *Mns) DeleteMsg(receiptHandle string, visibilityTimeout int64) error {
	rsp, err := m.Config.queue.ChangeMessageVisibility(receiptHandle, visibilityTimeout)
	if err != nil {
		logrus.Errorf("set message visibility failed, cluster: %s, error: %v", m.Config.cluster, err)
		return err
	}
	logrus.Debugf("change message visibility, response: %v", rsp)
	err = m.Config.queue.DeleteMessage(rsp.ReceiptHandle)
	if err != nil {
		logrus.Infof("delete message failed, cluster: %s, id: %s, error: %v", m.Config.cluster, receiptHandle, err)
		return err
	}
	return nil
}

func (m *Mns) ReceiveMsg() (*ali_mns.MessageReceiveResponse, error) {
	var (
		rspContent ali_mns.MessageReceiveResponse
		rspErr     error
	)

	respChan := make(chan ali_mns.MessageReceiveResponse, 1)
	errChan := make(chan error, 1)
	end := make(chan int)
	go func() {
		select {
		case rspContent = <-respChan:
			{
				end <- 1
			}
		case rspErr = <-errChan:
			{
				// MessageNotExist Error can be ignored
				logrus.Errorf("cluster: %s, receive message error: %v", m.Config.cluster, rspErr)
				end <- 0
			}
		}
	}()
	m.Config.queue.ReceiveMessage(respChan, errChan, 10)
	// wait go goroutine receive message response
	<-end
	return &rspContent, rspErr
}

func (m *Mns) BathReceiveMsg() (*ali_mns.BatchMessageReceiveResponse, error) {
	var (
		rspContent ali_mns.BatchMessageReceiveResponse
		rspErr     error
	)

	respChan := make(chan ali_mns.BatchMessageReceiveResponse)
	errChan := make(chan error)
	end := make(chan int)
	go func() {
		select {
		case rspContent = <-respChan:
			{
				end <- 1
			}
		case rspErr = <-errChan:
			{
				end <- 0
			}
		}
	}()
	m.Config.queue.BatchReceiveMessage(respChan, errChan, m.Config.BathSize, 10)
	// wait go goroutine receive message response
	<-end
	return &rspContent, rspErr
}

func (m *Mns) GetEssActivityMsgs() ([]apistructs.EssActivityMsg, error) {
	var result []apistructs.EssActivityMsg
	rsp, err := m.BathReceiveMsg()
	if err != nil {
		if strings.Contains(err.Error(), "MessageNotExist") {
			return nil, nil
		}
		return nil, err
	}
	for _, msg := range rsp.Messages {
		content, err := base64.StdEncoding.DecodeString(msg.MessageBody)
		if err != nil {
			logrus.Errorf("base64 decode received message error: %v", err)
			// in batch process, continue process next one when error happened
			continue
		}
		var activityMsg apistructs.EssActivityMsg
		err = json.Unmarshal(content, &activityMsg)
		if err != nil {
			logrus.Errorf("cluster:%s, unmarshal ess activity msg error: %v, raw content: %v", m.Config.cluster, err, content)
			continue
		}
		activityMsg.ReceiptHandle = msg.ReceiptHandle
		logrus.Debugf("cluster: %s, ess activity message: %v", m.Config.cluster, activityMsg)
		result = append(result, activityMsg)
	}

	return result, nil
}

// get scale out instance info: map[instanceId]instanceIp
func (m *Mns) GetScaleOutInfo() (*apistructs.ScaleInfo, error) {
	msgs, err := m.GetEssActivityMsgs()
	if err != nil {
		logrus.Errorf("get ess activity error: %v", err)
		return nil, err
	}

	var deleteMsgIDs []string
	var result apistructs.ScaleInfo
	var req apistructs.EcsInfoReq
	req.AccessKeySecret = m.Config.accessKeySecret
	req.AccessKeyId = m.Config.accessKey
	req.Region = m.Config.region
	IDSet := set.New()

	for _, msg := range msgs {
		if msg.Event != ScalingOutSuccess.Sting() {
			deleteMsgIDs = append(deleteMsgIDs, msg.ReceiptHandle)
			logrus.Infof("ignore scaling activity, type: %s, activity: %v", msg.Event, msg)
		} else {
			// deduplicate instance ids, merge all instance ids into one query
			for _, id := range msg.Content.InstanceIds {
				if IDSet.Has(id) {
					continue
				}
				IDSet.Insert(id)
				req.InstanceIds = append(req.InstanceIds, id)
			}
			// each message id is uniq, no need to deduplicate
			result.ReceiptHandles = append(result.ReceiptHandles, msg.ReceiptHandle)
			logrus.Infof("scaling out activity, type: %s, activity: %v", msg.Event, msg)
		}
	}
	if deleteMsgIDs != nil {
		_, err := m.Config.queue.BatchDeleteMessage(deleteMsgIDs...)
		if err != nil {
			logrus.Errorf("batch delete messages failed, cluster: %s, error: %v", m.Config.cluster, err)
		}
	}

	if req.InstanceIds == nil {
		logrus.Debugf("no instance ids in request, cluster:%s", m.Config.cluster)
		return nil, nil
	}

	content, err := m.GetInstancesPrivateIp(req)
	if err != nil {
		logrus.Errorf("get instance private ip failed, request: %v, error: %v", req, err)
		return nil, err
	}
	result.Instances = content

	return &result, nil
}

// periodically fetch cluster info from db
func (m *Mns) PeriodicallyFetchClusters() chan *clusterpb.ClusterInfo {
	clusterInfoChan := make(chan *clusterpb.ClusterInfo, 100)
	ticker := time.NewTicker(time.Minute * 1)

	go func() {
		clustersInfo := m.FetchValidClusterInfo()
		for _, info := range clustersInfo {
			clusterInfoChan <- info
		}
		for range ticker.C {
			clustersInfo := m.FetchValidClusterInfo()
			for _, info := range clustersInfo {
				clusterInfoChan <- info
			}
		}
	}()

	return clusterInfoChan
}

func (m *Mns) FetchValidClusterInfo() []*clusterpb.ClusterInfo {
	ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := m.ClusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{})
	if err != nil {
		logrus.Error("failed get to get cluster list")
		return nil
	}
	clusters := resp.Data
	var result []*clusterpb.ClusterInfo
	for _, cluster := range clusters {
		err := m.validateOpsConfig(cluster.OpsConfig)
		if err != nil {
			logrus.Errorf("cluster: %s, error: %v", cluster.Name, err)
			continue
		}
		if cluster.OpsConfig.ScaleMode != "none" {
			cluster.OpsConfig.AccessKey = encrypt.AesDecrypt(cluster.OpsConfig.AccessKey, apistructs.TerraformEcyKey)
			cluster.OpsConfig.SecretKey = encrypt.AesDecrypt(cluster.OpsConfig.SecretKey, apistructs.TerraformEcyKey)
			cluster.OpsConfig.EcsPassword = encrypt.AesDecrypt(cluster.OpsConfig.EcsPassword, apistructs.TerraformEcyKey)
			if m.isEmpty(cluster.OpsConfig.AccessKey) || m.isEmpty(cluster.OpsConfig.SecretKey) || m.isEmpty(cluster.OpsConfig.EcsPassword) {
				logrus.Errorf("cluster: %s, empty access key", cluster.Name)
				continue
			}
			result = append(result, cluster)
		}
	}
	return result
}

func (m *Mns) validateOpsConfig(opsConf *clusterpb.OpsConfig) error {
	if opsConf == nil {
		err := fmt.Errorf("empty ops config")
		logrus.Error(err.Error())
		return err
	}

	if m.isEmpty(opsConf.AccessKey) || m.isEmpty(opsConf.SecretKey) || m.isEmpty(opsConf.Region) || m.isEmpty(opsConf.EcsPassword) {
		err := fmt.Errorf("invalid ops config")
		return err
	}
	return nil
}

func (m *Mns) isEmpty(str string) bool {
	return strings.Replace(str, " ", "", -1) == ""
}

func (m *Mns) Consume(clusterInfo *clusterpb.ClusterInfo) {
	mnsReq := m.getMnsReq(clusterInfo)
	err := m.InitConfig(mnsReq)
	if err != nil {
		logrus.Errorf("init mns failed, cluster: %s, error: %v.", clusterInfo.Name, err)
		return
	}

	// TODO: optimize
	// preprocess, check previous status
	err = m.PreProcess(clusterInfo)
	if err != nil {
		logrus.Errorf("preprocess failed, cluster name: %s, error: %v", clusterInfo.Name, err)
		return
	}

	// get scale out info
	info, err := m.GetScaleOutInfo()
	if err != nil {
		logrus.Errorf("cluster info: %v, get scale out info error: %v.", clusterInfo.Name, err)
		return
	}

	// ignoring invalid messages
	if info == nil {
		return
	}

	// custom label1: dice/org-{orgName}; custom label2: dice/ess-autoscale
	o, err := m.bdl.GetOrg(clusterInfo.OrgID)
	if err != nil {
		logrus.Errorf("failed to get org name, cluster: %s, org id: %d", clusterInfo.Name, clusterInfo.OrgID)
		return
	}
	addNodeReq, err := m.getAddNodesReq(clusterInfo, info.Instances)
	if err != nil {
		logrus.Errorf("get add nodes request failed, error: %v", err)
		return
	}
	addNodeReq.Labels = append(addNodeReq.Labels, "org-"+o.Name)

	// user id -- "1110": represent system user
	// add nodes
	id, err := m.nodes.AddNodes(*addNodeReq, apistructs.AutoScaleUserID)
	if err != nil {
		logrus.Errorf("cluster: %s, record id : %d, failed to add nodes, error: %v", clusterInfo.Name, id, err)
		return
	}

	// TODO: success or timeout, delete message;
	for _, handle := range info.ReceiptHandles {
		err = m.Config.queue.DeleteMessage(handle)
		if err != nil {
			logrus.Errorf("delete successfully consumed message failed, err: %v", err)
		}
	}
}

func (m *Mns) LockCluster(clusterName string) error {
	lockKey := fmt.Sprintf("%s/%s", apistructs.AutoScaleLockPrefix, clusterName)
	err := retry.DoWithInterval(func() error {
		return m.js.Put(context.Background(), lockKey, nil)
	}, 3, 500*time.Millisecond)
	logrus.Errorf("lock cluster failed, cluster name: %v, error: %v", clusterName, err)
	return err
}

func (m *Mns) UnlockCluster(clusterName string) error {
	lockKey := fmt.Sprintf("%s/%s", apistructs.AutoScaleLockPrefix, clusterName)
	err := retry.DoWithInterval(func() error {
		return m.js.Remove(context.Background(), lockKey, nil)
	}, 3, 500*time.Millisecond)
	logrus.Errorf("unlock cluster failed, cluster name: %v, error: %v", clusterName, err)
	return err
}

// return true: cluster locked, else unlocked
func (m *Mns) IsClusterLocked(clusterName string) (bool, error) {
	lockKey := fmt.Sprintf("%s/%s", apistructs.AutoScaleLockPrefix, clusterName)
	var notExist *bool
	var notFound bool
	var err error
	for i := 0; i < 3; i++ {
		notFound, err = m.js.Notfound(context.Background(), lockKey)
		if err == nil {
			notExist = &notFound
			break
		}
		time.Sleep(500 * time.Millisecond)
	}
	// Check failed, return error
	if notExist == nil {
		err := fmt.Errorf("check cluster lock key failed, key: %v, error: %v", lockKey, err)
		logrus.Errorf(err.Error())
		return false, err
	}

	return !(*notExist), nil
}

// check latest add nodes status, if failed, lock auto scale
func (m *Mns) PreProcess(info *clusterpb.ClusterInfo) error {

	// Check the cluster's history of adding/removing nodes to determine whether the cluster is need lock
	var req apistructs.RecordRequest
	req.PageNo = 0
	req.PageSize = 1
	req.UserIDs = []string{apistructs.AutoScaleUserID}
	req.ClusterNames = []string{info.Name}
	rsp, err := m.nodes.Query(req)
	if err != nil {
		logrus.Errorf("query ops record failed, request: %v, rsp: %v, error: %v", req, rsp, err)
		return err
	}
	if rsp == nil {
		err := fmt.Errorf("query ops record failed, empty response, request: %v", req)
		logrus.Errorf(err.Error())
		return err
	}
	if rsp.Data.Total == 0 {
		return nil
	}
	content := rsp.Data.List[0]
	if content.Status != string(dbclient.StatusTypeFailed) {
		return nil
	}
	// TODO: process failed status
	// For non-schedule tasks, save the information in the detail field of ops_record; for schedule tasks, only need the scale group id.
	// Failed to add or delete the machine, lock it to avoid adding machine next
	if content.RecordType == string(dbclient.RecordTypeAddEssNodes) || content.RecordType == string(dbclient.RecordTypeDeleteEssNodes) {
		err := m.LockCluster(info.Name)
		if err != nil {
			logrus.Errorf("mns lock cluster failed, error: %v", err)
			return err
		}
	}

	// Force delete ess nodes when add host failed
	if content.RecordType == string(dbclient.RecordTypeAddEssNodes) {
		var d apistructs.NodesRecordDetail
		if err := json.Unmarshal([]byte(content.Detail), &d); err != nil {
			logrus.Errorf("unmarshal record detail failed, detail: %v, error: %v", content.Detail, err)
			return err
		}
		var reqDL apistructs.DeleteNodesRequest
		reqDL.OrgID = uint64(info.OrgID)
		reqDL.ClusterName = info.Name
		reqDL.Hosts = d.Hosts
		reqDL.AccessKey = m.Config.accessKey
		reqDL.SecretKey = m.Config.accessKeySecret
		reqDL.Region = m.Config.region
		reqDL.ScalingGroupId = info.OpsConfig.EssGroupID
		reqDL.InstanceIDs = strings.Join(d.InstanceIDs, ",")
		reqDL.ForceDelete = true
		if _, err := m.nodes.DeleteEssNodes(reqDL, apistructs.AutoScaleUserID, strconv.Itoa(int(info.OrgID))); err != nil {
			logrus.Errorf("mns force delete ess nodes failed, cluster name: %v, error: %v", info.Name, err)
			return err
		}
	}

	return nil
}

func (m *Mns) getMnsReq(info *clusterpb.ClusterInfo) apistructs.MnsReq {
	var r apistructs.MnsReq
	r.Region = info.OpsConfig.Region
	r.AccessKeyId = info.OpsConfig.AccessKey
	r.AccessKeySecret = info.OpsConfig.SecretKey
	r.ClusterName = info.Name
	return r
}

func (m *Mns) getAddNodesReq(info *clusterpb.ClusterInfo, msg map[string]string) (*apistructs.AddNodesRequest, error) {
	var r apistructs.AddNodesRequest
	var d apistructs.NodesRecordDetail
	r.ClusterName = info.Name
	r.OrgID = uint64(info.OrgID)
	// hosts field come from msg received from mns
	for id, ip := range msg {
		r.Hosts = append(r.Hosts, ip)
		d.InstanceIDs = append(d.InstanceIDs, id)
	}
	d.Hosts = r.Hosts
	r.Labels = []string{"workspace-prod", "bigdata-job", "stateless-service", "job", AutoScaleLabel}
	r.Port = 22
	r.User = "root"
	r.Password = info.OpsConfig.EcsPassword
	r.Source = apistructs.AddNodesEssSource
	detail, err := json.Marshal(d)
	if err != nil {
		logrus.Errorf("marshal cmp record detail failed, info: %v, error: %v", d, err)
		return nil, err

	}
	r.Detail = string(detail)
	return &r, nil
}

func (m *Mns) Process(ctx context.Context) {
	ticker := time.NewTicker(time.Minute * 1)
	logrus.Infof("begin to execute mns...")
	var t uint64
	for range ticker.C {
		logrus.Infof("begin to execute mns in loop...")
		t += 1
		clustersInfo := m.FetchValidClusterInfo()
		for i, c := range clustersInfo {
			// Batch update ess/cron/job calculate records every five minutes
			if (uint64(i)+t)%5 == 0 {
				req := apistructs.RecordUpdateRequest{
					ClusterName: c.Name,
					UserID:      apistructs.AutoScaleUserID,
					OrgID:       strconv.Itoa(int(c.OrgID)),
					RecordType:  string(dbclient.RecordTypeDeleteEssNodes),
					PageSize:    1,
				}
				err := m.nodes.UpdateCronJobRecord(req)
				if err != nil {
					logrus.Errorf("update ess cron job record failed, request: %v, error: %v", req, err)
				}
			}
			// Get the mns message and deal with it
			m.Consume(c)
		}
	}
	<-ctx.Done()
}

func (m *Mns) Run() {
	// monitor mns queue, and process mns message
	go func() {
		for {
			ctx, cancel := context.WithCancel(context.Background())
			lock, err := dlock.New("/autoscale/mns", func() { cancel() })
			if err := lock.Lock(context.Background()); err != nil {
				logrus.Errorf("mns, failed to lock: %v", err)
				continue
			}

			if err != nil {
				logrus.Errorf("mns, failed to get dlock: %v", err)
				// try again
				continue
			}
			m.Process(ctx)
			if err := lock.Unlock(); err != nil {
				logrus.Errorf("failed to unlock: %v", err)
			}
		}
	}()
}
