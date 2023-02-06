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

package tsql

import (
	"fmt"
	"math"
	"reflect"
	"regexp"
	"strconv"
	"time"

	"github.com/recallsong/go-utils/reflectx"
)

// Operator .
type Operator uint32

// Operator values
const (
	ILLEGAL Operator = iota

	ADD    // +
	SUB    // -
	MUL    // *
	DIV    // /
	MOD    // %
	BITAND // &
	BITOR  // |
	BITXOR // ^

	AND // &&
	OR  // ||

	EQ       // =
	NEQ      // !=
	EQREGEX  // =~
	NEQREGEX // !~
	LT       // <
	LTE      // <=
	GT       // >
	GTE      // >=
)

var (
	operators = [...]string{
		ILLEGAL: "ILLEGAL",
		ADD:     "+",
		SUB:     "-",
		MUL:     "*",
		DIV:     "/",
		MOD:     "%",
		BITAND:  "&",
		BITOR:   "|",
		BITXOR:  "^",

		AND: "&&",
		OR:  "||",

		EQ:       "=",
		NEQ:      "!=",
		EQREGEX:  "=~",
		NEQREGEX: "!~",
		LT:       "<",
		LTE:      "<=",
		GT:       ">",
		GTE:      ">=",
	}
	operatorStrings map[string]Operator
)

func (op Operator) String() string {
	if int(op) >= len(operators) {
		return "ILLEGAL"
	}
	return operators[op]
}

func init() {
	operatorStrings = make(map[string]Operator, len(operators))
	for op, name := range operators {
		operatorStrings[name] = Operator(op)
	}
}

// ParseOperator .
func ParseOperator(name string) Operator {
	if op, ok := operatorStrings[name]; ok {
		return op
	}
	return ILLEGAL
}

// DataType .
type DataType uint32

// DataType values
const (
	NullType DataType = iota
	BoolType
	IntType
	UintType
	FloatType
	StringType
	TimeType
	DurationType
	RegexpType
	UnknownType
)

var types = [...]string{
	NullType:     "null",
	BoolType:     "bool",
	IntType:      "int",
	UintType:     "uint",
	FloatType:    "float",
	StringType:   "string",
	TimeType:     "time",
	DurationType: "duration",
	RegexpType:   "regexp",
	UnknownType:  "unknown",
}

func (t DataType) String() string {
	if int(t) >= len(types) {
		return "unknown"
	}
	return types[t]
}

// TypeOf .
func TypeOf(v interface{}) DataType {
	switch v.(type) {
	case nil:
		return NullType
	case bool:
		return BoolType
	case int, int8, int16, int32, int64:
		return IntType
	case uint, uint8, uint16, uint32, uint64:
		return UintType
	case float32, float64:
		return FloatType
	case string:
		return StringType
	case time.Time:
		return TimeType
	case time.Duration:
		return DurationType
	case *regexp.Regexp:
		return RegexpType
	}
	return UnknownType
}

// ValueOf .
func ValueOf(a interface{}) interface{} {
	switch v := a.(type) {
	case int:
		return int64(v)
	case int8:
		return int64(v)
	case int16:
		return int64(v)
	case int32:
		return int64(v)
	case int64:
		return int64(v)
	case uint:
		return uint64(v)
	case uint8:
		return uint64(v)
	case uint16:
		return uint64(v)
	case uint32:
		return uint64(v)
	case uint64:
		return uint64(v)
	case float32:
		return float64(v)
	case float64:
		return float64(v)
	}
	return a
}

// ErrDivideByZero .
var ErrDivideByZero = fmt.Errorf("number divide by zero")

// OperateValues .
func OperateValues(a interface{}, op Operator, b interface{}) (interface{}, error) {
	a, b = ValueOf(a), ValueOf(b)
	switch op {
	case ADD:
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case nil:
				return int64(0), nil
			case bool:
				return boolToInt(bv), nil
			case int64, uint64, float64, string, time.Time, time.Duration, *regexp.Regexp:
				return bv, nil
			}
		case bool:
			switch bv := b.(type) {
			case nil:
				return boolToInt(av), nil
			case bool:
				return boolToInt(av) + boolToInt(bv), nil
			case int64:
				return boolToInt(av) + bv, nil
			case uint64:
				return uint64(boolToInt(av)) + bv, nil
			case float64:
				return float64(boolToInt(av)) + bv, nil
			case string:
				return strconv.FormatBool(av) + bv, nil
			case time.Duration:
				return time.Duration(boolToInt(av) + int64(bv)), nil
			}
		case int64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return av + boolToInt(bv), nil
			case int64:
				return av + bv, nil
			case uint64:
				return uint64(av) + bv, nil
			case float64:
				return float64(av) + bv, nil
			case string:
				return strconv.FormatInt(av, 10) + bv, nil
			case time.Time:
				return bv.Add(time.Duration(av)), nil
			case time.Duration:
				return time.Duration(int64(av) + int64(bv)), nil
			}
		case uint64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return av + uint64(boolToInt(bv)), nil
			case int64:
				return av + uint64(bv), nil
			case uint64:
				return av + bv, nil
			case float64:
				return float64(av) + bv, nil
			case string:
				return strconv.FormatUint(av, 10) + bv, nil
			case time.Time:
				return bv.Add(time.Duration(av)), nil
			case time.Duration:
				return time.Duration(av + uint64(bv)), nil
			}
		case float64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return av + float64(boolToInt(bv)), nil
			case int64:
				return av + float64(bv), nil
			case uint64:
				return av + float64(bv), nil
			case float64:
				return av + bv, nil
			case string:
				return strconv.FormatFloat(av, 'f', -1, 64) + bv, nil
			case time.Time:
				return bv.Add(time.Duration(av)), nil
			case time.Duration:
				return time.Duration(float64(av) + float64(bv)), nil
			}
		case string:
			if b == nil {
				return a, nil
			}
			return av + fmt.Sprint(b), nil
		case time.Time:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case int64:
				return av.Add(time.Duration(bv)), nil
			case uint64:
				return av.Add(time.Duration(bv)), nil
			case float64:
				return av.Add(time.Duration(bv)), nil
			case string:
				return av.String() + bv, nil
			case time.Duration:
				return av.Add(bv), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return time.Duration(int64(av) + boolToInt(bv)), nil
			case int64:
				return time.Duration(int64(av) + int64(bv)), nil
			case uint64:
				return time.Duration(uint64(av) + uint64(bv)), nil
			case float64:
				return time.Duration(float64(av) + float64(bv)), nil
			case string:
				return av.String() + bv, nil
			case time.Time:
				return bv.Add(av), nil
			case time.Duration:
				return time.Duration(int64(av) + int64(bv)), nil
			}
		case *regexp.Regexp:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case string:
				return av.String() + bv, nil
			}
		}
	case SUB:
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case nil:
				return int64(0), nil
			case bool:
				return -boolToInt(bv), nil
			case int64:
				return -bv, nil
			case uint64:
				return -bv, nil
			case float64:
				return -bv, nil
			case string:
				if len(bv) == 0 {
					return int64(0), nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return -bn, nil
			case time.Duration:
				return time.Duration(-bv), nil
			case time.Time:
				return time.Duration(-bv.UnixNano()), nil
			}
		case bool:
			switch bv := b.(type) {
			case nil:
				return boolToInt(av), nil
			case bool:
				return boolToInt(av) - boolToInt(bv), nil
			case int64:
				return boolToInt(av) - bv, nil
			case uint64:
				return uint64(boolToInt(av)) - bv, nil
			case float64:
				return float64(boolToInt(av)) - bv, nil
			case string:
				if len(bv) == 0 {
					return boolToInt(av), nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return float64(boolToInt(av)) - bn, nil
			case time.Duration:
				return time.Duration(boolToInt(av) - int64(bv)), nil
			}
		case int64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return av - boolToInt(bv), nil
			case int64:
				return av - bv, nil
			case uint64:
				return uint64(av) - bv, nil
			case float64:
				return float64(av) - bv, nil
			case string:
				if len(bv) == 0 {
					return a, nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return float64(av) - bn, nil
			case time.Duration:
				return time.Duration(int64(av) - int64(bv)), nil
			}
		case uint64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return av - uint64(boolToInt(bv)), nil
			case int64:
				return uint64(av) - uint64(bv), nil
			case uint64:
				return av - bv, nil
			case float64:
				return float64(av) - bv, nil
			case string:
				if len(bv) == 0 {
					return a, nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return float64(av) - bn, nil
			case time.Duration:
				return time.Duration(uint64(av) - uint64(bv)), nil
			}
		case float64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return av - float64(boolToInt(bv)), nil
			case int64:
				return av - float64(bv), nil
			case uint64:
				return av - float64(bv), nil
			case float64:
				return av - bv, nil
			case string:
				if len(bv) == 0 {
					return a, nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return av - bn, nil
			case time.Duration:
				return time.Duration(float64(av) - float64(bv)), nil
			}
		case string:
			if bv, ok := b.(time.Duration); ok {
				an, err := time.ParseDuration(av)
				if err != nil {
					return nil, err
				}
				return time.Duration(int64(an) - int64(bv)), nil
			}
			var an float64
			if len(av) != 0 {
				v, err := strconv.ParseFloat(av, 64)
				if err != nil {
					return nil, err
				}
				an = v
			}
			switch bv := b.(type) {
			case nil:
				return an, nil
			case bool:
				return an - float64(boolToInt(bv)), nil
			case int64:
				return an - float64(bv), nil
			case uint64:
				return an - float64(bv), nil
			case float64:
				return an - bv, nil
			case string:
				if len(bv) == 0 {
					return an, nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return an - bn, nil
			}
		case time.Time:
			switch bv := b.(type) {
			case nil:
				return time.Duration(av.UnixNano()), nil
			case int64:
				return av.Add(-time.Duration(bv)), nil
			case uint64:
				return av.Add(-time.Duration(bv)), nil
			case float64:
				return av.Add(-time.Duration(bv)), nil
			case string:
				if len(bv) == 0 {
					return a, nil
				}
				bn, err := time.ParseDuration(bv)
				if err != nil {
					return nil, err
				}
				return av.Add(-bn), nil
			case time.Time:
				return av.Sub(bv), nil
			case time.Duration:
				return av.Add(-bv), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return time.Duration(int64(av) - boolToInt(bv)), nil
			case int64:
				return time.Duration(int64(av) - int64(bv)), nil
			case uint64:
				return time.Duration(uint64(av) - uint64(bv)), nil
			case float64:
				return time.Duration(float64(av) - float64(bv)), nil
			case string:
				if len(bv) == 0 {
					return a, nil
				}
				bn, err := time.ParseDuration(bv)
				if err != nil {
					return nil, err
				}
				return time.Duration(int64(av) - int64(bn)), nil
			case time.Duration:
				return time.Duration(int64(av) + int64(bv)), nil
			}
		case *regexp.Regexp:
			switch b.(type) {
			case nil:
				return a, nil
			}
		}
	case MUL:
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case nil, bool, int64:
				return int64(0), nil
			case uint64:
				return uint64(0), nil
			case float64:
				return float64(0), nil
			case string:
				if len(bv) == 0 {
					return int64(0), nil
				}
				_, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return float64(0), nil
			case time.Duration:
				return time.Duration(0), nil
			}
		case bool:
			switch bv := b.(type) {
			case nil:
				return int64(0), nil
			case bool:
				return boolToInt(av) * boolToInt(bv), nil
			case int64:
				return boolToInt(av) * bv, nil
			case uint64:
				return uint64(boolToInt(av)) * bv, nil
			case float64:
				return float64(boolToInt(av)) * bv, nil
			case string:
				if len(bv) == 0 {
					return int64(0), nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return float64(boolToInt(av)) * bn, nil
			case time.Duration:
				return time.Duration(boolToInt(av) * int64(bv)), nil
			}
		case int64:
			switch bv := b.(type) {
			case nil:
				return int64(0), nil
			case bool:
				return av * boolToInt(bv), nil
			case int64:
				return av * bv, nil
			case uint64:
				return uint64(av) * bv, nil
			case float64:
				return float64(av) * bv, nil
			case string:
				if len(bv) == 0 {
					return int64(0), nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return float64(av) * bn, nil
			case time.Duration:
				return time.Duration(int64(av) * int64(bv)), nil
			}
		case uint64:
			switch bv := b.(type) {
			case nil:
				return uint64(0), nil
			case bool:
				return av * uint64(boolToInt(bv)), nil
			case int64:
				return av * uint64(bv), nil
			case uint64:
				return av * bv, nil
			case float64:
				return float64(av) + bv, nil
			case string:
				if len(bv) == 0 {
					return uint64(0), nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return float64(av) * bn, nil
			case time.Duration:
				return time.Duration(av * uint64(bv)), nil
			}
		case float64:
			switch bv := b.(type) {
			case nil:
				return float64(0), nil
			case bool:
				return av * float64(boolToInt(bv)), nil
			case int64:
				return av * float64(bv), nil
			case uint64:
				return av * float64(bv), nil
			case float64:
				return av * bv, nil
			case string:
				if len(bv) == 0 {
					return float64(0), nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return float64(av) * bn, nil
			case time.Duration:
				return time.Duration(float64(av) * float64(bv)), nil
			}
		case string:
			var an float64
			if len(av) != 0 {
				v, err := strconv.ParseFloat(av, 64)
				if err != nil {
					return nil, err
				}
				an = v
			}
			switch bv := b.(type) {
			case nil:
				return float64(0), nil
			case bool:
				return an * float64(boolToInt(bv)), nil
			case int64:
				return an * float64(bv), nil
			case uint64:
				return an * float64(bv), nil
			case float64:
				return an * bv, nil
			case string:
				if len(bv) == 0 {
					return float64(0), nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return an * bn, nil
			case time.Duration:
				return time.Duration(float64(an) * float64(bv)), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case nil:
				return time.Duration(0), nil
			case int64:
				return time.Duration(int64(av) * int64(bv)), nil
			case uint64:
				return time.Duration(uint64(av) * uint64(bv)), nil
			case float64:
				return time.Duration(float64(av) * float64(bv)), nil
			case string:
				if len(bv) == 0 {
					return time.Duration(0), nil
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				return time.Duration(float64(av) * float64(bn)), nil
			case time.Duration:
				return time.Duration(int64(av) * int64(bv)), nil
			}
		}
	case DIV:
		if b == nil {
			return nil, nil // ErrDivideByZero
		}
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return int64(0), nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return int64(0), nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return uint64(0), nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(0), nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(0), nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(0), nil
			}
		case bool:
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return boolToInt(av) / bn, nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return boolToInt(av) / bv, nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return uint64(boolToInt(av)) / bv, nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(boolToInt(av)) / bv, nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(boolToInt(av)) / bn, nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(boolToInt(av) / int64(bv)), nil
			}
		case int64:
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av / boolToInt(bv), nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av / bv, nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return uint64(av) / bv, nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(av) / bv, nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(av) / bn, nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(int64(av) / int64(bv)), nil
			}
		case uint64:
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av / uint64(bn), nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av / uint64(bv), nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av / bv, nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(av) / bv, nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(av) / bn, nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(av / uint64(bv)), nil
			}
		case float64:
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av / float64(bn), nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av / float64(bv), nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av / float64(bv), nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av / bv, nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(av) / bn, nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(float64(av) * float64(bv)), nil
			}
		case string:
			var an float64
			if len(av) != 0 {
				v, err := strconv.ParseFloat(av, 64)
				if err != nil {
					return nil, err
				}
				an = v
			}
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return an / float64(bn), nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return an / float64(bv), nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return an / float64(bv), nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return an / bv, nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return an / bn, nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(float64(an) / float64(bv)), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(int64(av) / int64(bv)), nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(uint64(av) / uint64(bv)), nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(float64(av) / float64(bv)), nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(float64(av) / float64(bn)), nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(int64(av) / int64(bv)), nil
			}
		}
	case MOD:
		if b == nil {
			return nil, nil // ErrDivideByZero
		}
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return int64(0), nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return int64(0), nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return uint64(0), nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(0), nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseFloat(bv, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return float64(0), nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(0), nil
			}
		case bool:
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return boolToInt(av) % bn, nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return boolToInt(av) % bv, nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return uint64(boolToInt(av)) % bv, nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return boolToInt(av) % int64(bv), nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseInt(bv, 10, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return boolToInt(av) % bn, nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(boolToInt(av) % int64(bv)), nil
			}
		case int64:
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av % boolToInt(bv), nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av % bv, nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return uint64(av) % bv, nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av % int64(bv), nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseInt(bv, 10, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return int64(av) % bn, nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(int64(av) % int64(bv)), nil
			}
		case uint64:
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av % uint64(bn), nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av % uint64(bv), nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av % bv, nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av % uint64(bv), nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseUint(bv, 10, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return av % bn, nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(av % uint64(bv)), nil
			}
		case float64:
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return int64(av) % bn, nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return int64(av) % bv, nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return uint64(av) % bv, nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return int64(av) % int64(bv), nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseInt(bv, 10, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return int64(av) % bn, nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(int64(av) % int64(bv)), nil
			}
		case string:
			var an int64
			if len(av) != 0 {
				v, err := strconv.ParseInt(av, 10, 64)
				if err != nil {
					return nil, err
				}
				an = v
			}
			switch bv := b.(type) {
			case bool:
				bn := boolToInt(bv)
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return an % bn, nil
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return an % bv, nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return uint64(an) % bv, nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return an % int64(bv), nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseInt(bv, 10, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return an % bn, nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(int64(an) % int64(bv)), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case int64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(int64(av) % int64(bv)), nil
			case uint64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(uint64(av) % uint64(bv)), nil
			case float64:
				if bv == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(int64(av) % int64(bv)), nil
			case string:
				if len(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				bn, err := strconv.ParseInt(bv, 10, 64)
				if err != nil {
					return nil, err
				}
				if bn == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(int64(av) % bn), nil
			case time.Duration:
				if int64(bv) == 0 {
					return nil, nil // ErrDivideByZero
				}
				return time.Duration(int64(av) % int64(bv)), nil
			}
		}
	case BITAND:
		switch av := a.(type) {
		case nil:
			switch b.(type) {
			case nil, bool, int64:
				return int64(0), nil
			case uint64:
				return uint64(0), nil
			case float64:
				return float64(0), nil
			case time.Duration:
				return time.Duration(0), nil
			}
		case bool:
			switch bv := b.(type) {
			case nil:
				return int64(0), nil
			case bool:
				return boolToInt(av) & boolToInt(bv), nil
			case int64:
				return boolToInt(av) & bv, nil
			case uint64:
				return uint64(boolToInt(av)) & bv, nil
			case float64:
				return math.Float64frombits(uint64(boolToInt(av)) & math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(boolToInt(av) & int64(bv)), nil
			}
		case int64:
			switch bv := b.(type) {
			case nil:
				return int64(0), nil
			case bool:
				return av & boolToInt(bv), nil
			case int64:
				return av & bv, nil
			case uint64:
				return uint64(av) & bv, nil
			case float64:
				return math.Float64frombits(uint64(av) & math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(int64(av) & int64(bv)), nil
			}
		case uint64:
			switch bv := b.(type) {
			case nil:
				return uint64(0), nil
			case bool:
				return av & uint64(boolToInt(bv)), nil
			case int64:
				return av & uint64(bv), nil
			case uint64:
				return av & bv, nil
			case float64:
				return math.Float64frombits(av & math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(av & uint64(bv)), nil
			}
		case float64:
			switch bv := b.(type) {
			case nil:
				return float64(0), nil
			case bool:
				return math.Float64frombits(math.Float64bits(av) & uint64(boolToInt(bv))), nil
			case int64:
				return math.Float64frombits(math.Float64bits(av) & uint64(bv)), nil
			case uint64:
				return math.Float64frombits(math.Float64bits(av) & bv), nil
			case float64:
				return math.Float64frombits(math.Float64bits(av) & math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(math.Float64bits(av) & uint64(bv)), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case nil:
				return time.Duration(0), nil
			case int64:
				return time.Duration(int64(av) & int64(bv)), nil
			case uint64:
				return time.Duration(uint64(av) & uint64(bv)), nil
			case float64:
				return time.Duration(uint64(av) & math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(int64(av) & int64(bv)), nil
			}
		}
	case BITOR:
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case nil:
				return int64(0), nil
			case bool:
				return boolToInt(bv), nil
			case int64, uint64, float64, time.Duration:
				return b, nil
			}
		case bool:
			switch bv := b.(type) {
			case nil:
				return boolToInt(av), nil
			case bool:
				return boolToInt(av) | boolToInt(bv), nil
			case int64:
				return boolToInt(av) | bv, nil
			case uint64:
				return uint64(boolToInt(av)) | bv, nil
			case float64:
				return math.Float64frombits(uint64(boolToInt(av)) | math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(boolToInt(av) | int64(bv)), nil
			}
		case int64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return av | boolToInt(bv), nil
			case int64:
				return av | bv, nil
			case uint64:
				return uint64(av) | bv, nil
			case float64:
				return math.Float64frombits(uint64(av) | math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(int64(av) | int64(bv)), nil
			}
		case uint64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return av | uint64(boolToInt(bv)), nil
			case int64:
				return av | uint64(bv), nil
			case uint64:
				return av | bv, nil
			case float64:
				return math.Float64frombits(av | math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(av | uint64(bv)), nil
			}
		case float64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return math.Float64frombits(math.Float64bits(av) | uint64(boolToInt(bv))), nil
			case int64:
				return math.Float64frombits(math.Float64bits(av) | uint64(bv)), nil
			case uint64:
				return math.Float64frombits(math.Float64bits(av) | bv), nil
			case float64:
				return math.Float64frombits(math.Float64bits(av) | math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(math.Float64bits(av) | uint64(bv)), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case int64:
				return time.Duration(int64(av) | int64(bv)), nil
			case uint64:
				return time.Duration(uint64(av) | uint64(bv)), nil
			case float64:
				return time.Duration(uint64(av) | math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(int64(av) | int64(bv)), nil
			}
		}
	case BITXOR:
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case nil:
				return int64(0), nil
			case bool:
				return boolToInt(bv), nil
			case int64, uint64, float64, time.Duration:
				return b, nil
			}
		case bool:
			switch bv := b.(type) {
			case nil:
				return boolToInt(av), nil
			case bool:
				return boolToInt(av) ^ boolToInt(bv), nil
			case int64:
				return boolToInt(av) ^ bv, nil
			case uint64:
				return uint64(boolToInt(av)) ^ bv, nil
			case float64:
				return math.Float64frombits(uint64(boolToInt(av)) ^ math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(boolToInt(av) ^ int64(bv)), nil
			}
		case int64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return av ^ boolToInt(bv), nil
			case int64:
				return av ^ bv, nil
			case uint64:
				return uint64(av) ^ bv, nil
			case float64:
				return math.Float64frombits(uint64(av) ^ math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(int64(av) ^ int64(bv)), nil
			}
		case uint64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return av ^ uint64(boolToInt(bv)), nil
			case int64:
				return av ^ uint64(bv), nil
			case uint64:
				return av ^ bv, nil
			case float64:
				return math.Float64frombits(av ^ math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(av ^ uint64(bv)), nil
			}
		case float64:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case bool:
				return math.Float64frombits(math.Float64bits(av) ^ uint64(boolToInt(bv))), nil
			case int64:
				return math.Float64frombits(math.Float64bits(av) ^ uint64(bv)), nil
			case uint64:
				return math.Float64frombits(math.Float64bits(av) ^ bv), nil
			case float64:
				return math.Float64frombits(math.Float64bits(av) ^ math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(math.Float64bits(av) | uint64(bv)), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case nil:
				return a, nil
			case int64:
				return time.Duration(int64(av) ^ int64(bv)), nil
			case uint64:
				return time.Duration(uint64(av) ^ uint64(bv)), nil
			case float64:
				return time.Duration(uint64(av) ^ math.Float64bits(bv)), nil
			case time.Duration:
				return time.Duration(int64(av) ^ int64(bv)), nil
			}
		}
	case AND:
		return valueToBool(a) && valueToBool(b), nil
	case OR:
		return valueToBool(a) || valueToBool(b), nil
	case EQ:
		return a == b, nil
	case NEQ:
		return a != b, nil
	case EQREGEX:
		switch av := a.(type) {
		case string:
			switch bv := b.(type) {
			case *regexp.Regexp:
				return bv.Match(reflectx.StringToBytes(av)), nil
			}
		case *regexp.Regexp:
			switch bv := b.(type) {
			case string:
				return av.Match(reflectx.StringToBytes(bv)), nil
			}
		}
	case NEQREGEX:
		switch av := a.(type) {
		case string:
			switch bv := b.(type) {
			case *regexp.Regexp:
				return !bv.Match(reflectx.StringToBytes(av)), nil
			}
		case *regexp.Regexp:
			switch bv := b.(type) {
			case string:
				return !av.Match(reflectx.StringToBytes(bv)), nil
			}
		}
	case LT:
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case nil:
				return false, nil
			case bool:
				return 0 < boolToInt(bv), nil
			case int64:
				return 0 < bv, nil
			case uint64:
				return 0 < bv, nil
			case float64:
				return 0 < bv, nil
			case string:
				return "" < bv, nil
			case time.Duration:
				return 0 < int64(bv), nil
			case time.Time:
				return 0 < bv.UnixNano(), nil
			case *regexp.Regexp:
				return true, nil
			}
		case bool:
			switch bv := b.(type) {
			case nil:
				return false, nil
			case bool:
				return boolToInt(av) < boolToInt(bv), nil
			case int64:
				return boolToInt(av) < bv, nil
			case uint64:
				return uint64(boolToInt(av)) < bv, nil
			case float64:
				return float64(boolToInt(av)) < bv, nil
			case string:
				return strconv.FormatBool(av) < bv, nil
			case time.Duration:
				return boolToInt(av) < int64(bv), nil
			case time.Time:
				return boolToInt(av) < bv.UnixNano(), nil
			}
		case int64:
			switch bv := b.(type) {
			case nil:
				return av < 0, nil
			case bool:
				return av < boolToInt(bv), nil
			case int64:
				return av < bv, nil
			case uint64:
				return uint64(av) < bv, nil
			case float64:
				return float64(av) < bv, nil
			case string:
				return strconv.FormatInt(av, 10) < bv, nil
			case time.Duration:
				return int64(av) < int64(bv), nil
			case time.Time:
				return av < bv.UnixNano(), nil
			}
		case uint64:
			switch bv := b.(type) {
			case nil:
				return av < 0, nil
			case bool:
				return av < uint64(boolToInt(bv)), nil
			case int64:
				return av < uint64(bv), nil
			case uint64:
				return av < bv, nil
			case float64:
				return float64(av) < bv, nil
			case string:
				return strconv.FormatUint(av, 10) < bv, nil
			case time.Duration:
				return av < uint64(bv), nil
			case time.Time:
				return av < uint64(bv.UnixNano()), nil
			}
		case float64:
			switch bv := b.(type) {
			case nil:
				return av < 0, nil
			case bool:
				return av < float64(boolToInt(bv)), nil
			case int64:
				return av < float64(bv), nil
			case uint64:
				return av < float64(bv), nil
			case float64:
				return av < bv, nil
			case string:
				return strconv.FormatFloat(av, 'f', -1, 64) < bv, nil
			case time.Duration:
				return float64(av) < float64(bv), nil
			case time.Time:
				return av < float64(bv.UnixNano()), nil
			}
		case string:
			if b == nil {
				return av < "", nil
			}
			return av < fmt.Sprint(av), nil
		case time.Time:
			switch bv := b.(type) {
			case nil:
				return false, nil
			case bool:
				return av.UnixNano() < boolToInt(bv), nil
			case int64:
				return av.UnixNano() < bv, nil
			case uint64:
				return uint64(av.UnixNano()) < bv, nil
			case float64:
				return float64(av.UnixNano()) < bv, nil
			case string:
				return av.String() < bv, nil
			case time.Duration:
				return av.UnixNano() < int64(bv), nil
			case time.Time:
				return av.Before(bv), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case nil:
				return int64(av) < 0, nil
			case bool:
				return int64(av) < boolToInt(bv), nil
			case int64:
				return int64(av) < int64(bv), nil
			case uint64:
				return uint64(av) < uint64(bv), nil
			case float64:
				return float64(av) < float64(bv), nil
			case string:
				return av.String() < bv, nil
			case time.Duration:
				return int64(av) < int64(bv), nil
			case time.Time:
				return int64(av) < bv.UnixNano(), nil
			}
		case *regexp.Regexp:
			switch bv := b.(type) {
			case nil:
				return false, nil
			case string:
				return av.String() < bv, nil
			case *regexp.Regexp:
				return av.String() < bv.String(), nil
			}
		}
	case LTE:
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case nil, bool, time.Time, *regexp.Regexp:
				return true, nil
			case int64:
				return 0 <= bv, nil
			case uint64:
				return 0 <= bv, nil
			case float64:
				return 0 <= bv, nil
			case string:
				return "" <= bv, nil
			case time.Duration:
				return 0 <= int64(bv), nil
			}
		case bool:
			switch bv := b.(type) {
			case nil:
				return boolToInt(av) <= 0, nil
			case bool:
				return boolToInt(av) <= boolToInt(bv), nil
			case int64:
				return boolToInt(av) <= bv, nil
			case uint64:
				return uint64(boolToInt(av)) <= bv, nil
			case float64:
				return float64(boolToInt(av)) <= bv, nil
			case string:
				return strconv.FormatBool(av) <= bv, nil
			case time.Duration:
				return boolToInt(av) <= int64(bv), nil
			case time.Time:
				return boolToInt(av) <= bv.UnixNano(), nil
			}
		case int64:
			switch bv := b.(type) {
			case nil:
				return av <= 0, nil
			case bool:
				return av <= boolToInt(bv), nil
			case int64:
				return av <= bv, nil
			case uint64:
				return uint64(av) <= bv, nil
			case float64:
				return float64(av) <= bv, nil
			case string:
				return strconv.FormatInt(av, 10) <= bv, nil
			case time.Duration:
				return int64(av) <= int64(bv), nil
			case time.Time:
				return av <= bv.UnixNano(), nil
			}
		case uint64:
			switch bv := b.(type) {
			case nil:
				return av <= 0, nil
			case bool:
				return av <= uint64(boolToInt(bv)), nil
			case int64:
				return av <= uint64(bv), nil
			case uint64:
				return av <= bv, nil
			case float64:
				return float64(av) <= bv, nil
			case string:
				return strconv.FormatUint(av, 10) <= bv, nil
			case time.Duration:
				return av <= uint64(bv), nil
			case time.Time:
				return av <= uint64(bv.UnixNano()), nil
			}
		case float64:
			switch bv := b.(type) {
			case nil:
				return av <= 0, nil
			case bool:
				return av <= float64(boolToInt(bv)), nil
			case int64:
				return av <= float64(bv), nil
			case uint64:
				return av <= float64(bv), nil
			case float64:
				return av <= bv, nil
			case string:
				return strconv.FormatFloat(av, 'f', -1, 64) <= bv, nil
			case time.Duration:
				return float64(av) <= float64(bv), nil
			case time.Time:
				return av <= float64(bv.UnixNano()), nil
			}
		case string:
			if b == nil {
				return av <= "", nil
			}
			return av <= fmt.Sprint(av), nil
		case time.Time:
			switch bv := b.(type) {
			case nil:
				return av.UnixNano() <= 0, nil
			case bool:
				return av.UnixNano() <= boolToInt(bv), nil
			case int64:
				return av.UnixNano() <= bv, nil
			case uint64:
				return uint64(av.UnixNano()) <= uint64(bv), nil
			case float64:
				return float64(av.UnixNano()) <= bv, nil
			case string:
				return av.String() <= bv, nil
			case time.Time:
				return av.UnixNano() <= bv.UnixNano(), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case nil:
				return int64(av) <= 0, nil
			case int64:
				return int64(av) <= int64(bv), nil
			case uint64:
				return uint64(av) <= uint64(bv), nil
			case float64:
				return float64(av) <= float64(bv), nil
			case string:
				return av.String() <= bv, nil
			case time.Duration:
				return int64(av) <= int64(bv), nil
			case time.Time:
				return int64(av) <= bv.UnixNano(), nil
			}
		case *regexp.Regexp:
			switch bv := b.(type) {
			case nil:
				return false, nil
			case string:
				return av.String() <= bv, nil
			case *regexp.Regexp:
				return av.String() <= bv.String(), nil
			}
		}
	case GT:
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case nil, bool, string, time.Time, *regexp.Regexp:
				return false, nil
			case int64:
				return 0 > bv, nil
			case uint64:
				return 0 > bv, nil
			case float64:
				return 0 > bv, nil
			case time.Duration:
				return 0 > int64(bv), nil
			}
		case bool:
			switch bv := b.(type) {
			case nil:
				return boolToInt(av) > 0, nil
			case bool:
				return boolToInt(av) > boolToInt(bv), nil
			case int64:
				return boolToInt(av) > bv, nil
			case uint64:
				return uint64(boolToInt(av)) > bv, nil
			case float64:
				return float64(boolToInt(av)) > bv, nil
			case string:
				return strconv.FormatBool(av) > bv, nil
			case time.Duration:
				return boolToInt(av) > int64(bv), nil
			case time.Time:
				return boolToInt(av) > bv.UnixNano(), nil
			}
		case int64:
			switch bv := b.(type) {
			case nil:
				return av > 0, nil
			case bool:
				return av > boolToInt(bv), nil
			case int64:
				return av > bv, nil
			case uint64:
				return uint64(av) > bv, nil
			case float64:
				return float64(av) > bv, nil
			case string:
				return strconv.FormatInt(av, 10) > bv, nil
			case time.Duration:
				return int64(av) > int64(bv), nil
			case time.Time:
				return av > bv.UnixNano(), nil
			}
		case uint64:
			switch bv := b.(type) {
			case nil:
				return av > 0, nil
			case bool:
				return av > uint64(boolToInt(bv)), nil
			case int64:
				return av > uint64(bv), nil
			case uint64:
				return av > bv, nil
			case float64:
				return float64(av) > bv, nil
			case string:
				return strconv.FormatUint(av, 10) > bv, nil
			case time.Duration:
				return av > uint64(bv), nil
			case time.Time:
				return av > uint64(bv.UnixNano()), nil
			}
		case float64:
			switch bv := b.(type) {
			case nil:
				return av > 0, nil
			case bool:
				return av > float64(boolToInt(bv)), nil
			case int64:
				return av > float64(bv), nil
			case uint64:
				return av > float64(bv), nil
			case float64:
				return av > bv, nil
			case string:
				return strconv.FormatFloat(av, 'f', -1, 64) > bv, nil
			case time.Duration:
				return float64(av) > float64(bv), nil
			case time.Time:
				return av > float64(bv.UnixNano()), nil
			}
		case string:
			if b == nil {
				return av > "", nil
			}
			return av > fmt.Sprint(av), nil
		case time.Time:
			switch bv := b.(type) {
			case nil:
				return av.UnixNano() > 0, nil
			case bool:
				return av.UnixNano() > boolToInt(bv), nil
			case int64:
				return av.UnixNano() > bv, nil
			case uint64:
				return uint64(av.UnixNano()) > bv, nil
			case float64:
				return float64(av.UnixNano()) > bv, nil
			case string:
				return av.String() > bv, nil
			case time.Duration:
				return av.UnixNano() > int64(bv), nil
			case time.Time:
				return av.After(bv), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case nil:
				return int64(av) > 0, nil
			case bool:
				return int64(av) > boolToInt(bv), nil
			case int64:
				return int64(av) > int64(bv), nil
			case uint64:
				return uint64(av) > uint64(bv), nil
			case float64:
				return float64(av) > float64(bv), nil
			case string:
				return av.String() > bv, nil
			case time.Duration:
				return int64(av) > int64(bv), nil
			case time.Time:
				return int64(av) > bv.UnixNano(), nil
			}
		case *regexp.Regexp:
			switch bv := b.(type) {
			case nil:
				return true, nil
			case string:
				return av.String() > bv, nil
			case *regexp.Regexp:
				return av.String() > bv.String(), nil
			}
		}
	case GTE:
		switch av := a.(type) {
		case nil:
			switch bv := b.(type) {
			case nil:
				return true, nil
			case bool:
				return 0 >= boolToInt(bv), nil
			case int64:
				return 0 >= bv, nil
			case uint64:
				return 0 >= bv, nil
			case float64:
				return 0 >= bv, nil
			case string:
				return "" >= bv, nil
			case time.Duration:
				return 0 >= int64(bv), nil
			case time.Time:
				return 0 >= bv.UnixNano(), nil
			case *regexp.Regexp:
				return false, nil
			}
		case bool:
			switch bv := b.(type) {
			case nil:
				return boolToInt(av) >= 0, nil
			case bool:
				return boolToInt(av) >= boolToInt(bv), nil
			case int64:
				return boolToInt(av) >= bv, nil
			case uint64:
				return uint64(boolToInt(av)) >= bv, nil
			case float64:
				return float64(boolToInt(av)) >= bv, nil
			case string:
				return strconv.FormatBool(av) >= bv, nil
			case time.Duration:
				return boolToInt(av) >= int64(bv), nil
			case time.Time:
				return boolToInt(av) >= bv.UnixNano(), nil
			}
		case int64:
			switch bv := b.(type) {
			case nil:
				return av >= 0, nil
			case bool:
				return av >= boolToInt(bv), nil
			case int64:
				return av >= bv, nil
			case uint64:
				return uint64(av) >= bv, nil
			case float64:
				return float64(av) >= bv, nil
			case string:
				return strconv.FormatInt(av, 10) >= bv, nil
			case time.Duration:
				return int64(av) >= int64(bv), nil
			case time.Time:
				return av >= bv.UnixNano(), nil
			}
		case uint64:
			switch bv := b.(type) {
			case nil:
				return av >= 0, nil
			case bool:
				return av >= uint64(boolToInt(bv)), nil
			case int64:
				return av >= uint64(bv), nil
			case uint64:
				return av >= bv, nil
			case float64:
				return float64(av) >= bv, nil
			case string:
				return strconv.FormatUint(av, 10) >= bv, nil
			case time.Duration:
				return av >= uint64(bv), nil
			case time.Time:
				return av >= uint64(bv.UnixNano()), nil
			}
		case float64:
			switch bv := b.(type) {
			case nil:
				return av >= 0, nil
			case bool:
				return av >= float64(boolToInt(bv)), nil
			case int64:
				return av >= float64(bv), nil
			case uint64:
				return av >= float64(bv), nil
			case float64:
				return av >= bv, nil
			case string:
				return strconv.FormatFloat(av, 'f', -1, 64) >= bv, nil
			case time.Duration:
				return float64(av) >= float64(bv), nil
			case time.Time:
				return av >= float64(bv.UnixNano()), nil
			}
		case string:
			if b == nil {
				return true, nil
			}
			return av >= fmt.Sprint(av), nil
		case time.Time:
			switch bv := b.(type) {
			case nil:
				return av.UnixNano() >= 0, nil
			case bool:
				return av.UnixNano() >= boolToInt(bv), nil
			case int64:
				return av.UnixNano() >= bv, nil
			case uint64:
				return uint64(av.UnixNano()) >= uint64(bv), nil
			case float64:
				return float64(av.UnixNano()) >= bv, nil
			case string:
				return av.String() >= bv, nil
			case time.Time:
				return av.UnixNano() >= bv.UnixNano(), nil
			}
		case time.Duration:
			switch bv := b.(type) {
			case nil:
				return int64(av) >= 0, nil
			case int64:
				return int64(av) >= int64(bv), nil
			case uint64:
				return uint64(av) >= uint64(bv), nil
			case float64:
				return float64(av) >= float64(bv), nil
			case string:
				return av.String() >= bv, nil
			case time.Duration:
				return int64(av) >= int64(bv), nil
			case time.Time:
				return int64(av) >= bv.UnixNano(), nil
			}
		case *regexp.Regexp:
			switch bv := b.(type) {
			case nil:
				return true, nil
			case string:
				return av.String() >= bv, nil
			case *regexp.Regexp:
				return av.String() >= bv.String(), nil
			}
		}
	default:
		return nil, fmt.Errorf("invaild operator %s", op)
	}
	return nil, fmt.Errorf("not support %s %s %s", reflect.TypeOf(a), op, reflect.TypeOf(b))
}

func valueToBool(v interface{}) bool {
	switch val := v.(type) {
	case nil:
		return false
	case bool:
		return val
	case int64:
		return val != 0
	case uint64:
		return val != 0
	case float64:
		return val != 0
	case string:
		return len(val) > 0
	case time.Time:
		return !val.IsZero()
	case time.Duration:
		return val != 0
	case *regexp.Regexp:
		return val != nil
	}
	return v != nil
}

func boolToInt(v bool) int64 {
	if v {
		return 1
	}
	return 0
}
