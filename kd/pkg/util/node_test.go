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
	"reflect"
	"testing"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
)

func TestGetNodeAddress(t *testing.T) {
	type args struct {
		node     *corev1.Node
		addrType corev1.NodeAddressType
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "InternalIP",
			args: args{
				node: &corev1.Node{
					Status: corev1.NodeStatus{
						Addresses: []corev1.NodeAddress{
							{Type: corev1.NodeInternalIP, Address: "192.168.1.1"},
						},
					},
				},
				addrType: corev1.NodeInternalIP,
			},
			want:    "192.168.1.1",
			wantErr: false,
		},
		{
			name: "Hostname",
			args: args{
				node: &corev1.Node{
					Status: corev1.NodeStatus{
						Addresses: []corev1.NodeAddress{
							{Type: corev1.NodeHostName, Address: "node1"},
						},
					},
				},
				addrType: corev1.NodeHostName,
			},
			want:    "node1",
			wantErr: false,
		},
		{
			name: "Empty",
			args: args{
				node: &corev1.Node{
					Status: corev1.NodeStatus{
						Addresses: []corev1.NodeAddress{
							{Type: corev1.NodeInternalIP, Address: "192.168.1.1"},
							{Type: corev1.NodeHostName, Address: "node1"},
						},
					},
				},
				addrType: corev1.NodeExternalDNS,
			},
			want:    "",
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetNodeAddress(tt.args.node, tt.args.addrType)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetNodeAddress() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("GetNodeAddress() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsNodeAddressTypeSupported(t *testing.T) {
	type args struct {
		addrType corev1.NodeAddressType
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{name: "Hostname", args: args{addrType: corev1.NodeHostName}, want: true},
		{name: "InternalIP", args: args{addrType: corev1.NodeInternalIP}, want: true},
		{name: "InternalDNS", args: args{addrType: corev1.NodeInternalDNS}, want: true},
		{name: "ExternalIP", args: args{addrType: corev1.NodeExternalIP}, want: true},
		{name: "ExternalDNS", args: args{addrType: corev1.NodeExternalDNS}, want: true},
		{name: "EmptyAddress", args: args{addrType: corev1.NodeAddressType("EmptyAddress")}, want: false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsNodeAddressTypeSupported(tt.args.addrType); got != tt.want {
				t.Errorf("IsAddressTypeSupported() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetNodeReservationFromAnnotation(t *testing.T) {
	type args struct {
		anno map[string]string
	}
	tests := []struct {
		name string
		args args
		want corev1.ResourceList
	}{
		// TODO: Add test cases.
		{
			name: "reserve nothing",
			args: args{},
			want: nil,
		},
		{
			name: "reserve cpu only by quantity",
			args: args{map[string]string{
				apiext.AnnotationNodeReservation: GetNodeAnnoReservedJson(apiext.NodeReservation{
					Resources: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("10")},
				})}},
			want: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("10")},
		},
		{
			name: "reserve cpu only by specific cpus",
			args: args{map[string]string{
				apiext.AnnotationNodeReservation: GetNodeAnnoReservedJson(apiext.NodeReservation{
					ReservedCPUs: "0-1",
				})}},
			want: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("2")},
		},
		{
			name: "reserve cpu by specific cpus and quantity",
			args: args{map[string]string{
				apiext.AnnotationNodeReservation: GetNodeAnnoReservedJson(apiext.NodeReservation{
					Resources:    corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("10")},
					ReservedCPUs: "0-1",
				})}},
			want: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("2")},
		},
		{
			name: "reserve memory by quantity",
			args: args{map[string]string{
				apiext.AnnotationNodeReservation: GetNodeAnnoReservedJson(apiext.NodeReservation{
					Resources: corev1.ResourceList{corev1.ResourceMemory: resource.MustParse("10")},
				})}},
			want: corev1.ResourceList{corev1.ResourceMemory: resource.MustParse("10")},
		},
		{
			name: "reserve memory and cpu by quantity",
			args: args{map[string]string{
				apiext.AnnotationNodeReservation: GetNodeAnnoReservedJson(apiext.NodeReservation{
					Resources: corev1.ResourceList{
						corev1.ResourceMemory: resource.MustParse("10"),
						corev1.ResourceCPU:    resource.MustParse("10"),
					},
				})}},
			want: corev1.ResourceList{
				corev1.ResourceMemory: resource.MustParse("10"),
				corev1.ResourceCPU:    resource.MustParse("10"),
			},
		},
		{
			name: "reserve memory by quantity and reserve cpu by specific cpus",
			args: args{map[string]string{
				apiext.AnnotationNodeReservation: GetNodeAnnoReservedJson(apiext.NodeReservation{
					Resources: corev1.ResourceList{
						corev1.ResourceMemory: resource.MustParse("10"),
					},
					ReservedCPUs: "0-1",
				})}},
			want: corev1.ResourceList{
				corev1.ResourceMemory: resource.MustParse("10"),
				corev1.ResourceCPU:    resource.MustParse("2"),
			},
		},
		{
			name: "reserve memory by quantity, reserve cpu by specific cpus and quantity",
			args: args{map[string]string{
				apiext.AnnotationNodeReservation: GetNodeAnnoReservedJson(apiext.NodeReservation{
					Resources: corev1.ResourceList{
						corev1.ResourceMemory: resource.MustParse("10"),
						corev1.ResourceCPU:    resource.MustParse("5"),
					},
					ReservedCPUs: "0-1",
				})}},
			want: corev1.ResourceList{
				corev1.ResourceMemory: resource.MustParse("10"),
				corev1.ResourceCPU:    resource.MustParse("2"),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetNodeReservationFromAnnotation(tt.args.anno); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetNodeReservationFromAnnotation() = %v, want %v", got, tt.want)
			}
		})
	}
}
