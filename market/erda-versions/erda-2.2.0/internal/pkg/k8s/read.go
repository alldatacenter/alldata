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
	"bytes"
	"encoding/json"
	"fmt"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	extensionsv1beta1 "k8s.io/api/extensions/v1beta1"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

// ReadDaemonSet 通过 API Server 地址获取 DaemonSet
func ReadDaemonSet(as, namespace, name string) (ds appsv1.DaemonSet, err error) {
	var b bytes.Buffer
	resp, err := httpclient.New().
		Get(as).
		Path("/apis/apps/v1/namespaces/" + namespace + "/daemonsets/" + name).
		Do().Body(&b)
	if err != nil {
		return ds, fmt.Errorf("failed to read daemonset, namespace: %s, name: %s, (%s)\n\t%s",
			namespace, name, err.Error(), b.String())
	}
	if resp.IsNotfound() {
		return ds, ErrNotFound
	}
	if !resp.IsOK() {
		return ds, fmt.Errorf("failed to read daemonset, namespace: %s, name: %s, (status code is %d)\n\t%s", namespace, name, resp.StatusCode(), b.String())
	}
	err = json.Unmarshal(b.Bytes(), &ds)
	if err != nil {
		return ds, fmt.Errorf("failed to read daemonset, namespace: %s, name: %s, (%s)", namespace, name, err.Error())
	}
	return ds, nil
}

// ReadDeployment 通过 API Server 地址获取 Deployment
func ReadDeployment(as, namespace, name string) (deploy appsv1.Deployment, err error) {
	var b bytes.Buffer
	resp, err := httpclient.New().
		Get(as).
		Path("/apis/apps/v1/namespaces/" + namespace + "/deployments/" + name).
		Do().Body(&b)
	if err != nil {
		return deploy, fmt.Errorf("failed to read deployment, namespace: %s, name: %s, (%s)\n\t%s",
			namespace, name, err.Error(), b.String())
	}
	if resp.IsNotfound() {
		return deploy, ErrNotFound
	}
	if !resp.IsOK() {
		return deploy, fmt.Errorf("failed to read deployment, namespace: %s, name: %s, (status code is %d)\n\t%s", namespace, name, resp.StatusCode(), b.String())
	}
	err = json.Unmarshal(b.Bytes(), &deploy)
	if err != nil {
		return deploy, fmt.Errorf("failed to read deployment, namespace: %s, name: %s, (%s)", namespace, name, err.Error())
	}
	return deploy, nil
}

// ReadService 通过 API Server 地址获取 Service
func ReadService(as, namespace, name string) (svc corev1.Service, err error) {
	var b bytes.Buffer
	resp, err := httpclient.New().
		Get(as).
		Path("/api/v1/namespaces/" + namespace + "/services/" + name).
		Do().Body(&b)
	if err != nil {
		return svc, fmt.Errorf("failed to read service, namespace: %s, name: %s, (%s)\n\t%s",
			namespace, name, err.Error(), b.String())
	}
	if resp.IsNotfound() {
		return svc, ErrNotFound
	}
	if !resp.IsOK() {
		return svc, fmt.Errorf("failed to read service, namespace: %s, name: %s, (status code is %d)\n\t%s", namespace, name, resp.StatusCode(), b.String())
	}
	err = json.Unmarshal(b.Bytes(), &svc)
	if err != nil {
		return svc, fmt.Errorf("failed to read service, namespace: %s, name: %s, (%s)", namespace, name, err.Error())
	}
	return svc, nil
}

// ReadIngress 通过 API Server 地址获取 Ingress
func ReadIngress(as, namespace, name string) (ingress extensionsv1beta1.Ingress, err error) {
	var b bytes.Buffer
	resp, err := httpclient.New().
		Get(as).
		Path("/apis/extensions/v1beta1/namespaces/" + namespace + "/ingresses/" + name).
		Do().Body(&b)
	if err != nil {
		return ingress, fmt.Errorf("failed to read ingress, namespace: %s, name: %s, (%s)\n\t%s",
			namespace, name, err.Error(), b.String())
	}
	if resp.IsNotfound() {
		return ingress, ErrNotFound
	}
	if !resp.IsOK() {
		return ingress, fmt.Errorf("failed to read ingress, namespace: %s, name: %s, (status code is %d)\n\t%s", namespace, name, resp.StatusCode(), b.String())
	}
	err = json.Unmarshal(b.Bytes(), &ingress)
	if err != nil {
		return ingress, fmt.Errorf("failed to read ingress, namespace: %s, name: %s, (%s)", namespace, name, err.Error())
	}
	return ingress, nil
}
