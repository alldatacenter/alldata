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

package table

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestTableNormal(t *testing.T) {
	assert.Nil(t, NewTable().Header([]string{"Header1", "Header2"}).
		Data([][]string{{"D1-1", "D1-2"}, {"D2-1", "D2-2"}}).
		Flush())
}

func TestTableOnlyHeader(t *testing.T) {
	assert.Nil(t, NewTable().Header([]string{"aaa", "bbb"}).Flush())
}

func TestTableEmptyStr(t *testing.T) {
	var buf strings.Builder
	assert.Nil(t, NewTable(WithWriter(&buf)).Header([]string{"", "bb"}).Flush())
	assert.True(t, strings.Contains(buf.String(), "<NIL>"))

	var buf2 strings.Builder
	assert.Nil(t, NewTable(WithWriter(&buf2)).Data([][]string{{"", "bb"}, {"", ""}}).Flush())
	assert.Equal(t, 3, strings.Count(buf2.String(), "<nil>"))
}

func TestTableLongData(t *testing.T) {
	NewTable().Header([]string{"h1", "h2", "h3"}).
		Data([][]string{{"short", "long-long-long-long-long", "short"}}).Flush()
}

func TestVerticalTable(t *testing.T) {
	NewTable(WithVertical()).Header([]string{"h1hhh", "h2hhh", "hhh3"}).
		Data([][]string{{"short", "long-long-long-long-long", "short"}}).Flush()

}

func TestOnlyData(t *testing.T) {
	assert.Nil(t, NewTable(WithVertical()).Data([][]string{{"d1", "d2"}}).Flush())
}
