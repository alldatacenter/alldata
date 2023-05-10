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

package sls

import (
	"fmt"
	"time"

	sls "github.com/aliyun/aliyun-log-go-sdk"
	slsconsumer "github.com/aliyun/aliyun-log-go-sdk/consumer"
	"github.com/recallsong/go-utils/conv"
	"github.com/recallsong/go-utils/encoding/md5x"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda/internal/pkg/metrics/report"
)

// Consumer .
type Consumer struct {
	log      logs.Logger
	account  *Account
	endpoint string
	project  string
	logStore string
	handler  ConsumeFunc

	id     string
	worker *slsconsumer.ConsumerWorker
}

func newConsumer(log logs.Logger, a *Account, endpoint, project, logStore string, handler ConsumeFunc) *Consumer {
	c := &Consumer{
		log:      log,
		account:  a,
		endpoint: endpoint,
		project:  project,
		logStore: logStore,
		handler:  handler,
	}
	if handler != nil {
		c.init()
	}
	return c
}

func (c *Consumer) init() {
	c.id = c.project + "/" + c.logStore
	group := c.account.Group
	if len(group) <= 0 {
		group = fmt.Sprintf("sls-%s-%s-%s-group", c.account.AccessKey, c.project, c.logStore)
		group = md5x.SumString(group).String()
	}
	c.log.Infof("create log consumer{ak=%q, endpoint=%q, project=%q, store=%q, group=%q}", c.account.AccessKey, c.endpoint, c.project, c.logStore, group)
	c.worker = slsconsumer.InitConsumerWorker(slsconsumer.LogHubConfig{
		Endpoint:          c.endpoint,
		AccessKeyID:       c.account.AccessKey,
		AccessKeySecret:   c.account.AccessSecretKey,
		Project:           c.project,
		Logstore:          c.logStore,
		ConsumerGroupName: group,
		ConsumerName:      fmt.Sprintf("sls-%s-%s-%s", c.account.AccessKey, c.project, c.logStore),
		CursorPosition:    slsconsumer.END_CURSOR,
	}, c.handler)
}

func (c *Consumer) Start() error {
	if c.worker != nil {
		c.worker.Start()
	}
	return nil
}

func (c *Consumer) Close() error {
	if c.worker != nil {
		c.log.Infof("stop log consumer{ak=%q, endpoint=%q, project=%q, store=%q}", c.account.AccessKey, c.endpoint, c.project, c.logStore)
		c.worker.StopAndWait()
	}
	return nil
}

func (p *provider) GetHandler(project, store, typ string, account *Account) ConsumeFunc {
	switch typ {
	case "slb":
		return p.slbHandler(account, typ)
	}
	return func(shardID int, groups *sls.LogGroupList) string {
		// do nothing
		return ""
	}
}

const batchSize = 100

func (p *provider) slbHandler(account *Account, typ string) ConsumeFunc {
	return func(shardID int, groups *sls.LogGroupList) string {
		var metrics report.Metrics
		for _, item := range groups.LogGroups {
			for _, log := range item.Logs {
				m := &report.Metric{
					Name:      "slb_access",
					Timestamp: int64(log.GetTime()) * int64(time.Second),
					Tags:      make(map[string]string),
					Fields:    make(map[string]interface{}),
				}
				for k, v := range account.Tags {
					m.Tags[k] = v
				}
				for _, kv := range log.Contents {
					// client_ip = 120.79.15.254
					// time = 2021-11-16T12:01:06+08:00
					// request_method = GET
					// scheme = https
					// http_host = romens-rpc.dice.yimeiduo.com.cn
					// request_uri = /entmember-rpc/api/member/for-small-deer/v2/get-member?phone=18798468305&token=6b7d932e883b434e9b31aef3df13deee
					// request_length = 291
					// server_protocol = HTTP/1.1
					// body_bytes_sent = 360
					// http_referer = -
					// http_user_agent = Apache-HttpClient/4.5.12 (Java/1.8.0_211)
					// http_x_real_ip = -
					// http_x_forwarded_for = -
					// status = 200
					// request_time = 0.014
					// upstream_addr = 172.16.2.126:80
					// upstream_response_time = 0.014
					// upstream_status = 200
					// host = romens-rpc.dice.yimeiduo.com.cn
					// slbid = lb-wz95c3xjs15keeolzw6xi
					// vip_addr = 47.107.107.3
					// slb_vport = 443
					// ssl_cipher = ECDHE-RSA-AES128-GCM-SHA256
					// ssl_protocol = TLSv1.2
					// read_request_time = 0
					// write_response_time = 0
					// tcpinfo_rtt = 2353
					// client_port = 38334
					key := kv.GetKey()
					value := kv.GetValue()
					switch key {
					case "request_length":
						m.Fields[key] = conv.ParseFloat64(value, 0)
					case "body_bytes_sent":
						m.Fields[key] = conv.ParseFloat64(value, 0)
					case "status", "upstream_status":
						m.Fields[key] = conv.ParseFloat64(value, 0)
						m.Tags[key] = value
					case "request_time":
						m.Fields[key] = conv.ParseFloat64(value, 0)
					case "upstream_response_time":
						m.Fields[key] = conv.ParseFloat64(value, 0)
					case "read_request_time", "write_response_time":
						m.Fields[key] = conv.ParseFloat64(value, 0)
					default:
						m.Tags[key] = value
					}
				}
				metrics = append(metrics, m)
				if len(metrics) >= batchSize {
					err := p.Reporter.Send(metrics)
					if err != nil {
						p.Log.Error(err)
					}
					metrics = nil
				}
			}
		}
		if len(metrics) > 0 {
			err := p.Reporter.Send(metrics)
			if err != nil {
				p.Log.Error(err)
			}
		}
		return ""
	}
}
