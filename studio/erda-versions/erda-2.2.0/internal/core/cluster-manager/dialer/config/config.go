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

package config

import "time"

type Config struct {
	Debug           bool          `default:"false" desc:"enable debug logging"`
	NeedClusterInfo bool          `default:"true" desc:"need agent register cluster info"`
	AuthWhitelist   string        `desc:"auth whitelist, will skip auth"`
	Timeout         time.Duration `default:"60s" desc:"default timeout"`
	Listen          string        `default:":80" desc:"listen address"`
}
