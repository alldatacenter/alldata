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

package util

import (
	"reflect"
	"runtime"
	"time"

	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/component-base/featuregate"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/features"
)

// RunFeature runs moduleFunc only if interval > 0 AND at least one feature dependency is enabled
func RunFeature(moduleFunc func(), featureDependency []featuregate.Feature, interval int, stopCh <-chan struct{}) bool {
	return RunFeatureWithInit(func() error { return nil }, moduleFunc, featureDependency, interval, stopCh)
}

// RunFeatureWithInit runs moduleFunc only if interval > 0 , at least one feature dependency is enabled
// and moduleInit function returns nil
func RunFeatureWithInit(moduleInit func() error, moduleFunc func(), featureDependency []featuregate.Feature, interval int, stopCh <-chan struct{}) bool {
	moduleInitName := runtime.FuncForPC(reflect.ValueOf(moduleInit).Pointer()).Name()
	moduleFuncName := runtime.FuncForPC(reflect.ValueOf(moduleFunc).Pointer()).Name()
	if interval <= 0 {
		klog.Infof("time interval %v is disabled, skip run %v module", interval, moduleFuncName)
		return false
	}

	moduleFuncEnabled := len(featureDependency) == 0
	for _, feature := range featureDependency {
		if features.DefaultKoordletFeatureGate.Enabled(feature) {
			moduleFuncEnabled = true
			break
		}
	}
	if !moduleFuncEnabled {
		klog.Infof("all feature dependency %v is disabled, skip run module %v", featureDependency, moduleFuncName)
		return false
	}

	klog.Infof("starting %v feature init module", moduleInitName)
	if err := moduleInit(); err != nil {
		klog.Errorf("starting %v feature init module error %v", moduleInitName, err)
		return false
	}

	klog.Infof("starting %v feature dependency module, interval seconds %v", moduleFuncName, interval)
	go wait.Until(moduleFunc, time.Duration(interval)*time.Second, stopCh)
	return true
}
