package utils

import (
	"bufio"
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strings"

	"github.com/edgenesis/shifu/pkg/k8s/crd/usermetrics/types"
	"github.com/edgenesis/shifu/pkg/logger"
)

const (
	URL_EXTERNAL_IP        = "http://cip.cc"
	URL_IP_LINE            = "<pre>IP"
	URL_SHIFU_TELEMETRY    = "https://telemetry.shifu.dev/shifu-telemetry/"
	URL_DEFAULT_PUBLIC_IP  = "0.0.0.0"
	TASK_RUN_DEMO_KIND     = "run_shifu_release"
	DEFAULT_SOURCE         = "default"
	HTTP_CONTENT_TYPE_JSON = "application/json"
)

var TelemetryIntervalInSecond int

func GetPublicIPAddr(url string) (string, error) {
	resp, err := http.Get(url)
	if err != nil {
		return "", fmt.Errorf("error getting public IP")
	}

	defer resp.Body.Close()
	if resp.StatusCode == http.StatusOK {
		bodyBytes, err := io.ReadAll(resp.Body)
		if err != nil {
			logger.Errorf("Error getting response of IP query")
			return "", err
		}

		responseText := string(bodyBytes)
		scanner := bufio.NewScanner(strings.NewReader(responseText))
		for scanner.Scan() {
			if strings.Contains(scanner.Text(), URL_IP_LINE) {
				ipString := strings.Split(scanner.Text(), ": ")
				return ipString[len(ipString)-1], nil
			}
		}

	}
	return "", errors.New("Did not find IP in return query")
}

func SendUserMetrics(telemetry types.UserMetricsResponse) error {
	postBodyJson, err := json.Marshal(telemetry)
	if err != nil {
		logger.Errorf("Error marshaling telemetry")
		return err
	}

	resp, err := http.Post(URL_SHIFU_TELEMETRY, HTTP_CONTENT_TYPE_JSON, bytes.NewBuffer(postBodyJson))
	if err != nil {
		logger.Errorln("error posting telemetry, errors: ", err)
		return err
	}

	defer resp.Body.Close()
	return nil
}
