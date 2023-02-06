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

package rlog

import (
	"bytes"
	"fmt"
	"strings"
	"testing"

	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

func TestPErrorAndReturn(t *testing.T) {
	err := fmt.Errorf("failed to get schedulable tasks, err: %s", "internal error")
	err = PErrorAndReturn(1, err)
	assert.Error(t, err)
	assert.True(t, true, strings.HasPrefix(err.Error(), pErrorFormat))
}

func TestInfof(t *testing.T) {
	var buf bytes.Buffer
	logrus.SetOutput(&buf)
	s := "start watching"
	Infof(s)
	assert.True(t, strings.Contains(buf.String(), fmt.Sprintf(errorFormat, s)))
}
