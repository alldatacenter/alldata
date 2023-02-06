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
	"fmt"
	"io/ioutil"
	"regexp"
	"strings"

	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/strutil"
)

type GlobalInnerKey string

func (s GlobalInnerKey) String() string {
	return string(s)
}

func (s GlobalInnerKey) Normal() string {
	m := strings.TrimPrefix(s.String(), "_")
	n := strings.TrimPrefix(m, "_")
	return n
}

const (
	GlobalInnerKeyCtxBundle GlobalInnerKey = "_ctxBundle_"
	GlobalInnerKeyUserIDs   GlobalInnerKey = "_userIDs_"
	GlobalInnerKeyError     GlobalInnerKey = "_error_"
	// userID & orgID
	GlobalInnerKeyIdentity GlobalInnerKey = "_identity_"

	// Default Rendering Key
	DefaultRenderingKey = "__DefaultRendering__"

	// Rendering 从 InParams 绑定
	InParamsStateBindingKey = "__InParams__"
)

type ContextBundle struct {
	Bdl         *bundle.Bundle
	I18nPrinter *message.Printer
	Identity    apistructs.Identity
	InParams    map[string]interface{}
	// add language name for change language
	Locale string
}

// scenario name: scenario default protocol
var DefaultProtocols = make(map[string]apistructs.ComponentProtocol)
var DefaultProtocolsRaw = make(map[string]string)

// default path: libs/erda-configs/permission
func InitDefaultCompProtocols(path string) {
	var err error
	defer func() {
		if err != nil {
			logrus.Errorf("load default component protocol failed, err:%v", err)
			panic(err)
		}
	}()
	rd, err := ioutil.ReadDir(path)
	if err != nil {
		return
	}
	for _, fi := range rd {
		if fi.IsDir() {
			fullDir := path + "/" + fi.Name()
			InitDefaultCompProtocols(fullDir)
		} else {
			fullName := path + "/" + fi.Name()
			yamlFile, er := ioutil.ReadFile(fullName)
			if er != nil {
				err = er
				return
			}
			var p apistructs.ComponentProtocol
			if er := yaml.Unmarshal(yamlFile, &p); er != nil {
				err = er
				return
			}
			DefaultProtocols[p.Scenario] = p
		}
	}
}

var CpPlaceHolderRe = regexp.MustCompile(`\${{[ ]{1}([^{}\s]+)[ ]{1}}}`)

const I18n = "i18n"

func LoadDefaultProtocol(ctx context.Context, scenario string) (apistructs.ComponentProtocol, error) {
	rawYamlStr, ok := DefaultProtocolsRaw[scenario]
	if !ok {
		p, ok := DefaultProtocols[scenario]
		if !ok {
			err := fmt.Errorf("default protocol not exist, scenario:%s", scenario)
			return apistructs.ComponentProtocol{}, err
		}
		return p, nil
	}
	bdl := ctx.Value(GlobalInnerKeyCtxBundle.String()).(ContextBundle)
	locale := bdl.Bdl.GetLocale(bdl.Locale)
	replaced := strutil.ReplaceAllStringSubmatchFunc(CpPlaceHolderRe, rawYamlStr, func(v []string) string {
		if len(v) == 2 && strings.HasPrefix(v[1], I18n+".") {
			key := strings.TrimPrefix(v[1], I18n+".")
			if len(key) > 0 {
				return locale.Get(key)
			}
		}
		return v[0]
	})
	var p apistructs.ComponentProtocol
	if err := yaml.Unmarshal([]byte(replaced), &p); err != nil {
		return apistructs.ComponentProtocol{}, fmt.Errorf("failed to parse protocol yaml i18n, err: %v", err)
	}
	return p, nil
}

func deepCopy(src, dst interface{}) error {
	r, err := yaml.Marshal(src)
	if err != nil {
		return fmt.Errorf("marshal failed")
	}
	if err := yaml.Unmarshal(r, dst); err != nil {
		return fmt.Errorf("unmarshal failed")
	}
	return nil
}

func GetProtoComp(p *apistructs.ComponentProtocol, compName string) (c *apistructs.Component, err error) {
	if p.Components == nil {
		err = fmt.Errorf("empty protocol components")
		return
	}

	c, ok := p.Components[compName]
	if !ok {
		err = fmt.Errorf("empty component [%s] in protocol", compName)
		return
	}
	return
}

func GetCompStateKV(c *apistructs.Component, sk string) (interface{}, error) {
	if c == nil {
		err := fmt.Errorf("empty component")
		return nil, err
	}
	if _, ok := c.State[sk]; !ok {
		err := fmt.Errorf("state key [%s] not exist in component [%s] state", sk, c.Name)
		return nil, err
	}
	return c.State[sk], nil
}

func SetCompStateValueFromComps(c *apistructs.Component, key string, value interface{}) error {
	if c == nil {
		err := fmt.Errorf("empty component")
		return err
	}
	if key == "" {
		err := fmt.Errorf("empty state key")
		return err
	}
	if v, ok := c.State[key]; ok {
		logrus.Infof("state key already exist in component, component:%s, key:%s, value old:%+v, new:%+v", c.Name, key, v, value)
	}
	if c.State == nil {
		c.State = map[string]interface{}{}
	}
	c.State[key] = value
	return nil
}

func SetCompStateKVFromInParams(c *apistructs.Component, key string, value interface{}) error {
	if c == nil {
		err := fmt.Errorf("empty component")
		return err
	}
	if key == "" {
		err := fmt.Errorf("empty inParams key")
		return err
	}
	if v, ok := c.State[key]; ok {
		logrus.Infof("state key already exist in component, component:%s, key:%s, value old:%+v, new:%+v", c.Name, key, v, value)
	}
	if c.State == nil {
		c.State = map[string]interface{}{}
	}
	c.State[key] = value
	return nil
}

func GetProtoCompStateValue(p *apistructs.ComponentProtocol, compName, sk string) (interface{}, error) {
	c, err := GetProtoComp(p, compName)
	if err != nil {
		return nil, err
	}
	v, err := GetCompStateKV(c, sk)
	if err != nil {
		return nil, err
	}
	return v, nil
}

func GetProtoInParamsValue(inParams map[string]interface{}, key string) interface{} {
	if inParams == nil {
		inParams = make(map[string]interface{})
	}
	return inParams[key]
}

func ParseStateBound(b string) (comp, key string, err error) {
	prefix := "{{"
	suffix := "}}"
	if !strings.HasPrefix(b, prefix) {
		err = fmt.Errorf("state bound not prefix with {{")
		return
	}
	if !strings.HasSuffix(b, "}}") {
		err = fmt.Errorf("state bound not suffix with }}")
		return
	}
	b = strings.TrimPrefix(b, prefix)
	b = strings.TrimPrefix(b, " ")
	b = strings.TrimSuffix(b, suffix)
	b = strings.TrimSuffix(b, " ")
	s := strings.Split(b, ".")
	if len(s) != 2 {
		err = fmt.Errorf("invalide bound expression: %s, with not exactly one '.' ", b)
		return
	}
	comp = s[0]
	key = s[1]
	return
}

func ProtoCompStateRending(ctx context.Context, p *apistructs.ComponentProtocol, r apistructs.RendingItem) error {
	if p == nil {
		err := fmt.Errorf("empty protocol")
		return err
	}
	pc, err := GetProtoComp(p, r.Name)
	if err != nil {
		logrus.Errorf("get protocol component failed, err:%v", err)
		return err
	}
	// inParams
	inParams := ctx.Value(GlobalInnerKeyCtxBundle.String()).(ContextBundle).InParams
	for _, state := range r.State {
		// parse state bound info
		stateFrom, stateFromKey, err := ParseStateBound(state.Value)
		if err != nil {
			logrus.Errorf("parse component state bound failed, component:%s, state bound:%+v", pc.Name, state)
			return err
		}
		var stateFromValue interface{}
		switch stateFrom {
		case InParamsStateBindingKey: // {{ inParams.key }} 表示从 inParams 绑定
			stateFromValue = GetProtoInParamsValue(inParams, stateFromKey)
		default: // 否则，从其他组件 state 绑定
			// get bound key value
			stateFromValue, err = GetProtoCompStateValue(p, stateFrom, stateFromKey)
			if err != nil {
				logrus.Errorf("failed to get component state value, component: %s, key: %s", stateFrom, stateFromKey)
				return err
			}
		}
		// set component state value
		err = SetCompStateValueFromComps(pc, state.Name, stateFromValue)
		if err != nil {
			logrus.Errorf("failed to set component state, component: %s, state key: %s, value: %+v", pc.Name, state.Name, stateFromValue)
			return err
		}
	}
	return nil
}

func SetGlobalStateKV(p *apistructs.ComponentProtocol, key string, value interface{}) {
	if p.GlobalState == nil {
		var gs = make(apistructs.GlobalStateData)
		p.GlobalState = &gs
	}
	s := p.GlobalState
	(*s)[key] = value
}

func GetGlobalStateKV(p *apistructs.ComponentProtocol, key string) interface{} {
	if p.GlobalState == nil {
		return nil
	}
	return (*p.GlobalState)[key]
}

func PolishProtocol(req *apistructs.ComponentProtocol) {
	if req == nil {
		return
	}
	// polish component name
	for name, component := range req.Components {
		component.Name = name
	}
}
