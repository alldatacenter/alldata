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

package dao

import (
	"github.com/pkg/errors"
)

// dao层错误码统一定义
var (
	ErrNotFoundOrg         = errors.New("org not found")
	ErrNotFoundProject     = errors.New("project not found")
	ErrNotFoundApplication = errors.New("application not found")
	ErrNotFoundMember      = errors.New("member not found")
	ErrNotFoundTicket      = errors.New("ticket not found")
	ErrNotFoundPublisher   = errors.New("publisher not found")
	ErrNotFoundCertificate = errors.New("certificate not found")
	ErrNotFoundApprove     = errors.New("approve not found")
	ErrNotFoundUsecase     = errors.New("usecase not found")
)
