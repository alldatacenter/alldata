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

package utils

import (
	"testing"
	"time"
)

func Test_formatDuration(t *testing.T) {
	type args struct {
		d         Duration
		precision int
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{

		{
			"",
			args{
				Duration(1*time.Microsecond + 999*time.Nanosecond),
				2,
			},
			"2µs",
			false,
		},
		{
			"",
			args{
				Duration(1*time.Microsecond + 999*time.Nanosecond),
				0,
			},
			"2µs",
			false,
		},
		{
			"",
			args{
				Duration(1*time.Millisecond + 999*time.Microsecond),
				0,
			},
			"2ms",
			false,
		},
		{
			"",
			args{
				Duration(1*time.Second + 999*time.Millisecond),
				0,
			},
			"2s",
			false,
		},
		{
			"",
			args{
				Duration(567*time.Microsecond + 700*time.Nanosecond),
				0,
			},
			"568µs",
			false,
		},
		{
			"",
			args{
				Duration(569*time.Microsecond + 700*time.Nanosecond),
				0,
			},
			"570µs",
			false,
		},
		{
			"",
			args{
				Duration(1*time.Second + 567*time.Millisecond),
				2,
			},
			"1.57s",
			false,
		},
		{
			"",
			args{
				Duration(1*time.Second + 100*time.Millisecond),
				5,
			},
			"1.1s",
			false,
		},
		{
			"",
			args{
				Duration(1 * time.Second),
				1,
			},
			"1s",
			false,
		},
		{
			"",
			args{
				Duration(1*time.Second + 100*time.Millisecond),
				1,
			},
			"1.1s",
			false,
		},
		{
			"",
			args{
				Duration(1*time.Second + 500*time.Millisecond),
				1,
			},
			"1.5s",
			false,
		},
		{
			"",
			args{
				Duration(30*time.Second + 100*time.Millisecond + 1*time.Microsecond),
				2,
			},
			"30.1s",
			false,
		},
		{
			"",
			args{
				Duration(30*time.Second + 115*time.Millisecond + 1*time.Microsecond),
				2,
			},
			"30.12s",
			false,
		},
		{
			"",
			args{
				Duration(24*time.Hour + 30*time.Second + 115*time.Millisecond + 1*time.Microsecond),
				2,
			},
			"24h0m30.12s",
			false,
		},
		{
			"",
			args{
				Duration(24*time.Hour + 30*time.Second + 115*time.Millisecond + 1*time.Microsecond),
				0,
			},
			"24h0m30s",
			false,
		},
		{
			"",
			args{
				Duration(10000*time.Hour + 59*time.Second + 99*time.Millisecond + 999*time.Microsecond + 999*time.Nanosecond),
				3,
			},
			"10000h0m59.1s",
			false,
		},
		{
			"",
			args{
				Duration(10000*time.Hour + 59*time.Second + 999*time.Millisecond + 999*time.Microsecond + 999*time.Nanosecond),
				10000,
			},
			"10000h0m59.999999999s",
			false,
		},
		{
			"",
			args{
				Duration(10000*time.Hour + 59*time.Second + 999*time.Millisecond + 999*time.Microsecond + 999*time.Nanosecond),
				8,
			},
			"10000h1m0s",
			false,
		},
		{
			"",
			args{
				Duration(24*time.Hour + 30*time.Second + 115*time.Millisecond + 1*time.Microsecond),
				-1,
			},
			"",
			true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := tt.args.d.FormatDuration(tt.args.precision)
			if (err != nil) != tt.wantErr {
				t.Errorf("formatDuration() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("formatDuration() = %v, want %v", got, tt.want)
			}
		})
	}
}
