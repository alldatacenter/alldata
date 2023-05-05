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
	netv1beta1_api "istio.io/client-go/pkg/apis/networking/v1beta1"
	netv1beta1 "istio.io/client-go/pkg/clientset/versioned/typed/networking/v1beta1"
	"k8s.io/client-go/rest"

	"github.com/erda-project/erda/pkg/clientgo/restclient"
)

// NewNetworkingClient creates a new NetworkingV1beta1 for the given addr.
func NewNetworkingClient(addr string) (*netv1beta1.NetworkingV1beta1Client, error) {
	config := restclient.GetDefaultConfig("")
	config.GroupVersion = &netv1beta1_api.SchemeGroupVersion
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
	return netv1beta1.New(client), nil
}
