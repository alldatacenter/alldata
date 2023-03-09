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

package extension

import (
	"fmt"
	"strconv"
)

const (
	// AnnotationEvictionCost indicates the eviction cost. It can be used to set to an int32.
	// Although the K8s community has [Pod Deletion Cost #2255](https://github.com/kubernetes/enhancements/issues/2255),
	// it is not a general mechanism. To avoid conflicts with components that use `Pod Deletion Cost`,
	// users can individually mark the eviction cost for Pods.
	// The implicit eviction cost for pods that don't set the annotation is 0, negative values are permitted.
	// If set the cost with `math.MaxInt32`, it means the Pod will not be evicted.
	// Pods with lower eviction cost are preferred to be evicted before pods with higher eviction cost.
	// If a batch of Pods to be evicted have the same priority, they will be sorted by cost,
	// and the Pod with the smallest cost will be evicted.
	AnnotationEvictionCost = SchedulingDomainPrefix + "/eviction-cost"
)

func GetEvictionCost(annotations map[string]string) (int32, error) {
	if value, exist := annotations[AnnotationEvictionCost]; exist {
		// values that start with plus sign (e.g, "+10") or leading zeros (e.g., "008") are not valid.
		if !validFirstDigit(value) {
			return 0, fmt.Errorf("invalid value %q", value)
		}

		i, err := strconv.ParseInt(value, 10, 32)
		if err != nil {
			// make sure we default to 0 on error.
			return 0, err
		}
		return int32(i), nil
	}
	return 0, nil
}

func validFirstDigit(str string) bool {
	if len(str) == 0 {
		return false
	}
	return str[0] == '-' || (str[0] == '0' && str == "0") || (str[0] >= '1' && str[0] <= '9')
}
