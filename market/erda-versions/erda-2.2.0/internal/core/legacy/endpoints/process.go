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

package endpoints

// import (
// 	"encoding/json"
// 	"time"
//
// 	"github.com/confluentinc/confluent-kafka-go/kafka"
// 	"github.com/pkg/errors"
// 	"github.com/sirupsen/logrus"
//
// 	"github.com/erda-project/erda/internal/core/legacy/conf"
// 	"github.com/erda-project/erda/internal/core/legacy/model"
// 	"github.com/erda-project/erda/internal/core/legacy/types"
// 	"github.com/erda-project/erda/pkg/strutil"
// )
//
// const (
// 	// 15min 全量同步一把 running containers
// 	measurementAllRunningContainers = "metaserver_all_containers"
// 	measurementContainer            = "metaserver_container"
// 	measurementHost                 = "metaserver_host"
// )
//
// // MsgTimeout 消息超时时间
// var MsgTimeout = 3 * time.Minute
//
// func (e *Endpoints) Consumer() {
//
// 	c, err := kafka.NewConsumer(&kafka.ConfigMap{
// 		"bootstrap.servers":        conf.KafkaBrokers(),
// 		"group.id":                 conf.KafkaGroup(),
// 		"auto.offset.reset":        "latest",
// 		"auto.commit.interval.ms":  1000,
// 		"message.send.max.retries": 10000000,
// 	})
// 	if err != nil {
// 		logrus.Errorf("failed to create new kafka consumer: %v", err)
// 		return
// 	}
//
// 	defer func() {
// 		if err = c.Close(); err != nil {
// 			logrus.Error(err)
// 		}
// 	}()
//
// 	hostTopic := conf.KafkaHostTopic()
// 	contianerTopic := conf.KafkaContainerTopic()
// 	topics := []string{hostTopic, contianerTopic}
// 	logrus.Infof("cmdb topics: %v", topics)
//
// 	if err = c.SubscribeTopics(topics, nil); err != nil {
// 		logrus.Errorf("failed to subscribe kafka topics: %v", err)
// 		return
// 	}
//
// 	for {
// 		msg, err := c.ReadMessage(-1)
// 		if err != nil {
// 			logrus.Errorf("failed to read kafka message: %v", err)
// 			continue
// 		}
//
// 		logrus.Debugf("read message from kafka, topic: %s, value: %s, timestamp: %s",
// 			*msg.TopicPartition.Topic, string(msg.Value), msg.Timestamp)
//
// 		switch *msg.TopicPartition.Topic {
// 		case hostTopic:
// 			e.processHost(msg)
// 		case contianerTopic:
// 			e.processContainer(msg)
// 		default:
// 			logrus.Warnf("invalid kafka topic: %s", *msg.TopicPartition.Topic)
// 		}
// 	}
// }
//
// // TODO: 使用 cluster node 表进行重构
// func (e *Endpoints) processHost(msg *kafka.Message) {
// 	var hostMsg types.MetaserverMSG
// 	var h *model.Host
// 	var err error
//
// 	if err := json.Unmarshal(msg.Value, &hostMsg); err != nil {
// 		logrus.Warnf("failed to unmarshal host message: %v", err)
// 		return
// 	}
//
// 	msgName := hostMsg.Name
//
// 	if msgName == measurementHost {
// 		if h, err = initHost(hostMsg.Fields); err != nil {
// 			logrus.Errorf("failed to init host struct: %v", err)
// 			return
// 		}
// 		if err = e.host.CreateOrUpdate(h); err != nil {
// 			logrus.Errorf("failed to sync host: %+v, (%v)", h, err)
// 		}
// 	}
// }
//
// func (e *Endpoints) processContainer(msg *kafka.Message) {
// 	var containerMsg types.MetaserverMSG
// 	if err := json.Unmarshal(msg.Value, &containerMsg); err != nil {
// 		logrus.Errorf("failed to unmarshal container message: %v", err)
// 		return
// 	}
//
// 	switch containerMsg.Name {
// 	case measurementContainer:
// 		container := e.container.ConvertToContainer(containerMsg.Fields)
// 		if !types.IsValidAgentInstanceStatus(container.Status) {
// 			logrus.Warningf("ignore invalid status(%s), not in (Starting, Killed, OOM)", container.Status)
// 			return
// 		}
//
// 		if err := e.container.CreateOrUpdateContainer(container); err != nil {
// 			logrus.Errorf("failed to update container, id: %d, error: %v", container.ID, err)
// 			return
// 		}
// 	case measurementAllRunningContainers:
// 		msgStart := time.Unix(containerMsg.TimeStamp/int64(time.Second/time.Nanosecond), 0)
// 		delayTime := time.Now().Sub(msgStart)
// 		if delayTime > MsgTimeout {
// 			logrus.Warningf("all running containers message expired, delaySeconds: %vs, timeout: %vs, msg: %+v",
// 				delayTime.Seconds(), MsgTimeout.Seconds(), containerMsg)
// 			return
// 		}
//
// 		containers := make([]*model.Container, 0, len(containerMsg.Fields))
// 		for _, v := range containerMsg.Fields { // k为containerIDs
// 			var fields map[string]interface{}
// 			if err := json.Unmarshal(([]byte)(v.(string)), &fields); err != nil {
// 				logrus.Errorf("failed to json unmarshal all container fields: %v", err)
// 				return
// 			}
//
// 			container := e.container.ConvertToContainer(fields)
// 			containers = append(containers, container)
// 		}
// 		if len(containers) == 0 {
// 			return
// 		}
// 		if err := e.container.SyncContainerInfo(containers); err != nil {
// 			logrus.Errorf("failed to sync container info, %v", err)
// 			return
// 		}
// 	}
// }
//
// func initHost(fields map[string]interface{}) (*model.Host, error) {
// 	var h model.Host
//
// 	if len(fields) == 0 {
// 		return nil, errors.Errorf("invalid fields: inputs is null")
// 	}
//
// 	if clusterName, ok := fields["cluster_full_name"]; ok {
// 		h.Cluster = clusterName.(string)
// 	}
//
// 	if ip, ok := fields["private_addr"]; ok {
// 		h.PrivateAddr = ip.(string)
// 	}
//
// 	if hostname, ok := fields["hostname"]; ok {
// 		h.Name = hostname.(string)
// 	}
//
// 	if cpus, ok := fields["cpus"]; ok {
// 		h.Cpus = cpus.(float64)
// 	}
//
// 	if mem, ok := fields["memory"]; ok {
// 		h.Memory = (int64)(mem.(float64))
// 	}
//
// 	if disk, ok := fields["disk"]; ok {
// 		h.Disk = (int64)(disk.(float64))
// 	}
//
// 	if os, ok := fields["os"]; ok {
// 		h.OS = os.(string)
// 	}
//
// 	if kernelVersion, ok := fields["kernel_version"]; ok {
// 		h.KernelVersion = kernelVersion.(string)
// 	}
//
// 	if sysTime, ok := fields["system_time"]; ok {
// 		h.SystemTime = sysTime.(string)
// 	}
//
// 	if labels, ok := fields["labels"]; ok {
//
// 		orgNames, hostLabels := parseLabel(labels.(string))
// 		h.Labels = hostLabels
// 		if orgNames != "" {
// 			h.OrgName = orgNames
// 		}
// 	}
//
// 	if timestamp, ok := fields["timestamp"]; ok {
// 		h.TimeStamp = (int64)(timestamp.(float64))
// 	}
//
// 	return &h, nil
// }
//
// // parseLabel label格式解析，提取出orgName，并从label里去除orgName
// // 	1. dcos labels: MESOS_ATTRIBUTES=dice_tags:any,org-terminus,workspace-dev
// //	2. k8s labels: K8S_ATTRIBUTES=dice_tags:org-terminus,workspace-dev
// func parseLabel(labels string) (string, string) {
// 	if labels == "" {
// 		return "", ""
// 	}
// 	tmp := strutil.Split(labels, ":")
// 	if len(tmp) != 2 {
// 		return "", ""
// 	}
// 	la := strutil.Split(tmp[1], ",")
// 	var (
// 		orgLabel   []string
// 		otherLabel []string
// 	)
// 	for _, v := range la {
// 		if strutil.HasPrefixes(strutil.Trim(v), "org-") {
// 			orgLabel = append(orgLabel, strutil.TrimPrefixes(strutil.Trim(v), "org-"))
// 		}
// 		otherLabel = append(otherLabel, strutil.Trim(v))
// 	}
// 	return strutil.Join(orgLabel, ","), strutil.Join(otherLabel, ",")
// }
