/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package resource_executor

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestNoopResourceExecutor_UpdateRequest(t *testing.T) {
	type args struct {
		response interface{}
		request  interface{}
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "the only case - should always work",
			args: args{
				response: nil,
				request:  nil,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		n := &NoopResourceExecutor{}
		err := n.UpdateRequest(tt.args.response, tt.args.request)
		assert.Equal(t, tt.wantErr, err != nil, err)
	}
}
