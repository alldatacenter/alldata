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
	clientID        string
	config          *config.Config
	nextAuth2Master int32
	selector        selector.Selector
	master          *selector.Node
	client          rpc.RPCClient
	masterHBRetry   int
	unreportedTimes int
	publishTopics   []string
	brokerCheckSum  int64
	brokerMap       map[string]*metadata.Node
	brokerMu        sync.Mutex
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
		config:          config,
		clientID:        clientName,
		selector:        selector,
		client:          client,
		unreportedTimes: 0,
		brokerMap:       make(map[string]*metadata.Node),
		publishTopics:   config.Producer.Topics,
	}

	err = p.register2Master(true)
	if err != nil {
		return nil, err
	}

	log.Infof("[PRODUCER] start producer success, client=%s", clientID)
	return p, nil
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

func (p *producer) Publish(topics []string) {
	println("todo in later commits")
}
