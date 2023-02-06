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

package clickhouse

import (
	"testing"

	"github.com/doug-martin/goqu/v9"
	"github.com/stretchr/testify/assert"
)

func Test_ExprClone(t *testing.T) {
	expr1 := goqu.From("t1").Where(goqu.C("id").Eq("1"))
	sql1, _, _ := expr1.ToSQL()
	sql1_1, _, _ := expr1.ToSQL()
	assert.Equal(t, sql1, sql1_1)

	expr2 := expr1.Where(goqu.C("name").Eq("foo"))
	sql2, _, _ := expr2.ToSQL()
	assert.NotEqual(t, sql1, sql2)

	sql1_2, _, _ := expr1.ToSQL()
	assert.Equal(t, sql1, sql1_2)
}
