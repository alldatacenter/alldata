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

package v1alpha1

import (
	rbacv1alpha1_api "istio.io/client-go/pkg/apis/rbac/v1alpha1"
	rbacv1alpha1 "istio.io/client-go/pkg/clientset/versioned/typed/rbac/v1alpha1"
	"k8s.io/client-go/rest"

	"github.com/erda-project/erda/pkg/clientgo/restclient"
)

func NewRBACClient(addr string) (*rbacv1alpha1.RbacV1alpha1Client, error) {
	config := restclient.GetDefaultConfig("")
	config.GroupVersion = &rbacv1alpha1_api.SchemeGroupVersion
	var client rest.Interface
	var err error
	if addr != "" {
		client, err = restclient.NewInetRESTClient(addr, config)
	} else {
		client, err = rest.RESTClientFor(config)
	}
	if err != nil {
		return nil, err
	}
	return rbacv1alpha1.New(client), nil
}
