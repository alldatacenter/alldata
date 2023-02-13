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

package dbclient

import (
	"testing"
	"time"
)

func Test_clientConfig_url(t *testing.T) {
	type fields struct {
		URL             string
		Host            string
		Port            int
		Username        string
		Password        string
		Database        string
		MaxIdle         int
		MaxConn         int
		ConnMaxLifetime time.Duration
		LogLevel        string
		ShowSQL         bool
		PROPERTIES      string
		TLS             string
	}
	tests := []struct {
		name   string
		fields fields
		want   string
	}{
		{
			name: "test_not_have_tls",
			fields: fields{
				Username:   "erda",
				Host:       "erda",
				PROPERTIES: "erda",
				Database:   "erda",
				Port:       3306,
				Password:   "erda",
			},
			want: "erda:erda@tcp(erda:3306)/erda?erda",
		},
		{
			name: "test_have_tls",
			fields: fields{
				Username:   "erda",
				Host:       "erda",
				PROPERTIES: "erda",
				Database:   "erda",
				Port:       3306,
				TLS:        "custom",
				Password:   "erda",
			},
			want: "erda:erda@tcp(erda:3306)/erda?erda&tls=custom",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cfg := &clientConfig{
				URL:             tt.fields.URL,
				Host:            tt.fields.Host,
				Port:            tt.fields.Port,
				Username:        tt.fields.Username,
				Password:        tt.fields.Password,
				Database:        tt.fields.Database,
				MaxIdle:         tt.fields.MaxIdle,
				MaxConn:         tt.fields.MaxConn,
				ConnMaxLifetime: tt.fields.ConnMaxLifetime,
				LogLevel:        tt.fields.LogLevel,
				ShowSQL:         tt.fields.ShowSQL,
				PROPERTIES:      tt.fields.PROPERTIES,
				TLS:             tt.fields.TLS,
			}
			if got := cfg.url(); got != tt.want {
				t.Errorf("url() = %v, want %v", got, tt.want)
			}
		})
	}
}
