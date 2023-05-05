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
	"io"

	"github.com/extrame/xls"
	"github.com/pkg/errors"
	"github.com/tealeg/xlsx"
)

type File interface {
	io.Reader
	io.ReaderAt
	io.Seeker
}

type WorkBook interface {
	Rows(sheetName string) [][]string
}

type xlsxWorkBook struct {
	xlsx.File
}

func (wb *xlsxWorkBook) Rows(sheetName string) [][]string {
	sheet, ok := wb.Sheet[sheetName]
	if !ok {
		return nil
	}
	rows := make([][]string, sheet.MaxRow+1)
	numCols := sheet.MaxCol + 1
	for i := range rows {
		rows[i] = make([]string, numCols)
		for j := range rows[i] {
			rows[i][j] = sheet.Cell(i, j).Value
		}
	}
	return rows
}

type xlsWorkBook struct {
	xls.WorkBook
}

func (wb *xlsWorkBook) Rows(sheetName string) [][]string {
	var sheet *xls.WorkSheet
	for i := 0; i < wb.NumSheets(); i++ {
		if s := wb.GetSheet(i); s.Name == sheetName {
			sheet = s
			break
		}
	}
	if sheet == nil {
		return nil
	}
	rows := make([][]string, sheet.MaxRow+1)
	numCols := 0
	for i := range rows {
		if row := sheet.Row(i); row != nil && row.LastCol()+1 > numCols {
			numCols = row.LastCol() + 1
		}
	}
	for i := range rows {
		rows[i] = make([]string, numCols)
		row := sheet.Row(i)
		if row == nil {
			continue
		}
		for j := range rows[i] {
			rows[i][j] = row.Col(j)
		}
	}
	return rows
}

func NewWorkBook(f File, ext string, size int64) (WorkBook, error) {
	switch ext {
	case ".xls":
		wb, err := xls.OpenReader(f, "utf-8")
		if err != nil {
			return nil, err
		}
		return &xlsWorkBook{*wb}, nil
	case ".xlsx":
		wb, err := xlsx.OpenReaderAt(f, size)
		if err != nil {
			return nil, err
		}
		return &xlsxWorkBook{*wb}, nil
	default:
		return nil, errors.Errorf("not support excel file type, type: %s", ext)
	}
}

func FirstNonempty(rows [][]string) int {
	for i, row := range rows {
		if !IsEmpty(row) {
			return i
		}
	}
	return -1
}

func IsEmpty(row []string) bool {
	for _, cell := range row {
		if cell != "" {
			return false
		}
	}
	return true
}
