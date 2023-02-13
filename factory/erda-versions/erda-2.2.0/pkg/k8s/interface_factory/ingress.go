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

package interface_factory

import (
	"errors"

	extensionsv1beta1 "k8s.io/api/extensions/v1beta1"
	v1 "k8s.io/api/networking/v1"
	"k8s.io/client-go/kubernetes"

	erdaextensionsv1beta1 "github.com/erda-project/erda/pkg/k8s/typed/extensions/v1beta1"
	erdav1 "github.com/erda-project/erda/pkg/k8s/typed/networking/v1"
	"github.com/erda-project/erda/pkg/k8s/union_interface"
)

const IngressKind = "Ingress"

func CreateIngressesHelper(client kubernetes.Interface) (union_interface.IngressesHelper, error) {
	// k8s version >=1.19
	exist, err := IsResourceExist(client, IngressKind, v1.SchemeGroupVersion.String())
	if err != nil {
		return nil, err
	}
	if exist {
		return erdav1.NewIngressHelper(client.NetworkingV1()), nil
	}
	// k8s version < 1.19
	exist, err = IsResourceExist(client, IngressKind, extensionsv1beta1.SchemeGroupVersion.String())
	if err != nil {
		return nil, err
	}
	if exist {
		return erdaextensionsv1beta1.NewIngressHelper(client.ExtensionsV1beta1()), nil
	}

	return nil, errors.New("there is no ingress kind")
}
