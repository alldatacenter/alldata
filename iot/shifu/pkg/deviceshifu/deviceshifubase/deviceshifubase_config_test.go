package deviceshifubase

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"path"
	"reflect"
	"testing"

	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"

	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	clientcmdapi "k8s.io/client-go/tools/clientcmd/api"

	"gopkg.in/yaml.v3"
)

// Str and default value
const (
	MockDeviceCmStr              = "configmap_snippet.yaml"
	MockDeviceWritFilePermission = 0644
	MockDeviceConfigPath         = "etc"
	MockConfigFile               = "mockconfig"
)

var MockDeviceConfigFolder = path.Join("etc", "edgedevice", "config")

type ConfigMapData struct {
	Data struct {
		DriverProperties string `yaml:"driverProperties"`
		Instructions     string `yaml:"instructions"`
		Telemetries      string `yaml:"telemetries"`
	} `yaml:"data"`
}

func TestNewDeviceShifuConfig(t *testing.T) {
	var (
		InstructionValueTypeInt32       = "Int32"
		InstructionReadWriteW           = "W"
		TelemetrySettingInterval  int64 = 1000
	)

	var DriverProperties = DeviceShifuDriverProperties{
		DriverSku:       "Edgenesis Mock Device",
		DriverImage:     "edgenesis/mockdevice:v0.0.1",
		DriverExecution: "python mock_driver.py",
	}

	var mockDeviceInstructions = map[string]*DeviceShifuInstruction{
		"get_reading": nil,
		"get_status":  nil,
		"set_reading": {
			DeviceShifuInstructionProperties: []DeviceShifuInstructionProperty{
				{
					ValueType:    InstructionValueTypeInt32,
					ReadWrite:    InstructionReadWriteW,
					DefaultValue: nil,
				},
			},
			DeviceShifuProtocolProperties: nil,
		},
		"start": nil,
		"stop":  nil,
	}

	var mockDeviceTelemetries = &DeviceShifuTelemetries{
		DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
			DeviceShifuTelemetryUpdateIntervalInMilliseconds: &TelemetrySettingInterval,
		},
	}

	mockdsc, err := NewDeviceShifuConfig(MockDeviceConfigFolder)
	if err != nil {
		t.Errorf(err.Error())
	}

	eq := reflect.DeepEqual(DriverProperties, mockdsc.DriverProperties)
	if !eq {
		t.Errorf("DriverProperties mismatch")
	}

	eq = reflect.DeepEqual(mockDeviceInstructions, mockdsc.Instructions.Instructions)
	if !eq {
		t.Errorf("Instruction mismatch")
	}

	eq = reflect.DeepEqual(mockDeviceTelemetries.DeviceShifuTelemetrySettings.DeviceShifuTelemetryUpdateIntervalInMilliseconds,
		mockdsc.Telemetries.DeviceShifuTelemetrySettings.DeviceShifuTelemetryUpdateIntervalInMilliseconds)
	if !eq {
		t.Errorf("Telemetries mismatch")
	}

}

func GenerateConfigMapFromSnippet(fileName string, folder string) error {
	snippetFile, err := os.ReadFile(fileName)
	if err != nil {
		return err
	}

	var cmData ConfigMapData
	err = yaml.Unmarshal(snippetFile, &cmData)
	if err != nil {
		logger.Fatalf("Error parsing ConfigMap %v, error: %v", fileName, err)
		return err
	}

	var MockDeviceConfigMapping = map[string]string{
		path.Join(MockDeviceConfigFolder, ConfigmapDriverPropertiesStr): cmData.Data.DriverProperties,
		path.Join(MockDeviceConfigFolder, ConfigmapInstructionsStr):     cmData.Data.Instructions,
		path.Join(MockDeviceConfigFolder, ConfigmapTelemetriesStr):      cmData.Data.Telemetries,
	}

	err = os.MkdirAll(MockDeviceConfigFolder, os.ModePerm)
	if err != nil {
		logger.Fatalf("Error creating path for: %v", MockDeviceConfigFolder)
		return err
	}

	for outputDir, data := range MockDeviceConfigMapping {
		err = os.WriteFile(outputDir, []byte(data), MockDeviceWritFilePermission)
		if err != nil {
			logger.Fatalf("Error creating configFile for: %v", outputDir)
			return err
		}
	}
	return nil
}

func Test_getRestConfig(t *testing.T) {
	testCases := []struct {
		Name      string
		path      string
		expErrStr string
	}{
		{
			"case 1 have empty kubepath get config failure",
			"",
			"unable to load in-cluster configuration, KUBERNETES_SERVICE_HOST and KUBERNETES_SERVICE_PORT must be defined",
		},
		{
			"case 2 use kubepath get config failure",
			"kubepath",
			"stat kubepath: no such file or directory",
		},
	}

	for _, c := range testCases {
		t.Run(c.Name, func(t *testing.T) {
			config, err := getRestConfig(c.path)
			if len(c.expErrStr) > 0 {
				assert.Equal(t, c.expErrStr, err.Error())
				assert.Nil(t, config)
			} else {
				assert.Nil(t, err)
			}
		})
	}
}

func Test_newEdgeDeviceRestClient(t *testing.T) {
	testCases := []struct {
		Name      string
		config    *rest.Config
		expResult string
		expErrStr string
	}{
		{
			"case 1 can generate client with empty config",
			&rest.Config{},
			"v1alpha1",
			"",
		},
	}

	for _, c := range testCases {
		t.Run(c.Name, func(t *testing.T) {
			res, err := newEdgeDeviceRestClient(c.config)
			if len(c.expErrStr) > 0 {
				assert.Equal(t, c.expErrStr, err.Error())
			} else {
				assert.Nil(t, err)
			}
			assert.Equal(t, res.APIVersion().Version, c.expResult)
		})
	}
}

func TestNewEdgeDevice(t *testing.T) {

	ms := mockHttpServer(t)
	defer ms.Close()

	writeMockConfigFile(t, ms.URL)

	testCases := []struct {
		Name      string
		config    *EdgeDeviceConfig
		expErrStr string
	}{
		{
			"case 1 have mock config can get mock edge device",
			&EdgeDeviceConfig{
				NameSpace:      "test",
				DeviceName:     "httpdevice",
				KubeconfigPath: MockConfigFile,
			},
			"",
		},
	}

	for _, c := range testCases {
		t.Run(c.Name, func(t *testing.T) {
			ds, _, err := NewEdgeDevice(c.config)
			if len(c.expErrStr) > 0 {
				assert.Equal(t, c.expErrStr, err.Error())
				assert.Nil(t, ds)
			} else {
				assert.Nil(t, err)
				assert.Equal(t, "test_namespace", ds.Namespace)
				assert.Equal(t, "test_name", ds.Name)
			}
		})
	}
}

type MockResponse struct {
	v1alpha1.EdgeDevice
}

func mockHttpServer(t *testing.T) *httptest.Server {
	mockrs := MockResponse{
		EdgeDevice: v1alpha1.EdgeDevice{
			ObjectMeta: metav1.ObjectMeta{
				Name:      "test_name",
				Namespace: "test_namespace",
			},
			Status: v1alpha1.EdgeDeviceStatus{
				EdgeDevicePhase: (*v1alpha1.EdgeDevicePhase)(unitest.ToPointer("Success")),
			},
		},
	}

	dsByte, _ := json.Marshal(mockrs)

	// Implements the http.Handler interface to be passed to httptest.NewServer
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		path := r.URL.Path
		switch path {
		case "/apis/shifu.edgenesis.io/v1alpha1/namespaces/test/edgedevices/httpdevice":
			w.Header().Add("Content-Type", "application/json")
			w.WriteHeader(http.StatusOK)
			_, err := w.Write(dsByte)
			if err != nil {
				t.Errorf("failed to write response")
			}
		default:
			t.Errorf("Not expected to request: %s", r.URL.Path)
		}
	}))
	return server
}

func writeMockConfigFile(t *testing.T, serverURL string) {
	fakeConfig := clientcmdapi.NewConfig()
	fakeConfig.APIVersion = "v1"
	fakeConfig.CurrentContext = "alpha"

	fakeConfig.Clusters["alpha"] = &clientcmdapi.Cluster{
		Server:                serverURL,
		InsecureSkipTLSVerify: true,
	}

	fakeConfig.Contexts["alpha"] = &clientcmdapi.Context{
		Cluster: "alpha",
	}

	err := clientcmd.WriteToFile(*fakeConfig, MockConfigFile)
	if err != nil {
		t.Errorf("write mock file failed")
	}

}

func TestDeviceShifuTelemetrySettingsInit(t *testing.T) {
	testCases := []struct {
		name           string
		input          *DeviceShifuTelemetrySettings
		expectedOutput *DeviceShifuTelemetrySettings
		expectedErr    string
	}{
		{
			name:  "testCase1 init all data",
			input: &DeviceShifuTelemetrySettings{},
			expectedOutput: &DeviceShifuTelemetrySettings{
				DeviceShifuTelemetryUpdateIntervalInMilliseconds: unitest.ToPointer(DeviceDefaultTelemetryUpdateIntervalInMS),
				DeviceShifuTelemetryTimeoutInMilliseconds:        unitest.ToPointer(DeviceTelemetryTimeoutInMS),
				DeviceShifuTelemetryInitialDelayInMilliseconds:   unitest.ToPointer(DeviceTelemetryInitialDelayInMS),
				DeviceShifuTelemetryDefaultPushToServer:          unitest.ToPointer(false),
			},
			expectedErr: "",
		},
		{
			name: "testCase2 init with some data",
			input: &DeviceShifuTelemetrySettings{
				DeviceShifuTelemetryUpdateIntervalInMilliseconds: unitest.ToPointer[int64](123),
			},
			expectedOutput: &DeviceShifuTelemetrySettings{
				DeviceShifuTelemetryUpdateIntervalInMilliseconds: unitest.ToPointer[int64](123),
				DeviceShifuTelemetryTimeoutInMilliseconds:        unitest.ToPointer(DeviceTelemetryTimeoutInMS),
				DeviceShifuTelemetryInitialDelayInMilliseconds:   unitest.ToPointer(DeviceTelemetryInitialDelayInMS),
				DeviceShifuTelemetryDefaultPushToServer:          unitest.ToPointer(false),
			},
			expectedErr: "",
		},
		{
			name:        "testCase3 init with nil",
			expectedErr: "only structs, maps, and slices are supported",
		},
	}
	for _, tC := range testCases {
		t.Run(tC.name, func(t *testing.T) {
			err := tC.input.load()
			assert.Equal(t, tC.expectedOutput, tC.input)
			if tC.expectedErr == "" {
				assert.Nil(t, err)
			} else {
				assert.NotNil(t, err)
				assert.Equal(t, tC.expectedErr, err.Error())
			}
		})
	}
}

func TestDeviceShifuTelemetriesInit(t *testing.T) {
	defaultTelemetrySettings := &DeviceShifuTelemetrySettings{
		DeviceShifuTelemetryUpdateIntervalInMilliseconds: unitest.ToPointer(DeviceDefaultTelemetryUpdateIntervalInMS),
		DeviceShifuTelemetryTimeoutInMilliseconds:        unitest.ToPointer(DeviceTelemetryTimeoutInMS),
		DeviceShifuTelemetryInitialDelayInMilliseconds:   unitest.ToPointer(DeviceTelemetryInitialDelayInMS),
		DeviceShifuTelemetryDefaultPushToServer:          unitest.ToPointer(false),
	}
	testCases := []struct {
		name           string
		input          *DeviceShifuTelemetries
		expectedOutput *DeviceShifuTelemetries
		expectedErr    string
	}{
		{
			name: "testCase1 without Telemetries Map",
			input: &DeviceShifuTelemetries{
				DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{},
			},
			expectedOutput: &DeviceShifuTelemetries{
				DeviceShifuTelemetries:       make(map[string]*DeviceShifuTelemetry),
				DeviceShifuTelemetrySettings: defaultTelemetrySettings,
			},
			expectedErr: "",
		},
		{
			name: "testCase2 without Telemetry",
			input: &DeviceShifuTelemetries{
				DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{"test": nil},
			},
			expectedOutput: &DeviceShifuTelemetries{
				DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{
					"test": {
						DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
							InitialDelayMs: unitest.ToPointer(DeviceInstructionInitialDelay),
							IntervalMs:     unitest.ToPointer(DeviceInstructionInitialDelay),
						}}},
				DeviceShifuTelemetrySettings: defaultTelemetrySettings,
			},
		},
		{
			name:  "testCase3 DeviceShifuTelemetries is emptu",
			input: &DeviceShifuTelemetries{},
			expectedOutput: &DeviceShifuTelemetries{
				DeviceShifuTelemetries:       map[string]*DeviceShifuTelemetry{},
				DeviceShifuTelemetrySettings: defaultTelemetrySettings,
			},
			expectedErr: "",
		},
	}
	for _, tC := range testCases {
		t.Run(tC.name, func(t *testing.T) {
			err := tC.input.load()
			assert.Equal(t, tC.expectedOutput, tC.input)
			if tC.expectedErr == "" {
				assert.Nil(t, err)
			} else {
				assert.NotNil(t, err)
				assert.Equal(t, tC.expectedErr, err.Error())
			}
		})
	}
}

func TestDeviceShifuTelemetryInit(t *testing.T) {
	testCases := []struct {
		name           string
		input          *DeviceShifuTelemetry
		expectedOutput *DeviceShifuTelemetry
		expectedErr    string
	}{
		{
			name:  "testCase1 init all data",
			input: &DeviceShifuTelemetry{},
			expectedOutput: &DeviceShifuTelemetry{
				DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
					InitialDelayMs: unitest.ToPointer(DeviceInstructionInitialDelay),
					IntervalMs:     unitest.ToPointer(DeviceInstructionInitialDelay),
				},
			},
			expectedErr: "",
		},
		{
			name: "testCase2 init with some data",
			input: &DeviceShifuTelemetry{
				DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
					InitialDelayMs: unitest.ToPointer[int64](123),
				},
			},
			expectedOutput: &DeviceShifuTelemetry{
				DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
					InitialDelayMs: unitest.ToPointer[int64](123),
					IntervalMs:     unitest.ToPointer(DeviceInstructionInitialDelay),
				},
			},
			expectedErr: "",
		},
		{
			name:        "testCase3 init with nil",
			expectedErr: "only structs, maps, and slices are supported",
		},
	}
	for _, tC := range testCases {
		t.Run(tC.name, func(t *testing.T) {
			err := tC.input.load()
			assert.Equal(t, tC.expectedOutput, tC.input)
			if tC.expectedErr == "" {
				assert.Nil(t, err)
			} else {
				assert.NotNil(t, err)
				assert.Equal(t, tC.expectedErr, err.Error())
			}
		})
	}
}

func TestDeviceshifuInstructionSettingsInit(t *testing.T) {
	testCases := []struct {
		name           string
		input          *DeviceShifuInstructionSettings
		expectedOutput *DeviceShifuInstructionSettings
		expectedErr    string
	}{
		{
			name:  "testCase1 init all data",
			input: &DeviceShifuInstructionSettings{},
			expectedOutput: &DeviceShifuInstructionSettings{
				DefaultTimeoutSeconds: unitest.ToPointer(DeviceDefaultGlobalTimeoutInSeconds),
			},
			expectedErr: "",
		},
		{
			name:        "testCase2 init with nil",
			expectedErr: "only structs, maps, and slices are supported",
		},
	}
	for _, tC := range testCases {
		t.Run(tC.name, func(t *testing.T) {
			err := tC.input.load()
			assert.Equal(t, tC.expectedOutput, tC.input)
			if tC.expectedErr == "" {
				assert.Nil(t, err)
			} else {
				assert.NotNil(t, err)
				assert.Equal(t, tC.expectedErr, err.Error())
			}
		})
	}
}

func TestDeviceShifuInstructions(t *testing.T) {
	testCases := []struct {
		name           string
		input          *DeviceShifuInstructions
		expectedOutput *DeviceShifuInstructions
		expectedErr    string
	}{
		{
			name: "testCase1 with DefaultTimeoutSeconds",
			input: &DeviceShifuInstructions{
				InstructionSettings: &DeviceShifuInstructionSettings{
					DefaultTimeoutSeconds: unitest.ToPointer[int64](123),
				},
			},
			expectedOutput: &DeviceShifuInstructions{
				Instructions: map[string]*DeviceShifuInstruction{},
				InstructionSettings: &DeviceShifuInstructionSettings{
					DefaultTimeoutSeconds: unitest.ToPointer[int64](123),
				},
			},
			expectedErr: "",
		},
		{
			name:  "testCase2 with empty",
			input: &DeviceShifuInstructions{},
			expectedOutput: &DeviceShifuInstructions{
				Instructions: map[string]*DeviceShifuInstruction{},
				InstructionSettings: &DeviceShifuInstructionSettings{
					DefaultTimeoutSeconds: unitest.ToPointer(DeviceDefaultGlobalTimeoutInSeconds),
				},
			},
			expectedErr: "",
		},
	}
	for _, tC := range testCases {
		t.Run(tC.name, func(t *testing.T) {
			err := tC.input.load()
			assert.Equal(t, tC.expectedOutput, tC.input)
			if tC.expectedErr == "" {
				assert.Nil(t, err)
			} else {
				assert.NotNil(t, err)
				assert.Equal(t, tC.expectedErr, err.Error())
			}
		})
	}
}

func TestDeviceShifuDriverProperties(t *testing.T) {
	testCases := []struct {
		name           string
		input          *DeviceShifuDriverProperties
		expectedOutput *DeviceShifuDriverProperties
		expectedErr    string
	}{
		{
			name: "testCase1 with DefaultTimeoutSeconds",
			input: &DeviceShifuDriverProperties{
				DriverSku: "test",
			},
			expectedOutput: &DeviceShifuDriverProperties{
				DriverSku:       "test",
				DriverImage:     "defaultImage",
				DriverExecution: "defaultExecution",
			},
			expectedErr: "",
		},
		{
			name:  "testCase2 with empty",
			input: &DeviceShifuDriverProperties{},
			expectedOutput: &DeviceShifuDriverProperties{
				DriverSku:       "defaultSku",
				DriverImage:     "defaultImage",
				DriverExecution: "defaultExecution",
			},
			expectedErr: "",
		},
		{
			name:        "testCase3 with nil",
			expectedErr: "only structs, maps, and slices are supported",
		},
	}
	for _, tC := range testCases {
		t.Run(tC.name, func(t *testing.T) {
			err := tC.input.load()
			assert.Equal(t, tC.expectedOutput, tC.input)
			if tC.expectedErr == "" {
				assert.Nil(t, err)
			} else {
				assert.NotNil(t, err)
				assert.Equal(t, tC.expectedErr, err.Error())
			}
		})
	}
}

func TestDeviceShifuConfig(t *testing.T) {
	testCases := []struct {
		name           string
		input          *DeviceShifuConfig
		expectedOutput *DeviceShifuConfig
		expectedErr    string
	}{
		{
			name: "testCase1 with some data empty",
			input: &DeviceShifuConfig{
				Instructions: DeviceShifuInstructions{
					Instructions: map[string]*DeviceShifuInstruction{},
					InstructionSettings: &DeviceShifuInstructionSettings{
						DefaultTimeoutSeconds: unitest.ToPointer(DeviceDefaultGlobalTimeoutInSeconds),
					},
				},
				Telemetries: &DeviceShifuTelemetries{
					DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{
						"aaa": nil,
					},
					DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
						DeviceShifuTelemetryUpdateIntervalInMilliseconds: unitest.ToPointer[int64](123),
						DeviceShifuTelemetryDefaultPushToServer:          unitest.ToPointer(true),
						DeviceShifuTelemetryDefaultCollectionService:     unitest.ToPointer("endpoint"),
					},
				},
				DriverProperties: DeviceShifuDriverProperties{
					DriverSku: "testSKU",
				},
			},
			expectedOutput: &DeviceShifuConfig{
				Instructions: DeviceShifuInstructions{
					Instructions: map[string]*DeviceShifuInstruction{},
					InstructionSettings: &DeviceShifuInstructionSettings{
						DefaultTimeoutSeconds: unitest.ToPointer(DeviceDefaultGlobalTimeoutInSeconds),
					},
				},
				Telemetries: &DeviceShifuTelemetries{
					DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{
						"aaa": {DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
							InitialDelayMs: unitest.ToPointer(DeviceInstructionInitialDelay),
							IntervalMs:     unitest.ToPointer(DeviceInstructionInitialDelay),
						}},
					},
					DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
						DeviceShifuTelemetryUpdateIntervalInMilliseconds: unitest.ToPointer[int64](123),
						DeviceShifuTelemetryTimeoutInMilliseconds:        unitest.ToPointer(DeviceTelemetryTimeoutInMS),
						DeviceShifuTelemetryInitialDelayInMilliseconds:   unitest.ToPointer(DeviceTelemetryInitialDelayInMS),
						DeviceShifuTelemetryDefaultPushToServer:          unitest.ToPointer(true),
						DeviceShifuTelemetryDefaultCollectionService:     unitest.ToPointer("endpoint"),
					},
				},
				DriverProperties: DeviceShifuDriverProperties{
					DriverSku:       "testSKU",
					DriverImage:     "defaultImage",
					DriverExecution: "defaultExecution",
				},
			},
			expectedErr: "",
		},
		{
			name:  "testCase2 with empty",
			input: &DeviceShifuConfig{},
			expectedOutput: &DeviceShifuConfig{
				Instructions: DeviceShifuInstructions{
					Instructions: map[string]*DeviceShifuInstruction{},
					InstructionSettings: &DeviceShifuInstructionSettings{
						DefaultTimeoutSeconds: unitest.ToPointer(DeviceDefaultGlobalTimeoutInSeconds),
					},
				},
				Telemetries: &DeviceShifuTelemetries{
					DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{},
					DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
						DeviceShifuTelemetryUpdateIntervalInMilliseconds: unitest.ToPointer(DeviceDefaultTelemetryUpdateIntervalInMS),
						DeviceShifuTelemetryTimeoutInMilliseconds:        unitest.ToPointer(DeviceTelemetryTimeoutInMS),
						DeviceShifuTelemetryInitialDelayInMilliseconds:   unitest.ToPointer(DeviceTelemetryInitialDelayInMS),
						DeviceShifuTelemetryDefaultPushToServer:          unitest.ToPointer(false),
					},
				},
				DriverProperties: DeviceShifuDriverProperties{
					DriverSku:       "defaultSku",
					DriverImage:     "defaultImage",
					DriverExecution: "defaultExecution",
				},
			},
			expectedErr: "",
		},
	}
	for _, tC := range testCases {
		t.Run(tC.name, func(t *testing.T) {
			err := tC.input.load()
			assert.Equal(t, tC.expectedOutput, tC.input)
			if tC.expectedErr == "" {
				assert.Nil(t, err)
			} else {
				assert.NotNil(t, err)
				assert.Equal(t, tC.expectedErr, err.Error())
			}
		})
	}
}
