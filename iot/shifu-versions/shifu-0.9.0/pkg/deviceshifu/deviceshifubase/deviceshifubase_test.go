package deviceshifubase

import (
	"errors"
	"os"
	"testing"

	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
	"github.com/stretchr/testify/assert"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestMain(m *testing.M) {
	err := GenerateConfigMapFromSnippet(MockDeviceCmStr, MockDeviceConfigFolder)
	if err != nil {
		logger.Errorf("error when generateConfigmapFromSnippet, err: %v", err)
		os.Exit(-1)
	}
	m.Run()
	err = os.RemoveAll(MockDeviceConfigPath)
	if err != nil {
		logger.Fatal(err)
	}
}

func TestStartTelemetryCollection(t *testing.T) {
	mockds := &DeviceShifuBase{
		Name: "test",
		EdgeDevice: &v1alpha1.EdgeDevice{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: "test_namespace",
			},
		},
		DeviceShifuConfig: &DeviceShifuConfig{
			Telemetries: &DeviceShifuTelemetries{
				DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
					DeviceShifuTelemetryDefaultPushToServer:      unitest.ToPointer(true),
					DeviceShifuTelemetryDefaultCollectionService: unitest.ToPointer("test_endpoint-1"),
				},
				DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{
					"device_healthy": {
						DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
							PushSettings: &DeviceShifuTelemetryPushSettings{
								DeviceShifuTelemetryPushToServer:      unitest.ToPointer(false),
								DeviceShifuTelemetryCollectionService: unitest.ToPointer("test_endpoint-1"),
							},
							InitialDelayMs: unitest.ToPointer(int64(1)),
						},
					},
				},
			},
		},
		RestClient: mockRestClientFor("{\"spec\": {\"address\": \"http://192.168.15.48:12345/test_endpoint-1\",\"type\": \"HTTP\"}}", t),
	}

	testCases := []struct {
		Name        string
		inputDevice *DeviceShifuBase
		fn          func() (bool, error)
		expErrStr   string
	}{
		{
			"case 1 fn true with nil error",
			mockds,
			func() (bool, error) {
				return true, nil
			},
			"",
		},
		{
			"case 2 fn false with nil error",
			mockds,
			func() (bool, error) {
				return false, nil
			},
			"",
		},
		{
			"case 3 fn false with error",
			mockds,
			func() (bool, error) {
				return false, errors.New("exit")
			},
			"",
		},
	}

	for _, c := range testCases {
		t.Run(c.Name, func(t *testing.T) {
			err := c.inputDevice.telemetryCollection(c.fn)
			if len(c.expErrStr) > 0 {
				assert.Equal(t, c.expErrStr, err.Error())
			} else {
				assert.Nil(t, err)
			}
		})
	}

}

func TestNew(t *testing.T) {
	testCases := []struct {
		Name      string
		metaData  *DeviceShifuMetaData
		expErrStr string
		initEnv   func(t *testing.T)
	}{
		{
			"case 1 have empty name can not new device base",
			&DeviceShifuMetaData{},
			"DeviceShifu's name can't be empty",
			func(t *testing.T) {},
		},
		{
			"case 2 have empty configpath meta new device base",
			&DeviceShifuMetaData{
				Name: "test",
			},
			"Error parsing ConfigMap at /etc/edgedevice/config",
			func(t *testing.T) {},
		},
		{
			"case 3 have empty KubeConfigPath meta new device base",
			&DeviceShifuMetaData{
				Name:           "test",
				ConfigFilePath: "etc/edgedevice/config",
			},
			"unable to load in-cluster configuration, KUBERNETES_SERVICE_HOST and KUBERNETES_SERVICE_PORT must be defined",
			func(t *testing.T) {},
		},
		{
			"case 4 KubeConfigPath is NULL",
			&DeviceShifuMetaData{
				Name:           "test",
				ConfigFilePath: "etc/edgedevice/config",
				KubeConfigPath: "NULL",
				Namespace:      "default",
			},
			"",
			func(t *testing.T) {
				t.Setenv("KUBERNETES_SERVICE_HOST", "127.0.0.1")
				t.Setenv("KUBERNETES_SERVICE_PORT", "1080")
			},
		},
	}
	for _, c := range testCases {
		t.Run(c.Name, func(t *testing.T) {
			c.initEnv(t)
			base, mux, err := New(c.metaData)
			if len(c.expErrStr) > 0 {
				assert.Equal(t, c.expErrStr, err.Error())
				assert.Nil(t, base)
				assert.Nil(t, mux)
			} else {
				assert.NotNil(t, base)
				assert.NotNil(t, mux)
			}

		})
	}

}
