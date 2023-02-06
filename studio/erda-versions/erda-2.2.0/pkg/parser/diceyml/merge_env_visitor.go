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

type MergeEnvVisitor struct {
	DefaultVisitor
	envObj         *EnvObject
	currentService string
	globalEnvs     map[string]string
}

func NewMergeEnvVisitor(envObj *EnvObject) DiceYmlVisitor {
	return &MergeEnvVisitor{envObj: envObj}
}

func (o *MergeEnvVisitor) VisitObject(v DiceYmlVisitor, obj *Object) {
	overrideIfNotZero(o.envObj.Envs, &obj.Envs)
}

func (o *MergeEnvVisitor) VisitServices(v DiceYmlVisitor, obj *Services) {
	services := map[string]struct{}{}
	for name := range *obj {
		services[name] = struct{}{}
	}

	for name := range o.envObj.Services {
		if _, ok := services[name]; !ok {
			(*obj)[name] = new(Service)
		}
	}

	for name, v_ := range *obj {
		o.currentService = name
		services[name] = struct{}{}
		if v_ == nil {
			*v_ = Service{}
		}
		v_.Accept(v)
	}
	o.currentService = ""

}

func (o *MergeEnvVisitor) VisitService(v DiceYmlVisitor, obj *Service) {
	if o.currentService == "" {
		panic("should not be empty")
	}
	if s, ok := o.envObj.Services[o.currentService]; !ok || s == nil {
		return
	}
	overrideIfNotZero(o.envObj.Services[o.currentService].Image, &obj.Image)
	overrideIfNotZero(o.envObj.Services[o.currentService].Cmd, &obj.Cmd)
	overrideIfNotZero(o.envObj.Services[o.currentService].Ports, &obj.Ports)
	overrideIfNotZero(o.envObj.Services[o.currentService].Envs, &obj.Envs)
	overrideIfNotZero(o.envObj.Services[o.currentService].Hosts, &obj.Hosts)
	overrideIfNotZero(o.envObj.Services[o.currentService].Binds, &obj.Binds)
	overrideIfNotZero(o.envObj.Services[o.currentService].Volumes, &obj.Volumes)
	overrideIfNotZero(o.envObj.Services[o.currentService].DependsOn, &obj.DependsOn)
	overrideIfNotZero(o.envObj.Services[o.currentService].Expose, &obj.Expose)
}

func (o *MergeEnvVisitor) VisitResources(v DiceYmlVisitor, obj *Resources) {
	if o.currentService == "" {
		panic("should not be empty")
	}
	if s, ok := o.envObj.Services[o.currentService]; !ok || s == nil {
		return
	}

	overrideIfNotZero(o.envObj.Services[o.currentService].Resources.CPU, &obj.CPU)
	overrideIfNotZero(o.envObj.Services[o.currentService].Resources.Mem, &obj.Mem)
	overrideIfNotZero(o.envObj.Services[o.currentService].Resources.MaxCPU, &obj.MaxCPU)
	overrideIfNotZero(o.envObj.Services[o.currentService].Resources.MaxMem, &obj.MaxMem)
	overrideIfNotZero(o.envObj.Services[o.currentService].Resources.Disk, &obj.Disk)
	overrideIfNotZero(o.envObj.Services[o.currentService].Resources.Network, &obj.Network)
	overrideIfNotZero(o.envObj.Services[o.currentService].Resources.EmptyDirCapacity, &obj.EmptyDirCapacity)
	overrideIfNotZero(o.envObj.Services[o.currentService].Resources.EphemeralStorageCapacity, &obj.EphemeralStorageCapacity)
}

func (o *MergeEnvVisitor) VisitDeployments(v DiceYmlVisitor, obj *Deployments) {
	if o.currentService == "" {
		panic("should not be empty")
	}
	if s, ok := o.envObj.Services[o.currentService]; !ok || s == nil {
		return
	}
	overrideIfNotZero(o.envObj.Services[o.currentService].Deployments.Replicas, &obj.Replicas)
	if !isZero(o.envObj.Services[o.currentService].Deployments.Selectors) {
		obj.Selectors = o.envObj.Services[o.currentService].Deployments.Selectors
	}
	overrideIfNotZero(o.envObj.Services[o.currentService].Deployments.Policies, &obj.Policies)
	overrideIfNotZero(o.envObj.Services[o.currentService].Deployments.Labels, &obj.Labels)
}

func (o *MergeEnvVisitor) VisitHTTPCheck(v DiceYmlVisitor, obj *HTTPCheck) {
	if o.currentService == "" {
		panic("should not be empty")
	}
	if s, ok := o.envObj.Services[o.currentService]; !ok || s == nil {
		return
	}
	if o.envObj.Services[o.currentService].HealthCheck.HTTP == nil {
		return
	}
	overrideIfNotZero(o.envObj.Services[o.currentService].HealthCheck.HTTP.Port, &obj.Port)
	overrideIfNotZero(o.envObj.Services[o.currentService].HealthCheck.HTTP.Path, &obj.Path)
	overrideIfNotZero(o.envObj.Services[o.currentService].HealthCheck.HTTP.Duration, &obj.Duration)
}

func (o *MergeEnvVisitor) VisitExecCheck(v DiceYmlVisitor, obj *ExecCheck) {
	if o.currentService == "" {
		panic("should not be empty")
	}
	if s, ok := o.envObj.Services[o.currentService]; !ok || s == nil {
		return
	}
	if o.envObj.Services[o.currentService].HealthCheck.Exec == nil {
		return
	}
	overrideIfNotZero(o.envObj.Services[o.currentService].HealthCheck.Exec.Cmd, &obj.Cmd)
	overrideIfNotZero(o.envObj.Services[o.currentService].HealthCheck.Exec.Duration, &obj.Duration)
}

func (o *MergeEnvVisitor) VisitAddOns(v DiceYmlVisitor, obj *AddOns) {
	if len(o.envObj.AddOns) == 0 {
		return
	}

	newAddons := AddOns{}
	override(&o.envObj.AddOns, &newAddons)

	*obj = newAddons
}

func MergeEnv(obj *Object, env string) {
	envObj := obj.Environments[env]
	if envObj == nil {
		return
	}
	visitor := NewMergeEnvVisitor(envObj)
	obj.Accept(visitor)
}
