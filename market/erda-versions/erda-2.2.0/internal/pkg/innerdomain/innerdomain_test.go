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

package innerdomain

//import (
//	"testing"
//
//	"github.com/pkg/errors"
//	"github.com/stretchr/testify/assert"
//)
//
//func TestParse(t *testing.T) {
//	testcases := []struct {
//		domain               string
//		err                  error
//		expectk8sErr         error
//		expectk8sdomain      string
//		expectmarathondomain string
//	}{
//		{
//			"prototype.prod-6056.services.v1.runtimes.marathon.l4lb.thisdcos.directory",
//			nil,
//			ErrNoLegacyK8SAddr,
//			"",
//			"",
//		},
//		{
//			"eventbox.marathon.l4lb.thisdcos.directory",
//			nil,
//			nil,
//			"eventbox.dice.svc.cluster.local",
//			"eventbox.marathon.l4lb.thisdcos.directory",
//		},
//		{
//			"eventbox.marathon.l4lb.thisdcos.directory:8888",
//			nil,
//			nil,
//			"eventbox.dice.svc.cluster.local",
//			"eventbox.marathon.l4lb.thisdcos.directory",
//		},
//		{
//			"service.toolonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglong.svc.cluster.local",
//			ErrTooLongNamespace,
//			nil, "", "",
//		},
//		{
//			"service.bad.domain",
//			ErrMarathonLegacyAddr,
//			nil, "", "",
//		},
//		{
//			"servicename.namespace.svc.cluster.local",
//			nil,
//			nil,
//			"servicename.namespace.svc.cluster.local",
//			"servicename.namespace.marathon.l4lb.thisdcos.directory",
//		},
//		{
//			"servicename.svc.cluster.local",
//			ErrMarathonLegacyAddr,
//			nil, "", "",
//		},
//		{
//			"consul.consul-afdb5eb0327848e19f3d414eb345dfdd.addons-2126.v1.runtimes.marathon.l4lb.thisdcos.directory",
//			nil,
//			ErrNoLegacyK8SAddr,
//			"", "",
//		},
//		{
//			"open.dice.marathon.l4lb.thisdcos.directory:8081",
//			nil,
//			nil,
//			"open.dice.svc.cluster.local",
//			"open.dice.marathon.l4lb.thisdcos.directory",
//		},
//	}
//
//	for _, c := range testcases {
//		innerdomain, err := Parse(c.domain)
//		if c.err != nil {
//			assert.Equal(t, c.err, errors.Cause(err), c.domain)
//			continue
//		}
//		k8s, err := innerdomain.K8S()
//		if c.expectk8sErr != nil {
//			assert.Equal(t, c.expectk8sErr, err, "%v, %+v", c.domain, innerdomain.domaininfo)
//			continue
//		}
//		assert.Equal(t, c.expectk8sdomain, k8s, c.domain)
//		marathon, err := innerdomain.Marathon()
//		assert.Nil(t, err)
//		assert.Equal(t, c.expectmarathondomain, marathon, c.domain)
//	}
//}
