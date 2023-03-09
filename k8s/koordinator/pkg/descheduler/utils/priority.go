/*
Copyright 2022 The Koordinator Authors.
Copyright 2017 The Kubernetes Authors.

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

package utils

import (
	"fmt"

	schedulingv1 "k8s.io/client-go/listers/scheduling/v1"
	"k8s.io/kubernetes/pkg/apis/scheduling"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
)

const SystemCriticalPriority = scheduling.SystemCriticalPriority

// GetPriorityFromPriorityClass gets priority from the given priority class.
// If no priority class is provided, it will return SystemCriticalPriority by default.
func GetPriorityFromPriorityClass(priorityClassLister schedulingv1.PriorityClassLister, name string) (int32, error) {
	if name != "" {
		priorityClass, err := priorityClassLister.Get(name)
		if err != nil {
			return 0, err
		}
		return priorityClass.Value, nil
	}
	return SystemCriticalPriority, nil
}

// GetPriorityValueFromPriorityThreshold gets priority from the given PriorityThreshold.
// It will return SystemCriticalPriority by default.
func GetPriorityValueFromPriorityThreshold(priorityClassLister schedulingv1.PriorityClassLister, priorityThreshold *deschedulerconfig.PriorityThreshold) (priority int32, err error) {
	if priorityThreshold == nil {
		return SystemCriticalPriority, nil
	}
	if priorityThreshold.Value != nil {
		priority = *priorityThreshold.Value
	} else {
		priority, err = GetPriorityFromPriorityClass(priorityClassLister, priorityThreshold.Name)
		if err != nil {
			return 0, fmt.Errorf("unable to get priority value from the priority class: %v", err)
		}
	}
	if priority > SystemCriticalPriority {
		return 0, fmt.Errorf("priority threshold can't be greater than %d", SystemCriticalPriority)
	}
	return
}
