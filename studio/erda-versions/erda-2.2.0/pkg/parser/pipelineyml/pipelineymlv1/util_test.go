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

package pipelineymlv1

import (
	"fmt"
	"regexp"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/erda-project/erda/pkg/metadata"
)

func TestRegexp(t *testing.T) {
	re := regexp.MustCompile(`[^a-zA-Z0-9-]`)
	fmt.Println(re.ReplaceAllString("A string - with, awful characters$@!.", ""))
}

func TestGenerateTaskUUID(t *testing.T) {
	fmt.Println(GenerateTaskUUID(1, "- Makefile-1 ", 0, "-do !@#$ what i want to do-", "uuid"))
}

func TestApplyKVsWithPriority(t *testing.T) {
	// priority: e1 > e2 > e3
	e1 := map[string]string{
		"NAME": "linjun",
		"E1":   "V1",
	}
	e2 := map[string]string{
		"NAME": "name2",
		"E2":   "V2",
	}
	e3 := map[string]string{
		"NAME": "name3",
		"E3":   "V3",
	}

	result := ApplyKVsWithPriority(e3, e2, e1)
	require.Equal(t, 4, len(result))
	require.Equal(t, "linjun", result["NAME"])
}

func TestMap2MetadataFields(t *testing.T) {
	m := map[string]string{
		"oss.access.key": "1",
		"OSS_ACCESS_KEY": "2",
	}
	metas := Map2MetadataFields(m)
	require.True(t, len(metas) == 2)
}

func TestMetadataFields2Map(t *testing.T) {
	metas := []metadata.MetadataField{
		{Name: "oss.access.key", Value: "1"},
		{Name: "OSS_ACCESS_KEY", Value: "2"},
	}
	m := MetadataFields2Map(metas)
	require.True(t, len(m) == 2)
	require.Equal(t, m["oss.access.key"], "1")
	require.Equal(t, m["OSS_ACCESS_KEY"], "2")
}
