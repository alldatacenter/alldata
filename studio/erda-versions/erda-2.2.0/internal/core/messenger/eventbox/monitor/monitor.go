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

package monitor

import (
	"context"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/core/messenger/eventbox/constant"
	"github.com/erda-project/erda/pkg/jsonstore"
	_ "github.com/erda-project/erda/pkg/monitor"
	"github.com/erda-project/erda/pkg/persist_stat"
	"github.com/erda-project/erda/pkg/persist_stat/backend"
)

var (
	std, _ = New()
)

//go:generate stringer -type=InfoType
type InfoType int

const (
	EtcdInput InfoType = iota
	EtcdInputDrop
	HTTPInput
	DINGDINGOutput
	DINGDINGWorkNoticeOutput
	MYSQLOutput
	HTTPOutput
	LastType
)

func infoTypeList() []InfoType {
	return []InfoType{
		EtcdInput,
		EtcdInputDrop,
		HTTPInput,
		DINGDINGOutput,
		DINGDINGWorkNoticeOutput,
		MYSQLOutput,
		HTTPOutput,
	}
}

type MonitorInfo struct {
	Tp        InfoType
	Detail    string
	timestamp time.Time
}

type Monitor struct {
	pstat persist_stat.PersistStoreStat

	js jsonstore.JsonStore
}

func New() (*Monitor, error) {
	js, err := jsonstore.New()
	if err != nil {
		return nil, err
	}
	pstat := backend.NewEtcd(js, "eventbox")
	pstat.SetPolicy(persist_stat.Policy{AccumTp: persist_stat.SUM, Interval: 60, PreserveDays: 1})
	m := &Monitor{js: js, pstat: pstat}

	go func() {
		for {
			m.StatMessageRemain()
			time.Sleep(5 * time.Minute)
		}
	}()
	return m, nil
}

func (m *Monitor) Notify(info MonitorInfo) {
	m.pstat.Emit(info.Tp.String(), 1)
}

func (m *Monitor) StatMessageRemain() {
	// 5s 内2次查看message数量都>100则告警
	f := func() ([]string, error) {
		ks, err := m.js.ListKeys(context.Background(), constant.MessageDir)
		if err != nil {
			return nil, err
		}
		return ks, nil
	}
	ks1, err := f()
	if err != nil {
		logrus.Errorf("[alert] eventbox monitor listkeys fail: %v", err)
		return
	}
	time.Sleep(5 * time.Second)
	ks2, err := f()
	if err != nil {
		logrus.Errorf("[alert] eventbox monitor listkeys fail: %v", err)
		return
	}
	if len(ks1) > 100 && len(ks2) > 100 {
		logrus.Errorf("[alert] eventbox remain %d messages (etcd: /eventbox/messages/)", len(ks2))
	}
	return
}

func Notify(info MonitorInfo) {
	std.Notify(info)
}
