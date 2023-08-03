//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dataproxy

import (
	"errors"
	"testing"
)

func Test_getErrorCode(t *testing.T) {
	type args struct {
		err error
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		// TODO: Add test cases.
		{
			name: "nil",
			args: args{err: nil},
			want: errOK.strCode,
		},
		{
			name: "errSendTimeout",
			args: args{err: errSendTimeout},
			want: errSendTimeout.strCode,
		},
		{
			name: "errSendFailed",
			args: args{err: errSendFailed},
			want: errSendFailed.strCode,
		},
		{
			name: "errProducerClosed",
			args: args{err: errProducerClosed},
			want: errProducerClosed.strCode,
		},
		{
			name: "errSendQueueIsFull",
			args: args{err: errSendQueueIsFull},
			want: errSendQueueIsFull.strCode,
		},
		{
			name: "errContextExpired",
			args: args{err: errContextExpired},
			want: errContextExpired.strCode,
		},
		{
			name: "unknown",
			args: args{err: errors.New("unknown")},
			want: errUnknown.strCode,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getErrorCode(tt.args.err); got != tt.want {
				t.Errorf("getErrorCode() = %v, want %v", got, tt.want)
			}
		})
	}
}
