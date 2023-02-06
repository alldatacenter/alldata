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
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	extensionsv1beta1 "k8s.io/api/extensions/v1beta1"
)

// ReplaceOrCreateDaemonSet 通过 API Server 地址创建 DaemonSet
func ReplaceOrCreateDaemonSet(as string, ds appsv1.DaemonSet) error {
	_, err := ReadDaemonSet(as, ds.Namespace, ds.Name)
	if err == ErrNotFound {
		return CreateDaemonSet(as, ds)
	}
	if err != nil {
		return err
	}
	return ReplaceDaemonSet(as, ds)
}

// ReplaceOrCreateDeployment 通过 API Server 地址创建 Deployment
func ReplaceOrCreateDeployment(as string, deploy appsv1.Deployment) error {
	_, err := ReadDeployment(as, deploy.Namespace, deploy.Name)
	if err == ErrNotFound {
		return CreateDeployment(as, deploy)
	}
	if err != nil {
		return err
	}
	return ReplaceDeployment(as, deploy)
}

// ReplaceOrCreateService 通过 API Server 地址创建 Service
func ReplaceOrCreateService(as string, svc corev1.Service) error {
	old, err := ReadService(as, svc.Namespace, svc.Name)
	if err == ErrNotFound {
		return CreateService(as, svc)
	}
	if err != nil {
		return err
	}
	svc.ObjectMeta.ResourceVersion = old.ObjectMeta.ResourceVersion
	svc.Spec.ClusterIP = old.Spec.ClusterIP
	return ReplaceService(as, svc)
}

// ReplaceOrCreateIngress 通过 API Server 地址创建 Ingress
func ReplaceOrCreateIngress(as string, ingress extensionsv1beta1.Ingress) error {
	_, err := ReadIngress(as, ingress.Namespace, ingress.Name)
	if err == ErrNotFound {
		return CreateIngress(as, ingress)
	}
	if err != nil {
		return err
	}
	return ReplaceIngress(as, ingress)
}
