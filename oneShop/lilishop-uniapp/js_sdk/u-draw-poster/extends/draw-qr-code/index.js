import uQRCode from "./uQRCode";
export default {
    name: "drawQrCode",
    handle: uQRCode.make.bind(uQRCode),
    errorCorrectLevel: uQRCode.errorCorrectLevel
};
