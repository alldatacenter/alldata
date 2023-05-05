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
	"fmt"
	"reflect"
	"strconv"

	"gopkg.in/yaml.v2"
)

type FieldnameValidateVisitor struct {
	DefaultVisitor
	RawMap             map[interface{}]interface{}
	currentService     map[interface{}]interface{}
	currentServiceName string
	collectErrors      ValidateError
	k8sSnippet         *K8SSnippet
}

func NewFieldnameValidateVisitor(raw []byte) (DiceYmlVisitor, error) {
	var rawmap interface{}
	if err := yaml.Unmarshal(raw, &rawmap); err != nil {
		return nil, err
	}
	tp := reflect.TypeOf(rawmap)
	if tp.Kind() != reflect.Map || tp.Key().Kind() != reflect.Interface || tp.Elem().Kind() != reflect.Interface {
		return nil, fmt.Errorf("not a map[interface{}]interface{} type")
	}

	return &FieldnameValidateVisitor{
		RawMap:        rawmap.(map[interface{}]interface{}),
		collectErrors: ValidateError{},
	}, nil
}

func (o *FieldnameValidateVisitor) VisitObject(v DiceYmlVisitor, obj *Object) {
	for k := range o.RawMap {
		switch i := k.(type) {
		case string:
			if !contain(i, []string{"version", "meta", "envs", "services", "addons", "environments", "jobs", "values"}) {
				o.collectErrors[yamlHeaderRegex(i)] = fmt.Errorf("field '%s' not one of [version, meta, envs, services, addons, environments, jobs, values]", i)
			}
		default:
			o.collectErrors[yamlHeaderRegex("_"+strconv.Itoa(len(o.collectErrors)))] = fmt.Errorf("%v not string type", k)
		}
	}
}

func (o *FieldnameValidateVisitor) VisitServices(v DiceYmlVisitor, obj *Services) {
	services, ok := o.RawMap["services"].(map[interface{}]interface{})
	if !ok {
		return
	}
	for name, v_ := range *obj {
		s, ok := services[name].(map[interface{}]interface{})
		if !ok {
			continue
		}
		o.currentService = s
		o.currentServiceName = name
		v_.Accept(v)
	}
	o.currentService = nil
	o.currentServiceName = ""
}

func (o *FieldnameValidateVisitor) VisitService(v DiceYmlVisitor, obj *Service) {
	for k := range o.currentService {
		switch i := k.(type) {
		case string:
			if !contain(i, []string{"image", "image_username", "image_password", "cmd", "labels", "ports", "envs", "hosts", "resources", "volumes", "deployments", "depends_on", "expose", "health_check", "binds", "sidecars", "init", "traffic_security", "endpoints", "mesh_enable", "k8s_snippet"}) {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentServiceName}, i)] = fmt.Errorf("[%s] field '%s' not one of [image, image_username, image_password, cmd, ports, envs, hosts, labels, resources, volumes, deployments, depends_on, expose, health_check, binds, sidecars，init, traffic_security, endpoints, mesh_enable, k8s_snippet]", o.currentServiceName, i)
			}
		default:
			o.collectErrors[yamlHeaderRegex("_"+strconv.Itoa(len(o.collectErrors)))] = fmt.Errorf("[%s] %v not string type", o.currentServiceName, k)
		}
	}
}

func (o *FieldnameValidateVisitor) VisitAddOns(v DiceYmlVisitor, obj *AddOns) {
	addons, ok := o.RawMap["addons"].(map[interface{}]interface{})
	if !ok {
		return
	}
	for addonName, v := range addons {
		addon, ok := v.(map[interface{}]interface{})
		if !ok {
			continue
		}
		for k := range addon {
			switch i := k.(type) {
			case string:
				if !contain(i, []string{"plan", "as", "options", "image", "actions"}) {
					o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{"addons", addonName.(string)}, i)] = fmt.Errorf("field '%s' not one of [plan, as, options, image, actions]", i)
				}
			default:
				o.collectErrors[yamlHeaderRegex("_"+strconv.Itoa(len(o.collectErrors)))] = fmt.Errorf("%v not string type", k)
			}
		}
	}
}

func (o *FieldnameValidateVisitor) VisitResources(v DiceYmlVisitor, obj *Resources) {
	res, ok := o.currentService["resources"].(map[interface{}]interface{})
	if !ok {
		return
	}
	for k := range res {
		switch i := k.(type) {
		case string:
			if !contain(i, []string{"cpu", "max_cpu", "mem", "max_mem", "disk", "network", "emptydir_size", "ephemeral_storage_size"}) {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentServiceName, "resources"}, i)] = fmt.Errorf("[%s]/[resources] field '%s' not one of [cpu, max_cpu, mem, max_mem, disk, network, emptydir_size, ephemeral_storage_size]", o.currentServiceName, i)
			}
		default:
			o.collectErrors[yamlHeaderRegex("_"+strconv.Itoa(len(o.collectErrors)))] = fmt.Errorf("[%s]/[resources] %v not string type", o.currentServiceName, k)
		}

		if sk, ok := k.(string); ok && sk == "network" {
			v := res[k]
			_, ok := v.(map[interface{}]interface{})
			if !ok {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentServiceName, "resources"}, "network")] = fmt.Errorf("[%s]/[resources]/[network] %v not map[interface{}]interface{} type", o.currentServiceName, v)
			}
		}
	}

}

func (o *FieldnameValidateVisitor) VisitDeployments(v DiceYmlVisitor, obj *Deployments) {
	dep, ok := o.currentService["deployments"].(map[interface{}]interface{})
	if !ok {
		return
	}
	for k := range dep {
		switch i := k.(type) {
		case string:
			if !contain(i, []string{"workload", "replicas", "policies", "labels", "selectors"}) {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentServiceName, "deployments"}, i)] = fmt.Errorf("[%s]/[deployments] field '%s' not one of [replicas, policies, labels, selectors]", o.currentServiceName, i)
			}
		default:
			o.collectErrors[yamlHeaderRegex("_"+strconv.Itoa(len(o.collectErrors)))] = fmt.Errorf("[%s]/[deployments] %v not string type", o.currentServiceName, k)
		}
	}
}

func (o *FieldnameValidateVisitor) VisitHealthCheck(v DiceYmlVisitor, obj *HealthCheck) {
	hc, ok := o.currentService["health_check"].(map[interface{}]interface{})
	if !ok {
		return
	}
	for k := range hc {
		switch i := k.(type) {
		case string:
			if !contain(i, []string{"http", "exec"}) {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentServiceName, "health_check"}, i)] = fmt.Errorf("[%s]/[health_check] field '%s' not one of [http, exec]", o.currentServiceName, i)
			}
		default:
			o.collectErrors[yamlHeaderRegex("_"+strconv.Itoa(len(o.collectErrors)))] = fmt.Errorf("[%s]/[health_check] %v not string type", o.currentServiceName, k)
		}
	}
}

func (o *FieldnameValidateVisitor) VisitHTTPCheck(v DiceYmlVisitor, obj *HTTPCheck) {
	hc, ok := o.currentService["health_check"].(map[interface{}]interface{})
	if !ok {
		return
	}
	http, ok := hc["http"].(map[interface{}]interface{})
	if !ok {
		return
	}
	for k := range http {
		switch i := k.(type) {
		case string:
			if !contain(i, []string{"port", "path", "duration"}) {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentServiceName, "health_check", "http"}, i)] = fmt.Errorf("[%s]/[health_check]/[http] field '%s' not one of [port, path, duration]", o.currentServiceName, i)
			}
		default:
			o.collectErrors[yamlHeaderRegex("_"+strconv.Itoa(len(o.collectErrors)))] = fmt.Errorf("[%s]/[health_check]/[http] %v not string type", o.currentServiceName, k)
		}
	}
}

func (o *FieldnameValidateVisitor) VisitExecCheck(v DiceYmlVisitor, obj *ExecCheck) {
	hc, ok := o.currentService["health_check"].(map[interface{}]interface{})
	if !ok {
		return
	}
	exec, ok := hc["exec"].(map[interface{}]interface{})
	if !ok {
		return
	}
	for k := range exec {
		switch i := k.(type) {
		case string:
			if !contain(i, []string{"cmd", "duration"}) {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentServiceName, "health_check", "exec"}, i)] = fmt.Errorf("[%s]/[health_check]/[exec] field '%s' not one of [cmd, duration]", o.currentServiceName, i)
			}
		default:
			o.collectErrors[yamlHeaderRegex("_"+strconv.Itoa(len(o.collectErrors)))] = fmt.Errorf("[%s]/[health_check]/[exec] %v not string type", o.currentServiceName, k)
		}
	}
}

func (o *FieldnameValidateVisitor) VisitK8SSnippet(v DiceYmlVisitor, obj *K8SSnippet) {
	res, ok := o.currentService["k8s_snippet"].(map[interface{}]interface{})
	if !ok {
		return
	}
	for k := range res {
		switch i := k.(type) {
		case string:
			if !contain(i, []string{"container"}) {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentServiceName, "k8s_snippet"}, i)] = fmt.Errorf(`[%s]/[k8s_snippet] field '%s' not one of [ container ]`, o.currentServiceName, i)
			}
		default:
			o.collectErrors[yamlHeaderRegex("_"+strconv.Itoa(len(o.collectErrors)))] = fmt.Errorf("[%s]/[k8s_snippet] %v not string type", o.currentServiceName, k)
		}
	}
}

func (o *FieldnameValidateVisitor) VisitContainerSnippet(v DiceYmlVisitor, obj *ContainerSnippet) {
	snippet, ok := o.currentService["k8s_snippet"].(map[interface{}]interface{})
	if !ok {
		return
	}
	res, ok := snippet["container"].(map[interface{}]interface{})
	if !ok {
		return
	}
	for k := range res {
		switch i := k.(type) {
		case string:
			if !contain(i, []string{"workingDir", "envFrom", "env", "livenessProbe", "readinessProbe", "startupProbe", "lifeCycle", "terminationMessagePath", "terminationMessagePolicy", "imagePullPolicy", "securityContext", "stdin", "stdinOnce", "tty"}) {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentServiceName, "k8s_snippet", "container"}, i)] = fmt.Errorf(`[%s]/[k8s_snippet]/[container] field '%s' is not supported, only support this container fields ["workingDir", "envFrom", "env", "livenessProbe", "readinessProbe", "startupProbe", "lifeCycle", "terminationMessagePath", "terminationMessagePolicy", "imagePullPolicy", "securityContext", "stdin", "stdinOnce", "tty"]`, o.currentServiceName, i)
			}
		default:
			o.collectErrors[yamlHeaderRegex("_"+strconv.Itoa(len(o.collectErrors)))] = fmt.Errorf("[%s]/[k8s_snippet]/[container] %v not string type", o.currentServiceName, k)
		}
	}
}

func contain(s string, slist []string) bool {
	for i := range slist {
		if s == slist[i] {
			return true
		}
	}
	return false
}

func FieldnameValidate(obj *Object, raw []byte) ValidateError {
	visitor, err := NewFieldnameValidateVisitor(raw)
	if err != nil {
		return ValidateError{yamlHeaderRegex("_0"): err}
	}
	obj.Accept(visitor)
	return visitor.(*FieldnameValidateVisitor).collectErrors
}
