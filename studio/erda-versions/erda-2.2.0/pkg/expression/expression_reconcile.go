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

package expression

import (
	"encoding/json"
	"fmt"
	"regexp"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"
	"gopkg.in/Knetic/govaluate.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/mock"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pexpr"
	"github.com/erda-project/erda/pkg/strutil"
)

// 匹配 ${{ xxx }}
var Re = pexpr.PhRe
var OldRe = regexp.MustCompile(`\${([^{}]+)}`)

const (
	Dirs         = "dirs"
	Outputs      = "outputs"
	Random       = "random"
	Params       = "params"
	Globals      = "globals"
	Configs      = "configs"
	Base64Decode = "base64-decode"
	TriggerLabel = "triggers"
	I18n         = "i18n"
)

const (
	TaskNotJumpOver  SignType = 1 // task 不跳过
	TaskJumpOver     SignType = 0 // task 跳过
	LeftPlaceholder           = "${{"
	RightPlaceholder          = "}}"

	OldLeftPlaceholder  = "${"
	OldRightPlaceholder = "}"
)

var MockString = []string{"string", "integer", "integer_length", "float", "boolean", "upper", "lower", "mobile", "digital_letters", "letters", "character", "timestamp",
	"timestamp_hour", "timestamp_after_hour", "timestamp_day", "timestamp_after_day", "timestamp_ms", "timestamp_ms_hour", "timestamp_ms_after_hour",
	"timestamp_ms_day", "timestamp_ms_after_day", "timestamp_ns", "timestamp_ns_hour", "timestamp_ns_after_hour", "timestamp_ns_day",
	"timestamp_ns_after_day", "date", "date_day", "datetime", "datetime_hour"}

type SignType int

type ExpressionExecSign struct {
	Sign      SignType
	Msg       string
	Err       error
	Condition string
}

func Reconcile(condition string) (sign ExpressionExecSign) {

	// panic handler
	defer func() {
		if r := recover(); r != nil {
			logrus.Errorf("pkg.expression: invalid condition: %s, panic: %v", condition, r)
			sign = ExpressionExecSign{
				Sign: TaskJumpOver,
				Err:  fmt.Errorf("expression %q exec failed, action skip", condition),
			}
		}
	}()

	// 表达式为空，不跳过
	if condition == "" {
		return ExpressionExecSign{
			Sign: TaskNotJumpOver,
		}
	}

	condition = ReplacePlaceholder(condition)
	condition = strings.Trim(condition, " ")

	expression, err := govaluate.NewEvaluableExpression(condition)
	if err != nil {
		return ExpressionExecSign{
			Sign: TaskJumpOver,
			Err:  fmt.Errorf("new expression %s error %v, action skip", condition, err),
		}
	}

	result, err := expression.Evaluate(nil)
	if err != nil {
		return ExpressionExecSign{
			Sign: TaskJumpOver,
			Err:  fmt.Errorf("expression %s exec error %v, action skip", condition, err),
		}
	}

	done, _ := result.(bool)
	if !done {
		return ExpressionExecSign{
			Sign: TaskJumpOver,
			Msg:  fmt.Sprintf("run %s expression fail, action skip", condition),
		}
	}

	return ExpressionExecSign{
		Sign: TaskNotJumpOver,
	}
}

func ReplacePlaceholder(condition string) string {
	condition = strings.TrimPrefix(condition, LeftPlaceholder)
	condition = strings.TrimSuffix(condition, RightPlaceholder)
	return condition
}

func AppendPlaceholder(condition string) string {
	return fmt.Sprintf("%s %s %s", LeftPlaceholder, condition, RightPlaceholder)
}

// str 整段包含 ${{}} 的字符串
// placeholderParams eval 方法的参数
// matchType 匹配那些类型
// 该方法是将 str 进行正则获取全部匹配的表达式，然后根据匹配的表达式执行 eval 方法，最后将表达式替换成执行的值
func MatchEval(str string, placeholderParams map[string]string, matchType ...string) (string, error) {
	matchSlice := pexpr.LoosePhRe.FindAllString(str, -1)
	for _, v := range matchSlice {
		var find = false
		for _, match := range matchType {
			if strings.HasPrefix(v, LeftPlaceholder+" "+match) || strings.HasPrefix(v, OldLeftPlaceholder+match) {
				find = true
				break
			}
		}
		if !find {
			continue
		}

		result, err := pexpr.Eval(v, placeholderParams)
		if err != nil {
			return str, err
		}
		evalStr, err := EvalResultToString(result)
		if err != nil {
			return str, fmt.Errorf("format result %v to string error: %v", result, err)
		}
		str = strings.ReplaceAll(str, v, evalStr)
	}
	return str, nil
}

func EvalResultToString(result interface{}) (string, error) {
	switch result.(type) {
	case string:
		return result.(string), nil
	case int:
		return strconv.Itoa(result.(int)), nil
	case float64:
		return strconv.FormatFloat(result.(float64), 'f', -1, 64), nil
	case float32:
		return strconv.FormatFloat(result.(float64), 'f', -1, 32), nil
	case bool:
		return strconv.FormatBool(result.(bool)), nil
	}

	jsonByte, err := json.Marshal(result)
	if err != nil {
		return "", err
	}
	return string(jsonByte), nil
}

func ReplaceRandomParams(ori string) string {
	replaced := strutil.ReplaceAllStringSubmatchFunc(Re, ori, func(sub []string) string {
		inner := sub[1]
		inner = strings.Trim(inner, " ")

		ss := strings.SplitN(inner, ".", 3)

		if len(ss) < 2 {
			return sub[0]
		}

		switch ss[0] {
		case Random:
			typeValue := ss[1]
			value := mock.MockValue(typeValue)
			return fmt.Sprintf("%v", value)
		default:
			return sub[0]
		}
	})
	return replaced
}

func GenConfigParams(key string) string {
	return fmt.Sprintf("%s %s.%s %s", LeftPlaceholder, Configs, key, RightPlaceholder)
}

func GenAutotestConfigParams(key string) string {
	return fmt.Sprintf("%s %s.%s.%s %s", LeftPlaceholder, Configs, apistructs.PipelineSourceAutoTest.String(), key, RightPlaceholder)
}

func GenDirsRef(alias string) string {
	return fmt.Sprintf("%s %s.%s %s", LeftPlaceholder, Dirs, alias, RightPlaceholder)
}

func GenParamsRef(param string) string {
	return fmt.Sprintf("%s %s.%s %s", LeftPlaceholder, Params, param, RightPlaceholder)
}

func GenOldParamsRef(param string) string {
	return fmt.Sprintf("%s%s.%s%s", OldLeftPlaceholder, Params, param, OldRightPlaceholder)
}

func GenRandomRef(key string) string {
	return fmt.Sprintf("%s %s.%s %s", LeftPlaceholder, Random, key, RightPlaceholder)
}

func GenOutputRef(alias, outputName string) string {
	return fmt.Sprintf("%s %s.%s.%s %s", LeftPlaceholder, Outputs, alias, outputName, RightPlaceholder)
}

func DecodeOutputKey(express string) (string, bool) {
	replaced := strutil.ReplaceAllStringSubmatchFunc(Re, express, func(sub []string) string {
		inner := sub[1]
		inner = strings.Trim(inner, " ")

		ss := strings.SplitN(inner, ".", 3)

		if len(ss) < 3 {
			return sub[0]
		}

		switch ss[0] {
		case Outputs:
			return ss[2]
		default:
			return sub[0]
		}
	})

	if express == replaced {
		return express, false
	}

	return replaced, true
}

func Quote(s string) string {
	return strconv.Quote(s)
}
