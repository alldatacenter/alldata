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
	"encoding/json"
	"strconv"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

const (
	// LabelReservationOrder controls the preference logic for Reservation.
	// Reservation with lower order is preferred to be selected before Reservation with higher order.
	// But if it is 0, Reservation will be selected according to the capacity score.
	LabelReservationOrder = SchedulingDomainPrefix + "/reservation-order"

	// AnnotationReservationAllocated represents the reservation allocated by the pod.
	AnnotationReservationAllocated = SchedulingDomainPrefix + "/reservation-allocated"
)

const (
	// AnnotationCustomUsageThresholds represents the user-defined resource utilization threshold.
	// For specific value definitions, see CustomUsageThresholds
	AnnotationCustomUsageThresholds = SchedulingDomainPrefix + "/usage-thresholds"

	// AnnotationDeviceAllocated represents the device allocated by the pod
	AnnotationDeviceAllocated = SchedulingDomainPrefix + "/device-allocated"
)

const (
	AnnotationGangPrefix = "gang.scheduling.koordinator.sh"
	// AnnotationGangName specifies the name of the gang
	AnnotationGangName = AnnotationGangPrefix + "/name"

	// AnnotationGangMinNum specifies the minimum number of the gang that can be executed
	AnnotationGangMinNum = AnnotationGangPrefix + "/min-available"

	// AnnotationGangWaitTime specifies gang's max wait time in Permit Stage
	AnnotationGangWaitTime = AnnotationGangPrefix + "/waiting-time"

	// AnnotationGangTotalNum specifies the total children number of the gang
	// If not specified,it will be set with the AnnotationGangMinNum
	AnnotationGangTotalNum = AnnotationGangPrefix + "/total-number"

	// AnnotationGangMode defines the Gang Scheduling operation when failed scheduling
	// Support GangModeStrict and GangModeNonStrict, default is GangModeStrict
	AnnotationGangMode = AnnotationGangPrefix + "/mode"

	// AnnotationGangGroups defines which gangs are bundled as a group
	// The gang will go to bind only all gangs in one group meet the conditions
	AnnotationGangGroups = AnnotationGangPrefix + "/groups"

	// AnnotationGangTimeout means that the entire gang cannot be scheduled due to timeout
	// The annotation is added by the scheduler when the gang times out
	AnnotationGangTimeout = AnnotationGangPrefix + "/timeout"

	GangModeStrict    = "Strict"
	GangModeNonStrict = "NonStrict"
)

// CustomUsageThresholds supports user-defined node resource utilization thresholds.
type CustomUsageThresholds struct {
	// UsageThresholds indicates the resource utilization threshold of the whole machine.
	UsageThresholds map[corev1.ResourceName]int64 `json:"usageThresholds,omitempty"`
	// ProdUsageThresholds indicates the resource utilization threshold of Prod Pods compared to the whole machine
	ProdUsageThresholds map[corev1.ResourceName]int64 `json:"prodUsageThresholds,omitempty"`
	// AggregatedUsage supports resource utilization filtering and scoring based on percentile statistics
	AggregatedUsage *CustomAggregatedUsage `json:"aggregatedUsage,omitempty"`
}

type CustomAggregatedUsage struct {
	// UsageThresholds indicates the resource utilization threshold of the machine based on percentile statistics
	UsageThresholds map[corev1.ResourceName]int64 `json:"usageThresholds,omitempty"`
	// UsageAggregationType indicates the percentile type of the machine's utilization when filtering
	UsageAggregationType slov1alpha1.AggregationType `json:"usageAggregationType,omitempty"`
	// UsageAggregatedDuration indicates the statistical period of the percentile of the machine's utilization when filtering
	UsageAggregatedDuration *metav1.Duration `json:"usageAggregatedDuration,omitempty"`
}

func GetCustomUsageThresholds(node *corev1.Node) (*CustomUsageThresholds, error) {
	usageThresholds := &CustomUsageThresholds{}
	data, ok := node.Annotations[AnnotationCustomUsageThresholds]
	if !ok {
		return usageThresholds, nil
	}
	err := json.Unmarshal([]byte(data), usageThresholds)
	if err != nil {
		return nil, err
	}
	return usageThresholds, nil
}

type ReservationAllocated struct {
	Name string    `json:"name,omitempty"`
	UID  types.UID `json:"uid,omitempty"`
}

func GetReservationAllocated(pod *corev1.Pod) (*ReservationAllocated, error) {
	if pod.Annotations == nil {
		return nil, nil
	}
	data, ok := pod.Annotations[AnnotationReservationAllocated]
	if !ok {
		return nil, nil
	}
	reservationAllocated := &ReservationAllocated{}
	err := json.Unmarshal([]byte(data), reservationAllocated)
	if err != nil {
		return nil, err
	}
	return reservationAllocated, nil
}

func SetReservationAllocated(pod *corev1.Pod, r *schedulingv1alpha1.Reservation) {
	if pod.Annotations == nil {
		pod.Annotations = map[string]string{}
	}
	reservationAllocated := &ReservationAllocated{
		Name: r.Name,
		UID:  r.UID,
	}
	data, _ := json.Marshal(reservationAllocated) // assert no error
	pod.Annotations[AnnotationReservationAllocated] = string(data)
}

func RemoveReservationAllocated(pod *corev1.Pod, r *schedulingv1alpha1.Reservation) (bool, error) {
	reservationAllocated, err := GetReservationAllocated(pod)
	if err != nil {
		return false, err
	}
	if reservationAllocated != nil && reservationAllocated.Name == r.Name && reservationAllocated.UID == r.UID {
		delete(pod.Annotations, AnnotationReservationAllocated)
		return true, nil
	}
	return false, nil
}

// DeviceAllocations would be injected into Pod as form of annotation during Pre-bind stage.
/*
{
  "gpu": [
    {
      "minor": 0,
      "resources": {
        "koordinator.sh/gpu-core": 100,
        "koordinator.sh/gpu-mem-ratio": 100,
        "koordinator.sh/gpu-mem": "16Gi"
      }
    },
    {
      "minor": 1,
      "resources": {
        "koordinator.sh/gpu-core": 100,
        "koordinator.sh/gpu-mem-ratio": 100,
        "koordinator.sh/gpu-mem": "16Gi"
      }
    }
  ]
}
*/
type DeviceAllocations map[schedulingv1alpha1.DeviceType][]*DeviceAllocation

type DeviceAllocation struct {
	Minor     int32               `json:"minor"`
	Resources corev1.ResourceList `json:"resources"`
}

func GetDeviceAllocations(podAnnotations map[string]string) (DeviceAllocations, error) {
	deviceAllocations := DeviceAllocations{}
	data, ok := podAnnotations[AnnotationDeviceAllocated]
	if !ok {
		return nil, nil
	}
	err := json.Unmarshal([]byte(data), &deviceAllocations)
	if err != nil {
		return nil, err
	}
	return deviceAllocations, nil
}

func SetDeviceAllocations(pod *corev1.Pod, allocations DeviceAllocations) error {
	if pod.Annotations == nil {
		pod.Annotations = map[string]string{}
	}

	data, err := json.Marshal(allocations)
	if err != nil {
		return err
	}

	pod.Annotations[AnnotationDeviceAllocated] = string(data)
	return nil
}

var GetMinNum = func(pod *corev1.Pod) (int, error) {
	minRequiredNum, err := strconv.ParseInt(pod.Annotations[AnnotationGangMinNum], 10, 32)
	if err != nil {
		return 0, err
	}
	return int(minRequiredNum), nil
}

var GetGangName = func(pod *corev1.Pod) string {
	return pod.Annotations[AnnotationGangName]
}
