
local templateCode = 'sms_123';
local regionId = 'cn-hangzhou';
local assessKeyId = 'foo';

{
    "SignName": "erda",
    "TemplateCode": templateCode,
    "Version": "2017-05-25",
    "Timestamp": std.extVar("date"),
    "SignatureVersion": "1.0",
    "RegionId": regionId,
    "Action": "SendSms",
    "Format": "JSON",
    "SignatureMethod": "HMAC-SHA1",
    "SignatureType": "",
    "SignatureNonce": std.extVar("signatureNonce"),
    "AccessKeyId": assessKeyId,
    "Signature": std.extVar("signature")
}
