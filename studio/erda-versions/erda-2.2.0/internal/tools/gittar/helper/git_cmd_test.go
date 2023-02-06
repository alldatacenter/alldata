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

package helper

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRemoveEndMarkerFromHeader(t *testing.T) {
	tt := []struct {
		header string
		want   string
	}{
		{"00aa0000000000000000000000000000000000000000 5cf4abb264b4a3a0d99e2b967ef8980a1cc41e77 refs/heads/test1 report-status-v2 side-band-64k object-format=sha1 agent=git/2.33.10000",
			"00aa0000000000000000000000000000000000000000 5cf4abb264b4a3a0d99e2b967ef8980a1cc41e77 refs/heads/test1 report-status-v2 side-band-64k object-format=sha1 agent=git/2.33.1",
		},
		{
			"00aa0000000000000000000000000000000000000000 5cf4abb264b4a3a0d99e2b967ef8980a1cc41e77 refs/heads/test2 report-status-v2 side-band-64k object-format=sha1 agent=git/2.33.100660000000000000000000000000000000000000000 5cf4abb264b4a3a0d99e2b967ef8980a1cc41e77 refs/heads/test30000",
			"00aa0000000000000000000000000000000000000000 5cf4abb264b4a3a0d99e2b967ef8980a1cc41e77 refs/heads/test2 report-status-v2 side-band-64k object-format=sha1 agent=git/2.33.100660000000000000000000000000000000000000000 5cf4abb264b4a3a0d99e2b967ef8980a1cc41e77 refs/heads/test3",
		},
	}
	for _, v := range tt {
		assert.Equal(t, v.want, string(removeEndMarkerFromHeader([]byte(v.header))))
	}
}
