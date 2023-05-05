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

package apistructs

import (
	"database/sql/driver"
	"encoding/json"

	"github.com/pkg/errors"
)

type SonarIssueGetRequest struct {
	Type  string `schema:"type"`
	Key   string `schema:"key"`
	AppID uint64 `schema:"applicationId"`
}

type SonarConfig struct {
	Host       string `json:"host"`
	Token      string `json:"token"`
	ProjectKey string `json:"projectKey"`
}

func (config *SonarConfig) Value() (driver.Value, error) {
	if b, err := json.Marshal(config); err != nil {
		return nil, errors.Wrapf(err, "failed to marshal sonar config")
	} else {
		return string(b), nil
	}
}

func (config *SonarConfig) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.New("invalid scan source for sonar config")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, config); err != nil {
		return errors.Wrapf(err, "failed to unmarshal sonar config")
	}
	return nil
}
