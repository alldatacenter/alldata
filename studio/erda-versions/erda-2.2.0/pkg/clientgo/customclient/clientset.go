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

package customclient

import (
	configv1alpha2 "istio.io/client-go/pkg/clientset/versioned/typed/config/v1alpha2"
	netv1alpha3 "istio.io/client-go/pkg/clientset/versioned/typed/networking/v1alpha3"
	netv1beta1 "istio.io/client-go/pkg/clientset/versioned/typed/networking/v1beta1"
	rbacv1alpha1 "istio.io/client-go/pkg/clientset/versioned/typed/rbac/v1alpha1"
	secv1beta1 "istio.io/client-go/pkg/clientset/versioned/typed/security/v1beta1"
	"k8s.io/client-go/discovery"

	erdaconfigv1alpha2 "github.com/erda-project/erda/pkg/clientgo/clientset/versioned/typed/config/v1alpha2"
	flinkoperatorv1beta1 "github.com/erda-project/erda/pkg/clientgo/clientset/versioned/typed/flinkoperator/v1beta1"
	erdanetworkingv1alpha3 "github.com/erda-project/erda/pkg/clientgo/clientset/versioned/typed/networking/v1alpha3"
	erdanetworkingv1beta1 "github.com/erda-project/erda/pkg/clientgo/clientset/versioned/typed/networking/v1beta1"
	openYurtV1alpha1 "github.com/erda-project/erda/pkg/clientgo/clientset/versioned/typed/openyurt/v1alpha1"
	erdarbacv1alpha1 "github.com/erda-project/erda/pkg/clientgo/clientset/versioned/typed/rbac/v1alpha1"
	erdasecurityv1beta1 "github.com/erda-project/erda/pkg/clientgo/clientset/versioned/typed/security/v1beta1"
	erdadiscovery "github.com/erda-project/erda/pkg/clientgo/discovery"
)

// Clientset contains the clients for groups. Each group has exactly one
// version included in a Clientset.
type Clientset struct {
	*discovery.DiscoveryClient
	flinkoperatorV1beta1 *flinkoperatorv1beta1.FlinkoperatorV1beta1Client
	openYurtV1alpha1     *openYurtV1alpha1.AppsV1alpha1Client
	configV1alpha2       *configv1alpha2.ConfigV1alpha2Client
	networkingV1alpha3   *netv1alpha3.NetworkingV1alpha3Client
	networkingV1beta1    *netv1beta1.NetworkingV1beta1Client
	rbacV1alpha1         *rbacv1alpha1.RbacV1alpha1Client
	securityV1beta1      *secv1beta1.SecurityV1beta1Client
}

// FlinkoperatorV1beta1 retrieves the FlinkoperatorV1beta1Client
func (c *Clientset) FlinkoperatorV1beta1() flinkoperatorv1beta1.FlinkoperatorV1beta1Interface {
	return c.flinkoperatorV1beta1
}

// Open retrieves the OpenYurt AppsV1alpha1Client
func (c *Clientset) OpenYurtV1alpha1() openYurtV1alpha1.AppsV1alpha1Interface {
	return c.openYurtV1alpha1
}

// Discovery retrieves the DiscoveryClient
func (c *Clientset) Discovery() discovery.DiscoveryInterface {
	if c == nil {
		return nil
	}
	return c.DiscoveryClient
}

func (c *Clientset) ConfigV1alpha2() configv1alpha2.ConfigV1alpha2Interface {
	return c.configV1alpha2
}

func (c *Clientset) NetworkingV1alpha3() netv1alpha3.NetworkingV1alpha3Interface {
	return c.networkingV1alpha3
}

func (c *Clientset) NetworkingV1beta1() netv1beta1.NetworkingV1beta1Interface {
	return c.networkingV1beta1
}

func (c *Clientset) RbacV1alpha1() rbacv1alpha1.RbacV1alpha1Interface {
	return c.rbacV1alpha1
}

func (c *Clientset) SecurityV1beta1() secv1beta1.SecurityV1beta1Interface {
	return c.securityV1beta1
}

// NewCustomClientSet creates a new Clientset for the given addr.
func NewCustomClientSet(addr string) (*Clientset, error) {
	var cs Clientset
	var err error
	cs.flinkoperatorV1beta1, err = flinkoperatorv1beta1.NewFlinkOpeartorClient(addr)
	if err != nil {
		return nil, err
	}

	cs.openYurtV1alpha1, err = openYurtV1alpha1.NewOpenYurtClient(addr)
	if err != nil {
		return nil, err
	}

	cs.networkingV1alpha3, err = erdanetworkingv1alpha3.NewNetworkingClient(addr)
	if err != nil {
		return nil, err
	}

	cs.networkingV1beta1, err = erdanetworkingv1beta1.NewNetworkingClient(addr)
	if err != nil {
		return nil, err
	}

	cs.rbacV1alpha1, err = erdarbacv1alpha1.NewRBACClient(addr)
	if err != nil {
		return nil, err
	}

	cs.configV1alpha2, err = erdaconfigv1alpha2.NewConfigClient(addr)
	if err != nil {
		return nil, err
	}

	cs.securityV1beta1, err = erdasecurityv1beta1.NewSecurityClient(addr)
	if err != nil {
		return nil, err
	}

	cs.DiscoveryClient, err = erdadiscovery.NewDiscoveryClient(addr)
	if err != nil {
		return nil, err
	}

	return &cs, nil
}
