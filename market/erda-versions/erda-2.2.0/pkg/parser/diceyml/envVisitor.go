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

// 原本的parser会把 global env 塞到各个 service 的 env 去
package diceyml

type EnvVisitor struct {
	DefaultVisitor
	globalEnv map[string]string
}

func NewEnvVisitor() DiceYmlVisitor {
	return &EnvVisitor{}
}

func (o *EnvVisitor) VisitObject(v DiceYmlVisitor, obj *Object) {
	override(obj.Envs, &o.globalEnv)
	obj.Services.Accept(v)
}

func (o *EnvVisitor) VisitService(v DiceYmlVisitor, obj *Service) {
	if obj.Envs == nil {
		obj.Envs = map[string]string{}
	}
	for k, v := range o.globalEnv {
		if _, ok := obj.Envs[k]; !ok {

			obj.Envs[k] = v
		}
	}
}

func ExpandGlobalEnv(obj *Object) {
	visitor := NewEnvVisitor()
	obj.Accept(visitor)
}
