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

package reservation

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func TestNewReservePod(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		r := &schedulingv1alpha1.Reservation{
			ObjectMeta: metav1.ObjectMeta{
				Name: "reserve-pod-0",
			},
			Spec: schedulingv1alpha1.ReservationSpec{
				Template: &corev1.PodTemplateSpec{
					ObjectMeta: metav1.ObjectMeta{
						Name: "reserve-pod-0",
					},
					Spec: corev1.PodSpec{
						NodeName: "test-node-0",
					},
				},
				Owners: []schedulingv1alpha1.ReservationOwner{
					{
						Object: &corev1.ObjectReference{
							Kind: "Pod",
							Name: "test-pod-0",
						},
					},
				},
				TTL: &metav1.Duration{Duration: 30 * time.Minute},
			},
		}
		reservePod := NewReservePod(r)
		assert.NotNil(t, reservePod)
		assert.True(t, IsReservePod(reservePod))
	})
}

func TestIsReservationActive(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		rPending := &schedulingv1alpha1.Reservation{
			ObjectMeta: metav1.ObjectMeta{
				Name: "reserve-pod-0",
			},
			Spec: schedulingv1alpha1.ReservationSpec{
				Template: &corev1.PodTemplateSpec{
					ObjectMeta: metav1.ObjectMeta{
						Name: "reserve-pod-0",
					},
					Spec: corev1.PodSpec{
						NodeName: "test-node-0",
					},
				},
				Owners: []schedulingv1alpha1.ReservationOwner{
					{
						Object: &corev1.ObjectReference{
							Kind: "Pod",
							Name: "test-pod-0",
						},
					},
				},
				TTL: &metav1.Duration{Duration: 30 * time.Minute},
			},
		}
		assert.Equal(t, false, IsReservationActive(rPending))

		rActive := rPending.DeepCopy()
		rActive.Status = schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node-0",
		}
		assert.Equal(t, true, IsReservationActive(rActive))
	})
}

func TestIsReservationAvailable(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		r := &schedulingv1alpha1.Reservation{}
		got := IsReservationAvailable(r)
		assert.False(t, got)

		r = &schedulingv1alpha1.Reservation{
			Status: schedulingv1alpha1.ReservationStatus{
				Phase:    schedulingv1alpha1.ReservationAvailable,
				NodeName: "test-node-0",
			},
		}
		got = IsReservationAvailable(r)
		assert.True(t, got)
	})
}

func TestIsReservationSucceeded(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		r := &schedulingv1alpha1.Reservation{}
		got := IsReservationSucceeded(r)
		assert.False(t, got)

		r = &schedulingv1alpha1.Reservation{
			Status: schedulingv1alpha1.ReservationStatus{
				Phase:    schedulingv1alpha1.ReservationAvailable,
				NodeName: "test-node-0",
			},
		}
		got = IsReservationSucceeded(r)
		assert.False(t, got)

		r = &schedulingv1alpha1.Reservation{
			Status: schedulingv1alpha1.ReservationStatus{
				Phase:    schedulingv1alpha1.ReservationSucceeded,
				NodeName: "test-node-0",
			},
		}
		got = IsReservationSucceeded(r)
		assert.True(t, got)
	})
}

func TestIsReservationFailed(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		r := &schedulingv1alpha1.Reservation{}
		got := IsReservationFailed(r)
		assert.False(t, got)

		r = &schedulingv1alpha1.Reservation{
			Status: schedulingv1alpha1.ReservationStatus{
				Phase: schedulingv1alpha1.ReservationFailed,
			},
		}
		got = IsReservationFailed(r)
		assert.True(t, got)
	})
}

func TestIsReservationExpired(t *testing.T) {
	tests := []struct {
		name string
		arg  *schedulingv1alpha1.Reservation
		want bool
	}{
		{
			name: "not panic for nil",
			arg:  nil,
			want: false,
		},
		{
			name: "not panic for empty",
			arg:  &schedulingv1alpha1.Reservation{},
			want: false,
		},
		{
			name: "available reservation is not expired",
			arg: &schedulingv1alpha1.Reservation{
				Status: schedulingv1alpha1.ReservationStatus{
					Phase:    schedulingv1alpha1.ReservationAvailable,
					NodeName: "test-node-0",
				},
			},
			want: false,
		},
		{
			name: "scheduled failed reservation is not expired",
			arg: &schedulingv1alpha1.Reservation{
				Status: schedulingv1alpha1.ReservationStatus{
					Conditions: []schedulingv1alpha1.ReservationCondition{
						{
							Type:               schedulingv1alpha1.ReservationConditionScheduled,
							Status:             schedulingv1alpha1.ConditionStatusFalse,
							Reason:             schedulingv1alpha1.ReasonReservationUnschedulable,
							Message:            "xxx",
							LastTransitionTime: metav1.Now(),
							LastProbeTime:      metav1.Now(),
						},
					},
				},
			},
			want: false,
		},
		{
			name: "check expired reservation",
			arg: &schedulingv1alpha1.Reservation{
				Status: schedulingv1alpha1.ReservationStatus{
					Phase: schedulingv1alpha1.ReservationFailed,
					Conditions: []schedulingv1alpha1.ReservationCondition{
						{
							Type:               schedulingv1alpha1.ReservationConditionScheduled,
							Status:             schedulingv1alpha1.ConditionStatusFalse,
							Reason:             schedulingv1alpha1.ReasonReservationUnschedulable,
							Message:            "xxx",
							LastTransitionTime: metav1.Now(),
							LastProbeTime:      metav1.Now(),
						},
						{
							Type:               schedulingv1alpha1.ReservationConditionReady,
							Status:             schedulingv1alpha1.ConditionStatusFalse,
							Reason:             schedulingv1alpha1.ReasonReservationExpired,
							LastTransitionTime: metav1.Now(),
							LastProbeTime:      metav1.Now(),
						},
					},
				},
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := IsReservationExpired(tt.arg)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestGetReservationSchedulerName(t *testing.T) {
	tests := []struct {
		name string
		arg  *schedulingv1alpha1.Reservation
		want string
	}{
		{
			name: "empty reservation",
			arg:  nil,
			want: corev1.DefaultSchedulerName,
		},
		{
			name: "empty template",
			arg:  &schedulingv1alpha1.Reservation{},
			want: corev1.DefaultSchedulerName,
		},
		{
			name: "empty scheduler name",
			arg: &schedulingv1alpha1.Reservation{
				Spec: schedulingv1alpha1.ReservationSpec{
					Template: &corev1.PodTemplateSpec{},
				},
			},
			want: corev1.DefaultSchedulerName,
		},
		{
			name: "get scheduler name successfully",
			arg: &schedulingv1alpha1.Reservation{
				Spec: schedulingv1alpha1.ReservationSpec{
					Template: &corev1.PodTemplateSpec{
						Spec: corev1.PodSpec{
							SchedulerName: "test-scheduler",
						},
					},
				},
			},
			want: "test-scheduler",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := GetReservationSchedulerName(tt.arg)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_matchReservationOwners(t *testing.T) {
	type args struct {
		pod *corev1.Pod
		r   *schedulingv1alpha1.Reservation
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "no owner to match",
			args: args{
				pod: &corev1.Pod{},
				r: &schedulingv1alpha1.Reservation{
					Spec: schedulingv1alpha1.ReservationSpec{
						Owners: nil,
					},
				},
			},
			want: false,
		},
		{
			name: "match objRef",
			args: args{
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name:      "test-pod-0",
						Namespace: "test",
					},
				},
				r: &schedulingv1alpha1.Reservation{
					Spec: schedulingv1alpha1.ReservationSpec{
						Owners: []schedulingv1alpha1.ReservationOwner{
							{
								Object: &corev1.ObjectReference{
									Name:      "test-pod-0",
									Namespace: "test",
								},
							},
						},
					},
				},
			},
			want: true,
		},
		{
			name: "match controllerRef",
			args: args{
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name:      "test-sts-0-0",
						Namespace: "test",
						OwnerReferences: []metav1.OwnerReference{
							{
								Name:       "test-sts-0",
								Controller: pointer.Bool(true),
								Kind:       "StatefulSet",
								APIVersion: "apps/v1",
							},
						},
					},
				},
				r: &schedulingv1alpha1.Reservation{
					Spec: schedulingv1alpha1.ReservationSpec{
						Owners: []schedulingv1alpha1.ReservationOwner{
							{
								Controller: &schedulingv1alpha1.ReservationControllerReference{
									OwnerReference: metav1.OwnerReference{
										Name:       "test-sts-0",
										Controller: pointer.Bool(true),
									},
									Namespace: "test",
								},
							},
						},
					},
				},
			},
			want: true,
		},
		{
			name: "match labels",
			args: args{
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name:      "test-pod-1",
						Namespace: "test",
						Labels: map[string]string{
							"aaa": "bbb",
							"ccc": "ddd",
						},
					},
				},
				r: &schedulingv1alpha1.Reservation{
					Spec: schedulingv1alpha1.ReservationSpec{
						Owners: []schedulingv1alpha1.ReservationOwner{
							{
								LabelSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"aaa": "bbb",
									},
								},
							},
						},
					},
				},
			},
			want: true,
		},
		{
			name: "fail on one term of owner spec",
			args: args{
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name:      "test-pod-1",
						Namespace: "test",
						Labels: map[string]string{
							"aaa": "bbb",
							"ccc": "ddd",
						},
					},
				},
				r: &schedulingv1alpha1.Reservation{
					Spec: schedulingv1alpha1.ReservationSpec{
						Owners: []schedulingv1alpha1.ReservationOwner{
							{
								Object: &corev1.ObjectReference{
									Name: "test-pod-2",
								},
								LabelSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"aaa": "bbb",
										"xxx": "yyy",
									},
								},
							},
						},
					},
				},
			},
			want: false,
		},
		{
			name: "match one of owner specs",
			args: args{
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name:      "test-pod-2",
						Namespace: "test",
						Labels: map[string]string{
							"aaa": "bbb",
							"ccc": "ddd",
						},
					},
				},
				r: &schedulingv1alpha1.Reservation{
					Spec: schedulingv1alpha1.ReservationSpec{
						Owners: []schedulingv1alpha1.ReservationOwner{
							{
								Object: &corev1.ObjectReference{
									Name:      "test-pod-0",
									Namespace: "test",
								},
							},
							{
								LabelSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"aaa": "bbb",
									},
								},
							},
						},
					},
				},
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := MatchReservationOwners(tt.args.pod, tt.args.r)
			assert.Equal(t, tt.want, got)
		})
	}
}
