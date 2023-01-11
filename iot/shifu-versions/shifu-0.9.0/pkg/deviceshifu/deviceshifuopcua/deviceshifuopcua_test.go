package deviceshifuopcua

import (
	"io"
	"os"
	"testing"

	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"github.com/edgenesis/shifu/pkg/logger"

	"k8s.io/apimachinery/pkg/util/wait"
)

func TestMain(m *testing.M) {
	err := GenerateConfigMapFromSnippet(MockDeviceCmStr, MockDeviceConfigFolder)
	if err != nil {
		logger.Errorf("error when generateConfigmapFromSnippet,err: %v", err)
		os.Exit(-1)
	}
	m.Run()
	err = os.RemoveAll(MockDeviceConfigPath)
	if err != nil {
		logger.Fatal(err)
	}
}

func TestNew(t *testing.T) {
	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
		Name:           "TeststartHTTPServer",
		ConfigFilePath: "etc/edgedevice/config",
		KubeConfigPath: deviceshifubase.DeviceKubeconfigDoNotLoadStr,
		Namespace:      "TeststartHTTPServerNamespace",
	}

	_, err := New(deviceShifuMetadata)
	if err != nil {
		t.Errorf("Failed creating new deviceshifu")
	}
}

func TestDeviceShifuEmptyNamespace(t *testing.T) {
	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
		Name:           "TestDeviceShifuEmptyNamespace",
		ConfigFilePath: "etc/edgedevice/config",
		KubeConfigPath: deviceshifubase.DeviceKubeconfigDoNotLoadStr,
	}

	_, err := New(deviceShifuMetadata)
	if err != nil {
		logger.Errorf("%v", err)
	} else {
		t.Errorf("DeviceShifu Test with empty namespace failed")
	}
}

func TestStart(t *testing.T) {
	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
		Name:           "TestStartOPCUA",
		ConfigFilePath: "etc/edgedevice/config",
		KubeConfigPath: deviceshifubase.DeviceKubeconfigDoNotLoadStr,
		Namespace:      "TestStartNamespace",
	}

	mockds, err := New(deviceShifuMetadata)
	if err != nil {
		t.Errorf("Failed creating new deviceshifu")
	}

	if err := mockds.Start(wait.NeverStop); err != nil {
		t.Errorf("DeviceShifu.Start failed due to: %v", err.Error())
	}

	if err := mockds.Stop(); err != nil {
		t.Errorf("unable to stop mock deviceShifu, error: %+v", err)
	}
}

func TestDeviceHealthHandler(t *testing.T) {
	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
		Name:           "TeststartHTTPServerOPCUA",
		ConfigFilePath: "etc/edgedevice/config",
		KubeConfigPath: deviceshifubase.DeviceKubeconfigDoNotLoadStr,
		Namespace:      "TeststartHTTPServerNamespace",
	}

	mockds, err := New(deviceShifuMetadata)
	if err != nil {
		t.Errorf("Failed creating new deviceshifu")
	}

	if err := mockds.Start(wait.NeverStop); err != nil {
		t.Errorf("DeviceShifu.Start failed due to: %v", err.Error())
	}

	resp, err := unitest.RetryAndGetHTTP("http://localhost:8080/health", 3)
	if err != nil {
		t.Errorf("HTTP GET returns an error %v", err.Error())
	}

	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		t.Errorf("unable to read response body, error: %v", err.Error())
	}

	if string(body) != deviceshifubase.DeviceIsHealthyStr {
		t.Errorf("%+v", body)
	}

	if err := mockds.Stop(); err != nil {
		t.Errorf("unable to stop mock deviceShifu, error: %+v", err)
	}
}
