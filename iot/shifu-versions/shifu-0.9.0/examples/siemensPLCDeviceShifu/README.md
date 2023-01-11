## To build:

`docker build . -t edgehub/plc-device:v0.0.1`

## To run the Siemens PLC deviceShifu, update the following fields in `shifu/examples/siemensPLCDeviceShifu/plc-deployment/plc-deviceshifu-deployment.yaml`

```
......
            - name: PLC_ADDR
              value: "192.168.0.1"   # IP for PLC
            - name: PLC_RACK
              value: "0"             # Rack number of PLC
            - name: PLC_SLOT
              value: "1"             # Slot number for PLC
......
```