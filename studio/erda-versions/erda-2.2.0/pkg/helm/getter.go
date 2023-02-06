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

package helm

import (
	"os"

	"k8s.io/apimachinery/pkg/api/meta"
	"k8s.io/client-go/discovery"
	"k8s.io/client-go/discovery/cached/memory"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/restmapper"
	"k8s.io/client-go/tools/clientcmd"
)

// RESTClientGetterImpl impl genericclioptions.RESTClientGetter
type RESTClientGetterImpl struct {
	rc *rest.Config
}

// NewRESTClientGetterImpl new RESTClientGetterImpl
func NewRESTClientGetterImpl(rc *rest.Config) *RESTClientGetterImpl {
	return &RESTClientGetterImpl{
		rc: rc,
	}
}

func (r *RESTClientGetterImpl) ToRESTConfig() (*rest.Config, error) {
	return r.rc, nil
}

func (r *RESTClientGetterImpl) ToDiscoveryClient() (discovery.CachedDiscoveryInterface, error) {
	if dc, err := discovery.NewDiscoveryClientForConfig(r.rc); err != nil {
		return nil, err
	} else {
		return memory.NewMemCacheClient(dc), nil
	}
}

func (r *RESTClientGetterImpl) ToRESTMapper() (meta.RESTMapper, error) {
	dc, err := r.ToDiscoveryClient()
	if err != nil {
		return nil, err
	}

	mapper := restmapper.NewDeferredDiscoveryRESTMapper(dc)

	return restmapper.NewShortcutExpander(mapper, dc), nil
}

func (r *RESTClientGetterImpl) ToRawKubeConfigLoader() clientcmd.ClientConfig {
	loadingRules := clientcmd.NewDefaultClientConfigLoadingRules()
	loadingRules.DefaultClientConfig = &clientcmd.DefaultClientConfig

	overrides := &clientcmd.ConfigOverrides{ClusterDefaults: clientcmd.ClusterDefaults}

	// e.g. helm.sh/helm/v3/pkg/cli/environment.go
	overrides.Context.Namespace = os.Getenv(EnvHelmNamespace)

	return clientcmd.NewNonInteractiveDeferredLoadingClientConfig(loadingRules, overrides)
}
