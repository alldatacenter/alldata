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
	"testing"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
)

func getReservedJson(reserved NodeReservation) string {
	result := ""
	resultBytes, err := json.Marshal(&reserved)
	if err == nil {
		result = string(resultBytes)
	}

	return result
}

func TestGetReservedCPUs(t *testing.T) {
	type args struct {
		anno map[string]string
	}
	tests := []struct {
		name  string
		args  args
		want  string
		want1 int
	}{
		{
			name: "node.annotation is nil",
			args: args{
				nil,
			},
			want:  "",
			want1: 0,
		},
		{
			name: "node.annotation not nil but without cpu reserved",
			args: args{
				map[string]string{
					AnnotationNodeReservation: getReservedJson(NodeReservation{
						Resources:    nil,
						ReservedCPUs: "",
					}),
				},
			},
			want:  "",
			want1: 0,
		},
		{
			name: "reserve cpu only by quantity",
			args: args{
				map[string]string{
					AnnotationNodeReservation: getReservedJson(NodeReservation{
						Resources: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("10")},
					}),
				},
			},
			want:  "",
			want1: 10,
		},
		{
			name: "reserve cpu only by quantity but value not integer",
			args: args{
				map[string]string{
					AnnotationNodeReservation: getReservedJson(NodeReservation{
						Resources: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("2.5")},
					}),
				},
			},
			want:  "",
			want1: 3,
		},
		{
			name: "reserve cpu only by quantity but value is negative",
			args: args{
				map[string]string{
					AnnotationNodeReservation: getReservedJson(NodeReservation{
						Resources: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("-2")},
					}),
				},
			},
			want:  "",
			want1: 0,
		},
		{
			name: "reserve cpu only by specific cpus",
			args: args{
				map[string]string{
					AnnotationNodeReservation: getReservedJson(NodeReservation{
						ReservedCPUs: "0-1",
					}),
				},
			},
			want:  "0-1",
			want1: 0,
		},
		{
			name: "reserve cpu only by specific cpus but core id is unavailable",
			args: args{
				map[string]string{
					AnnotationNodeReservation: getReservedJson(NodeReservation{
						ReservedCPUs: "-1",
					}),
				},
			},
			want:  "-1",
			want1: 0,
		},
		{
			name: "reserve cpu by specific cpus and quantity",
			args: args{
				map[string]string{
					AnnotationNodeReservation: getReservedJson(NodeReservation{
						Resources:    corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("10")},
						ReservedCPUs: "0-1",
					}),
				},
			},
			want:  "0-1",
			want1: 0,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, got1 := GetReservedCPUs(tt.args.anno)
			if got != tt.want {
				t.Errorf("GetReservedCPUs() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("GetReservedCPUs() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}
