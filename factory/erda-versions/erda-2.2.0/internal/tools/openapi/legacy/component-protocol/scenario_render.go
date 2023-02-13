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

package component_protocol

import (
	"context"
	"encoding/json"
	"fmt"
	"reflect"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
)

type CompRenderSpec struct {
	// 具体的场景名
	Scenario string `json:"scenario"`
	// 具体的组件名
	CompName string `json:"name"`
	// 组件（包含渲染函数）创建函数
	RenderC RenderCreator
}

type RenderCreator func() CompRender

type CompRender interface {
	Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error
}

// componentName: componentRender
type ScenarioRender map[string]*CompRenderSpec

// scenario: componentName: componentRender
var ScenarioRenders = make(map[string]*ScenarioRender)

func Register(r *CompRenderSpec) error {
	if r == nil {
		err := fmt.Errorf("invalid register request")
		return err
	}
	if r.Scenario == "" || r.CompName == "" {
		err := fmt.Errorf("invalid register request, empty scenario [%s] or compName [%s]", r.Scenario, r.CompName)
		return err
	}

	logrus.Infof("start to register scenario:%s", r.Scenario)
	// if scenario key not exit, crate it
	if _, ok := ScenarioRenders[r.Scenario]; !ok {
		s := make(ScenarioRender)
		ScenarioRenders[r.Scenario] = &s
	}
	// if compName key not exist, create it and the CompRenderSpec
	s := ScenarioRenders[r.Scenario]
	if _, ok := (*s)[r.CompName]; !ok {
		(*s)[r.CompName] = r
	} else {
		err := fmt.Errorf("register render failed, component [%s] already exist", r.CompName)
		return err
	}
	logrus.Infof("register render success, render:%+v", *r)
	return nil
}

func GetScenarioRenders(scenario string) (*ScenarioRender, error) {
	var r *ScenarioRender
	r, ok := ScenarioRenders[scenario]
	if !ok {
		err := fmt.Errorf("scenario not exist, scenario: %s", scenario)
		return r, err
	}
	if r == nil {
		err := fmt.Errorf("empty scenario [%s]", scenario)
		return nil, err
	}
	return r, nil
}

type emptyComp struct{}

func (ca *emptyComp) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	return nil
}

func GetCompRender(r *ScenarioRender, comp, typ string) (*CompRenderSpec, error) {
	if r == nil || comp == "" {
		err := fmt.Errorf("empty scenario render or compnent name")
		return nil, err
	}
	var c *CompRenderSpec
	if _, ok := (*r)[comp]; !ok {
		switch typ {
		case "Container", "LRContainer", "RowContainer", "SplitPage", "Popover", "Title", "Drawer":
			return &CompRenderSpec{RenderC: func() CompRender { return &emptyComp{} }}, nil
		}
		err := fmt.Errorf("get component failed, compnent:%s", comp)
		return nil, err
	}
	c = (*r)[comp]
	if c == nil {
		err := fmt.Errorf("empty component render, compnent:%s", comp)
		return nil, err
	}
	return c, nil
}

func GetScenarioKey(req apistructs.ComponentProtocolScenario) (string, error) {
	if req.ScenarioType == "" && req.ScenarioKey == "" {
		err := fmt.Errorf("empty scenario key")
		return "", err
	}

	if req.ScenarioType != "" {
		return req.ScenarioType, nil
	}

	return req.ScenarioKey, nil
}

// 前端触发的事件转换，如果是组件自身的事件，则透传；
// 否则, (1) 组件名为空，界面刷新：InitializeOperation
// 		(2) 通过协议定义的Rending触发的事件：RenderingOperation
func EventConvert(receiver string, event apistructs.ComponentEvent) apistructs.ComponentEvent {
	if receiver == event.Component {
		return event
	} else if event.Component != "" {
		return apistructs.ComponentEvent{Operation: apistructs.RenderingOperation}
	} else {
		return apistructs.ComponentEvent{Operation: apistructs.InitializeOperation}
	}
}

func checkDebugOptions(debugOptions *apistructs.ComponentProtocolDebugOptions) error {
	if debugOptions == nil {
		return nil
	}
	if debugOptions.ComponentKey == "" {
		return fmt.Errorf("debug missing component key")
	}
	return nil
}

func polishComponentRendering(debugOptions *apistructs.ComponentProtocolDebugOptions, compRendering []apistructs.RendingItem) []apistructs.RendingItem {
	if debugOptions == nil || debugOptions.ComponentKey == "" {
		return compRendering
	}
	var result []apistructs.RendingItem
	for _, item := range compRendering {
		if item.Name == debugOptions.ComponentKey {
			result = append(result, item)
			break
		}
	}
	return result
}

func RunScenarioRender(ctx context.Context, req *apistructs.ComponentProtocolRequest) error {
	// check debug options
	if err := checkDebugOptions(req.DebugOptions); err != nil {
		logrus.Errorf("failed to check debug options, request: %+v, err: %v", req, err)
		return err
	}

	var isDefaultProtocol bool
	sk, err := GetScenarioKey(req.Scenario)
	if err != nil {
		logrus.Errorf("get scenario key failed, request:%+v, err:%v", req.Scenario, err)
		return err
	}

	if req.Protocol == nil || req.Event.Component == "" {
		isDefaultProtocol = true
		p, err := LoadDefaultProtocol(ctx, sk)
		if err != nil {
			logrus.Errorf("load default protocol failed, scenario:%+v, err:%v", req.Scenario, err)
			return err
		}
		tmp := new(apistructs.ComponentProtocol)
		err = deepCopy(p, tmp)
		if err != nil {
			logrus.Errorf("deep copy failed, err:%v", err)
			return err

		}
		req.Protocol = tmp
	}

	sr, err := GetScenarioRenders(sk)
	if err != nil {
		logrus.Errorf("get scenario render failed, err:%v", err)
		return err
	}

	var compRending []apistructs.RendingItem
	if isDefaultProtocol {
		// 如果是加载默认协议，则渲染所有组件，不存在组件及状态依赖
		crs, ok := req.Protocol.Rendering[DefaultRenderingKey]
		if !ok {
			// 如果不存在默认DefaultRenderingKey，则随机从map中获取各组件key，渲染所有组件，无state绑定
			for k := range *sr {
				if _, ok := req.Protocol.Components[k]; !ok {
					continue
				}
				ri := apistructs.RendingItem{
					Name: k,
				}
				compRending = append(compRending, ri)
			}
		} else {
			// 如果存在默认DefaultRenderingKey，则获取默认定义的Rending
			compRending = append(compRending, crs...)
		}

	} else {
		// 如果是前端触发一个组件操作，则先渲染该组件;
		// 再根据定义的渲染顺序，依次完成其他组件的state注入和渲染;
		compName := req.Event.Component
		compRending = append(compRending, apistructs.RendingItem{Name: compName})

		crs, ok := req.Protocol.Rendering[compName]
		if !ok {
			logrus.Infof("empty protocol rending for component:%s", compName)
		} else {
			compRending = append(compRending, crs...)
		}
	}
	compRending = polishComponentRendering(req.DebugOptions, compRending)

	if req.Protocol.GlobalState == nil {
		gs := make(apistructs.GlobalStateData)
		req.Protocol.GlobalState = &gs
	}

	// clean pre render error
	SetGlobalStateKV(req.Protocol, GlobalInnerKeyError.String(), "")

	PolishProtocol(req.Protocol)

	for _, v := range compRending {
		// 组件状态渲染
		err := ProtoCompStateRending(ctx, req.Protocol, v)
		if err != nil {
			logrus.Errorf("protocol component state rending failed, request:%+v, err:%v", v, err)
			return err
		}
		// 获取协议中相关组件
		c, err := GetProtoComp(req.Protocol, v.Name)
		if err != nil {
			logrus.Errorf("get component from protocol failed, scenario:%s, component:%s", sk, req.Event.Component)
			return nil
		}
		// 获取组件渲染函数
		cr, err := GetCompRender(sr, v.Name, c.Type)
		if err != nil {
			logrus.Errorf("get component render failed, scenario:%s, component:%s", sk, req.Event.Component)
			return err
		}
		// 生成组件对应事件，如果不是组件自身事件则为默认事件
		event := EventConvert(v.Name, req.Event)
		// 运行组件渲染函数
		start := time.Now() // 获取当前时间
		err = wrapCompRender(cr.RenderC(), req.Protocol.Version).Render(ctx, c, req.Scenario, event, req.Protocol.GlobalState)
		if err != nil {
			logrus.Errorf("render component failed,err: %s, scenario:%+v, component:%s", err.Error(), req.Scenario, cr.CompName)
			return err
		}
		elapsed := time.Since(start)
		fmt.Println(req.Scenario, cr.CompName, " 执行完成耗时：", elapsed)
	}
	return nil
}

func wrapCompRender(cr CompRender, ver string) CompRender {
	// 没版本的原始代码
	if ver == "" {
		return cr
	}
	return &compRenderWrapper{cr: cr}
}

type compRenderWrapper struct {
	cr CompRender
}

func (w *compRenderWrapper) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) (err error) {
	if err = unmarshal(&w.cr, c); err != nil {
		return
	}
	defer func() {
		if err != nil {
			// not marshal invoke fail
			return
		}
		err = marshal(&w.cr, c)
	}()
	err = w.cr.Render(ctx, c, scenario, event, gs)
	return
}

func unmarshal(cr *CompRender, c *apistructs.Component) error {
	v, err := json.Marshal(c)
	if err != nil {
		return err
	}
	return json.Unmarshal(v, cr)
}

func marshal(cr *CompRender, c *apistructs.Component) error {
	var tmp apistructs.Component
	v, err := json.Marshal(cr)
	if err != nil {
		return err
	}
	err = json.Unmarshal(v, &tmp)
	if err != nil {
		return err
	}
	tr := reflect.TypeOf(*cr).Elem()
	fields := getAllFields(c)
	for _, fieldName := range fields {
		if f, ok := tr.FieldByName(fieldName); ok {
			if tag := f.Tag.Get("json"); tag == "-" {
				continue
			}
			switch fieldName {
			case "Version":
				c.Version = tmp.Version
			case "Type":
				c.Type = tmp.Type
			case "Name":
				c.Name = tmp.Name
			case "Props":
				c.Props = tmp.Props
			case "Data":
				c.Data = tmp.Data
			case "State":
				c.State = tmp.State
			case "Operations":
				c.Operations = tmp.Operations
			}
		}
	}
	return nil
}

func getAllFields(o interface{}) (f []string) {
	t := reflect.TypeOf(o)
	if t.Kind() == reflect.Ptr {
		t = t.Elem()
	}
	for i := 0; i < t.NumField(); i++ {
		f = append(f, t.Field(i).Name)
	}
	return
}
