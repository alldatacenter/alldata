/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package metriccache

import (
	"fmt"
	"reflect"
	"sort"
	"time"
)

func fieldAvgOfMetricList(metricsList interface{}, aggregateParam AggregateParam) (float64, error) {
	sum := 0.0

	inputType := reflect.TypeOf(metricsList).Kind()
	if inputType != reflect.Slice && inputType != reflect.Array {
		return 0, fmt.Errorf("metrics input type must be slice or array, %v is illegal", inputType.String())
	}

	metrics := reflect.ValueOf(metricsList)
	if metrics.Len() == 0 {
		return 0, fmt.Errorf("metric input is empty")
	}

	for i := 0; i < metrics.Len(); i++ {
		metricStruct := metrics.Index(i)
		fieldValue := metricStruct.FieldByName(aggregateParam.ValueFieldName)
		fieldType := fieldValue.Type().Kind()
		if fieldType != reflect.Float32 && fieldType != reflect.Float64 {
			return 0, fmt.Errorf("field type must be float32 or float64, %v is illegal", fieldType.String())
		}
		sum += fieldValue.Float()
	}
	return sum / float64(metrics.Len()), nil
}

func fieldPercentileOfMetricList(metricsList interface{}, aggregateParam AggregateParam, percentile float32) (float64, error) {
	if percentile <= 0 || percentile > 1 || float32(int32(percentile*1000))/1000 != percentile {
		return 0, fmt.Errorf("metrics percentile must be a fixed-point number between 0.001 to 1.000, %v is illegal",
			percentile)
	}

	inputType := reflect.TypeOf(metricsList).Kind()
	if inputType != reflect.Slice && inputType != reflect.Array {
		return 0, fmt.Errorf("metrics input type must be slice or array, %v is illegal", inputType.String())
	}

	metrics := reflect.ValueOf(metricsList)
	if metrics.Len() == 0 {
		return 0, fmt.Errorf("metric input is empty")
	}

	// NOTE: use a general sort for the small case
	sortList := make([]float64, metrics.Len())

	for i := 0; i < metrics.Len(); i++ {
		metricStruct := metrics.Index(i)
		fieldValue := metricStruct.FieldByName(aggregateParam.ValueFieldName)
		fieldType := fieldValue.Type().Kind()
		if fieldType != reflect.Float32 && fieldType != reflect.Float64 {
			return 0, fmt.Errorf("field type must be float32 or float64, %v is illegal", fieldType.String())
		}
		sortList[i] = fieldValue.Float()
	}

	sort.Slice(sortList, func(i, j int) bool {
		return sortList[i] < sortList[j]
	})

	idx := int(float32(metrics.Len())*percentile) - 1
	if idx < 0 {
		idx = 0
	}
	return sortList[idx], nil
}

func fieldLastOfMetricList(metricsList interface{}, aggregateParam AggregateParam) (float64, error) {
	lastValue := 0.0
	lastTime := int64(0)

	inputType := reflect.TypeOf(metricsList).Kind()
	if inputType != reflect.Slice && inputType != reflect.Array {
		return 0, fmt.Errorf("metrics input type must be slice or array, %v is illegal", inputType.String())
	}

	metrics := reflect.ValueOf(metricsList)
	if metrics.Len() == 0 {
		return 0, fmt.Errorf("metric input is empty")
	}

	for i := 0; i < metrics.Len(); i++ {
		metricStruct := metrics.Index(i)
		fieldValue := metricStruct.FieldByName(aggregateParam.ValueFieldName)
		if !fieldValue.IsValid() {
			return 0, fmt.Errorf("fieldValue not Valid, metricStruct: %v ", metricStruct)
		}
		fieldType := fieldValue.Type().Kind()
		if fieldType != reflect.Float32 && fieldType != reflect.Float64 {
			return 0, fmt.Errorf("field type must be float32 or float64, %v is illegal", fieldType.String())
		}

		fieldTimeValue := metricStruct.FieldByName(aggregateParam.TimeFieldName)
		if !fieldTimeValue.IsValid() {
			return 0, fmt.Errorf("fieldTimeValue not Valid, metricStruct: %v ", metricStruct)
		}

		if !fieldTimeValue.CanInterface() {
			return 0, fmt.Errorf("fieldTimeValue can not Interface, metricStruct: %v ", metricStruct)
		}

		timestamp, ok := fieldTimeValue.Interface().(time.Time)
		if !ok {
			return 0, fmt.Errorf("timestamp field type must be *time.Time, and value must not be nil. %v is illegal! ", fieldTimeValue)
		}
		if timestamp.UnixNano() > lastTime {
			lastTime = timestamp.UnixNano()
			lastValue = fieldValue.Float()
		}
	}
	return lastValue, nil
}

func fieldCountOfMetricList(metricsList interface{}, aggregateParam AggregateParam) (float64, error) {
	inputType := reflect.TypeOf(metricsList).Kind()
	if inputType != reflect.Slice && inputType != reflect.Array {
		return 0, fmt.Errorf("metrics input type must be slice or array, %v is illegal", inputType.String())
	}

	metrics := reflect.ValueOf(metricsList)
	return float64(metrics.Len()), nil
}

func percentileFuncOfMetricList(percentile float32) AggregationFunc {
	return func(metricsList interface{}, param AggregateParam) (float64, error) {
		return fieldPercentileOfMetricList(metricsList, param, percentile)
	}
}

func fieldLastOfMetricListBool(metricsList interface{}, aggregateParam AggregateParam) (bool, error) {
	lastValue := false
	lastTime := int64(0)

	inputType := reflect.TypeOf(metricsList).Kind()
	if inputType != reflect.Slice && inputType != reflect.Array {
		return false, fmt.Errorf("metrics input type must be slice or array, %v is illegal", inputType.String())
	}

	metrics := reflect.ValueOf(metricsList)
	if metrics.Len() == 0 {
		return false, fmt.Errorf("metric input is empty")
	}

	for i := 0; i < metrics.Len(); i++ {
		metricStruct := metrics.Index(i)
		fieldValue := metricStruct.FieldByName(aggregateParam.ValueFieldName)
		if !fieldValue.IsValid() {
			return false, fmt.Errorf("fieldValue not Valid, metricStruct: %v ", metricStruct)
		}
		fieldType := fieldValue.Type().Kind()
		if fieldType != reflect.Bool {
			return false, fmt.Errorf("field type must be float32 or float64, %v is illegal", fieldType.String())
		}

		fieldTimeValue := metricStruct.FieldByName(aggregateParam.TimeFieldName)
		if !fieldTimeValue.IsValid() {
			return false, fmt.Errorf("fieldTimeValue not Valid, metricStruct: %v ", metricStruct)
		}

		if !fieldTimeValue.CanInterface() {
			return false, fmt.Errorf("fieldTimeValue can not Interface, metricStruct: %v ", metricStruct)
		}

		timestamp, ok := fieldTimeValue.Interface().(time.Time)
		if !ok {
			return false, fmt.Errorf("timestamp field type must be *time.Time, and value must not be nil. %v is illegal! ", fieldTimeValue)
		}
		if timestamp.UnixNano() > lastTime {
			lastTime = timestamp.UnixNano()
			lastValue = fieldValue.Bool()
		}
	}
	return lastValue, nil
}

// metricsListSortedByTime is a list of metrics sort by timestamp from old to new
func generateMetricAggregateInfo(metricsListSortedByTime interface{}) (*AggregateInfo, error) {
	inputType := reflect.TypeOf(metricsListSortedByTime).Kind()
	if inputType != reflect.Slice && inputType != reflect.Array {
		return nil, fmt.Errorf("metrics input type must be slice or array, %v is illegal", inputType.String())
	}

	aggregateInfo := &AggregateInfo{}
	metrics := reflect.ValueOf(metricsListSortedByTime)
	length := metrics.Len()
	if length == 0 {
		return aggregateInfo, nil
	}
	aggregateInfo.MetricsCount = int64(length)

	firstMetric := metrics.Index(0)
	firstTimeValue := firstMetric.FieldByName("Timestamp")
	if firstTime, err := reflectValueToTime(&firstTimeValue); err == nil {
		aggregateInfo.MetricStart = firstTime
	} else {
		return aggregateInfo, err
	}

	lastMetric := metrics.Index(length - 1)
	lastTimeValue := lastMetric.FieldByName("Timestamp")
	if lastTime, err := reflectValueToTime(&lastTimeValue); err == nil {
		aggregateInfo.MetricEnd = lastTime
	} else {
		return aggregateInfo, err
	}
	return aggregateInfo, nil
}

func reflectValueToTime(v *reflect.Value) (*time.Time, error) {
	if v == nil {
		return nil, fmt.Errorf("input value is nil")
	}
	if !v.IsValid() {
		return nil, fmt.Errorf("fieldTimeValue not Valid, metricStruct: %v ", v)
	}
	timeStruct, ok := v.Interface().(time.Time)
	if !ok {
		return nil, fmt.Errorf("time struct interface convert failed")
	}
	return &timeStruct, nil
}
