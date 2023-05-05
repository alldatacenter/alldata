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
	"k8s.io/api/extensions/v1beta1"
	extensionsv1beta1 "k8s.io/client-go/kubernetes/typed/extensions/v1beta1"
	"k8s.io/client-go/rest"

	"github.com/erda-project/erda/pkg/clientgo/clientset/versioned/scheme"
	"github.com/erda-project/erda/pkg/clientgo/restclient"
)

func NewExtensionsV1beta1Client(addr string) (*extensionsv1beta1.ExtensionsV1beta1Client, error) {

	var (
		client rest.Interface
		err    error
		config *rest.Config
	)
	if addr != "" {
		config = restclient.GetDefaultConfig("")
		config.GroupVersion = &v1beta1.SchemeGroupVersion
		client, err = restclient.NewInetRESTClient(addr, config)
	} else {
		config, err = rest.InClusterConfig()
		if err != nil {
			return nil, err
		}
		config.APIPath = "/apis"
		config.GroupVersion = &v1beta1.SchemeGroupVersion
		config.NegotiatedSerializer = scheme.Codecs.WithoutConversion()
		rest.SetKubernetesDefaults(config)
		client, err = rest.RESTClientFor(config)
	}
	if err != nil {
		return nil, err
	}
	return extensionsv1beta1.New(client), nil
}
