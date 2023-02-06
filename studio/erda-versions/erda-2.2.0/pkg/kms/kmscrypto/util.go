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

package kmscrypto

import (
	"encoding/base64"
	"encoding/hex"
	"fmt"
)

// DecodeString decodes string data to bytes in designed encoded type
func DecodeString(data string, encodedType Encode) ([]byte, error) {
	var keyDecoded []byte
	var err error
	switch encodedType {
	case String:
		keyDecoded = []byte(data)
	case HEX:
		keyDecoded, err = hex.DecodeString(data)
	case Base64:
		keyDecoded, err = base64.StdEncoding.DecodeString(data)
	default:
		return keyDecoded, fmt.Errorf("unsupported encodedType: %d", encodedType)
	}
	return keyDecoded, err
}

// EncodeToString encodes data to string with encode type
func EncodeToString(data []byte, encodeType Encode) (string, error) {
	switch encodeType {
	case HEX:
		return hex.EncodeToString(data), nil
	case Base64:
		return base64.StdEncoding.EncodeToString(data), nil
	case String:
		return string(data), nil
	default:
		return "", fmt.Errorf("unsupported encodeType: %d", encodeType)
	}
}
