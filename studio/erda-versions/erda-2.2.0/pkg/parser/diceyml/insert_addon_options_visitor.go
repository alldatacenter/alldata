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

package diceyml

import (
	"strings"
)

type InsertAddonOptionsVisitor struct {
	DefaultVisitor
	env           EnvType
	addonPlan     string
	options       map[string]string
	fromEnvObject bool
}

func NewInsertAddonOptionsVisitor(env EnvType, addonPlan string, options map[string]string) DiceYmlVisitor {
	return &InsertAddonOptionsVisitor{addonPlan: addonPlan, options: options, env: env}
}

func (o *InsertAddonOptionsVisitor) VisitAddOns(v DiceYmlVisitor, obj *AddOns) {
	if o.env != BaseEnv && !o.fromEnvObject {
		return
	}
	for _, v_ := range *obj {
		v_.Accept(v)
	}
}

func (o *InsertAddonOptionsVisitor) VisitAddOn(v DiceYmlVisitor, obj *AddOn) {
	splitted := strings.Split(obj.Plan, ":")

	if len(splitted) < 1 {
		return
	}

	if strings.TrimSpace(splitted[0]) == o.addonPlan {
		for k, v := range o.options {
			obj.Options[k] = v
		}
	}
}

func (o *InsertAddonOptionsVisitor) VisitEnvObjects(v DiceYmlVisitor, obj *EnvObjects) {
	if o.env == BaseEnv {
		return
	}
	for name, v_ := range *obj {
		if name == o.env.String() {
			v_.Accept(v)
		}
	}
}

func (o *InsertAddonOptionsVisitor) VisitEnvObject(v DiceYmlVisitor, obj *EnvObject) {
	o.fromEnvObject = true
	obj.AddOns.Accept(v)
	o.fromEnvObject = false
}

func InsertAddonOptions(obj *Object, env EnvType, addonPlan string, options map[string]string) {
	visitor := NewInsertAddonOptionsVisitor(env, addonPlan, options)
	obj.Accept(visitor)
}
