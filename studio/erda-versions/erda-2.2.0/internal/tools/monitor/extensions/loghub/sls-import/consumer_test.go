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

package slsimport

import (
	"fmt"
)

func Example_http_status() {
	status, typ, err := parseHTTPStatus("201")
	fmt.Println(status, typ, err)

	status, typ, err = parseHTTPStatus("404")
	fmt.Println(status, typ, err)

	status, typ, err = parseHTTPStatus("599")
	fmt.Println(status, typ, err)

	status, typ, err = parseHTTPStatus("1990")
	fmt.Println(status, typ, err)

	// Output:
	// 201 2XX <nil>
	// 404 4XX <nil>
	// 599 5XX <nil>
	// 1990 19XX <nil>
}
