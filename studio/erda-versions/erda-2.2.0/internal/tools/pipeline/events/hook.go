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

package events

type Hook interface {
	HandleWebhook
	HandleWebSocket
	HandleDingDing
	HandleHTTP
	HandleDB
}

type HandleWebhook interface{ HandleWebhook() error }
type HandleWebSocket interface{ HandleWebSocket() error }
type HandleDingDing interface{ HandleDingDing() error }
type HandleHTTP interface{ HandleHTTP() error }
type HandleDB interface{ HandleDB() error }

type HookType string

const (
	HookTypeWebHook   HookType = "WEBHOOK"
	HookTypeWebSocket HookType = "WEBSOCKET"
	HookTypeDINGDING  HookType = "DINGDING"
	HookTypeHTTP      HookType = "HTTP"
	HookTypeDB        HookType = "DB"
)
