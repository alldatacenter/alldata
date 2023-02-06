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

package errors_test

import (
	"fmt"
	"testing"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/errors"
)

func TestIsRouteOptionAlreadyDefinedInIngressError(t *testing.T) {
	var err error
	_, _, ok := errors.IsRouteOptionAlreadyDefinedInIngressError(err)
	if ok {
		t.Fatal("error")
	}
	err = fmt.Errorf("some error")
	_, _, ok = errors.IsRouteOptionAlreadyDefinedInIngressError(err)
	if ok {
		t.Fatal("error")
	}
	err = fmt.Errorf(`admission webhook \"validate.nginx.ingress.kubernetes.io\" denied the request: host \"test-gateway.test.terminus.io\" and path \"/auto-cmp-project/.*\" is already defined in ingress addon-api-gateway--ta10d36f534ad4e219eaedb5f882d6029/dice-test-378-unity-d8e469`)
	namespace, name, ok := errors.IsRouteOptionAlreadyDefinedInIngressError(err)
	if !ok {
		t.Fatal("error")
	}
	if namespace != "addon-api-gateway--ta10d36f534ad4e219eaedb5f882d6029" {
		t.Fatal("error")
	}
	if name != "dice-test-378-unity-d8e469" {
		t.Fatal("error")
	}
}
