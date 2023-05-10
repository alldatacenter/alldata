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

package steve

import (
	"context"

	"github.com/rancher/wrangler/pkg/generated/controllers/apiextensions.k8s.io"
	apiextensionsv1 "github.com/rancher/wrangler/pkg/generated/controllers/apiextensions.k8s.io/v1"
	"github.com/rancher/wrangler/pkg/generated/controllers/apiregistration.k8s.io"
	apiregistrationv1 "github.com/rancher/wrangler/pkg/generated/controllers/apiregistration.k8s.io/v1"
	"github.com/rancher/wrangler/pkg/generated/controllers/core"
	corev1 "github.com/rancher/wrangler/pkg/generated/controllers/core/v1"
	"github.com/rancher/wrangler/pkg/generated/controllers/rbac"
	rbacv1 "github.com/rancher/wrangler/pkg/generated/controllers/rbac/v1"
	"github.com/rancher/wrangler/pkg/generic"
	"github.com/rancher/wrangler/pkg/start"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
)

type Controllers struct {
	RESTConfig *rest.Config
	K8s        kubernetes.Interface
	Core       corev1.Interface
	RBAC       rbacv1.Interface
	API        apiregistrationv1.Interface
	CRD        apiextensionsv1.Interface
	starters   []start.Starter
}

func (c *Controllers) Start(ctx context.Context) error {
	return start.All(ctx, 5, c.starters...)
}

func NewController(cfg *rest.Config, opts *generic.FactoryOptions) (*Controllers, error) {
	c := &Controllers{}

	core, err := core.NewFactoryFromConfigWithOptions(cfg, opts)
	if err != nil {
		return nil, err
	}
	c.starters = append(c.starters, core)

	rbac, err := rbac.NewFactoryFromConfigWithOptions(cfg, opts)
	if err != nil {
		return nil, err
	}
	c.starters = append(c.starters, rbac)

	api, err := apiregistration.NewFactoryFromConfigWithOptions(cfg, opts)
	if err != nil {
		return nil, err
	}
	c.starters = append(c.starters, api)

	crd, err := apiextensions.NewFactoryFromConfigWithOptions(cfg, opts)
	if err != nil {
		return nil, err
	}
	//c.starters = append(c.starters, crd)

	c.K8s, err = kubernetes.NewForConfig(cfg)
	if err != nil {
		return nil, err
	}
	c.Core = core.Core().V1()
	c.RBAC = rbac.Rbac().V1()
	c.API = api.Apiregistration().V1()
	c.CRD = crd.Apiextensions().V1()
	c.RESTConfig = cfg

	return c, nil
}
