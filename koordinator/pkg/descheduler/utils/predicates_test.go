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
	"reflect"
	"testing"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestUniqueSortTolerations(t *testing.T) {
	tests := []struct {
		name                string
		tolerations         []corev1.Toleration
		expectedTolerations []corev1.Toleration
	}{
		{
			name: "sort by key",
			tolerations: []corev1.Toleration{
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key3",
					Operator: corev1.TolerationOpEqual,
					Value:    "value3",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
			expectedTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key3",
					Operator: corev1.TolerationOpEqual,
					Value:    "value3",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
		},
		{
			name: "sort by value",
			tolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value3",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
			expectedTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value3",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
		},
		{
			name: "sort by effect",
			tolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoExecute,
				},
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectPreferNoSchedule,
				},
			},
			expectedTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoExecute,
				},
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectPreferNoSchedule,
				},
			},
		},
		{
			name: "sort unique",
			tolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
			expectedTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			resultTolerations := uniqueSortTolerations(test.tolerations)
			if !reflect.DeepEqual(resultTolerations, test.expectedTolerations) {
				t.Errorf("tolerations not sorted as expected, \n\tgot: %#v, \n\texpected: %#v", resultTolerations, test.expectedTolerations)
			}
		})
	}
}

func TestTolerationsEqual(t *testing.T) {
	tests := []struct {
		name                              string
		leftTolerations, rightTolerations []corev1.Toleration
		equal                             bool
	}{
		{
			name: "identical lists",
			leftTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
			rightTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
			equal: true,
		},
		{
			name: "equal lists",
			leftTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
			rightTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
			equal: true,
		},
		{
			name: "non-equal lists",
			leftTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
			rightTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoExecute,
				},
			},
			equal: false,
		},
		{
			name: "different sizes lists",
			leftTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
			},
			rightTolerations: []corev1.Toleration{
				{
					Key:      "key1",
					Operator: corev1.TolerationOpEqual,
					Value:    "value1",
					Effect:   corev1.TaintEffectNoSchedule,
				},
				{
					Key:      "key2",
					Operator: corev1.TolerationOpEqual,
					Value:    "value2",
					Effect:   corev1.TaintEffectNoExecute,
				},
			},
			equal: false,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			equal := TolerationsEqual(test.leftTolerations, test.rightTolerations)
			if equal != test.equal {
				t.Errorf("TolerationsEqual expected to be %v, got %v", test.equal, equal)
			}
		})
	}
}

func TestUniqueSortNodeSelectorRequirements(t *testing.T) {
	tests := []struct {
		name                 string
		requirements         []corev1.NodeSelectorRequirement
		expectedRequirements []corev1.NodeSelectorRequirement
	}{
		{
			name: "Identical requirements",
			requirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1"},
				},
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1"},
				},
			},
			expectedRequirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1"},
				},
			},
		},
		{
			name: "Sorted requirements",
			requirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1"},
				},
				{
					Key:      "k2",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v2"},
				},
			},
			expectedRequirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1"},
				},
				{
					Key:      "k2",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v2"},
				},
			},
		},
		{
			name: "Sort values",
			requirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v2", "v1"},
				},
			},
			expectedRequirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v2"},
				},
			},
		},
		{
			name: "Sort by key",
			requirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k3",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v2"},
				},
				{
					Key:      "k2",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v2"},
				},
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v2"},
				},
			},
			expectedRequirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v2"},
				},
				{
					Key:      "k2",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v2"},
				},
				{
					Key:      "k3",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v2"},
				},
			},
		},
		{
			name: "Sort by operator",
			requirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v2"},
				},
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpExists,
					Values:   []string{"v1", "v2"},
				},
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpGt,
					Values:   []string{"v1", "v2"},
				},
			},
			expectedRequirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpExists,
					Values:   []string{"v1", "v2"},
				},
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpGt,
					Values:   []string{"v1", "v2"},
				},
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v2"},
				},
			},
		},
		{
			name: "Sort by values",
			requirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v6", "v5"},
				},
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v2", "v1"},
				},
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v4", "v1"},
				},
			},
			expectedRequirements: []corev1.NodeSelectorRequirement{
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v2"},
				},
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v1", "v4"},
				},
				{
					Key:      "k1",
					Operator: corev1.NodeSelectorOpIn,
					Values:   []string{"v5", "v6"},
				},
			},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			resultRequirements := uniqueSortNodeSelectorRequirements(test.requirements)
			if !reflect.DeepEqual(resultRequirements, test.expectedRequirements) {
				t.Errorf("Requirements not sorted as expected, \n\tgot: %#v, \n\texpected: %#v", resultRequirements, test.expectedRequirements)
			}
		})
	}
}

func TestUniqueSortNodeSelectorTerms(t *testing.T) {
	tests := []struct {
		name          string
		terms         []corev1.NodeSelectorTerm
		expectedTerms []corev1.NodeSelectorTerm
	}{
		{
			name: "Identical terms",
			terms: []corev1.NodeSelectorTerm{
				{
					MatchExpressions: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
					},
					MatchFields: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
					},
				},
			},
			expectedTerms: []corev1.NodeSelectorTerm{
				{
					MatchExpressions: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
					},
					MatchFields: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
					},
				},
			},
		},
		{
			name: "Sorted terms",
			terms: []corev1.NodeSelectorTerm{
				{
					MatchExpressions: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
						{
							Key:      "k2",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
					},
					MatchFields: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
						{
							Key:      "k2",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
					},
				},
			},
			expectedTerms: []corev1.NodeSelectorTerm{
				{
					MatchExpressions: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
						{
							Key:      "k2",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
					},
					MatchFields: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
						{
							Key:      "k2",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
					},
				},
			},
		},
		{
			name: "Sort terms",
			terms: []corev1.NodeSelectorTerm{
				{
					MatchExpressions: []corev1.NodeSelectorRequirement{
						{
							Key:      "k2",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v2", "v1"},
						},
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v3", "v1"},
						},
					},
				},
			},
			expectedTerms: []corev1.NodeSelectorTerm{
				{
					MatchExpressions: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1", "v2"},
						},
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1", "v3"},
						},
						{
							Key:      "k2",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
					},
				},
			},
		},
		{
			name: "Unique sort terms",
			terms: []corev1.NodeSelectorTerm{
				{
					MatchExpressions: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v2", "v1"},
						},
						{
							Key:      "k2",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v2", "v1"},
						},
					},
				},
				{
					MatchExpressions: []corev1.NodeSelectorRequirement{
						{
							Key:      "k2",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v2", "v1"},
						},
					},
				},
			},
			expectedTerms: []corev1.NodeSelectorTerm{
				{
					MatchExpressions: []corev1.NodeSelectorRequirement{
						{
							Key:      "k1",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1", "v2"},
						},
						{
							Key:      "k2",
							Operator: corev1.NodeSelectorOpIn,
							Values:   []string{"v1"},
						},
					},
				},
			},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			resultTerms := uniqueSortNodeSelectorTerms(test.terms)
			if !reflect.DeepEqual(resultTerms, test.expectedTerms) {
				t.Errorf("Terms not sorted as expected, \n\tgot: %#v, \n\texpected: %#v", resultTerms, test.expectedTerms)
			}
		})
	}
}

func TestNodeSelectorTermsEqual(t *testing.T) {
	tests := []struct {
		name                        string
		leftSelector, rightSelector corev1.NodeSelector
		equal                       bool
	}{
		{
			name: "identical selectors",
			leftSelector: corev1.NodeSelector{
				NodeSelectorTerms: []corev1.NodeSelectorTerm{
					{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "k1",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v2"},
							},
							{
								Key:      "k2",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1"},
							},
						},
					},
				},
			},
			rightSelector: corev1.NodeSelector{
				NodeSelectorTerms: []corev1.NodeSelectorTerm{
					{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "k1",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v2"},
							},
							{
								Key:      "k2",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1"},
							},
						},
					},
				},
			},
			equal: true,
		},
		{
			name: "equal selectors",
			leftSelector: corev1.NodeSelector{
				NodeSelectorTerms: []corev1.NodeSelectorTerm{
					{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "k2",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v1"},
							},
							{
								Key:      "k1",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v2"},
							},
						},
					},
				},
			},
			rightSelector: corev1.NodeSelector{
				NodeSelectorTerms: []corev1.NodeSelectorTerm{
					{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "k1",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v2"},
							},
							{
								Key:      "k2",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1"},
							},
						},
					},
				},
			},
			equal: true,
		},
		{
			name: "non-equal selectors in values",
			leftSelector: corev1.NodeSelector{
				NodeSelectorTerms: []corev1.NodeSelectorTerm{
					{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "k1",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v2"},
							},
							{
								Key:      "k2",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1"},
							},
						},
					},
				},
			},
			rightSelector: corev1.NodeSelector{
				NodeSelectorTerms: []corev1.NodeSelectorTerm{
					{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "k1",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v2"},
							},
							{
								Key:      "k2",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v2"},
							},
						},
					},
				},
			},
			equal: false,
		},
		{
			name: "non-equal selectors in keys",
			leftSelector: corev1.NodeSelector{
				NodeSelectorTerms: []corev1.NodeSelectorTerm{
					{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "k3",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1"},
							},
							{
								Key:      "k1",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v2"},
							},
							{
								Key:      "k2",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1"},
							},
						},
					},
				},
			},
			rightSelector: corev1.NodeSelector{
				NodeSelectorTerms: []corev1.NodeSelectorTerm{
					{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "k1",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v2"},
							},
							{
								Key:      "k2",
								Operator: corev1.NodeSelectorOpIn,
								Values:   []string{"v1", "v2"},
							},
						},
					},
				},
			},
			equal: false,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			equal := NodeSelectorsEqual(&test.leftSelector, &test.rightSelector)
			if equal != test.equal {
				t.Errorf("NodeSelectorsEqual expected to be %v, got %v", test.equal, equal)
			}
		})
	}
}

func TestPodMatchNodeSelector(t *testing.T) {
	tests := []struct {
		name    string
		pod     *corev1.Pod
		node    *corev1.Node
		want    bool
		wantErr bool
	}{
		{
			name: "match",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod",
				},
			},
			node: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node",
				},
			},
			want:    true,
			wantErr: false,
		},
		{
			name: "failed to match nil node",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod",
				},
			},
			want:    false,
			wantErr: true,
		},
		{
			name: "failed to match node with nodeSelector",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod",
				},
				Spec: corev1.PodSpec{
					NodeSelector: map[string]string{
						"test-label": "value",
					},
				},
			},
			node: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node",
				},
			},
			want:    false,
			wantErr: false,
		},
		{
			name: "failed to match node with nodeAffinity",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod",
				},
				Spec: corev1.PodSpec{
					Affinity: &corev1.Affinity{
						NodeAffinity: &corev1.NodeAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
								NodeSelectorTerms: []corev1.NodeSelectorTerm{
									{
										MatchExpressions: []corev1.NodeSelectorRequirement{
											{
												Key:      "test-node-type",
												Operator: corev1.NodeSelectorOpIn,
												Values: []string{
													"test-node-type-A",
												},
											},
										},
									},
								},
							},
						},
					},
				},
			},
			node: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node",
				},
			},
			want:    false,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := PodMatchNodeSelector(tt.pod, tt.node)
			if (err != nil) != tt.wantErr {
				t.Errorf("PodMatchNodeSelector() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("PodMatchNodeSelector() got = %v, want %v", got, tt.want)
			}
		})
	}
}
