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

package apitestsv2

import (
	"fmt"
	"reflect"

	"github.com/erda-project/erda/apistructs"
)

func checkBodyType(body apistructs.APIBody, expectType reflect.Kind) error {
	if body.Content == nil {
		return nil
	}
	_type := reflect.TypeOf(body.Content).Kind()
	if _type != expectType {
		return fmt.Errorf("invalid body type (%s) while Content-Type is %s", _type.String(), body.Type.String())
	}
	return nil
}
