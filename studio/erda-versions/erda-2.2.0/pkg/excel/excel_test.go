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

package excel

import (
	"mime/multipart"
	"path/filepath"
	"reflect"

	"github.com/erda-project/erda/apistructs"
)

func DecXlsFromFile(file multipart.File, head *multipart.FileHeader, titleRows int, sheetName string, excelTcs []apistructs.TestCaseExcel) error {
	wb, err := NewWorkBook(file, filepath.Ext(head.Filename), head.Size)
	if err != nil {
		return err
	}
	return DecXlsForm(wb, sheetName, titleRows, excelTcs)
}

func DecXlsForm(wb WorkBook, sheetName string, titleRows int, excelTcs []apistructs.TestCaseExcel) error {
	rows := wb.Rows(sheetName)
	if rows == nil {
		return nil
	}

	headIndex := FirstNonempty(rows)
	if headIndex == -1 {
		return nil
	}

	var excelTc apistructs.TestCaseExcel
	var i int
	for i = headIndex + titleRows; i < len(rows); i++ {
		row := rows[i]
		if row[0] != "" {
			excelTcs = append(excelTcs, excelTc)
			excelTc = apistructs.TestCaseExcel{}
		}

		var fieldIndex int
		ucValue := reflect.ValueOf(excelTc)
		ucType := reflect.TypeOf(excelTc)

		for j := 0; j < len(rows[i]); j++ {
			if fieldIndex >= ucType.NumField() {
				break
			}

			destValue := ucValue.Elem().Field(fieldIndex)
			eleType := ucValue.Elem().Field(fieldIndex).Type().Kind()

			switch eleType {
			case reflect.Struct:
				var k int
				for k = 0; k < ucType.Field(fieldIndex).Type.Elem().NumField(); k++ {
					if rows[i][j+k] != "" {
						destValue.Field(k).Set(reflect.ValueOf(rows[i][j+k]))
					}
				}
				j += k - 1
			case reflect.Slice:
				var (
					k          int
					needAppend bool
				)
				destNew := reflect.New(destValue.Type().Elem()).Elem()
				for k = 0; k < ucType.Field(fieldIndex).Type.Elem().NumField(); k++ {
					if rows[i][j+k] != "" {
						destNew.Field(k).Set(reflect.ValueOf(rows[i][j+k]))
						needAppend = true
					}
				}
				j += k - 1
				if needAppend {
					destValue.Set(reflect.Append(destValue, destNew))
				}
			default:
				if rows[i][j] != "" {
					destValue.Set(reflect.ValueOf(rows[i][j]))
				}
			}
			fieldIndex++
		}
	}

	// 填充最后一个 useCase
	excelTcs = append(excelTcs, excelTc)

	return nil
}
