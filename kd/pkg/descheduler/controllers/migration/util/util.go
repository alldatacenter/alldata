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
	"math"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"

	"github.com/koordinator-sh/koordinator/apis/extension"
	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/migration/reservation"
)

func GetCondition(status *sev1alpha1.PodMigrationJobStatus, conditionType sev1alpha1.PodMigrationJobConditionType) (int, *sev1alpha1.PodMigrationJobCondition) {
	if len(status.Conditions) == 0 {
		return -1, nil
	}
	for i := range status.Conditions {
		if status.Conditions[i].Type == conditionType {
			return i, &status.Conditions[i]
		}
	}
	return -1, nil
}

func UpdateCondition(status *sev1alpha1.PodMigrationJobStatus, condition *sev1alpha1.PodMigrationJobCondition) bool {
	condition.LastTransitionTime = metav1.Now()
	// Try to find this PodMigrationJob condition.
	conditionIndex, oldCondition := GetCondition(status, condition.Type)

	if oldCondition == nil {
		// We are adding new PodMigrationJob condition.
		status.Conditions = append(status.Conditions, *condition)
		return true
	}
	// We are updating an existing condition, so we need to check if it has changed.
	if condition.Status == oldCondition.Status {
		condition.LastTransitionTime = oldCondition.LastTransitionTime
	}

	isEqual := condition.Status == oldCondition.Status &&
		condition.Reason == oldCondition.Reason &&
		condition.Message == oldCondition.Message &&
		condition.LastProbeTime.Equal(&oldCondition.LastProbeTime) &&
		condition.LastTransitionTime.Equal(&oldCondition.LastTransitionTime)

	status.Conditions[conditionIndex] = *condition
	// Return true if one of the fields have changed.
	return !isEqual
}

func IsMigratePendingPod(reservationObj reservation.Object) bool {
	pending := false
	for _, v := range reservationObj.GetReservationOwners() {
		if v.Object != nil && v.Controller == nil && v.LabelSelector == nil {
			pending = true
			break
		}
	}
	return pending
}

func GetMaxUnavailable(replicas int, intOrPercent *intstr.IntOrString) (int, error) {
	if intOrPercent == nil || intOrPercent.IntValue() == 0 {
		if replicas > 10 {
			s := intstr.FromString("10%")
			intOrPercent = &s
		} else if replicas >= 4 && replicas <= 10 {
			s := intstr.FromInt(2)
			intOrPercent = &s
		} else {
			s := intstr.FromInt(1)
			intOrPercent = &s
		}
	}
	maxUnavailable, err := intstr.GetScaledValueFromIntOrPercent(intOrPercent, replicas, false)
	if err != nil {
		return 0, err
	}
	if maxUnavailable > replicas {
		maxUnavailable = replicas
	}
	return maxUnavailable, nil
}

func GetMaxMigrating(replicas int, intOrPercent *intstr.IntOrString) (int, error) {
	return GetMaxUnavailable(replicas, intOrPercent)
}

// FilterPodWithMaxEvictionCost rejects if pod's eviction cost is math.MaxInt32
func FilterPodWithMaxEvictionCost(pod *corev1.Pod) bool {
	cost, _ := extension.GetEvictionCost(pod.Annotations)
	return !(cost == math.MaxInt32)
}
