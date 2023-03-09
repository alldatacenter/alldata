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
	"context"
	"encoding/json"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	clientset "k8s.io/client-go/kubernetes"
	kubefake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/utils/pointer"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordinatorclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	clientschedulingv1alpha1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/typed/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
)

func Test_MergeCfg(t *testing.T) {
	type TestingStruct struct {
		A *int64 `json:"a,omitempty"`
		B *int64 `json:"b,omitempty"`
	}
	type args struct {
		old interface{}
		new interface{}
	}
	tests := []struct {
		name    string
		args    args
		want    interface{}
		wantErr bool
	}{
		{
			name: "throw an error if the inputs' types are not the same",
			args: args{
				old: &TestingStruct{},
				new: pointer.Int64Ptr(1),
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "throw an error if any of the inputs is not a pointer",
			args: args{
				old: TestingStruct{},
				new: TestingStruct{},
			},
			want:    nil,
			wantErr: true,
		},
		{
			name:    "throw an error if any of inputs is nil",
			args:    args{},
			want:    nil,
			wantErr: true,
		},
		{
			name: "throw an error if any of inputs is nil 1",
			args: args{
				old: &TestingStruct{},
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "new is empty",
			args: args{
				old: &TestingStruct{
					A: pointer.Int64Ptr(0),
					B: pointer.Int64Ptr(1),
				},
				new: &TestingStruct{},
			},
			want: &TestingStruct{
				A: pointer.Int64Ptr(0),
				B: pointer.Int64Ptr(1),
			},
		},
		{
			name: "old is empty",
			args: args{
				old: &TestingStruct{},
				new: &TestingStruct{
					B: pointer.Int64Ptr(1),
				},
			},
			want: &TestingStruct{
				B: pointer.Int64Ptr(1),
			},
		},
		{
			name: "both are empty",
			args: args{
				old: &TestingStruct{},
				new: &TestingStruct{},
			},
			want: &TestingStruct{},
		},
		{
			name: "new one overwrites the old one",
			args: args{
				old: &TestingStruct{
					A: pointer.Int64Ptr(0),
					B: pointer.Int64Ptr(1),
				},
				new: &TestingStruct{
					B: pointer.Int64Ptr(2),
				},
			},
			want: &TestingStruct{
				A: pointer.Int64Ptr(0),
				B: pointer.Int64Ptr(2),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, gotErr := MergeCfg(tt.args.old, tt.args.new)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			if !tt.wantErr {
				assert.Equal(t, tt.want, got.(*TestingStruct))
			}
		})
	}
}

func TestMinInt64(t *testing.T) {
	type args struct {
		i int64
		j int64
	}
	tests := []struct {
		name string
		args args
		want int64
	}{
		{
			name: "i < j",
			args: args{
				i: 0,
				j: 1,
			},
			want: 0,
		},
		{
			name: "i > j",
			args: args{
				i: 1,
				j: 0,
			},
			want: 0,
		},
		{
			name: "i = j",
			args: args{
				i: 0,
				j: 0,
			},
			want: 0,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := MinInt64(tt.args.i, tt.args.j); got != tt.want {
				t.Errorf("MinInt64() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMaxInt64(t *testing.T) {
	type args struct {
		i int64
		j int64
	}
	tests := []struct {
		name string
		args args
		want int64
	}{
		{
			name: "i < j",
			args: args{
				i: 0,
				j: 1,
			},
			want: 1,
		},
		{
			name: "i > j",
			args: args{
				i: 1,
				j: 0,
			},
			want: 1,
		},
		{
			name: "i = j",
			args: args{
				i: 0,
				j: 0,
			},
			want: 0,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := MaxInt64(tt.args.i, tt.args.j); got != tt.want {
				t.Errorf("MaxInt64() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_GeneratePodPatch(t *testing.T) {
	pod1 := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{Name: "test-container-1"},
				{Name: "test-container-2"},
			},
		},
	}
	patchAnnotation := map[string]string{"test_case": "Test_GeneratePodPatch"}
	pod2 := pod1.DeepCopy()
	pod2.SetAnnotations(patchAnnotation)
	patchBytes, err := GeneratePodPatch(pod1, pod2)
	if err != nil {
		t.Errorf("error creating patch bytes %v", err)
	}
	var patchMap map[string]interface{}
	err = json.Unmarshal(patchBytes, &patchMap)
	if err != nil {
		t.Errorf("error unmarshalling json patch : %v", err)
	}
	metadata, ok := patchMap["metadata"].(map[string]interface{})
	if !ok {
		t.Errorf("error converting metadata to version map")
	}
	annotation, _ := metadata["annotations"].(map[string]interface{})
	if fmt.Sprint(annotation) != fmt.Sprint(patchAnnotation) {
		t.Errorf("expect patchBytes: %q, got: %q", patchAnnotation, annotation)
	}
}

type fakeReservationClientSet struct {
	koordinatorclientset.Interface
	clientschedulingv1alpha1.SchedulingV1alpha1Interface
	clientschedulingv1alpha1.ReservationInterface
	reservations map[string]*schedulingv1alpha1.Reservation
	patchErr     map[string]bool
}

func (f *fakeReservationClientSet) SchedulingV1alpha1() clientschedulingv1alpha1.SchedulingV1alpha1Interface {
	return f
}

func (f *fakeReservationClientSet) Reservations() clientschedulingv1alpha1.ReservationInterface {
	return f
}

func (f *fakeReservationClientSet) Patch(ctx context.Context, name string, pt types.PatchType, data []byte, opts metav1.PatchOptions, subresources ...string) (result *schedulingv1alpha1.Reservation, err error) {
	if f.patchErr[name] {
		return nil, fmt.Errorf("patch error")
	}
	r, ok := f.reservations[name]
	if !ok {
		return nil, fmt.Errorf("reservation not found")
	}
	return r, nil
}

type fakeExtendedHandle struct {
	client      *kubefake.Clientset
	koordClient *fakeReservationClientSet
	frameworkext.ExtendedHandle
}

func (f *fakeExtendedHandle) ClientSet() clientset.Interface {
	return f.client
}

func (f *fakeExtendedHandle) KoordinatorClientSet() koordinatorclientset.Interface {
	return f.koordClient
}

func TestPatch_PatchPodOrReservation(t *testing.T) {
	testNormalPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-1",
		},
	}
	testR := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation-0",
			UID:  "123456",
		},
	}
	testReservePod := NewReservePod(testR)
	type fields struct {
		handle      framework.Handle
		annotations map[string]string
	}
	type args struct {
		pod *corev1.Pod
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "nothing to patch for normal pod",
			fields: fields{
				handle: &fakeExtendedHandle{
					client: kubefake.NewSimpleClientset(testNormalPod),
				},
			},
			args: args{
				pod: &corev1.Pod{},
			},
			wantErr: false,
		},
		{
			name: "patch successfully for normal pod",
			fields: fields{
				handle: &fakeExtendedHandle{
					client: kubefake.NewSimpleClientset(testNormalPod),
				},
				annotations: map[string]string{
					"aaa": "bbb",
				},
			},
			args: args{
				pod: testNormalPod,
			},
			wantErr: false,
		},
		{
			name: "nothing to patch for reserve pod",
			fields: fields{
				handle: &fakeExtendedHandle{
					koordClient: &fakeReservationClientSet{},
				},
			},
			args: args{
				pod: testReservePod,
			},
			wantErr: false,
		},
		{
			name: "patch successfully for reserve pod",
			fields: fields{
				handle: &fakeExtendedHandle{
					koordClient: &fakeReservationClientSet{
						reservations: map[string]*schedulingv1alpha1.Reservation{
							testR.Name: testR,
						},
					},
				},
				annotations: map[string]string{
					"aaa": "bbb",
				},
			},
			args: args{
				pod: testReservePod,
			},
			wantErr: false,
		},
		{
			name: "patch error for reserve pod",
			fields: fields{
				handle: &fakeExtendedHandle{
					koordClient: &fakeReservationClientSet{
						reservations: map[string]*schedulingv1alpha1.Reservation{
							testR.Name: testR,
						},
						patchErr: map[string]bool{
							testR.Name: true,
						},
					},
				},
				annotations: map[string]string{
					"aaa": "bbb",
				},
			},
			args: args{
				pod: testReservePod,
			},
			wantErr: true,
		},
		{
			name: "patch not found for reserve pod",
			fields: fields{
				handle: &fakeExtendedHandle{
					koordClient: &fakeReservationClientSet{
						reservations: map[string]*schedulingv1alpha1.Reservation{},
					},
				},
				annotations: map[string]string{
					"aaa": "bbb",
				},
			},
			args: args{
				pod: testReservePod,
			},
			wantErr: true,
		},
		{
			name: "missing clientset for reserve pod",
			fields: fields{
				handle: &fakeExtendedHandle{},
				annotations: map[string]string{
					"aaa": "bbb",
				},
			},
			args: args{
				pod: testReservePod,
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, gotErr := NewPatch().WithHandle(tt.fields.handle).AddAnnotations(tt.fields.annotations).PatchPodOrReservation(tt.args.pod)
			assert.Equal(t, tt.wantErr, gotErr != nil)
		})
	}
}
