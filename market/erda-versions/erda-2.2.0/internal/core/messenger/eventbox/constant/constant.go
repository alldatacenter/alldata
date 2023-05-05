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

package constant

import "path/filepath"

var (
	EventboxDir = "/eventbox"
	MessageDir  = filepath.Join(EventboxDir, "messages")

	// schemon input
	// ScheMonServiceLockKey  = filepath.Join(EventboxDir, "schemonservicelock")
	// ScheMonServiceWatchDir = filepath.Join("/dice/service")
	// ScheMonServiceQueryURL = "/v1/notify/runtime/%s/%s" // /v1/notify/runtime/<namespace>/<name>
	ScheMonJobLockKey  = filepath.Join(EventboxDir, "schemonjoblock")
	ScheMonJobWatchDir = filepath.Join("/dice/job/")
	ScheMonJobQueryURL = "/v1/notify/job/%s/%s" // /v1/notify/job/<namespace>/<name>

	// register
	RegisterDir      = filepath.Join(EventboxDir, "register")
	RegisterLabelKey = "/REGISTERED_LABEL"

	// webhook
	WebhookLabelKey = "/WEBHOOK"
	WebhookDir      = filepath.Join(EventboxDir, "webhook")
)
