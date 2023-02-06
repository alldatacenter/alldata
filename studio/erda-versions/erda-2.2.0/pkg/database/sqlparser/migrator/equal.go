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

package migrator

import (
	"fmt"
	"sort"

	"github.com/pingcap/parser/mysql"
	"github.com/pingcap/tidb/types"
)

type Equal struct {
	equal  bool
	reason string
}

func (e *Equal) Equal() bool {
	return e.equal
}

func (e *Equal) Reason() string {
	return e.reason
}

func FieldTypeEqual(l, r *types.FieldType) *Equal {
	if l.Tp != r.Tp {
		return &Equal{
			equal:  false,
			reason: fmt.Sprintf("FieldType.Tp is not equal, expected: %s, actual: %s; ", l.String(), r.String()),
		}
	}

	if !(l.Flen == -1 || r.Flen == -1) && (l.Flen != r.Flen) && !mysql.IsIntegerType(l.Tp) {
		return &Equal{
			equal:  false,
			reason: fmt.Sprintf("FieldType.Flen is not equal, expected: %v, actual: %v; ", l.Flen, r.Flen),
		}
	}

	if !(l.Decimal == -1 || r.Decimal == -1) && l.Decimal != r.Decimal {
		return &Equal{
			equal:  false,
			reason: fmt.Sprintf("FieldType.Decimal is not equal, expected: %v, actual: %v; ", l.Decimal, r.Decimal),
		}
	}

	if l.Collate != r.Collate {
		return &Equal{
			equal:  false,
			reason: fmt.Sprintf("FieldType.Collate is not equal, expected: %s, actual: %s; ", l.Collate, r.Collate),
		}
	}

	if mysql.HasUnsignedFlag(l.Flag) != mysql.HasUnsignedFlag(r.Flag) {
		return &Equal{
			equal: false,
			reason: fmt.Sprintf("FieldType's HasUnsignedFlag is not equal, expected unsigned flag: %v, actual unsigned flag: %v; ",
				mysql.HasUnsignedFlag(l.Flag), mysql.HasUnsignedFlag(r.Flag)),
		}
	}

	if len(l.Elems) != len(r.Elems) {
		return &Equal{
			equal:  false,
			reason: fmt.Sprintf("FieldType.Elems length is not equal, expected: %v, actual: %v; ", l.Elems, r.Elems),
		}
	}

	sort.Strings(l.Elems)
	sort.Strings(r.Elems)
	for i := range l.Elems {
		if l.Elems[i] != r.Elems[i] {
			return &Equal{
				equal:  false,
				reason: fmt.Sprintf("FieldType.Elems[%v] is not equal, expected: %s, actual: %s; ", i, l.Elems[i], r.Elems[i]),
			}
		}
	}

	return &Equal{equal: true}
}
