# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [5.6.63]
- update DocumentAuditing api 
- add WebpageAuditing api

## [5.6.62]
- add the merge bucket interface, such like rename etc

## [5.6.61]
-  update ci region validate

## [5.6.60]
- refine get presigned url
- add must auth headers
- fix InstanceCredentialsUtils

## [5.6.59]
- add BatchImageAuditing api
- update auditing params

## [5.6.58]
- make aes ctr encryption mode can set iv

## [5.6.57]
- update AudioAuditing api
- add VideoAuditing new params

## [5.6.56]
- add encryption mode aes cbc

## [5.6.55]
- add batch image auditing api
- add DocumentAuditing api
- update CI Auditing api

## [5.6.54]
- default use https request
- use strict sign
- refine list buckets

## [5.6.53] - 2021-8-31
- add clientConfig new params
- update CI request httpProtocol rules

## [5.6.52] - 2021-8-18
- fix get bucket no ruleId replication error
- add bucekt referer configuration

## [5.6.51] - 2021-8-3
- update text auditing api to support more detectType
- add auditing util

## [5.6.50] - 2021-7-20
- add text auditing new params
- encryption client update: support encrypt & decrypt file with all cos sdk
- support delete bucket domain

## [5.6.49] - 2021-7-20
- fix resumable download does not release file channel

## [5.6.48] - 2021-7-20
- fix resumable download does not release file handler

## [5.6.47] - 2021-7-13

###Added
- update media transcode api
- add media transcode demo
- fix resumable download create directory error
- add some demos

## [5.6.46] - 2021-7-8

###Added
- add text auditing api and demo
- add image label api v1&v2
- update video auditing api
- update httpclient to 4.5.13 for security reasons
- add some demos

## [5.6.45] - 2021-6-10

###Added
- add tranferManager demo
- add getObjectUrl method

## [5.6.44] - 2021-5-29

###Added
-  add content auditing api and demo
-  copy object get version id
-  demo of copy object with user metadata

## [5.6.43] - 2021-5-24

###Added
-  add media processing callback parameters

## [5.6.42] - 2021-5-20

###Added
- add d http credential endpoint provider

## [5.6.41] - 2021-5-18

### Added
- add suppport for image blink watermaker use post

## [5.6.40] - 2021-5-13

### Added 
- add support for image persistence, qrcode recognition, blink watermark
- add support for resumable download

## [5.6.39] - 2021-3-24

### Added 
- add CI media concat job

## [5.6.38] - 2021-3-24

### Added
- transfer manager multipart complete with user object meta.

## [5.6.37] - 2021-3-10

### Added
- add KMS Encryption Client support.

## [5.6.24] - 2020-6-3

### Added
- add getCannedAccessControl for AccessControlList

## [5.6.20] - 2020-4-23

### Added
- update bcprov-jdk15on version 

## [5.6.19] - 2020-3-26

### Added
- add crc64 support for high level api 

## [5.6.18] - 2020-3-17

### Added
- add maz storage class

## [5.6.17] - 2020-3-5

### Added
- support presigned url not begin with sign=

## [5.6.16] - 2020-2-17

### Added
- add crc64 to utils
- add post object demo and signature

## [5.6.15] - 2020-2-3

### Added
- add trafficLimit support

## [5.6.13~5.6.14] - 2020-1-5

### Added
- 持CVM/CPM的角色绑定获取临时密钥

## [5.6.12] - 2020-1-4

### Added
- add upload part and complete crc get method

## [5.6.11] - 2019-12-31

### Added
- add append object api

## [5.6.10] - 2019-12-17

### Added

- add header utf-8 and iso 88591 convert
- 支持通过腾讯云CVM和黑石物理机绑定角色获取临时密钥
- support tagging,inventory,logging api
- add domain set/get support
- add put/get/delete bucket website support

## [5.6.9] - 2019-11-7

### Changed
- update jackson version

## [5.6.8] - 2019-9-26

### Changed
- change jackson-databind version

## [5.6.7] - 2019-8-28

### Changed
- drop bucket name upper bound check

## [5.6.6] - 2019-8-26

### Added
- add basic auth support

## [5.6.3] - 2019-8-15

### Added
- add acl ReadAcp and WriteAcp

## [5.6.2] - 2019-8-2

### Changed
- update jackson-databind version

## [5.6.1] - 2019-7-31

### Changed
- fix SSECustomer bug and add demo, test case
- add more information in userAgent

## [5.5.8] - 2019-7-15

### Changed
- change to use slf4j-api


## [5.5.7] - 2019-7-2

### Changed
- encode url path, replace the continuous slash with %2F except the first


## [5.5.6] - 2019-6-27

### Changed
- remove appid prefix check


## [5.5.5] - 2019-6-20

### Added
- add set fixed endpointAddr

## [5.5.4] - 2019-6-14

### Changed
- fix bigger than 5G file copy bug

## [5.5.3] - 2019-5-23

### Changed
- change jackson version

## [5.5.2] - 2019-4-22

### Changed
- fix UserSpecifiedEndpointBuilder constructor bug

## [5.5.1] - 2019-2-28

### Changed
- delete duplicate code

## [5.5.0] - 2019-1-31

### Added
- 支持文件直传归档存储

## [5.4.10] - 2018-12-11

### Added
- OSCredentialProvider中增加refresh方法

## [5.4.9] - 2018-11-22

### Changed
- 修改临时秘钥预签名的bug

## [5.4.8] - 2018-11-18

### Added
- 预签名支持临时秘钥


## [5.4.6] - 2018-11-9

### Added
- 支持秘钥通过provider接口传入

### Changed
- 修改httpclient配置
- 修复对content-encoding为gzip的文件下载时候的报错 2 支持获取bucket列表时候 初始化cosclient可以不传入region

## [5.4.5] - 2018-8-23

### Changed
- 1 增加设置policy接口 2 修复分块上传时存在upload获取result接口有可能阻塞的bug

## [5.4.4] - 2018-7-12

### Changed
- 增加客户端加密范例
