package utils

import (
	"fmt"
	"net/http"
	"strings"

	"github.com/edgenesis/shifu/pkg/logger"
)

// ParseHTTPGetParams Parse Params from HTTP get request
// for example example.com?a=1&b=2
// reply map[string]string{a:1,b:2},nil
// for url.Query() cannot parse symbol like % + and etc
func ParseHTTPGetParams(urlStr string) (map[string]string, error) {
	var paramStr string
	logger.Infof("url: ", urlStr)
	url := strings.Split(urlStr, "?")

	if len(url) <= 0 {
		return nil, fmt.Errorf("empty Query")
	} else if len(url) == 1 {
		paramStr = url[0]
	} else {
		paramStr = url[1]
	}

	params := strings.Split(paramStr, "&")

	result := make(map[string]string, len(params))

	for _, item := range params {
		info := strings.Split(item, "=")
		if len(info) == 2 {
			result[info[0]] = info[1]
		} else if len(info) == 1 {
			result[info[0]] = ""
		}
	}

	return result, nil
}

// CopyHeader HTTP header type:
// type Header map[string][]string
func CopyHeader(dst, src http.Header) {
	for header, headerValueList := range src {
		for _, value := range headerValueList {
			dst.Add(header, value)
		}
	}
}
