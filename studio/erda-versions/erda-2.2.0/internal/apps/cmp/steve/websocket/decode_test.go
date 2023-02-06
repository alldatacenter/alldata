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

package websocket

import (
	"encoding/base64"
	"testing"
)

func TestDecodeFrame(t *testing.T) {
	frame := []byte{129, 144, 155, 182, 186, 247, 250, 241, 236, 132, 249, 241, 130, 144, 255, 132, 131, 142, 249,
		241, 235, 202, 0, 62, 249, 122, 86, 4, 248, 80, 11, 4, 165, 72, 17, 5, 140, 87, 44,
	}
	data := DecodeFrames(frame)
	res, err := base64.StdEncoding.DecodeString(string(data[0]))
	if err != nil {
		t.Error(err)
	}
	if string(res) != "hello world" {
		t.Errorf("test failed, expect res %s, actual %s", "hello world", string(res))
	}
}
