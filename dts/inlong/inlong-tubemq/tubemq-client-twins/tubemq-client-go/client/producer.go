// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package client

import (
	"context"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"

	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/config"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/log"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/metadata"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/multiplexing"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/protocol"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/rpc"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/selector"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/transport"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
)

type producer struct {
	clientID            string
	config              *config.Config
	nextAuth2Master     int32
	selector            selector.Selector
	master              *selector.Node
	client              rpc.RPCClient
	masterHBRetry       int
	heartbeatManager    *heartbeatManager
	unreportedTimes     int
	publishTopics       []string
	publishTopicsMu     sync.Mutex
	brokerCheckSum      int64
	brokerMap           map[string]*metadata.Node
	brokerMu            sync.Mutex
	topicPartitionMap   map[string](map[int][]*metadata.Partition)
	topicPartitionMapMu sync.Mutex
	partitionRouter     *RoundRobinPartitionRouter
}

// NewProducer returns a producer which is constructed by a given config.
func NewProducer(config *config.Config) (Producer, error) {
	if err := config.ValidateProducer(); err != nil {
		return nil, err
	}
	log.Infof("The config of the producer is %s", config)

	selector, err := selector.Get("ip")
	if err != nil {
		return nil, err
	}

	clientName := util.NewClientID("", &clientID, tubeMQClientVersion)
	pool := multiplexing.NewPool()
	opts := &transport.Options{}
	if config.Net.TLS.Enable {
		opts.TLSEnable = true
		opts.CACertFile = config.Net.TLS.CACertFile
		opts.TLSCertFile = config.Net.TLS.TLSCertFile
		opts.TLSKeyFile = config.Net.TLS.TLSKeyFile
		opts.TLSServerName = config.Net.TLS.TLSServerName
	}

	client := rpc.New(pool, opts, config)

	p := &producer{
		config:            config,
		clientID:          clientName,
		selector:          selector,
		client:            client,
		unreportedTimes:   0,
		brokerMap:         make(map[string]*metadata.Node),
		publishTopics:     config.Producer.Topics,
		topicPartitionMap: make(map[string](map[int][]*metadata.Partition)),
		partitionRouter:   NewPartitionRouter(),
	}

	hbm := &heartbeatManager{
		producer:   p,
		heartbeats: make(map[string]*heartbeatMetadata),
	}
	p.heartbeatManager = hbm

	err = p.register2Master(true)
	if err != nil {
		return nil, err
	}

	// invoke the heartbeat when creating the producer
	p.heartbeatManager.registerMaster(p.master.Address)

	log.Infof("[PRODUCER] start producer success, client=%s", clientID)
	return p, nil
}

func (p *producer) Publish(topics []string) {
	p.publishTopicsMu.Lock()
	defer p.publishTopicsMu.Unlock()

	p.publishTopics = topics
}

func (p *producer) SendMessage(message *Message) (bool, int32, string) {
	partition := p.selectPartition(message)

	m := &metadata.Metadata{}
	node := &metadata.Node{}
	node.SetHost(util.GetLocalHost())
	node.SetAddress(partition.GetBroker().GetAddress())
	m.SetNode(node)

	ctx, cancel := context.WithTimeout(context.Background(), p.config.Net.ReadTimeout)
	defer cancel()

	rsp, err := p.client.SendMessageRequestP2B(ctx, m, p.clientID, partition, message.Data, message.Flag)
	if err != nil {
		log.Infof("[PRODUCER]SendMessage error %s", err.Error())
	}

	return rsp.GetSuccess(), rsp.GetErrCode(), rsp.GetErrMsg()
}

func (p *producer) register2Master(needChange bool) error {
	// if needChage, refresh the master list and start trying from the first master address
	if needChange {
		p.selector.Refresh(p.config.Producer.Masters)
	}

	retryCount := 0
	for {
		// select the next node and send request
		node, err := p.selector.Select(p.config.Producer.Masters)
		if err != nil {
			return err
		}
		p.master = node

		// send request
		rsp, err := p.sendRegRequest2Master()

		if err != nil || !rsp.GetSuccess() {
			// register2Master fail
			if err != nil {
				log.Errorf("[PRODUCER]register2Master error %s", err.Error())
			}

			// invode the rpc method successlly, but rsp code form tubemq server is not 200
			if rsp != nil {
				log.Errorf("[PRODUCER] register2Master(%s) failure exist register, client=%s, errCode: %d, errorMsg: %s",
					p.master.Address, p.clientID, rsp.GetErrCode(), rsp.GetErrMsg())
			}

			// no more available master address
			if !p.master.HasNext {
				log.Errorf("[PRODUCER] register2Master has tryed %s times, all failed!!!", retryCount+1)
				return errs.New(errs.RetRequestFailure, "No available master address to register, all tries fail!!!")
			}

			retryCount++
			log.Warnf("[PRODUCER] register2master(%s) failure, client=%s, retry count=%d",
				p.master.Address, p.clientID, retryCount)
			continue
		}

		// register successlly
		log.Infof("register2Master response %s", rsp.String())
		p.masterHBRetry = 0
		p.processRegisterResponseM2P(rsp)
		break
	}
	return nil
}

func (p *producer) sendRegRequest2Master() (*protocol.RegisterResponseM2P, error) {
	ctx, cancel := context.WithTimeout(context.Background(), p.config.Net.ReadTimeout)
	defer cancel()

	m := &metadata.Metadata{}
	node := &metadata.Node{}
	node.SetHost(util.GetLocalHost())
	node.SetAddress(p.master.Address)

	auth := &protocol.AuthenticateInfo{}
	if p.needGenMasterCertificateInfo(true) {
		util.GenMasterAuthenticateToken(auth, p.config.Net.Auth.UserName, p.config.Net.Auth.Password)
	}

	m.SetNode(node)

	rsp, err := p.client.RegisterRequestP2M(ctx, m, p.clientID)
	return rsp, err
}

func (p *producer) processRegisterResponseM2P(rsp *protocol.RegisterResponseM2P) {
	p.brokerCheckSum = rsp.GetBrokerCheckSum()
	brokerInfos := rsp.GetBrokerInfos()
	p.updateBrokerInfoList(brokerInfos)
}

func (p *producer) needGenMasterCertificateInfo(force bool) bool {
	needAdd := false
	if p.config.Net.Auth.Enable {
		if force {
			needAdd = true
			atomic.StoreInt32(&p.nextAuth2Master, 0)
		} else if atomic.LoadInt32(&p.nextAuth2Master) == 1 {
			if atomic.CompareAndSwapInt32(&p.nextAuth2Master, 1, 0) {
				needAdd = true
			}
		}
	}
	return needAdd
}

func (p *producer) updateBrokerInfoList(brokerInfos []string) {
	p.brokerMu.Lock()
	defer p.brokerMu.Unlock()
	for _, brokerInfo := range brokerInfos {
		node, _ := metadata.NewNode(true, strings.Trim(brokerInfo, " :"))
		p.brokerMap[strconv.FormatUint(uint64(node.GetID()), 10)] = node

	}
}

func (p *producer) updateTopicConfigure(topicInfos []string) {
	p.topicPartitionMapMu.Lock()
	defer p.topicPartitionMapMu.Unlock()

	var topicInfoList []*metadata.TopicInfo
	for _, topicInfo := range topicInfos {
		topicInfo = strings.Trim(topicInfo, " ")
		topicInfoTokens := strings.Split(topicInfo, "#")
		topicInfoSet := strings.Split(topicInfoTokens[1], ",")
		for _, s := range topicInfoSet {
			topicMetas := strings.Split(s, ":")
			brokerInfo := p.brokerMap[topicMetas[0]]
			partitionNum, _ := strconv.Atoi(topicMetas[1])
			storeNum, _ := strconv.Atoi(topicMetas[2])
			newBrokerNode, _ := metadata.NewNode(true, brokerInfo.String())
			topicInfoList = append(topicInfoList, metadata.NewTopicInfo(
				newBrokerNode,
				topicInfoTokens[0],
				partitionNum,
				storeNum,
			))
		}
	}

	for _, topicInfo := range topicInfoList {
		_, ok := p.topicPartitionMap[topicInfo.GetTopic()]
		if !ok {
			p.topicPartitionMap[topicInfo.GetTopic()] = make(map[int][]*metadata.Partition)
		}
		for j := 0; j < topicInfo.GetStoreNum(); j++ {
			for i := 0; i < topicInfo.GetPartitionNum(); i++ {
				partitionStr := topicInfo.GetNode().String() + "#" + topicInfo.GetTopic() + ":" + strconv.Itoa(i)
				part, err := metadata.NewPartition(partitionStr)
				if err != nil {
					println("New partition failed")
				}
				// p.topicPartitionMap[topicInfo.GetTopic()][int(topicInfo.GetNode().GetID())] = part
				partList := p.topicPartitionMap[topicInfo.GetTopic()][int(topicInfo.GetNode().GetID())]
				partList = append(partList, part)
				p.topicPartitionMap[topicInfo.GetTopic()][int(topicInfo.GetNode().GetID())] = partList
			}
		}
	}
}

func (p *producer) selectPartition(message *Message) *metadata.Partition {
	topicName := message.Topic
	var partitionList []*metadata.Partition
	for brokerId := range p.topicPartitionMap[topicName] {
		partitionList = append(partitionList, p.topicPartitionMap[topicName][brokerId]...)
	}
	selectIndex := p.partitionRouter.GetPartition(message, partitionList)
	return partitionList[selectIndex]
}
