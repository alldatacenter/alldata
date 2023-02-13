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

package actionrunner

// Conf .
type Conf struct {
	BuildPath           string            `json:"build_path"`
	OpenAPI             string            `json:"open_api"`
	Token               string            `json:"token"`
	MaxTask             int               `json:"max_task"`
	FailedTaskKeepHours int               `json:"failed_task_keep_hours"`
	Params              map[string]string `json:"params"`
	StartupCommands     []string          `json:"startup_commands"`
}
