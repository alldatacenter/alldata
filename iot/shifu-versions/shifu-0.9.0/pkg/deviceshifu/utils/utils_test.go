package utils

import (
	"net/http"
	"reflect"
	"testing"
)

func TestParseHTTPGetParams(t *testing.T) {
	url := "http://www.example.com?a=1&b=22&c=3&d="
	currentParam := map[string]string{
		"a": "1",
		"b": "22",
		"c": "3",
		"d": "",
	}

	params, err := ParseHTTPGetParams(url)
	if err != nil {
		t.Errorf("Error when ParseHTTPGetParams, %v", err)
	}
	if !reflect.DeepEqual(currentParam, params) {
		t.Errorf("Not math current Params,out: %v,current:%v", params, currentParam)
	}
}

func TestCopyHeader(t *testing.T) {
	url := "http://www.example.com"
	resp, err := http.Get(url)
	if err != nil {
		t.Errorf(err.Error())
	}
	responseHeader := resp.Header
	responseHeader2 := http.Header{}
	CopyHeader(responseHeader2, responseHeader)
	if !reflect.DeepEqual(responseHeader2, responseHeader) {
		t.Errorf("CopyHeader want: %v, got: %v", responseHeader, responseHeader2)
	}

	requestHeader := http.Header{}
	requestHeader.Add("Accept-Language", "zh-CN,zh;q=0.9")
	requestHeader.Add("Connection", "keep-alive")
	requestHeader2 := http.Header{}
	CopyHeader(requestHeader2, requestHeader)
	if !reflect.DeepEqual(requestHeader2, requestHeader) {
		t.Errorf("CopyHeader want: %v, got: %v", requestHeader, requestHeader2)
	}
}
