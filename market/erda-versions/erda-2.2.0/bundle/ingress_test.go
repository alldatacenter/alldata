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

package bundle

import (
	"testing"
)

func TestBundle_CreateOrUpdateIngress(t *testing.T) {
	// os.Setenv("HEPA_ADDR", "hepa.default.svc.cluster.local:8080")
	// defer func() {
	// 	os.Unsetenv("HEPA_ADDR")
	// }()
	// logrus.SetOutput(os.Stdout)
	// b := New(WithHepa())
	// path := "/123/$1"
	// err := b.CreateOrUpdateComponentIngress(apistructs.ComponentIngressUpdateRequest{
	// 	ComponentName: "hepa",
	// 	ComponentPort: 8080,
	// 	IngressName:   "hepa-test",
	// 	Routes: []apistructs.IngressRoute{
	// 		{
	// 			Domain: "hepatest.test.terminus.io",
	// 			Path:   "/(.*)",
	// 		},
	// 		{
	// 			Domain: "hepatest2.test.terminus.io",
	// 			Path:   "/(.*)",
	// 		},
	// 	},
	// 	RouteOptions: apistructs.RouteOptions{
	// 		RewritePath: &path,
	// 		UseRegex:    true,
	// 		Annotations: map[string]string{
	// 			"nginx.ingress.kubernetes.io/proxy-body-size": "0",
	// 		},
	// 	},
	// })
	// require.NoError(t, err)
}

func TestBundle_CreateOrUpdateIngressNexusDocker(t *testing.T) {
	// os.Setenv("HEPA_ADDR", "hepa.default.svc.cluster.local:8080")
	// defer func() {
	// 	os.Unsetenv("HEPA_ADDR")
	// }()
	// logrus.SetOutput(os.Stdout)
	// b := New(WithHepa())
	// path := "/repository/docker-hosted-platform/$1"
	// err := b.CreateOrUpdateComponentIngress(apistructs.ComponentIngressUpdateRequest{
	// 	ComponentName: "addon-nexus",
	// 	ComponentPort: 8081,
	// 	IngressName:   "addon-nexus-docker-hosted-platform",
	// 	Routes: []apistructs.IngressRoute{
	// 		{
	// 			Domain: "docker-hosted-nexus-sys.dev.terminus.io",
	// 			Path:   "/(.*)",
	// 		},
	// 	},
	// 	RouteOptions: apistructs.RouteOptions{
	// 		RewritePath: &path,
	// 		UseRegex:    true,
	// 		Annotations: map[string]string{
	// 			"nginx.ingress.kubernetes.io/proxy-body-size": "0",
	// 		},
	// 	},
	// })
	// require.NoError(t, err)
}
