package deviceshifubase

import (
	"context"
	"errors"

	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
	"github.com/imdario/mergo"

	"gopkg.in/yaml.v3"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/runtime/serializer"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	"knative.dev/pkg/configmap"
)

// DeviceShifuConfig data under Configmap, Settings of deviceShifu
type DeviceShifuConfig struct {
	DriverProperties         DeviceShifuDriverProperties
	Instructions             DeviceShifuInstructions
	Telemetries              *DeviceShifuTelemetries
	CustomInstructionsPython map[string]string `yaml:"customInstructionsPython"`
}

// DeviceShifuDriverProperties properties of deviceshifuDriver
type DeviceShifuDriverProperties struct {
	DriverSku       string `yaml:"driverSku"`
	DriverImage     string `yaml:"driverImage"`
	DriverExecution string `yaml:"driverExecution,omitempty"`
}

// DeviceShifuInstructions Instructions of devicehsifu
type DeviceShifuInstructions struct {
	Instructions        map[string]*DeviceShifuInstruction `yaml:"instructions"`
	InstructionSettings *DeviceShifuInstructionSettings    `yaml:"instructionSettings,omitempty"`
}

// DeviceShifuInstructionSettings Settings of all instructions
type DeviceShifuInstructionSettings struct {
	DefaultTimeoutSeconds *int64 `yaml:"defaultTimeoutSeconds,omitempty"`
}

// DeviceShifuInstruction Instruction of deviceshifu
type DeviceShifuInstruction struct {
	DeviceShifuInstructionProperties []DeviceShifuInstructionProperty `yaml:"argumentPropertyList,omitempty"`
	DeviceShifuProtocolProperties    map[string]string                `yaml:"protocolPropertyList,omitempty"`
}

// DeviceShifuInstructionProperty property of instruction
type DeviceShifuInstructionProperty struct {
	ValueType    string      `yaml:"valueType"`
	ReadWrite    string      `yaml:"readWrite"`
	DefaultValue interface{} `yaml:"defaultValue"`
}

// DeviceShifuTelemetryPushSettings settings of push under telemetry
type DeviceShifuTelemetryPushSettings struct {
	DeviceShifuTelemetryCollectionService *string `yaml:"telemetryCollectionService,omitempty"`
	DeviceShifuTelemetryPushToServer      *bool   `yaml:"pushToServer,omitempty"`
}

// DeviceShifuTelemetryProperties properties of Telemetry
type DeviceShifuTelemetryProperties struct {
	DeviceInstructionName *string                           `yaml:"instruction"`
	InitialDelayMs        *int64                            `yaml:"initialDelayMs,omitempty"`
	IntervalMs            *int64                            `yaml:"intervalMs,omitempty"`
	PushSettings          *DeviceShifuTelemetryPushSettings `yaml:"pushSettings,omitempty"`
}

// DeviceShifuTelemetrySettings settings of Telemetry
type DeviceShifuTelemetrySettings struct {
	DeviceShifuTelemetryUpdateIntervalInMilliseconds *int64  `yaml:"telemetryUpdateIntervalInMilliseconds,omitempty"`
	DeviceShifuTelemetryTimeoutInMilliseconds        *int64  `yaml:"telemetryTimeoutInMilliseconds,omitempty"`
	DeviceShifuTelemetryInitialDelayInMilliseconds   *int64  `yaml:"telemetryInitialDelayInMilliseconds,omitempty"`
	DeviceShifuTelemetryDefaultPushToServer          *bool   `yaml:"defaultPushToServer,omitempty"`
	DeviceShifuTelemetryDefaultCollectionService     *string `yaml:"defaultTelemetryCollectionService,omitempty"`
}

// DeviceShifuTelemetries Telemetries of deviceshifu
type DeviceShifuTelemetries struct {
	DeviceShifuTelemetrySettings *DeviceShifuTelemetrySettings    `yaml:"telemetrySettings,omitempty"`
	DeviceShifuTelemetries       map[string]*DeviceShifuTelemetry `yaml:"telemetries,omitempty"`
}

// DeviceShifuTelemetry properties of telemetry
type DeviceShifuTelemetry struct {
	DeviceShifuTelemetryProperties DeviceShifuTelemetryProperties `yaml:"properties,omitempty"`
}

// EdgeDeviceConfig config of EdgeDevice
type EdgeDeviceConfig struct {
	NameSpace      string
	DeviceName     string
	KubeconfigPath string
}

// default value
const (
	DeviceInstructionInitialDelay            int64 = 3000
	DeviceTelemetryTimeoutInMS               int64 = 3000
	DeviceTelemetryUpdateIntervalInMS        int64 = 3000
	DeviceTelemetryInitialDelayInMS          int64 = 3000
	DeviceDefaultConnectionTimeoutInMS       int64 = 3000
	DeviceDefaultRequestTimeoutInMS          int64 = 1000
	DeviceDefaultTelemetryUpdateIntervalInMS int64 = 1000
	DeviceDefaultGlobalTimeoutInSeconds      int64 = 3
	DefaultHTTPServerTimeoutInSeconds        int64 = 0
)

// NewDeviceShifuConfig Read the configuration under the path directory and return configuration
func NewDeviceShifuConfig(path string) (*DeviceShifuConfig, error) {
	if path == "" {
		return nil, errors.New("DeviceShifuConfig path can't be empty")
	}

	cfg, err := configmap.Load(path)
	if err != nil {
		return nil, err
	}

	dsc := &DeviceShifuConfig{}
	if driverProperties, ok := cfg[ConfigmapDriverPropertiesStr]; ok {
		err := yaml.Unmarshal([]byte(driverProperties), &dsc.DriverProperties)
		if err != nil {
			logger.Fatalf("Error parsing %v from ConfigMap, error: %v", ConfigmapDriverPropertiesStr, err)
			return nil, err
		}
	}

	// TODO: add validation to types and readwrite mode
	if instructions, ok := cfg[ConfigmapInstructionsStr]; ok {
		err := yaml.Unmarshal([]byte(instructions), &dsc.Instructions)
		if err != nil {
			logger.Fatalf("Error parsing %v from ConfigMap, error: %v", ConfigmapInstructionsStr, err)
			return nil, err
		}
	}

	if telemetries, ok := cfg[ConfigmapTelemetriesStr]; ok {
		err = yaml.Unmarshal([]byte(telemetries), &dsc.Telemetries)
		if err != nil {
			logger.Fatalf("Error parsing %v from ConfigMap, error: %v", ConfigmapTelemetriesStr, err)
			return nil, err
		}
	}

	if customInstructionsPython, ok := cfg[ConfigmapCustomizedInstructionsStr]; ok {
		err = yaml.Unmarshal([]byte(customInstructionsPython), &dsc.CustomInstructionsPython)
		if err != nil {
			logger.Fatalf("Error parsing %v from ConfigMap, error: %v", ConfigmapCustomizedInstructionsStr, err)
			return nil, err
		}
	}

	err = dsc.load()
	return dsc, err
}

// NewEdgeDevice new edgeDevice
func NewEdgeDevice(edgeDeviceConfig *EdgeDeviceConfig) (*v1alpha1.EdgeDevice, *rest.RESTClient, error) {
	config, err := getRestConfig(edgeDeviceConfig.KubeconfigPath)
	if err != nil {
		logger.Errorf("Error parsing incluster/kubeconfig, error: %v", err.Error())
		return nil, nil, err
	}

	client, err := newEdgeDeviceRestClient(config)
	if err != nil {
		logger.Errorf("Error creating EdgeDevice custom REST client, error: %v", err.Error())
		return nil, nil, err
	}
	ed := &v1alpha1.EdgeDevice{}
	err = client.Get().
		Namespace(edgeDeviceConfig.NameSpace).
		Resource(EdgedeviceResourceStr).
		Name(edgeDeviceConfig.DeviceName).
		Do(context.TODO()).
		Into(ed)
	if err != nil {
		logger.Errorf("Error GET EdgeDevice resource, error: %v", err.Error())
		return nil, nil, err
	}
	return ed, client, nil
}

func getRestConfig(kubeConfigPath string) (*rest.Config, error) {
	if kubeConfigPath != "" {
		return clientcmd.BuildConfigFromFlags("", kubeConfigPath)
	} else {
		return rest.InClusterConfig()
	}
}

// newEdgeDeviceRestClient new edgeDevice rest Client
func newEdgeDeviceRestClient(config *rest.Config) (*rest.RESTClient, error) {
	err := v1alpha1.AddToScheme(scheme.Scheme)
	if err != nil {
		logger.Errorf("cannot add to scheme, error: %v", err)
		return nil, err
	}
	crdConfig := config
	crdConfig.ContentConfig.GroupVersion = &schema.GroupVersion{Group: v1alpha1.GroupVersion.Group, Version: v1alpha1.GroupVersion.Version}
	crdConfig.APIPath = "/apis"
	crdConfig.NegotiatedSerializer = serializer.NewCodecFactory(scheme.Scheme)
	crdConfig.UserAgent = rest.DefaultKubernetesUserAgent()
	exampleRestClient, err := rest.UnversionedRESTClientFor(crdConfig)
	if err != nil {
		return nil, err
	}

	return exampleRestClient, nil
}

// init DeviceShifuConfig With default
func (dsConfig *DeviceShifuConfig) load() error {
	if err := dsConfig.DriverProperties.load(); err != nil {
		logger.Errorf("Error initializing DriverProperties, error %s", err.Error())
		return err
	}

	if dsConfig.Telemetries == nil {
		dsConfig.Telemetries = &DeviceShifuTelemetries{}
	}
	if err := dsConfig.Telemetries.load(); err != nil {
		logger.Errorf("Error initializing Telemetries, error %s", err.Error())
		return err
	}

	if err := dsConfig.Instructions.load(); err != nil {
		logger.Errorf("Error initializing Instructions, error %s", err.Error())
		return err
	}

	return nil
}

func (driverProperties *DeviceShifuDriverProperties) load() error {
	defaultProperties := &DeviceShifuDriverProperties{
		DriverSku:       "defaultSku",
		DriverImage:     "defaultImage",
		DriverExecution: "defaultExecution",
	}
	return mergo.Merge(driverProperties, defaultProperties)
}

func (instructions *DeviceShifuInstructions) load() error {
	if instructions.Instructions == nil {
		instructions.Instructions = map[string]*DeviceShifuInstruction{}
	}

	if instructions.InstructionSettings == nil {
		instructions.InstructionSettings = &DeviceShifuInstructionSettings{}
	}

	return instructions.InstructionSettings.load()
}

func (instructionSettings *DeviceShifuInstructionSettings) load() error {
	var (
		defaultTimeoutSeconds = DeviceDefaultGlobalTimeoutInSeconds
	)

	defaultDeviceshifuInstructionSettings := &DeviceShifuInstructionSettings{
		DefaultTimeoutSeconds: &defaultTimeoutSeconds,
	}

	return mergo.Merge(instructionSettings, defaultDeviceshifuInstructionSettings)
}

func (telemetries *DeviceShifuTelemetries) load() error {
	if telemetries.DeviceShifuTelemetries == nil {
		telemetries.DeviceShifuTelemetries = map[string]*DeviceShifuTelemetry{}
	}
	for id := range telemetries.DeviceShifuTelemetries {
		if telemetries.DeviceShifuTelemetries[id] == nil {
			telemetries.DeviceShifuTelemetries[id] = &DeviceShifuTelemetry{}
		}
		err := telemetries.DeviceShifuTelemetries[id].load()
		if err != nil {
			logger.Errorf("Error initializing telemetry, error %s", err.Error())
			return err
		}
	}

	if telemetries.DeviceShifuTelemetrySettings == nil {
		telemetries.DeviceShifuTelemetrySettings = &DeviceShifuTelemetrySettings{}
	}
	return telemetries.DeviceShifuTelemetrySettings.load()
}

func (telemetry *DeviceShifuTelemetry) load() error {
	var (
		defaultInitialDelay = DeviceInstructionInitialDelay
	)

	defaultDeviceShifuTelemetry := &DeviceShifuTelemetry{
		DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
			InitialDelayMs: &defaultInitialDelay,
			IntervalMs:     &defaultInitialDelay,
		},
	}

	return mergo.Merge(telemetry, defaultDeviceShifuTelemetry)
}

func (telemetrySettings *DeviceShifuTelemetrySettings) load() error {
	var (
		defaultUpdateInterval = DeviceDefaultTelemetryUpdateIntervalInMS
		defaultTimeout        = DeviceTelemetryTimeoutInMS
		defaultInitialDelay   = DeviceTelemetryInitialDelayInMS
		defaultPushToServer   = false
	)
	defaultDeviceshifuTelemtrySettings := DeviceShifuTelemetrySettings{
		DeviceShifuTelemetryUpdateIntervalInMilliseconds: &defaultUpdateInterval,
		DeviceShifuTelemetryTimeoutInMilliseconds:        &defaultTimeout,
		DeviceShifuTelemetryInitialDelayInMilliseconds:   &defaultInitialDelay,
		DeviceShifuTelemetryDefaultPushToServer:          &defaultPushToServer,
	}
	return mergo.Merge(telemetrySettings, defaultDeviceshifuTelemtrySettings)
}
