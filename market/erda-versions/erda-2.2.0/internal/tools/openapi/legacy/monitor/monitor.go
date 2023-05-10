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

// Package monitor collect and export openapi metrics
package monitor

import (
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/persist_stat"
	"github.com/erda-project/erda/pkg/persist_stat/backend"
)

var (
	std, _ = New()
)

// InfoType metric 分类
//go:generate stringer -type=InfoType
type InfoType int

const (
	// AuthFail auth 失败
	AuthFail InfoType = iota
	// AuthSucc auth 成功
	AuthSucc
	// APIInvokeCount api 调用次数
	APIInvokeCount
	// APIInvokeDuration api 调用时长
	APIInvokeDuration
	// API50xCount api 5xx 次数
	API50xCount
	// API40xCount api 4xx 次数
	API40xCount
	// LastType InfoType 个数
	LastType
)

// Info monitor 使用处需要提供的信息
type Info struct {
	Tp     InfoType
	Detail string
	Value  int64
}

// Monitor monitor struct
type Monitor struct {
	logger   *logrus.Logger
	pstatSum persist_stat.PersistStoreStat
	pstatAvg persist_stat.PersistStoreStat
}

// New 创建 Monitor
func New() (*Monitor, error) {
	js, err := jsonstore.New()
	if err != nil {
		return nil, err
	}
	pstatSum := backend.NewEtcd(js, "openapi")
	if err := pstatSum.SetPolicy(persist_stat.Policy{AccumTp: persist_stat.SUM, Interval: 60, PreserveDays: 1}); err != nil {
		return nil, err
	}
	pstatAvg := backend.NewEtcd(js, "openapi-avg")
	if err := pstatAvg.SetPolicy(persist_stat.Policy{AccumTp: persist_stat.AVG, Interval: 60, PreserveDays: 1}); err != nil {
		return nil, err
	}

	m := &Monitor{logger: logrus.New(), pstatSum: pstatSum, pstatAvg: pstatAvg}
	return m, nil
}

// Notify monitor 使用处调用这个接口
func (m *Monitor) Notify(info Info) {
	tag := info.Tp.String()
	if info.Detail != "" {
		tag = strings.Join([]string{tag, info.Detail}, "#")
	}
	v := int64(1)
	if info.Value != int64(0) {
		v = info.Value
	}
	if info.Tp == APIInvokeDuration {
		if err := m.pstatAvg.Emit(tag, v); err != nil {
			logrus.Errorf("pstatAvg emit: %v", err)
		}
	} else {
		if err := m.pstatSum.Emit(tag, v); err != nil {
			logrus.Errorf("pstatSum emit: %v", err)
		}
	}
}

// Notify 使用默认 Monitor 调用 Notify
func Notify(info Info) {
	std.Notify(info)
}
