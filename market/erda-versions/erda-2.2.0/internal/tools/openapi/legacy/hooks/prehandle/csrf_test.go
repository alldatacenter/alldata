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

package prehandle

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValidateToken(t *testing.T) {
	token, err := generateCSRFToken()
	assert.Nil(t, err)
	fmt.Printf("%x\n", token) // debug print
	_, err = validateCSRFToken(string(token))
	assert.Nil(t, err)
}
