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

package dop

import (
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

func Test_deleteWebhook(t *testing.T) {
	bdl := &bundle.Bundle{}
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "DeleteWebhook", func(bdl *bundle.Bundle, r apistructs.DeleteHookRequest) error {
		return nil
	})
	defer monkey.UnpatchAll()

	type args struct {
		bdl *bundle.Bundle
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name:    "test",
			args:    args{bdl: bdl},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := deleteWebhook(tt.args.bdl); (err != nil) != tt.wantErr {
				t.Errorf("deleteWebhook() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
