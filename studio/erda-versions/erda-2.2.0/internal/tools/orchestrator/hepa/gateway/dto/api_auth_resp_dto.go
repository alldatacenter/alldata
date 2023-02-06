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

package dto

import (
	log "github.com/sirupsen/logrus"
)

type ApiAuthData struct {
	Access bool `json:"access"`
}

type ApiAuthErr struct {
	Code string `json:"code"`
	Msg  string `json:"msg"`
}

type ApiAuthRespDto struct {
	Success bool        `json:"success"`
	Data    ApiAuthData `json:"data"`
	Err     ApiAuthErr  `json:"err"`
}

func (dto ApiAuthRespDto) HasPermission() bool {
	if !dto.Success {
		log.Errorf("auth resp failed, code:%s, msg:%s", dto.Err.Code, dto.Err.Msg)
		return false
	}
	return dto.Data.Access
}
