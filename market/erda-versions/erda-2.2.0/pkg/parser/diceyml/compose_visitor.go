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

type ComposeVisitor struct {
	DefaultVisitor
	envObj *Object
	env    string
}

func NewComposeVisitor(envObj *Object, env string) DiceYmlVisitor {
	return &ComposeVisitor{envObj: envObj, env: env}
}

func (o *ComposeVisitor) VisitEnvObjects(v DiceYmlVisitor, obj *EnvObjects) {

	if o.envObj == nil {
		return
	}

	if (*obj) == nil {
		obj_ := EnvObjects(map[string]*EnvObject{})
		*obj = obj_
	}

	if e, ok := (*obj)[o.env]; !ok || e == nil {
		(*obj)[o.env] = new(EnvObject)
	}

	overrideIfNotZero(o.envObj.Envs, &(*obj)[o.env].Envs)
	overrideIfNotZero(o.envObj.Services, &(*obj)[o.env].Services)
	overrideIfNotZero(o.envObj.AddOns, &(*obj)[o.env].AddOns)

}

func Compose(obj, envObj *Object, env string) {
	visitor := NewComposeVisitor(envObj, env)

	obj.Accept(visitor)

}
