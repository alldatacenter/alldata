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
	"reflect"
	"strconv"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/koordinator-sh/koordinator/apis/extension"
	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/migration/reservation"
)

func TestGetCondition(t *testing.T) {
	tests := []struct {
		name          string
		status        *sev1alpha1.PodMigrationJobStatus
		conditionType sev1alpha1.PodMigrationJobConditionType
		want          int
		want1         *sev1alpha1.PodMigrationJobCondition
	}{
		{
			name:          "get from empty conditions",
			status:        &sev1alpha1.PodMigrationJobStatus{},
			conditionType: sev1alpha1.PodMigrationJobConditionReservationCreated,
			want:          -1,
			want1:         nil,
		},
		{
			name: "get expected condition",
			status: &sev1alpha1.PodMigrationJobStatus{
				Conditions: []sev1alpha1.PodMigrationJobCondition{
					{
						Type:   sev1alpha1.PodMigrationJobConditionReservationScheduled,
						Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
					},
					{
						Type:   sev1alpha1.PodMigrationJobConditionReservationCreated,
						Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
					},
				},
			},
			conditionType: sev1alpha1.PodMigrationJobConditionReservationCreated,
			want:          1,
			want1: &sev1alpha1.PodMigrationJobCondition{
				Type:   sev1alpha1.PodMigrationJobConditionReservationCreated,
				Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
			},
		},
		{
			name: "get non-exist condition",
			status: &sev1alpha1.PodMigrationJobStatus{
				Conditions: []sev1alpha1.PodMigrationJobCondition{
					{
						Type:   sev1alpha1.PodMigrationJobConditionReservationScheduled,
						Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
					},
					{
						Type:   sev1alpha1.PodMigrationJobConditionReservationCreated,
						Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
					},
				},
			},
			conditionType: sev1alpha1.PodMigrationJobConditionPreemption,
			want:          -1,
			want1:         nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, got1 := GetCondition(tt.status, tt.conditionType)
			if got != tt.want {
				t.Errorf("GetCondition() got = %v, want %v", got, tt.want)
			}
			if !reflect.DeepEqual(got1, tt.want1) {
				t.Errorf("GetCondition() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}

func TestUpdateCondition(t *testing.T) {
	tests := []struct {
		name      string
		status    *sev1alpha1.PodMigrationJobStatus
		condition *sev1alpha1.PodMigrationJobCondition
		want      bool
	}{
		{
			name: "update exist condition",
			status: &sev1alpha1.PodMigrationJobStatus{
				Conditions: []sev1alpha1.PodMigrationJobCondition{
					{
						Type:   sev1alpha1.PodMigrationJobConditionReservationScheduled,
						Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
					},
					{
						Type:   sev1alpha1.PodMigrationJobConditionReservationCreated,
						Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
					},
				},
			},
			condition: &sev1alpha1.PodMigrationJobCondition{
				Type:    sev1alpha1.PodMigrationJobConditionReservationCreated,
				Status:  sev1alpha1.PodMigrationJobConditionStatusTrue,
				Message: "test for update exist condition",
			},
			want: true,
		},
		{
			name: "update exist same condition",
			status: &sev1alpha1.PodMigrationJobStatus{
				Conditions: []sev1alpha1.PodMigrationJobCondition{
					{
						Type:   sev1alpha1.PodMigrationJobConditionReservationScheduled,
						Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
					},
					{
						Type:   sev1alpha1.PodMigrationJobConditionReservationCreated,
						Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
					},
				},
			},
			condition: &sev1alpha1.PodMigrationJobCondition{
				Type:   sev1alpha1.PodMigrationJobConditionReservationCreated,
				Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
			},
			want: false,
		},
		{
			name: "update non-exist condition",
			status: &sev1alpha1.PodMigrationJobStatus{
				Conditions: []sev1alpha1.PodMigrationJobCondition{
					{
						Type:   sev1alpha1.PodMigrationJobConditionReservationScheduled,
						Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
					},
					{
						Type:   sev1alpha1.PodMigrationJobConditionReservationCreated,
						Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
					},
				},
			},
			condition: &sev1alpha1.PodMigrationJobCondition{
				Type:    sev1alpha1.PodMigrationJobConditionPreemption,
				Status:  sev1alpha1.PodMigrationJobConditionStatusTrue,
				Message: "test for update non-exist condition",
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := UpdateCondition(tt.status, tt.condition); got != tt.want {
				t.Errorf("UpdateCondition() = %v, want %v", got, tt.want)
			}
			_, cond := GetCondition(tt.status, tt.condition.Type)
			if !reflect.DeepEqual(cond, tt.condition) {
				t.Errorf("GetCondition() got1 = %v, want %v", cond, tt.condition)
			}
		})
	}
}

func TestIsMigratePendingPod(t *testing.T) {
	reservationObj := reservation.NewReservation(&sev1alpha1.Reservation{
		Spec: sev1alpha1.ReservationSpec{
			Owners: []sev1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Namespace: "default",
						Name:      "test",
						Kind:      "Pod",
					},
				},
			},
		},
	})
	assert.True(t, IsMigratePendingPod(reservationObj))
}

func TestFilterPodWithMaxEvictionCost(t *testing.T) {
	tests := []struct {
		name string
		cost int32
		want bool
	}{
		{
			name: "empty cost",
			cost: 0,
			want: true,
		},
		{
			name: "negative cost",
			cost: -100,
			want: true,
		},
		{
			name: "higher cost",
			cost: 100,
			want: true,
		},
		{
			name: "max cost",
			cost: math.MaxInt32,
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := FilterPodWithMaxEvictionCost(&corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Annotations: map[string]string{
						extension.AnnotationEvictionCost: strconv.Itoa(int(tt.cost)),
					},
				},
			})
			assert.Equal(t, tt.want, got)
		})
	}
}
