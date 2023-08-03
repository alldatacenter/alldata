package deviceshifuopcua

import (
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"path"
	"reflect"
	"time"

	"github.com/edgenesis/shifu/pkg/deviceshifu/utils"

	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
	"github.com/gopcua/opcua"
	"github.com/gopcua/opcua/ua"
)

// DeviceShifu implemented from deviceshifuBase and OPC UA Setting and client
type DeviceShifu struct {
	base              *deviceshifubase.DeviceShifuBase
	opcuaInstructions *OPCUAInstructions
	opcuaClient       *opcua.Client
}

// HandlerMetaData MetaData for OPC UA handler
type HandlerMetaData struct {
	edgeDeviceSpec v1alpha1.EdgeDeviceSpec
	instruction    string
	properties     *OPCUAInstructionProperty
}

// DeviceConfigmapCertificatePath default cert path
const (
	DeviceConfigmapCertificatePath string = "/etc/edgedevice/certificate"
	DeviceSecretPasswordPath       string = "/etc/edgedevice/secret/password"

	DefaultOPCUARequestMaxAge = 2000
)

// New This function creates a new Device Shifu based on the configuration
func New(deviceShifuMetadata *deviceshifubase.DeviceShifuMetaData) (*DeviceShifu, error) {
	if deviceShifuMetadata.Namespace == "" {
		return nil, fmt.Errorf("DeviceShifu's namespace can't be empty")
	}

	base, mux, err := deviceshifubase.New(deviceShifuMetadata)
	if err != nil {
		return nil, err
	}

	opcuaInstructions := CreateOPCUAInstructions(&base.DeviceShifuConfig.Instructions)
	if err != nil {
		return nil, fmt.Errorf("error parsing ConfigMap at %v", deviceShifuMetadata.ConfigFilePath)
	}

	var opcuaClient *opcua.Client

	if deviceShifuMetadata.KubeConfigPath != deviceshifubase.DeviceKubeconfigDoNotLoadStr {
		// switch for different Shifu Protocols
		switch protocol := *base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolOPCUA:

			ctx := context.Background()
			opcuaClient, err = establishOPCUAConnection(ctx, *base.EdgeDevice.Spec.Address, base.EdgeDevice.Spec.ProtocolSettings.OPCUASetting)
			if err != nil {
				return nil, err
			}

			for instruction, properties := range opcuaInstructions.Instructions {
				if properties.OPCUAInstructionProperty == nil {
					return nil, fmt.Errorf("instruction: %s's instructionProperties is nil", instruction)
				}
				HandlerMetaData := &HandlerMetaData{
					base.EdgeDevice.Spec,
					instruction,
					properties.OPCUAInstructionProperty,
				}

				var handler DeviceCommandHandlerOPCUA
				if base.EdgeDevice.Spec.ProtocolSettings.OPCUASetting.ConnectionTimeoutInMilliseconds == nil {
					timeout := deviceshifubase.DeviceDefaultConnectionTimeoutInMS
					handler = DeviceCommandHandlerOPCUA{opcuaClient, &timeout, HandlerMetaData}
				} else {
					timeout := base.EdgeDevice.Spec.ProtocolSettings.OPCUASetting.ConnectionTimeoutInMilliseconds
					handler = DeviceCommandHandlerOPCUA{opcuaClient, timeout, HandlerMetaData}
				}

				mux.HandleFunc("/"+instruction, handler.commandHandleFunc())
			}
		}
	}

	deviceshifubase.BindDefaultHandler(mux)

	ds := &DeviceShifu{
		base:              base,
		opcuaInstructions: opcuaInstructions,
		opcuaClient:       opcuaClient,
	}

	ds.base.UpdateEdgeDeviceResourcePhase(v1alpha1.EdgeDevicePending)
	return ds, nil
}

func establishOPCUAConnection(ctx context.Context, address string, setting *v1alpha1.OPCUASetting) (*opcua.Client, error) {
	endpoints, err := opcua.GetEndpoints(ctx, address)
	if err != nil {
		logger.Error("Cannot Get EndPoint Description")
		return nil, err
	}

	// TODO implement other option here
	ep := opcua.SelectEndpoint(endpoints, ua.SecurityPolicyURINone, ua.MessageSecurityModeNone)
	if ep == nil {
		logger.Error("Failed to find suitable endpoint")
		return nil, err
	}

	var options = make([]opcua.Option, 0)
	// TODO  implement different messageSecurityModes
	options = append(options,
		opcua.SecurityPolicy(ua.SecurityPolicyURINone),
		opcua.SecurityMode(ua.MessageSecurityModeNone),
		opcua.AutoReconnect(true),
	)

	switch ua.UserTokenTypeFromString(*setting.AuthenticationMode) {
	case ua.UserTokenTypeIssuedToken:
		options = append(options, opcua.AuthIssuedToken([]byte(*setting.IssuedToken)))
	case ua.UserTokenTypeCertificate:
		var privateKeyFileName = path.Join(DeviceConfigmapCertificatePath, *setting.PrivateKeyFileName)
		var certificateFileName = path.Join(DeviceConfigmapCertificatePath, *setting.CertificateFileName)
		cert, err := tls.LoadX509KeyPair(certificateFileName, privateKeyFileName)
		if err != nil {
			logger.Errorf("X509 Certificate Or PrivateKey load failed")
			return nil, err
		}
		options = append(options,
			opcua.CertificateFile(certificateFileName),
			opcua.PrivateKeyFile(privateKeyFileName),
			opcua.AuthCertificate(cert.Certificate[0]),
		)
	case ua.UserTokenTypeUserName:
		passwordByte, err := os.ReadFile(DeviceSecretPasswordPath)
		// secret will overwrite the password in edge device
		if err != nil {
			logger.Infof("secret load error: %v, password will be loaded from OPCUASetting.Password", err)
			options = append(options, opcua.AuthUsername(*setting.Username, *setting.Password))
		} else {
			logger.Infof("password loaded from secret")
			options = append(options, opcua.AuthUsername(*setting.Username, string(passwordByte)))
		}
	case ua.UserTokenTypeAnonymous:
		fallthrough
	default:
		if *setting.AuthenticationMode != "Anonymous" {
			logger.Errorf("Could not parse your input, you are in Anonymous Mode default")
		}

		options = append(options, opcua.AuthAnonymous())
	}

	options = append(options, opcua.SecurityFromEndpoint(ep, ua.UserTokenTypeFromString(*setting.AuthenticationMode)))
	opcuaClient := opcua.NewClient(address, options...)
	if err := opcuaClient.Connect(ctx); err != nil {
		logger.Errorf("Unable to connect to OPC UA server, error: %v", err)
		return nil, err
	}
	return opcuaClient, nil
}

// DeviceCommandHandlerOPCUA handler for opcua
type DeviceCommandHandlerOPCUA struct {
	client          *opcua.Client
	timeout         *int64
	HandlerMetaData *HandlerMetaData
}

func (handler DeviceCommandHandlerOPCUA) commandHandleFunc() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case http.MethodGet:
			handler.read(w, r)
		case http.MethodPost:
			handler.write(w, r)
		default:
			http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		}
	}
}

func (handler DeviceCommandHandlerOPCUA) read(w http.ResponseWriter, r *http.Request) {
	nodeID := handler.HandlerMetaData.properties.OPCUANodeID
	logger.Infof("Writing to NodeID: %v", nodeID)

	id, err := ua.ParseNodeID(nodeID)
	if err != nil {
		logger.Errorf("Failed to parse NodeID, error: %v", err)
		http.Error(w, "Failed to parse NodeID, error: "+err.Error(), http.StatusBadRequest)
		return
	}

	ctx := context.Background()
	resp, err := handler.readByNodeId(ctx, id)
	if err != nil {
		http.Error(w, "Failed to read message from Server, error: "+err.Error(), http.StatusBadRequest)
		logger.Errorf("Read failed: %s", err)
		return
	}

	if resp.Results[0].Status != ua.StatusOK {
		http.Error(w, "OPC UA response status is not OK "+fmt.Sprint(resp.Results[0].Status), http.StatusBadRequest)
		logger.Errorf("Status not OK: %v", resp.Results[0].Status)
		return
	}

	logger.Infof("%#v", resp.Results[0].Value.Value())

	w.WriteHeader(http.StatusOK)
	// TODO: Should handle different type of return values and return JSON/other data
	// types instead of plain text
	rawRespBody := resp.Results[0].Value.Value()
	rawRespBodyString := fmt.Sprintf("%v", rawRespBody)
	respString := rawRespBodyString

	handlerInstruction := handler.HandlerMetaData.instruction
	instructionFuncName, shouldUsePythonCustomProcessing := deviceshifubase.CustomInstructionsPython[handlerInstruction]
	logger.Infof("Instruction %v is custom: %v", handlerInstruction, shouldUsePythonCustomProcessing)
	if shouldUsePythonCustomProcessing {
		logger.Infof("Instruction %v has a python customized handler configured.\n", handlerInstruction)
		respString = utils.ProcessInstruction(deviceshifubase.PythonHandlersModuleName, instructionFuncName, rawRespBodyString, deviceshifubase.PythonScriptDir)
	}
	fmt.Fprintf(w, "%v", respString)
}

func (handler DeviceCommandHandlerOPCUA) readByNodeId(ctx context.Context, nodeId *ua.NodeID) (*ua.ReadResponse, error) {
	req := &ua.ReadRequest{
		MaxAge: DefaultOPCUARequestMaxAge,
		NodesToRead: []*ua.ReadValueID{
			{NodeID: nodeId},
		},
		TimestampsToReturn: ua.TimestampsToReturnBoth,
	}

	ctx, cancel := context.WithTimeout(ctx, time.Duration(*handler.timeout)*time.Millisecond)
	defer cancel()

	return handler.client.ReadWithContext(ctx, req)
}

type WriteRequest struct {
	Value interface{} `json:"value"`
}

func (handler DeviceCommandHandlerOPCUA) write(w http.ResponseWriter, r *http.Request) {
	nodeID := handler.HandlerMetaData.properties.OPCUANodeID
	logger.Infof("Requesting NodeID: %v", nodeID)

	id, err := ua.ParseNodeID(nodeID)
	if err != nil {
		logger.Errorf("invalid node id: %v", err)
		http.Error(w, "Failed to parse NodeID, error: "+err.Error(), http.StatusBadRequest)
		return
	}

	var request WriteRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		logger.Errorf("invalid request: %v", err)
		http.Error(w, "Failed to parse request, error: "+err.Error(), http.StatusBadRequest)
		return
	}
	logger.Infof("write data: %s", request.Value)

	// get old value by nodeId
	ctx := context.Background()
	readResponse, err := handler.readByNodeId(ctx, id)
	if err != nil {
		http.Error(w, "Failed to read message from Server, error: "+err.Error(), http.StatusBadRequest)
		logger.Errorf("Read failed: %s", err)
		return
	}

	if readResponse.Results[0].Status != ua.StatusOK {
		http.Error(w, "OPC UA response status is not OK "+fmt.Sprint(readResponse.Results[0].Status), http.StatusBadRequest)
		logger.Errorf("Status not OK: %v", readResponse.Results[0].Status)
		return
	}

	// create new value by old value's type and new value's value
	oldValue := readResponse.Results[0].Value.Value()
	newValue := convertValueToRef(oldValue, request.Value)
	if newValue == nil {
		oldType := readResponse.Results[0].Value.Type().String()
		http.Error(w, "wrong type of value, value's type should be "+oldType, http.StatusBadRequest)
		logger.Errorf("Failed to create new value")
		return
	}

	value, err := ua.NewVariant(newValue)
	if err != nil {
		logger.Errorf("invalid value: %v", err)
		http.Error(w, "Failed to parse value, error: "+err.Error(), http.StatusBadRequest)
		return
	}

	opcuaRequest := &ua.WriteRequest{
		NodesToWrite: []*ua.WriteValue{
			{
				NodeID:      id,
				AttributeID: ua.AttributeIDValue,
				Value: &ua.DataValue{
					EncodingMask: ua.DataValueValue,
					Value:        value,
				},
			},
		},
	}

	ctx, cancel := context.WithTimeout(ctx, time.Duration(*handler.timeout)*time.Millisecond)
	defer cancel()

	writeResponse, err := handler.client.WriteWithContext(ctx, opcuaRequest)
	if err != nil {
		http.Error(w, "Failed to write message to Server, error: "+err.Error(), http.StatusBadRequest)
		logger.Errorf("Write failed: %s", err)
		return
	}

	if writeResponse.Results[0] != ua.StatusOK {
		http.Error(w, "OPC UA response status is not OK "+fmt.Sprint(writeResponse.Results[0]), http.StatusBadRequest)
		logger.Errorf("Status not OK: %v", writeResponse.Results[0])
		return
	}

	w.WriteHeader(http.StatusOK)
}

func (ds *DeviceShifu) getOPCUANodeIDFromInstructionName(instructionName string) (string, error) {
	if instructionProperties, exists := ds.opcuaInstructions.Instructions[instructionName]; exists {
		return instructionProperties.OPCUAInstructionProperty.OPCUANodeID, nil
	}

	return "", fmt.Errorf("Instruction %v not found in list of deviceshifu instructions", instructionName)
}

func (ds *DeviceShifu) requestOPCUANodeID(nodeID string) error {
	id, err := ua.ParseNodeID(nodeID)
	if err != nil {
		logger.Fatalf("invalid node id: %v", err)
	}

	req := &ua.ReadRequest{
		MaxAge: DefaultOPCUARequestMaxAge,
		NodesToRead: []*ua.ReadValueID{
			{NodeID: id},
		},
		TimestampsToReturn: ua.TimestampsToReturnBoth,
	}

	ctx := context.Background()
	ctx, cancel := context.WithTimeout(ctx, time.Duration(deviceshifubase.DeviceDefaultRequestTimeoutInMS)*time.Millisecond)
	defer cancel()

	resp, err := ds.opcuaClient.ReadWithContext(ctx, req)
	if err != nil {
		logger.Errorf("Failed to read message from Server, error: %v " + err.Error())
		return err
	}

	if resp.Results[0].Status != ua.StatusOK {
		logger.Errorf("OPC UA response status is not OK, status: %v", resp.Results[0].Status)
		return err
	}

	logger.Infof("%#v", resp.Results[0].Value.Value())

	return nil
}

func (ds *DeviceShifu) collectOPCUATelemetry() (bool, error) {
	if ds.base.EdgeDevice.Spec.Protocol != nil {
		switch protocol := *ds.base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolOPCUA:
			telemetries := ds.base.DeviceShifuConfig.Telemetries.DeviceShifuTelemetries
			for telemetry, telemetryProperties := range telemetries {
				if ds.base.EdgeDevice.Spec.Address == nil {
					return false, fmt.Errorf("Device %v does not have an address", ds.base.Name)
				}

				if telemetryProperties.DeviceShifuTelemetryProperties.DeviceInstructionName == nil {
					return false, fmt.Errorf("Device %v telemetry %v does not have an instruction name", ds.base.Name, telemetry)
				}

				instruction := *telemetryProperties.DeviceShifuTelemetryProperties.DeviceInstructionName
				nodeID, err := ds.getOPCUANodeIDFromInstructionName(instruction)
				if err != nil {
					logger.Errorf("%v", err.Error())
					return false, err
				}

				if err = ds.requestOPCUANodeID(nodeID); err != nil {
					logger.Errorf("error checking telemetry: %v, error: %v", telemetry, err.Error())
					return false, err
				}

			}
		default:
			logger.Warnf("EdgeDevice protocol %v not supported in deviceshifu", protocol)
			return false, nil
		}
	}

	return true, nil

}

// Start start opcua telemetry
func (ds *DeviceShifu) Start(stopCh <-chan struct{}) error {
	return ds.base.Start(stopCh, ds.collectOPCUATelemetry)
}

// Stop http server
func (ds *DeviceShifu) Stop() error {
	return ds.base.Stop()
}

// convertValueToRef attempts to convert the given value to the type of the reference
// and assigns the converted value to the reference.
// The function returns the reference with the newly assigned value, or nil if conversion is not possible.
func convertValueToRef(ref interface{}, value interface{}) interface{} {
	// Check if the ref or value are nil.
	// If either is nil, log a warning and return nil.
	if ref == nil || value == nil {
		logger.Warnf("ref(%v) or newValue(%v) is nil", ref, value)
		return nil
	}

	// Copy the reference.
	newValue := ref
	// Create a reflect.Value for newValue. The Elem() function is used to get the actual value that the pointer points to.
	refElem := reflect.ValueOf(&newValue).Elem()

	// If the reference can't be set (it's unaddressable), return nil.
	if !refElem.CanSet() {
		return nil
	}

	// Get the reflect.Value of the new value.
	valueOfNewValue := reflect.ValueOf(value)
	// Get the type of the copied reference.
	typeOfRefCopy := reflect.TypeOf(newValue)

	// Check if the value can be converted to the type of the reference.
	// If it can't, log a warning and return nil.
	if !valueOfNewValue.CanConvert(typeOfRefCopy) {
		logger.Warnf("failed to convert value %s to type %v", valueOfNewValue, typeOfRefCopy)
		return nil
	}

	// Set the value of the reference to the converted value.
	refElem.Set(valueOfNewValue.Convert(typeOfRefCopy))
	// Return the reference with the newly assigned value.
	return newValue
}
