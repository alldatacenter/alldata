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

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/utils/pointer"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func TestStatusNodeNameIndexFunc(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		pod := &corev1.Pod{}
		got, err := StatusNodeNameIndexFunc(pod)
		assert.NoError(t, err)
		assert.Equal(t, []string{}, got)

		rPending := &schedulingv1alpha1.Reservation{
			ObjectMeta: metav1.ObjectMeta{
				Name: "reserve-pod-0",
			},
			Spec: schedulingv1alpha1.ReservationSpec{
				Template: &corev1.PodTemplateSpec{},
			},
		}
		got, err = StatusNodeNameIndexFunc(rPending)
		assert.NoError(t, err)
		assert.Equal(t, []string{}, got)

		rActive := rPending.DeepCopy()
		rActive.Status = schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node-0",
		}
		got, err = StatusNodeNameIndexFunc(rActive)
		assert.NoError(t, err)
		assert.Equal(t, []string{"test-node-0"}, got)
	})
}

func Test_matchReservationPorts(t *testing.T) {
	type args struct {
		pod *corev1.Pod
		r   *reservationInfo
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "no port to match",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Ports: []corev1.ContainerPort{
									{
										HostPort: 60000,
									},
								},
							},
						},
					},
				},
				r: &reservationInfo{
					Port: framework.HostPortInfo{},
				},
			},
			want: false,
		},
		{
			name: "match port",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Ports: []corev1.ContainerPort{
									{
										HostPort: 60000,
									},
								},
							},
						},
					},
				},
				r: &reservationInfo{
					Port: framework.HostPortInfo{
						"0.0.0.0": map[framework.ProtocolPort]struct{}{
							{
								Protocol: "TCP",
								Port:     60000,
							}: {},
						},
					},
				},
			},
			want: true,
		},
		{
			name: "mismatch protocol",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Ports: []corev1.ContainerPort{
									{
										HostPort: 60000,
									},
								},
							},
						},
					},
				},
				r: &reservationInfo{
					Port: framework.HostPortInfo{
						"0.0.0.0": map[framework.ProtocolPort]struct{}{
							{
								Protocol: "UDP",
								Port:     60000,
							}: {},
						},
					},
				},
			},
			want: false,
		},
		{
			name: "mismatch port",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Ports: []corev1.ContainerPort{
									{
										HostPort: 60000,
									},
								},
							},
						},
					},
				},
				r: &reservationInfo{
					Port: framework.HostPortInfo{
						"0.0.0.0": map[framework.ProtocolPort]struct{}{
							{
								Protocol: "TCP",
								Port:     60001,
							}: {},
						},
					},
				},
			},
			want: false,
		},
		{
			name: "dismatch ip",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Ports: []corev1.ContainerPort{
									{
										HostIP:   "192.168.0.2",
										HostPort: 60000,
									},
								},
							},
						},
					},
				},
				r: &reservationInfo{
					Port: framework.HostPortInfo{
						"192.168.0.1": map[framework.ProtocolPort]struct{}{
							{
								Protocol: "TCP",
								Port:     60000,
							}: {},
						},
					},
				},
			},
			want: false,
		},
		{
			name: "match by default",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Ports: []corev1.ContainerPort{
									{
										HostPort: 60000,
									},
								},
							},
						},
					},
				},
				r: &reservationInfo{
					Port: framework.HostPortInfo{
						"192.168.0.1": map[framework.ProtocolPort]struct{}{
							{
								Protocol: "TCP",
								Port:     60000,
							}: {},
						},
					},
				},
			},
			want: true,
		},
		{
			name: "match host port 0",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Ports: []corev1.ContainerPort{
									{
										HostIP: "0.0.0.0",
									},
								},
							},
						},
					},
				},
				r: &reservationInfo{
					Port: framework.HostPortInfo{
						"192.168.0.1": map[framework.ProtocolPort]struct{}{},
					},
				},
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := matchReservationPort(tt.args.pod, tt.args.r)
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
			got := matchReservationOwners(tt.args.pod, tt.args.r)
			assert.Equal(t, tt.want, got)
		})
	}
}
