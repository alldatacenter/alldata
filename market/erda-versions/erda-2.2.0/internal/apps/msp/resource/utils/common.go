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
	"encoding/base64"
	"encoding/json"
	"math/rand"
	"reflect"
	"strings"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/kms/kmscrypto"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

// GetRandomId 生成随机33位uuid，并且，（首字母开头 + 32位uuid）构成Id
func GetRandomId() string {
	str := "abcdefghijklmnopqrstuvwxyz"
	bytes := []byte(str)
	var result []byte
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	for i := 0; i < 1; i++ {
		result = append(result, bytes[r.Intn(len(bytes))])
	}
	return strings.Join([]string{string(result), uuid.UUID()}, "")
}

func JsonConvertObjToType(source interface{}, dest interface{}) error {
	destType := reflect.TypeOf(dest).String()
	if str, ok := source.(string); ok && destType != "*string" {
		bytes := []byte(str)
		err := json.Unmarshal(bytes, dest)
		return err
	}

	if bytes, err := json.Marshal(source); err != nil {
		return err
	} else {
		err = json.Unmarshal(bytes, dest)
		return err
	}
}

func JsonConvertObjToString(source interface{}) (string, error) {
	bytes, err := json.Marshal(source)
	if err != nil {
		return "", err
	}
	return string(bytes), nil
}

func AppendMap(base map[string]string, toadd map[string]string) {
	if toadd == nil {
		return
	}
	for k, v := range toadd {
		base[k] = v
	}
}

// AesDecrypt decode base64 data with AES
func AesDecrypt(data string) string {
	// decrypt using key="terminus-dice@20" with mode AES/GCM/NoPadding
	debase64Data, err := base64.StdEncoding.DecodeString(data)
	if err != nil {
		return data
	}

	key := "terminus-dice@20"
	plainText, err := kmscrypto.AesGcmDecrypt([]byte(key), debase64Data, nil)
	if err != nil {
		return data
	}

	return string(plainText)
}

// SetlabelsFromOptions set service labels from addon Options
func SetlabelsFromOptions(options, labels map[string]string) {
	// addons[xxx].options 中指定的 Option 创建为 labels
	if _, ok := options[diceyml.AddonDiskType]; ok {
		labels[diceyml.AddonDiskType] = options[diceyml.AddonDiskType]
	}

	if _, ok := options[diceyml.AddonVolumeSize]; ok {
		labels[diceyml.AddonVolumeSize] = options[diceyml.AddonVolumeSize]
	}

	if _, ok := options[diceyml.AddonSnapMaxHistory]; ok {
		labels[diceyml.AddonSnapMaxHistory] = options[diceyml.AddonSnapMaxHistory]
	}

	if _, ok := options[apistructs.AlibabaECILabel]; ok {
		labels[apistructs.AlibabaECILabel] = options[apistructs.AlibabaECILabel]
	}
}
