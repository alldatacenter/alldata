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
	"github.com/pkg/errors"
)

type SubscribeDto struct {
	SubscriberEmail string `json:"subscriberEmail"`
	Description     string `json:"description"`
}

func (dto SubscribeDto) CheckValid() error {
	if dto.SubscriberEmail == "" {
		return errors.Errorf("invalid subscribeDto: %+v", dto)
	}
	return nil
}
