package deviceshifuopcua

import (
	"context"
	"crypto/tls"
	"fmt"
	"net/http"
	"os"
	"path"
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
		return nil, fmt.Errorf("Error parsing ConfigMap at %v", deviceShifuMetadata.ConfigFilePath)
	}

	var opcuaClient *opcua.Client

	if deviceShifuMetadata.KubeConfigPath != deviceshifubase.DeviceKubeconfigDoNotLoadStr {
		// switch for different Shifu Protocols
		switch protocol := *base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolOPCUA:
			for instruction, properties := range opcuaInstructions.Instructions {
				HandlerMetaData := &HandlerMetaData{
					base.EdgeDevice.Spec,
					instruction,
					properties.OPCUAInstructionProperty,
				}

				ctx := context.Background()
				endpoints, err := opcua.GetEndpoints(ctx, *base.EdgeDevice.Spec.Address)
				if err != nil {
					logger.Fatal("Cannot Get EndPoint Description")
					return nil, err
				}

				ep := opcua.SelectEndpoint(endpoints, ua.SecurityPolicyURINone, ua.MessageSecurityModeNone)
				if ep == nil {
					logger.Fatal("Failed to find suitable endpoint")
				}

				var options = make([]opcua.Option, 0)
				// TODO  implement different messageSecurityModes
				options = append(options,
					opcua.SecurityPolicy(ua.SecurityPolicyURINone),
					opcua.SecurityMode(ua.MessageSecurityModeNone),
					opcua.AutoReconnect(false),
				)

				var setting = *base.EdgeDevice.Spec.ProtocolSettings.OPCUASetting
				switch ua.UserTokenTypeFromString(*setting.AuthenticationMode) {
				case ua.UserTokenTypeIssuedToken:
					options = append(options, opcua.AuthIssuedToken([]byte(*setting.IssuedToken)))
				case ua.UserTokenTypeCertificate:
					var privateKeyFileName = path.Join(DeviceConfigmapCertificatePath, *setting.PrivateKeyFileName)
					var certificateFileName = path.Join(DeviceConfigmapCertificatePath, *setting.CertificateFileName)
					cert, err := tls.LoadX509KeyPair(certificateFileName, privateKeyFileName)
					if err != nil {
						logger.Fatalf("X509 Certificate Or PrivateKey load Default")
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
				opcuaClient = opcua.NewClient(*base.EdgeDevice.Spec.Address, options...)
				if err := opcuaClient.Connect(ctx); err != nil {
					logger.Fatalf("Unable to connect to OPC UA server, error: %v", err)
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

// DeviceCommandHandlerOPCUA handler for opcua
type DeviceCommandHandlerOPCUA struct {
	client          *opcua.Client
	timeout         *int64
	HandlerMetaData *HandlerMetaData
}

func (handler DeviceCommandHandlerOPCUA) commandHandleFunc() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		nodeID := handler.HandlerMetaData.properties.OPCUANodeID
		logger.Infof("Requesting NodeID: %v", nodeID)

		id, err := ua.ParseNodeID(nodeID)
		if err != nil {
			logger.Fatalf("invalid node id: %v", err)
		}

		req := &ua.ReadRequest{
			MaxAge: 2000,
			NodesToRead: []*ua.ReadValueID{
				{NodeID: id},
			},
			TimestampsToReturn: ua.TimestampsToReturnBoth,
		}

		ctx := context.Background()
		ctx, cancel := context.WithTimeout(ctx, time.Duration(*handler.timeout)*time.Millisecond)
		defer cancel()

		resp, err := handler.client.ReadWithContext(ctx, req)
		handlerInstruction := handler.HandlerMetaData.instruction

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
		instructionFuncName, shouldUsePythonCustomProcessing := deviceshifubase.CustomInstructionsPython[handlerInstruction]
		logger.Infof("Instruction %v is custom: %v", handlerInstruction, shouldUsePythonCustomProcessing)
		if shouldUsePythonCustomProcessing {
			logger.Infof("Instruction %v has a python customized handler configured.\n", handlerInstruction)
			respString = utils.ProcessInstruction(deviceshifubase.PythonHandlersModuleName, instructionFuncName, rawRespBodyString, deviceshifubase.PythonScriptDir)
		}
		fmt.Fprintf(w, "%v", respString)
	}
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
		MaxAge: 2000,
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
