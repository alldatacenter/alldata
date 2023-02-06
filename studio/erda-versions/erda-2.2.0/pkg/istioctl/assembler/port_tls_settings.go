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
	"strings"

	"istio.io/api/networking/v1alpha3"
	"istio.io/api/security/v1beta1"

	"github.com/erda-project/erda/apistructs"
)

func NewPortTlsSettings(svc *apistructs.Service) (drSettings []*v1alpha3.TrafficPolicy_PortTrafficPolicy, paSettings map[uint32]*v1beta1.PeerAuthentication_MutualTLS) {
	paSettings = map[uint32]*v1beta1.PeerAuthentication_MutualTLS{}
	for _, port := range svc.Ports {
		if !strings.EqualFold(port.Protocol, "http") && !strings.EqualFold(port.Protocol, "grpc") {
			drSettings = append(drSettings, &v1alpha3.TrafficPolicy_PortTrafficPolicy{
				Port: &v1alpha3.PortSelector{
					Number: uint32(port.Port),
				},
				Tls: &v1alpha3.ClientTLSSettings{
					Mode: v1alpha3.ClientTLSSettings_DISABLE,
				},
			})
			paSettings[uint32(port.Port)] = &v1beta1.PeerAuthentication_MutualTLS{
				Mode: v1beta1.PeerAuthentication_MutualTLS_DISABLE,
			}
		}
	}
	return
}
