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

package errorsx

import (
	"testing"

	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
)

func TestIsPlatformError(t *testing.T) {
	pErr := PlatformErrorf("failed to do")
	err := Errorf("failed to do")
	assert.Equal(t, true, IsPlatformError(pErr))
	assert.Equal(t, false, IsPlatformError(err))
}

func TestIsUserError(t *testing.T) {
	uErr := UserErrorf("failed to do")
	err := Errorf("failed to do")
	assert.Equal(t, true, IsUserError(uErr))
	assert.Equal(t, false, IsUserError(err))
}

func TestIsContainUserError(t *testing.T) {
	pErr := PlatformErrorf("failed to do")
	uErr := UserErrorf("failed to do")
	assert.Equal(t, false, IsContainUserError(pErr))
	assert.Equal(t, true, IsContainUserError(uErr))
}

func TestIsNetworkError(t *testing.T) {
	sessionErr := errors.New("failed to find Session for client xxx")
	timeoutErr := errors.New("Get http://xxx.com: net/http TLS handshake timeout")
	normalErr := errors.New("failed to do")
	assert.Equal(t, true, IsNetworkError(sessionErr))
	assert.Equal(t, true, IsNetworkError(timeoutErr))
	assert.Equal(t, false, IsNetworkError(normalErr))
}
