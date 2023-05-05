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

package union_interface

import (
	"context"

	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/watch"
)

type MatchType int

const (
	PrefixMatch MatchType = 0
	ExactMatch  MatchType = 1
)

type IngressRoute struct {
	Domain    string
	Path      string
	MatchType MatchType
}

type IngressBackend struct {
	ServiceName string
	ServicePort int
}

type IngressMaterial struct {
	Name      string
	Namespace string
	Routes    []IngressRoute
	Backend   IngressBackend
	NeedTLS   bool
}

type IngressesHelper interface {
	Ingresses(namespace string) IngressInterface
	NewIngress(IngressMaterial) interface{}
	IngressAnnotationBatchSet(ingress interface{}, kvs map[string]string) error
	IngressAnnotationSet(ingress interface{}, key, value string) error
	IngressAnnotationBatchGet(ingress interface{}) (map[string]string, error)
	IngressAnnotationGet(ingress interface{}, key string) (string, error)
	IngressAnnotationClear(ingress interface{}, key string) error
}

type IngressInterface interface {
	Create(ctx context.Context, ingress interface{}, opts v1.CreateOptions) (interface{}, error)
	Update(ctx context.Context, ingress interface{}, opts v1.UpdateOptions) (interface{}, error)
	UpdateStatus(ctx context.Context, ingress interface{}, opts v1.UpdateOptions) (interface{}, error)
	Delete(ctx context.Context, name string, opts v1.DeleteOptions) error
	DeleteCollection(ctx context.Context, opts v1.DeleteOptions, listOpts v1.ListOptions) error
	Get(ctx context.Context, name string, opts v1.GetOptions) (interface{}, error)
	List(ctx context.Context, opts v1.ListOptions) (interface{}, error)
	Watch(ctx context.Context, opts v1.ListOptions) (watch.Interface, error)
	Patch(ctx context.Context, name string, pt types.PatchType, data []byte, opts v1.PatchOptions, subresources ...string) (result interface{}, err error)
	Apply(ctx context.Context, ingress interface{}, opts v1.ApplyOptions) (result interface{}, err error)
	ApplyStatus(ctx context.Context, ingress interface{}, opts v1.ApplyOptions) (result interface{}, err error)
}
