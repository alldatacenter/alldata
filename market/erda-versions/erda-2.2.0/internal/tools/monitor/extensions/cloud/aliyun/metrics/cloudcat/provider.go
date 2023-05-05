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

package cloudcat

import (
	"reflect"
	"strings"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	writer "github.com/erda-project/erda-infra/pkg/parallel-writer"
	"github.com/erda-project/erda-infra/providers/kafka"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/cloud/aliyun/metrics/cloudcat/api"
	g "github.com/erda-project/erda/internal/tools/monitor/extensions/cloud/aliyun/metrics/cloudcat/globals"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/cloud/aliyun/metrics/cloudcat/scheduler"
)

type define struct{}

func (d *define) Services() []string { return []string{"cloudcat"} }
func (d *define) Summary() string    { return "cloudcat" }
func (d *define) Dependencies() []string {
	return []string{"kafka-producer"}
}
func (d *define) Description() string { return d.Summary() }
func (d *define) Config() interface{} { return &g.Config{} }
func (d *define) Creator() servicehub.Creator {
	return func() servicehub.Provider {
		return &manager{}
	}
}

type manager struct {
	Cfg                 *g.Config
	Logger              logs.Logger
	writer              writer.Writer
	metaOrgClusters     []api.OrgInfo
	schedulers          []*scheduler.Scheduler
	schedulerChangedSig chan int
	done                chan struct{}
	ctx                 servicehub.Context
}

func (m *manager) Init(ctx servicehub.Context) error {
	g.Log = m.Logger
	g.Cfg = m.Cfg
	g.Cfg.Products = strings.Split(m.Cfg.ProductsCfg, ",")

	m.ctx = ctx
	if err := m.setWriter(); err != nil {
		return err
	}
	if err := m.initScheduler(); err != nil {
		return err
	}
	return nil
}

// Start .
func (m *manager) Start() error {
	m.start()
	select {
	case <-m.done:
		return nil
	}
}

func (m *manager) Close() error {
	for i := 0; i < len(m.schedulers); i++ {
		if m.schedulers[i].State == scheduler.RUNNING {
			m.schedulers[i].Close()
		}
	}
	close(m.done)
	return nil
}

// watch for org or cluster change
func (m *manager) sync() {
	timer := time.NewTicker(m.Cfg.AccountReload)
	for {
		select {
		case <-m.done:
			timer.Stop()
			return
		case <-timer.C:
		}
		g.Log.Infof("start reload account...")
		meta, err := api.ListOrgInfos()
		if err != nil {
			g.Log.Errorf("list org cluster error: %s", err)
			continue
		}
		same := reflect.DeepEqual(meta, m.metaOrgClusters)
		if !same {
			m.Close()
			if err := m.setWriter(); err != nil {
				g.Log.Errorf("set writer failed. err: %s", err)
				continue
			}
			if err := m.initScheduler(); err != nil {
				g.Log.Errorf("init failed, err %s", err)
				continue
			}
			m.start()
		}
	}
}

func (m *manager) retryFailedScheduler() {
	timer := time.NewTicker(g.Cfg.AccountReload)
	for {
		select {
		case <-m.done:
			timer.Stop()
			return
		case <-timer.C:
		}
		g.Log.Info("check & retry failure scheduler...")
		// retry
		allSuccess := true
		for i := 0; i < len(m.schedulers); i++ {
			if m.schedulers[i].State != scheduler.RUNNING {
				allSuccess = false
				if err := m.schedulers[i].Retry(); err == nil {
					go func() {
						m.schedulers[i].Start()
					}()
					go func() {
						m.schedulers[i].Sync(m.schedulerChangedSig)
					}()
				} else {
					g.Log.Errorf("retry scheduler=%s failed. err=%s", m.schedulers[i], err)
				}
			}
		}
		if allSuccess {
			timer.Stop()
			return
		}
	}
}

func (m *manager) setWriter() error {
	kaf := m.ctx.Service("kafka-producer").(kafka.Interface)
	w, err := kaf.NewProducer(&m.Cfg.Output)
	if err != nil {
		return errors.Errorf("fail to create kafka producer: %s", err)
	}
	m.writer = w
	return nil
}

func (m *manager) start() {
	for _, s := range m.schedulers {
		if s.State == scheduler.RUNNING {
			go func(ss *scheduler.Scheduler) { ss.Start() }(s)
			go func(ss *scheduler.Scheduler) { ss.Sync(m.schedulerChangedSig) }(s)
		}
	}
	go m.sync()
	go m.monitor()
	go m.retryFailedScheduler()
}

// watch for ith scheduler meta info change
func (m *manager) monitor() {
	for {
		select {
		case <-m.done:
			return
		case idx := <-m.schedulerChangedSig:
			g.Log.Infof("%dth scheduler <%s> MetaProjects info changed, start to recreate", idx, m.schedulers[idx])
			olds := m.schedulers[idx]
			if olds == nil {
				break
			}
			olds.Close()

			item := m.metaOrgClusters[idx]
			news, err := scheduler.New(item, m.Cfg, m.writer)
			m.schedulers[idx] = news
			if err != nil {
				g.Log.Errorf("create scheduler with <%s, %s>", item.OrgId, item.OrgName)
				break
			}
			go func() { m.schedulers[idx].Start() }()
			go func() { m.schedulers[idx].Sync(m.schedulerChangedSig) }()
			g.Log.Infof("%dth scheduler <%s> MetaProjects info changed, recreate complete", idx, m.schedulers[idx])
		}
	}
}

func (m *manager) initScheduler() error {
	m.done = make(chan struct{})
	m.schedulerChangedSig = make(chan int)

	meta, err := api.ListOrgInfos()
	if err != nil {
		g.Log.Infof("can not get org info: %s. will try again with %s interval", err, g.Cfg.AccountReload)
	}
	m.metaOrgClusters = meta

	schedulers := make([]*scheduler.Scheduler, len(m.metaOrgClusters))
	for i := 0; i < len(m.metaOrgClusters); i++ {
		item := m.metaOrgClusters[i]
		s, err := scheduler.New(item, m.Cfg, m.writer)
		if err != nil {
			g.Log.Errorf("create scheduler with <%s, %s> failed. err: %s", item.OrgId, item.OrgName, err)
		} else {
			g.Log.Infof("create scheduler with <%s, %s> successfully", item.OrgId, item.OrgName)
		}
		schedulers[i] = s
	}
	m.schedulers = schedulers
	return nil
}

func init() {
	servicehub.RegisterProvider("cloudcat", &define{})
}
