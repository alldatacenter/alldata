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

package assembler

import (
	securityv1beta1 "istio.io/api/security/v1beta1"
	typev1beta1 "istio.io/api/type/v1beta1"
	"istio.io/client-go/pkg/apis/security/v1beta1"

	"github.com/erda-project/erda/apistructs"
)

func NewPeerAuthentication(svc *apistructs.Service) *v1beta1.PeerAuthentication {
	result := &v1beta1.PeerAuthentication{}
	result.Name = svc.Name
	result.Spec.Mtls = &securityv1beta1.PeerAuthentication_MutualTLS{
		Mode: securityv1beta1.PeerAuthentication_MutualTLS_STRICT,
	}
	result.Spec.Selector = &typev1beta1.WorkloadSelector{
		MatchLabels: map[string]string{
			"app": svc.Name,
		},
	}
	return result
}
