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

package v1beta1

import (
	"context"

	"github.com/pkg/errors"
	. "k8s.io/api/extensions/v1beta1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/intstr"
	"k8s.io/apimachinery/pkg/watch"
	apply "k8s.io/client-go/applyconfigurations/extensions/v1beta1"
	. "k8s.io/client-go/kubernetes/typed/extensions/v1beta1"

	"github.com/erda-project/erda/pkg/k8s/union_interface"
)

type IngressHelper struct {
	client ExtensionsV1beta1Interface
}

func NewIngressHelper(client ExtensionsV1beta1Interface) IngressHelper {
	return IngressHelper{
		client: client,
	}
}

func getInstance(ingress interface{}) (*Ingress, error) {
	instance, ok := ingress.(*Ingress)
	if !ok {
		return nil, errors.Errorf("invalid ingress api type: %+v", ingress)
	}
	return instance, nil
}

func (ing IngressHelper) NewIngress(material union_interface.IngressMaterial) interface{} {
	ingress := &Ingress{}
	ingress.Name = material.Name
	ingress.Namespace = material.Namespace
	ingressBackend := IngressBackend{
		ServiceName: material.Backend.ServiceName,
		ServicePort: intstr.FromInt(material.Backend.ServicePort),
	}
	var domains []string
	for _, route := range material.Routes {
		domains = append(domains, route.Domain)
	}
	if material.NeedTLS {
		ingress.Spec.TLS = []IngressTLS{
			{
				Hosts: domains,
			},
		}
	}
	for _, route := range material.Routes {
		ingress.Spec.Rules = append(ingress.Spec.Rules, IngressRule{
			Host: route.Domain,
			IngressRuleValue: IngressRuleValue{
				HTTP: &HTTPIngressRuleValue{
					Paths: []HTTPIngressPath{
						{
							Path:    route.Path,
							Backend: ingressBackend,
						},
					},
				},
			},
		})
	}
	return ingress
}

func (ing IngressHelper) IngressAnnotationBatchSet(ingress interface{}, kvs map[string]string) error {
	instance, err := getInstance(ingress)
	if err != nil {
		return err
	}
	if len(instance.Annotations) == 0 {
		instance.Annotations = map[string]string{}
	}
	for key, value := range kvs {
		instance.Annotations[key] = value
	}
	return nil
}

func (ing IngressHelper) IngressAnnotationBatchGet(ingress interface{}) (map[string]string, error) {
	instance, err := getInstance(ingress)
	if err != nil {
		return nil, err
	}
	return instance.Annotations, nil
}

func (ing IngressHelper) IngressAnnotationGet(ingress interface{}, key string) (string, error) {
	instance, err := getInstance(ingress)
	if err != nil {
		return "", err
	}
	if instance.Annotations == nil {
		return "", nil
	}
	return instance.Annotations[key], nil
}

func (ing IngressHelper) IngressAnnotationSet(ingress interface{}, key, value string) error {
	instance, err := getInstance(ingress)
	if err != nil {
		return err
	}
	if len(instance.Annotations) == 0 {
		instance.Annotations = map[string]string{}
	}
	instance.Annotations[key] = value
	return nil
}

func (ing IngressHelper) IngressAnnotationClear(ingress interface{}, key string) error {
	instance, err := getInstance(ingress)
	if err != nil {
		return err
	}
	delete(instance.Annotations, key)
	return nil
}

func (ing IngressHelper) Ingresses(namespace string) union_interface.IngressInterface {
	return Ingresses{
		inner: ing.client.Ingresses(namespace),
	}
}

type Ingresses struct {
	inner IngressInterface
}

func (ing Ingresses) Create(ctx context.Context, ingress interface{}, opts v1.CreateOptions) (interface{}, error) {
	instance, err := getInstance(ingress)
	if err != nil {
		return nil, err
	}
	return ing.inner.Create(ctx, instance, opts)
}

func (ing Ingresses) Update(ctx context.Context, ingress interface{}, opts v1.UpdateOptions) (interface{}, error) {
	instance, err := getInstance(ingress)
	if err != nil {
		return nil, err
	}
	return ing.inner.Update(ctx, instance, opts)
}

func (ing Ingresses) UpdateStatus(ctx context.Context, ingress interface{}, opts v1.UpdateOptions) (interface{}, error) {
	instance, err := getInstance(ingress)
	if err != nil {
		return nil, err
	}
	return ing.inner.UpdateStatus(ctx, instance, opts)
}

func (ing Ingresses) Delete(ctx context.Context, name string, opts v1.DeleteOptions) error {
	return ing.inner.Delete(ctx, name, opts)
}

func (ing Ingresses) DeleteCollection(ctx context.Context, opts v1.DeleteOptions, listOpts v1.ListOptions) error {
	return ing.inner.DeleteCollection(ctx, opts, listOpts)
}

func (ing Ingresses) Get(ctx context.Context, name string, opts v1.GetOptions) (interface{}, error) {
	return ing.inner.Get(ctx, name, opts)
}

func (ing Ingresses) List(ctx context.Context, opts v1.ListOptions) (interface{}, error) {
	return ing.inner.List(ctx, opts)
}

func (ing Ingresses) Watch(ctx context.Context, opts v1.ListOptions) (watch.Interface, error) {
	return ing.inner.Watch(ctx, opts)
}

func (ing Ingresses) Patch(ctx context.Context, name string, pt types.PatchType, data []byte, opts v1.PatchOptions, subresources ...string) (result interface{}, err error) {
	return ing.inner.Patch(ctx, name, pt, data, opts, subresources...)
}

func (ing Ingresses) Apply(ctx context.Context, ingress interface{}, opts v1.ApplyOptions) (result interface{}, err error) {
	instance, ok := ingress.(*apply.IngressApplyConfiguration)
	if !ok {
		return nil, errors.Errorf("invalid ingress config type: %+v", ingress)
	}
	return ing.inner.Apply(ctx, instance, opts)
}

func (ing Ingresses) ApplyStatus(ctx context.Context, ingress interface{}, opts v1.ApplyOptions) (result interface{}, err error) {
	instance, ok := ingress.(*apply.IngressApplyConfiguration)
	if !ok {
		return nil, errors.Errorf("invalid ingress config type: %+v", ingress)
	}
	return ing.inner.ApplyStatus(ctx, instance, opts)
}
