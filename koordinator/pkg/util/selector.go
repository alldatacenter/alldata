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
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
)

func GetFastLabelSelector(ps *metav1.LabelSelector) (labels.Selector, error) {
	var selector labels.Selector
	if len(ps.MatchExpressions) == 0 && len(ps.MatchLabels) != 0 {
		selector = labels.SelectorFromValidatedSet(ps.MatchLabels)
		return selector, nil
	}

	return metav1.LabelSelectorAsSelector(ps)
}
