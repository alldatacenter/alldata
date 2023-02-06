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

package scheduler

import (
	"encoding/json"
	"fmt"
	"math/rand"
	"reflect"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/services/cms"
	"github.com/sirupsen/logrus"

	writer "github.com/erda-project/erda-infra/pkg/parallel-writer"
	"github.com/erda-project/erda-infra/providers/kafka"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/cloud/aliyun/metrics/cloudcat/api"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/cloud/aliyun/metrics/cloudcat/globals"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/cloud/aliyun/metrics/cloudcat/grabber"
)

var topic = "spot-telemetry"

const (
	PENDING = iota
	RUNNING
)

func init() {
	rand.Seed(time.Now().UnixNano())
}

type Scheduler struct {
	metaProjects      []cms.Resource
	grabbers          []*grabber.Grabber
	pipe              chan []*api.Metric
	done              chan struct{}
	writer            writer.Writer
	cfg               *globals.Config
	grabberChangedSig chan int
	// orgId, clusterName, orgName string
	info  api.OrgInfo
	index int // the index of manager.schedulers
	State int
}

func (s *Scheduler) String() string {
	return fmt.Sprintf("scheduler <%s, %s>", s.info.OrgName, s.info.OrgId)
}

func (s *Scheduler) initAliyunVendor() (vendor api.CloudVendor, err error) {
	defaultcfg := api.AliyunConfig{
		Timeout:          s.cfg.ReqLimitTimeout,
		MaxQPS:           s.cfg.MaxQPS,
		ReqLimit:         s.cfg.ReqLimit,
		ReqLimitDuration: s.cfg.ReqLimitDuration,
	}
	vendor, err = api.NewAliyunVendor(s.info.AccessKey, s.info.AccessSecret, defaultcfg)
	if err != nil {
		return nil, err
	}
	api.RegisterVendor(s.info.OrgId, vendor)
	return
}

//
func New(info api.OrgInfo, cfg *globals.Config, w writer.Writer) (sc *Scheduler, err error) {
	sc = &Scheduler{
		pipe:              make(chan []*api.Metric),
		cfg:               cfg,
		done:              make(chan struct{}),
		writer:            w,
		grabberChangedSig: make(chan int),
		info:              info,
		State:             PENDING,
	}
	if _, err := sc.initAliyunVendor(); err != nil {
		return sc, fmt.Errorf("create aliyun vendor failed. err=%s", err)
	}

	meta, err := api.ListProjectMeta(info.OrgId, cfg.Products)
	if err != nil {
		return sc, err
	}
	sc.metaProjects = meta

	gs, err := sc.createGrabbers(meta)
	if err != nil {
		return sc, err
	}
	sc.grabbers = gs
	for i := 0; i < len(gs); i++ {
		gs[i].Subscribe(sc.pipe)
	}
	sc.State = RUNNING
	return
}

func (s *Scheduler) Retry() error {
	if _, err := s.initAliyunVendor(); err != nil {
		return fmt.Errorf("create aliyun vendor failed. err=%s", err)
	}
	meta, err := api.ListProjectMeta(s.info.OrgId, s.cfg.Products)
	if err != nil {
		return err
	}
	s.metaProjects = meta

	gs, err := s.createGrabbers(meta)
	if err != nil {
		return err
	}
	s.grabbers = gs
	for i := 0; i < len(gs); i++ {
		gs[i].Subscribe(s.pipe)
	}
	s.State = RUNNING
	return nil
}

// Sync metaProjects
func (s *Scheduler) Sync(notify chan int) {
	// 1d
	timer := time.NewTicker(s.cfg.ProductListReload)
	timer.Stop()
	for {
		select {
		case <-s.done:
			timer.Stop()
			return
		case <-timer.C:
		}
		same, err := s.loadMetaProjects()
		if !same {
			notify <- s.index // send index to Scheduler
		}
		if err != nil {
			logrus.WithField("orgId", s.info.OrgId).Errorf("loadMetaProjects error: %+v", err)
		}
	}
}

func (s *Scheduler) Start() {
	go func() {
		if len(s.grabbers) == 0 {
			return
		}

		interval := time.Duration(s.cfg.GatherWindow.Seconds()/float64(len(s.grabbers))) * time.Second
		for i := 0; i < len(s.grabbers); i++ {
			if s.grabbers[i] != nil {
				go func(idx int) {
					s.grabbers[idx].Gather()
				}(i)
				go func(idx int) {
					s.grabbers[idx].Sync(s.grabberChangedSig)
				}(i)
				time.Sleep(interval)
			}

		}
	}()

	go func() { s.monitor() }()

	// consume & output
	for {
		select {
		case <-s.done:
			return
		case batch := <-s.pipe:
			if batch == nil {
				logrus.Infof("nil metric received")
				continue
			}
			// logrus.Infof("%d telemetry received", len(batch))

			// todo need processors
			for i := 0; i < len(batch); i++ {
				batch[i].Tags["org_id"] = s.info.OrgId
				batch[i].Tags["org_name"] = s.info.OrgName
				batch[i].Tags["_meta"] = "true"
				batch[i].Tags["_metric_scope"] = "org"
				batch[i].Tags["_metric_scope_id"] = s.info.OrgName
			}
			// todo as a pool
			go func() {
				for i := 0; i < len(batch); i++ {
					if err := s.send(batch[i]); err != nil {
						logrus.WithField("error", err).Error("send to kafka failed")
					}
				}
			}()
		}
	}
}

func (s *Scheduler) monitor() {
	for {
		select {
		case idx := <-s.grabberChangedSig:
			logrus.Infof("%dth grabber <%s> metaMetrics info changed, start to recreate", idx, s.grabbers[idx])
			oldg := s.grabbers[idx]
			oldg.Close()

			meta := s.metaProjects[idx]
			// recreate
			newg, err := grabber.New(meta.Namespace, s.cfg.GatherWindow, s.info.OrgId, idx)
			s.grabbers[idx] = newg
			if err != nil {
				logrus.WithField("error", err).Errorf("create grabber failed")
				break
			}
			s.grabbers[idx].Subscribe(s.pipe)
			go func() { s.grabbers[idx].Gather() }()
			go func() { s.grabbers[idx].Sync(s.grabberChangedSig) }()
			logrus.Infof("%dth grabber <%s> metaMetrics info changed, recreate complete", idx, s.grabbers[idx])
		case <-s.done:
			return
		}
	}
}

func (s *Scheduler) send(metric *api.Metric) error {
	data, err := json.Marshal(metric)
	if err != nil {
		return err
	}
	// fmt.Printf("======single metric======, topic: %s, data: %s\n", topic, string(data))
	return s.writer.Write(&kafka.Message{
		Topic: &topic,
		Data:  data,
	})
}

func (s *Scheduler) Close() error {
	for i := 0; i < len(s.grabbers); i++ {
		if s.grabbers[i] != nil {
			s.grabbers[i].Close()
		}
	}
	close(s.done)
	return nil
}

func (s *Scheduler) createGrabbers(meta []cms.Resource) (gs []*grabber.Grabber, err error) {
	gs = make([]*grabber.Grabber, len(meta))
	for i := 0; i < len(gs); i++ {
		// 官方文档建议，采集窗口为5-10min。
		g, err := grabber.New(meta[i].Namespace, s.cfg.GatherWindow, s.info.OrgId, i)
		gs[i] = g
		if err == api.ErrEmptyResults {
			logrus.Infof("grabber %+v with empty metaMetrics, ignore", meta[i].Namespace)
			continue
		}
		if err != nil {
			return nil, err
		}
	}
	return
}

func (s *Scheduler) loadMetaProjects() (same bool, err error) {
	meta, err := api.ListProjectMeta(s.info.OrgId, s.cfg.Products)
	if err != nil {
		return false, err
	}
	same = reflect.DeepEqual(meta, s.metaProjects)
	s.metaProjects = meta
	return
}
