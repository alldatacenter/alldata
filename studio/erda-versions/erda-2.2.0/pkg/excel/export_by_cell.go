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
	"fmt"
	"io"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"github.com/tealeg/xlsx/v3"
)

// ExportExcelByCell 支持 cell 粒度配置，可以实现单元格合并，样式调整等
func ExportExcelByCell(w io.Writer, data [][]Cell, sheetName string) error {
	begin := time.Now()
	defer func() {
		end := time.Now()
		logrus.Debugf("export excel cost: %fs", end.Sub(begin).Seconds())
	}()

	file := xlsx.NewFile()
	sheet, err := file.AddSheet(sheetName)
	if err != nil {
		return fmt.Errorf("failed to add sheet, sheetName: %s, err: %v", sheetName, err)
	}
	generateCell(sheet, data)
	return write(w, file, sheetName)
}

func generateCell(sheet *xlsx.Sheet, data [][]Cell) {
	for _, cells := range data {
		row := sheet.AddRow()
		for _, cell := range cells {
			xlsxCell := row.AddCell()
			xlsxCell.Value = cell.Value
			xlsxCell.HMerge = cell.HorizontalMergeNum
			xlsxCell.VMerge = cell.VerticalMergeNum
		}
	}

	style := xlsx.NewStyle()
	style.Alignment.Horizontal = "center"
	style.Alignment.Vertical = "center"
	style.Alignment.ShrinkToFit = true
	style.Alignment.WrapText = true

	_ = sheet.ForEachRow(func(r *xlsx.Row) error {
		_ = r.ForEachCell(func(c *xlsx.Cell) error {
			c.SetStyle(style)
			return nil
		}, xlsx.SkipEmptyCells)
		r.SetHeightCM(1.5)
		return nil
	}, xlsx.SkipEmptyRows)
}

func Export(w io.Writer, data [][]Cell, sheetName string) error {
	file := xlsx.NewFile()
	sheet, err := file.AddSheet(sheetName)
	if err != nil {
		return fmt.Errorf("failed to add sheet, sheetName: %s, err: %v", sheetName, err)
	}
	generateCell(sheet, data)
	if err := file.Write(w); err != nil {
		return errors.Errorf("failed to write content, sheetName: %s, err: %v", sheetName, err)
	}
	return nil
}

type XlsxFile struct {
	file *xlsx.File
}

func NewXLSXFile() *XlsxFile {
	return &XlsxFile{file: xlsx.NewFile()}
}

func AddSheetByCell(f *XlsxFile, data [][]Cell, sheetName string) error {
	sheet, err := f.file.AddSheet(sheetName)
	if err != nil {
		return fmt.Errorf("failed to add sheet, sheetName: %s, err: %v", sheetName, err)
	}

	for _, cells := range data {
		row := sheet.AddRow()
		for _, cell := range cells {
			xlsxCell := row.AddCell()
			xlsxCell.Value = cell.Value
			xlsxCell.HMerge = cell.HorizontalMergeNum
			xlsxCell.VMerge = cell.VerticalMergeNum
		}
	}

	style := xlsx.NewStyle()
	style.Alignment.Horizontal = "center"
	style.Alignment.Vertical = "center"
	style.Alignment.ShrinkToFit = true
	style.Alignment.WrapText = true

	_ = sheet.ForEachRow(func(r *xlsx.Row) error {
		_ = r.ForEachCell(func(c *xlsx.Cell) error {
			c.SetStyle(style)
			return nil
		}, xlsx.SkipEmptyCells)
		r.SetHeightCM(1.5)
		return nil
	}, xlsx.SkipEmptyRows)
	return nil
}

func WriteFile(w io.Writer, f *XlsxFile, filename string) error {
	return write(w, f.file, filename)
}
