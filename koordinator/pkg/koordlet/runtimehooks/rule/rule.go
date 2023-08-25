/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package rule

import (
	"reflect"
	"runtime"
	"sync"

	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

type Rule struct {
	name            string
	description     string
	parseRuleType   statesinformer.RegisterType
	parseRuleFn     ParseRuleFn
	callbacks       []UpdateCbFn
	systemSupported bool
}

type ParseRuleFn func(interface{}) (bool, error)
type UpdateCbFn func(pods []*statesinformer.PodMeta) error
type SysSupportFn func() bool

var globalHookRules map[string]*Rule
var globalRWMutex sync.RWMutex

func Register(name, description string, injectOpts ...InjectOption) *Rule {
	r, exist := find(name)
	if exist {
		klog.Fatalf("rule %s is conflict since name is already registered")
		return r
	}
	r.description = description
	for _, opt := range injectOpts {
		opt.Apply(r)
	}
	klog.V(5).Infof("new rule %v has registered", name)
	return r
}

func (r *Rule) runUpdateCallbacks(pods []*statesinformer.PodMeta) {
	for _, callbackFn := range r.callbacks {
		if err := callbackFn(pods); err != nil {
			cbName := runtime.FuncForPC(reflect.ValueOf(callbackFn).Pointer()).Name()
			klog.Warningf("executing %s callback function %s failed, error %v", r.name, cbName)
		}
	}
}

func find(name string) (*Rule, bool) {
	globalRWMutex.Lock()
	defer globalRWMutex.Unlock()
	if r, exist := globalHookRules[name]; exist {
		return r, true
	}
	newRule := &Rule{name: name, systemSupported: true}
	globalHookRules[name] = newRule
	return newRule, false
}

func UpdateRules(ruleType statesinformer.RegisterType, ruleObj interface{}, podsMeta []*statesinformer.PodMeta) {
	klog.V(3).Infof("applying %v rules with new %v, detail: %v",
		len(globalHookRules), ruleType.String(), util.DumpJSON(ruleObj))
	for _, r := range globalHookRules {
		if ruleType != r.parseRuleType {
			continue
		}
		if !r.systemSupported {
			klog.V(4).Infof("system unsupported for rule %s, do nothing during UpdateRules", r.name)
			continue
		}
		if r.parseRuleFn == nil {
			continue
		}
		updated, err := r.parseRuleFn(ruleObj)
		if err != nil {
			klog.Warningf("parse rule %s from nodeSLO failed, error: %v", r.name, err)
			continue
		}
		if updated {
			klog.V(3).Infof("rule %s is updated, run update callback for all %v pods", r.name, len(podsMeta))
			r.runUpdateCallbacks(podsMeta)
		}
	}
}

func init() {
	globalHookRules = map[string]*Rule{}
}
