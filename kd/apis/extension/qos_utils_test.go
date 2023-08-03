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
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestGetPodQoSClass(t *testing.T) {
	tests := []struct {
		name string
		arg  *corev1.Pod
		want QoSClass
	}{
		{
			name: "koord qos class not specified",
			arg:  &corev1.Pod{},
			want: QoSNone,
		},
		{
			name: "koord qos class not specified 1",
			arg: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{},
				},
			},
			want: QoSNone,
		},
		{
			name: "qos LS",
			arg: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						LabelPodQoS: string(QoSLS),
					},
				},
			},
			want: QoSLS,
		},
		{
			name: "qos BE",
			arg: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						LabelPodQoS: string(QoSBE),
					},
				},
			},
			want: QoSBE,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := GetPodQoSClass(tt.arg)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestGetQoSClassByAttrs(t *testing.T) {
	type args struct {
		labels      map[string]string
		annotations map[string]string
	}
	tests := []struct {
		name string
		args args
		want QoSClass
	}{
		{
			name: "koord qos class not specified",
			args: args{
				labels: map[string]string{},
			},
			want: QoSNone,
		},
		{
			name: "qos BE",
			args: args{
				labels: map[string]string{
					LabelPodQoS: string(QoSBE),
				},
			},
			want: QoSBE,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := GetQoSClassByAttrs(tt.args.labels, tt.args.annotations)
			assert.Equal(t, tt.want, got)
		})
	}
}
