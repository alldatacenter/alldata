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

package types

import (
	"strings"
)

const (
	OpusTypeExtensionAction  OpusType = "erda/extension/action"
	OpusTypeExtensionAddon   OpusType = "erda/extension/addon"
	OpusTypeArtifactsProject OpusType = "erda/artifacts/project"

	OpusLevelSystem OpusLevel = "sys"
	OpusLevelOrg    OpusLevel = "org"

	PutOnOpusModeAppend   PutOnOpusMode = "append"
	PutOnOpusModeOverride PutOnOpusMode = "override"

	LangUnknown Lang = "unknown"
	LangEn      Lang = "en"
	LangEnUs    Lang = "en_us"
	LangZh      Lang = "zh"
	LangZhCn    Lang = "zh_cn"
)

var (
	OpusTypeNames = map[OpusType]string{
		OpusTypeExtensionAction:  "Action",
		OpusTypeExtensionAddon:   "Addon",
		OpusTypeArtifactsProject: "Erda Artifacts",
	}

	LangTypes = map[Lang]string{
		LangUnknown: "unknown",
		LangEn:      "English",
		LangEnUs:    "English",
		LangZh:      "中文",
		LangZhCn:    "中文",
	}
)

type OpusType string

func (o OpusType) String() string {
	return string(o)
}

func (o OpusType) Equal(s string) bool {
	return strings.EqualFold(o.String(), s)
}

type OpusLevel string

func (o OpusLevel) String() string {
	return string(o)
}

func (o OpusLevel) Equal(s string) bool {
	return strings.EqualFold(o.String(), s)
}

type PutOnOpusMode string

func (o PutOnOpusMode) String() string {
	return string(o)
}

func (o PutOnOpusMode) Equal(s string) bool {
	return strings.EqualFold(o.String(), s)
}

type Lang string

func (o Lang) String() string {
	return string(o)
}

func (o Lang) Equal(s string) bool {
	return strings.EqualFold(strings.ReplaceAll(o.String(), "-", "_"), strings.ReplaceAll(s, "-", "_"))
}
