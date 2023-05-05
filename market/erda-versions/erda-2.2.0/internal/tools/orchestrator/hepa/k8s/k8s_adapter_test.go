// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package k8s

import (
	"context"
	"testing"

	"k8s.io/api/networking/v1beta1"
	"k8s.io/apimachinery/pkg/api/errors"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/watch"
	"k8s.io/client-go/kubernetes"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	"github.com/erda-project/erda/pkg/k8s/union_interface"
)

type ingressHelper struct{}

func (ingressHelper) Ingresses(namespace string) union_interface.IngressInterface {
	return ingressInterface{}
}
func (ingressHelper) NewIngress(union_interface.IngressMaterial) interface{} {
	return nil
}
func (ingressHelper) IngressAnnotationBatchSet(ingress interface{}, kvs map[string]string) error {
	return nil
}
func (ingressHelper) IngressAnnotationSet(ingress interface{}, key, value string) error {
	return nil
}
func (ingressHelper) IngressAnnotationBatchGet(ingress interface{}) (map[string]string, error) {
	return nil, nil
}
func (ingressHelper) IngressAnnotationGet(ingress interface{}, key string) (string, error) {
	return "", nil
}
func (ingressHelper) IngressAnnotationClear(ingress interface{}, key string) error {
	return nil
}

type ingressInterface struct{}

func (ingressInterface) Create(ctx context.Context, ingress interface{}, opts v1.CreateOptions) (interface{}, error) {
	return nil, nil
}
func (ingressInterface) Update(ctx context.Context, ingress interface{}, opts v1.UpdateOptions) (interface{}, error) {
	return nil, nil
}
func (ingressInterface) UpdateStatus(ctx context.Context, ingress interface{}, opts v1.UpdateOptions) (interface{}, error) {
	return nil, nil
}
func (ingressInterface) Delete(ctx context.Context, name string, opts v1.DeleteOptions) error {
	return nil
}
func (ingressInterface) DeleteCollection(ctx context.Context, opts v1.DeleteOptions, listOpts v1.ListOptions) error {
	return nil
}
func (ingressInterface) Get(ctx context.Context, name string, opts v1.GetOptions) (interface{}, error) {
	if name == "testnotfound" {
		return nil, errors.NewNotFound(v1beta1.Resource("ingresses"), "testnotfound")
	}
	return nil, nil
}
func (ingressInterface) List(ctx context.Context, opts v1.ListOptions) (interface{}, error) {
	return nil, nil
}
func (ingressInterface) Watch(ctx context.Context, opts v1.ListOptions) (watch.Interface, error) {
	return nil, nil
}
func (ingressInterface) Patch(ctx context.Context, name string, pt types.PatchType, data []byte, opts v1.PatchOptions, subresources ...string) (interface{}, error) {
	return nil, nil
}
func (ingressInterface) Apply(ctx context.Context, ingress interface{}, opts v1.ApplyOptions) (interface{}, error) {
	return nil, nil
}
func (ingressInterface) ApplyStatus(ctx context.Context, ingress interface{}, opts v1.ApplyOptions) (interface{}, error) {
	return nil, nil
}

func TestK8SAdapterImpl_DeleteIngress(t *testing.T) {
	type fields struct {
		client          *kubernetes.Clientset
		ingressesHelper union_interface.IngressesHelper
		pool            *util.GPool
	}
	type args struct {
		namespace string
		name      string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			"case1",
			fields{nil, ingressHelper{}, nil},
			args{"test", "testNotFound"},
			false,
		},
		{
			"case2",
			fields{nil, ingressHelper{}, nil},
			args{"test", "testExists"},
			false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			impl := &K8SAdapterImpl{
				client:          tt.fields.client,
				ingressesHelper: tt.fields.ingressesHelper,
				pool:            tt.fields.pool,
			}
			if err := impl.DeleteIngress(tt.args.namespace, tt.args.name); (err != nil) != tt.wantErr {
				t.Errorf("K8SAdapterImpl.DeleteIngress() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestK8SAdapterImpl_CreateOrUpdateIngress(t *testing.T) {
	type fields struct {
		client          *kubernetes.Clientset
		ingressesHelper union_interface.IngressesHelper
		pool            *util.GPool
	}
	type args struct {
		namespace string
		name      string
		routes    []IngressRoute
		backend   IngressBackend
		options   []RouteOptions
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    bool
		wantErr bool
	}{
		{
			"case1",
			fields{nil, ingressHelper{}, nil},
			args{"test", "testNotFound", nil, IngressBackend{}, nil},
			false,
			false,
		},
		{
			"case2",
			fields{nil, ingressHelper{}, nil},
			args{"test", "testExists", nil, IngressBackend{}, nil},
			true,
			false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			impl := &K8SAdapterImpl{
				client:          tt.fields.client,
				ingressesHelper: tt.fields.ingressesHelper,
				pool:            tt.fields.pool,
			}
			got, err := impl.CreateOrUpdateIngress(tt.args.namespace, tt.args.name, tt.args.routes, tt.args.backend, tt.args.options...)
			if (err != nil) != tt.wantErr {
				t.Errorf("K8SAdapterImpl.CreateOrUpdateIngress() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("K8SAdapterImpl.CreateOrUpdateIngress() = %v, want %v", got, tt.want)
			}
		})
	}
}
