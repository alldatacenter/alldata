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

package license

import (
	"encoding/json"
	"errors"
	"time"
)

var aesKey = "0123456789abcdef"

// ParseLicense 解析license
func ParseLicense(licenseKey string) (*License, error) {
	if licenseKey == "" {
		return nil, errors.New("licenseKey is empty")
	}
	bytes, err := AesDecrypt(licenseKey, aesKey)
	if err != nil {
		return nil, err
	}
	var license License
	err = json.Unmarshal([]byte(bytes), &license)
	return &license, err
}

type License struct {
	ExpireDate time.Time `json:"expireDate"`
	IssueDate  time.Time `json:"issueDate"`
	User       string    `json:"user"`
	Data       Data      `json:"data"`
}

type Data struct {
	MaxHostCount uint64 `json:"maxHostCount"`
}

func (license *License) IsExpired() bool {
	return license.ExpireDate.Before(time.Now())
}
