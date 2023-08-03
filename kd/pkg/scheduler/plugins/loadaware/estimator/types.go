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

package estimator

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
)

type FactoryFn func(args *config.LoadAwareSchedulingArgs, handle framework.Handle) (Estimator, error)

var Estimators = map[string]FactoryFn{
	defaultEstimatorName: NewDefaultEstimator,
}

type Estimator interface {
	Name() string
	Estimate(pod *corev1.Pod) (map[corev1.ResourceName]int64, error)
}

func NewEstimator(args *config.LoadAwareSchedulingArgs, handle framework.Handle) (Estimator, error) {
	factoryFn := Estimators[args.Estimator]
	if factoryFn == nil {
		factoryFn = NewDefaultEstimator
	}
	return factoryFn(args, handle)
}
