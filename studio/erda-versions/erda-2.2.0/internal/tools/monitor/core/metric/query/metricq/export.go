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

package metricq

//  Data export, a temporary scheme.

import (
	"io/ioutil"
	"net/http"
	"strings"

	"github.com/360EntSecGroup-Skylar/excelize/v2"
	uuid "github.com/satori/go.uuid"

	api "github.com/erda-project/erda/pkg/common/httpapi"
)

// exportMetrics .
func (p *provider) exportMetrics(r *http.Request, w http.ResponseWriter, param *QueryParams) interface{} {
	if len(param.Query) > 0 || strings.HasPrefix(param.QL, "influxql") {
		// Compatible with old table SQL schema queries
		err := r.ParseForm()
		if err != nil {
			return api.Errors.InvalidParameter(err)
		}
		ql, q, format := r.Form.Get("ql"), r.Form.Get("q"), r.Form.Get("format")
		r.Form.Del("ql")
		r.Form.Del("q")
		r.Form.Del("format")
		if len(format) == 0 {
			format = "influxdb"
		}
		if len(ql) == 0 {
			ql = "influxql"
		}
		if len(q) == 0 {
			byts, err := ioutil.ReadAll(r.Body)
			if err == nil {
				q = string(byts)
			}
		}
		_, data, err := p.q.QueryWithFormat(ql, q, format, api.Language(r), nil, nil, r.Form)
		if err != nil {
			return api.Errors.InvalidParameter(err)
		}
		return downloadExcelFile(w, data)
	}
	stmt := getQueryStatement(param.Scope, param.Aggregate, r)
	qlang := "json"
	if r.Method == http.MethodGet {
		qlang = "params"
	}
	if len(param.Format) <= 0 {
		param.Format = "chart"
	}
	resp, err := p.q.QueryWithFormatV1(qlang, stmt, param.Format, api.Language(r))
	if err != nil {
		return api.Errors.Internal(err, stmt)
	}
	data := resp.Data
	if param.Format == "chartv2" {
		if d, ok := data.(map[string]interface{}); ok {
			if _, ok := d["metricData"]; ok {
				return downloadExcelFile(w, data)
			}
		}
	}
	return api.Errors.InvalidParameter("not a table query")
}

func downloadExcelFile(w http.ResponseWriter, data interface{}) interface{} {
	headers, keys, list := []interface{}{}, []string{}, []map[string]interface{}{}
	m, ok := data.(map[string]interface{})
	if ok && m != nil {
		hs, _ := m["cols"].([]map[string]interface{})
		for _, h := range hs {
			title := h["title"]
			headers = append(headers, title)
			key, _ := h["dataIndex"].(string)
			keys = append(keys, key)
		}
		list, _ = m["metricData"].([]map[string]interface{})
	}
	// new excel file
	file := excelize.NewFile()
	streamWriter, err := file.NewStreamWriter(file.GetSheetName(0))
	if err != nil {
		return api.Errors.InvalidParameter(err)
	}
	cell, _ := excelize.CoordinatesToCellName(1, 1)
	if err := streamWriter.SetRow(cell, headers); err != nil {
		return api.Errors.InvalidParameter(err)
	}
	row := 2
	for _, item := range list {
		var vals []interface{}
		for _, key := range keys {
			vals = append(vals, item[key])
		}
		cell, _ := excelize.CoordinatesToCellName(1, row)
		if err := streamWriter.SetRow(cell, vals); err != nil {
			return err
		}
		row++
	}
	if err := streamWriter.Flush(); err != nil {
		return api.Errors.InvalidParameter(err)
	}
	filename := strings.Replace(uuid.NewV4().String(), "-", "", -1) + ".xlsx"
	w.Header().Set("Cache-Control", "no-cache, no-store, must-revalidate")
	w.Header().Set("Pragma", "no-cache")
	w.Header().Set("Expires", "0")
	w.Header().Set("charset", "utf-8")
	w.Header().Set("Content-Disposition", "attachment;filename="+filename)
	w.Header().Set("Content-Type", "application/octet-stream")
	// if err := file.SaveAs("test.xlsx"); err != nil {
	// 	return api.Errors.InvalidParameter(err)
	// }
	if _, err := file.WriteTo(w); err != nil {
		return api.Errors.InvalidParameter(err)
	}
	return nil
}

// // exportMetrics .
// func (p *provider) exportMetrics(r *http.Request, w http.ResponseWriter, params struct {
// 	Scope   string `query:"scope" validate:"required"`
// 	ScopeID string `query:"scopeId" validate:"required"`
// 	Metric  string `param:"metric"`
// }) interface{} {
// 	err := r.ParseForm()
// 	if err != nil {
// 		return api.Errors.InvalidParameter(err)
// 	}
// 	ql, q, format := r.Form.Get("ql"), r.Form.Get("q"), r.Form.Get("format")
// 	r.Form.Del("ql")
// 	r.Form.Del("q")
// 	r.Form.Del("format")
// 	if len(format) == 0 {
// 		format = "influxdb"
// 	}
// 	if len(ql) == 0 {
// 		ql = "influxql"
// 	}
// 	if len(q) == 0 {
// 		byts, err := ioutil.ReadAll(r.Body)
// 		if err == nil {
// 			q = string(byts)
// 		}
// 	}
// 	// Search metadata
// 	metrics, err := p.q.MetricMeta(api.Language(r), params.Scope, params.ScopeID, params.Metric)
// 	if err != nil {
// 		return api.Errors.Internal(err)
// 	}
// 	if len(metrics) <= 0 {
// 		return nil
// 	}
// 	// new excel file
// 	file := excelize.NewFile()
// 	streamWriter, err := file.NewStreamWriter(file.GetSheetName(0))
// 	if err != nil {
// 		return api.Errors.InvalidParameter(err)
// 	}
// 	metric := metrics[0]
// 	tagKeys := metric.TagsKeys()
// 	fieldKeys := metric.FieldsKeys()
// 	headers := []interface{}{
// 		"name",
// 		"timestamp",
// 	}
// 	for _, k := range tagKeys {
// 		headers = append(headers, "tags."+k)
// 	}
// 	for _, k := range fieldKeys {
// 		headers = append(headers, "fields."+k)
// 	}
// 	cell, _ := excelize.CoordinatesToCellName(1, 1)
// 	if err := streamWriter.SetRow(cell, headers); err != nil {
// 		return api.Errors.InvalidParameter(err)
// 	}
// 	row := 2
// 	err = p.q.ExportByTSQL(ql, q, format, api.Language(r), r.Form,
// 		func(id string, data []byte) error {
// 			source := make(map[string]interface{})
// 			err := json.Unmarshal(data, &source)
// 			if err != nil {
// 				return err
// 			}
// 			vals := []interface{}{
// 				source["name"],
// 				source["timestamp"],
// 			}
// 			for _, k := range tagKeys {
// 				vals = append(vals, getGetValueFromFlatMap(source, "tags."+k, "."))
// 			}
// 			for _, k := range fieldKeys {
// 				vals = append(vals, getGetValueFromFlatMap(source, "fields."+k, "."))
// 			}
// 			cell, _ := excelize.CoordinatesToCellName(1, row)
// 			if err := streamWriter.SetRow(cell, vals); err != nil {
// 				return err
// 			}
// 			row++
// 			return nil
// 		},
// 	)
// 	if err != nil {
// 		return api.Errors.InvalidParameter(err)
// 	}
// 	if err := streamWriter.Flush(); err != nil {
// 		return api.Errors.InvalidParameter(err)
// 	}
// 	filename := strings.Replace(uuid.NewV4().String(), "-", "", -1) + ".xlsx"
// 	w.Header().Set("Cache-Control", "no-cache, no-store, must-revalidate")
// 	w.Header().Set("Pragma", "no-cache")
// 	w.Header().Set("Expires", "0")
// 	w.Header().Set("charset", "utf-8")
// 	w.Header().Set("Content-Disposition", "attachment;filename="+filename)
// 	w.Header().Set("Content-Type", "application/octet-stream")
// 	// if err := file.SaveAs("test.xlsx"); err != nil {
// 	// 	return api.Errors.InvalidParameter(err)
// 	// }
// 	if _, err := file.WriteTo(w); err != nil {
// 		return api.Errors.InvalidParameter(err)
// 	}
// 	return nil
// }

// func getGetValueFromFlatMap(source map[string]interface{}, key string, sep string) interface{} {
// 	keys := strings.Split(key, sep)
// 	for i, k := range keys {
// 		v := source[k]
// 		if i < len(keys)-1 {
// 			v, ok := v.(map[string]interface{})
// 			if !ok {
// 				return nil
// 			}
// 			source = v
// 			continue
// 		}
// 		return v
// 	}
// 	return nil
// }
