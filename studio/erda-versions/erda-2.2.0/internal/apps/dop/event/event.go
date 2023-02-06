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

// Package event 事件信息
package event

import (
	"fmt"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/discover"
)

// Event 事件结构体
type Event struct {
	EventData
	bdl *bundle.Bundle
	// 事件集合，key为事件名称
	Events map[string]*apistructs.GittarRegisterHookRequest
}

// EventData 注册事件的数据结构
type EventData struct {
	// 事件名称
	Name string
	// 回调的路径
	Path string
	// 是否push触发，默认为true
	IsPush bool
}

// Option Event 类型定义
type Option func(*Event)

// New Event
func New(options ...Option) *Event {
	r := &Event{
		Events: make(map[string]*apistructs.GittarRegisterHookRequest),
	}

	for _, op := range options {
		op(r)
	}
	return r
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(f *Event) {
		f.bdl = bdl
	}
}

// Register 注册 gittar 事件
func (e *Event) Register(ed *EventData) error {
	if e.Events == nil {
		e.Events = make(map[string]*apistructs.GittarRegisterHookRequest)
	}

	if ed.Name == "" {
		return errors.New("need event name")
	}

	e.Events[ed.Name] = &apistructs.GittarRegisterHookRequest{
		Name:       ed.Name,
		URL:        fmt.Sprint("http://", discover.DOP(), ed.Path),
		PushEvents: ed.IsPush,
	}

	err := e.bdl.RegisterGittarHook(*e.Events[ed.Name])
	return err
}
