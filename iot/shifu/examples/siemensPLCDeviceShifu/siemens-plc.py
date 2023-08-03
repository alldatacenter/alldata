import os
import sys
import snap7
from flask import Flask, request

client = snap7.client.Client()


app = Flask(__name__)

ip = os.environ.get("PLC_ADDRESS")
port = os.environ.get("PLC_CONTAINER_PORT")
rack = os.environ.get("PLC_RACK")
slot = os.environ.get("PLC_SLOT")

def edit_single_bit(originalbyte, digitvalue, isset):
    if digitvalue > 7:
        digitvalue -= 8
        changebyte = originalbyte[1]
        if isset == 0:
            return bytes([originalbyte[0]]) + bytes([changebyte & ~(1 << digitvalue)])
        else:
            return bytes([originalbyte[0]]) + bytes([changebyte | (1 << digitvalue)])
    else:
        changebyte = originalbyte[0]
        if isset == 0:
            return bytes([changebyte & ~(1 << digitvalue)]) + bytes([originalbyte[1]])
        else:
            return bytes([changebyte | (1 << digitvalue)]) + bytes([originalbyte[1]])


@app.route('/sendsinglebit')
def send_single_bit():
    print("Changing single bit...")
    rootaddress = request.args.get('rootaddress')
    address = request.args.get('address', default=0, type=int)
    start = request.args.get('start', default=0, type=int)
    digit = request.args.get('digit', default=0, type=int)
    value = request.args.get('value', default=0, type=int)
    print("Memory area:{}, target address:{}, setting {} digit to {}".format(rootaddress, address, digit, value))
    area = ''
    btarr = bytearray([0b0])
    if rootaddress == 'M':
        area = snap7.types.Areas.MK
    elif rootaddress == 'Q':
        area = snap7.types.Areas.PA
    elif rootaddress == 'C':
        area = snap7.types.Areas.CT
    elif rootaddress == 'T':
        area = snap7.types.Areas.TM
    else:
        print("UnsupportedMemoryArea: root address", rootaddress, "not supported!")
        return "UnsupportedMemoryArea"
    originalbyte = client.read_area(area, address, start, snap7.types.S7WLByte)
    print("Original value is", bin(int.from_bytes(originalbyte, byteorder=sys.byteorder)), len(originalbyte))
    newbyte = edit_single_bit(originalbyte, digit, value)
    print("after edit")
    print(newbyte, type(newbyte))
    client.write_area(area, address, start, newbyte)
    verifybyte = client.read_area(area, address, start, snap7.types.S7WLByte)
    print("New value is", verifybyte)
    return "Changed from {} to {}".format(originalbyte, verifybyte)


@app.route('/getcontent')
def get_content():
    print("Getting content from memory areas...")
    rootaddress = request.args.get('rootaddress')
    address = request.args.get('address', default=0, type=int)
    start = request.args.get('start', default=0, type=int)
    print("Reading from", rootaddress, "for address", address, "starting", start)
    area = ''
    if rootaddress == 'M':
        area = snap7.types.Areas.MK
    elif rootaddress == 'Q':
        area = snap7.types.Areas.PA
    elif rootaddress == 'C':
        area = snap7.types.Areas.CT
    elif rootaddress == 'T':
        area = snap7.types.Areas.TM
    else:
        print("UnsupportedMemoryArea: root address", rootaddress, "not supported!")
        return "UnsupportedMemoryArea"

    res = client.read_area(area, address, start, snap7.types.S7WLByte)

    resbin = bin(int.from_bytes(res, sys.byteorder))
    print("Got", resbin, len(resbin))
    if len(resbin) < 18:
        resbin = resbin.split("b")[0] + "b"+ (18-len(resbin))*"0"  + resbin.split("b")[1]
    return resbin

@app.route('/getcpuordercode')
def get_cpu_ordercode():
    print("Getting CPU order code...")
    order_code = client.get_order_code().OrderCode
    print("CPU order code is", order_code)
    return order_code

if __name__ == "__main__":
    client.connect(ip, int(rack), int(slot))
    app.run(host="0.0.0.0", port=port)
