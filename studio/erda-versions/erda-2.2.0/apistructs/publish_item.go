// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package apistructs

import (
	"mime/multipart"
	"net/url"
	"strconv"
	"time"
)

const (
	//EraseSuccess 擦除成功
	EraseSuccess string = "success"
	//EraseFailure 擦除失败
	EraseFailure string = "failure"
	// Erasing 擦除中
	Erasing string = "erasing"

	PublishItemTypeMobile  = "MOBILE"
	PublishItemTypeLIBRARY = "LIBRARY"
)

type PublishItem struct {
	ID               int64     `json:"id"`
	Name             string    `json:"name"`
	DisplayName      string    `json:"displayName"`
	Logo             string    `json:"logo"`
	PublisherID      int64     `json:"publisherId"`
	AK               string    `json:"ak"`
	AI               string    `json:"ai"`
	Type             string    `json:"type"`
	Public           bool      `json:"public"`
	OrgID            int64     `json:"orgId"`
	Desc             string    `json:"desc"`
	Creator          string    `json:"creator"`
	DownloadUrl      string    `json:"downloadUrl"`
	CreatedAt        time.Time `json:"createdAt"`
	UpdatedAt        time.Time `json:"updatedAt"`
	NoJailbreak      bool      `json:"noJailbreak"`      // 越狱控制
	GeofenceLon      float64   `json:"geofenceLon"`      // 地理围栏，坐标经度
	GeofenceLat      float64   `json:"geofenceLat"`      // 地理围栏，坐标纬度
	GeofenceRadius   float64   `json:"geofenceRadius"`   // 地理围栏，合理半径
	GrayLevelPercent int       `json:"grayLevelPercent"` // 灰度百分比，0-100
	LatestVersion    string    `json:"latestVersion"`    // 最新版本
	// Deprecated: the feature is not available on the Erda platform
	RefCount        uint64   `json:"refCount"`        // 被引用数
	PreviewImages   []string `json:"previewImages"`   // 预览图
	BackgroundImage string   `json:"backgroundImage"` // 背景图
}

type PublishItemVersion struct {
	ID               uint64                   `json:"id"`
	Version          string                   `json:"version"`
	BuildID          string                   `json:"buildId"`
	PackageName      string                   `json:"packageName"`
	Public           bool                     `json:"public"`
	IsDefault        bool                     `json:"isDefault"`
	Desc             string                   `json:"desc"`
	Logo             string                   `json:"logo"`
	Resources        interface{}              `json:"resources"` //版本资源信息
	Meta             interface{}              `json:"meta"`      //元信息，项目应用id等
	Swagger          interface{}              `json:"swagger"`   //api定义
	OrgID            int64                    `json:"orgId"`
	CreatedAt        time.Time                `json:"createdAt"`
	UpdatedAt        time.Time                `json:"updatedAt"`
	Spec             string                   `json:"spec"`
	Readme           string                   `json:"readme"`
	MobileType       string                   `json:"mobileType"`
	TargetMobiles    map[string][]string      `json:"targetMobiles"` // h5包目标版本信息
	VersionStates    PublishItemVersionStates `json:"versionStates"`
	GrayLevelPercent int                      `json:"grayLevelPercent"` // 灰度百分比，0-100
}

// QueryPublishItemRequest 查询发布内容请求
type QueryPublishItemRequest struct {
	PageNo      int64  `query:"pageNo"`
	PageSize    int64  `query:"pageSize"`
	PublisherId int64  `query:"publisherId"`
	Name        string `query:"name"`
	Type        string `query:"type"`
	Public      string `query:"public"`
	Q           string `query:"q"`   // 模糊查询关键字
	Ids         string `query:"ids"` //批量id查询,用,分割
	OrgID       int64  `json:"-"`
}

func (q *QueryPublishItemRequest) FromValues(values url.Values) {
	var err error
	if q.PageSize, err = strconv.ParseInt(values.Get("pageSize"), 10, 64); err != nil {
		q.PageSize = 10
	}
	if q.PageNo, err = strconv.ParseInt(values.Get("pageNo"), 10, 64); err != nil {
		q.PageNo = 1
	}
	q.PublisherId, _ = strconv.ParseInt(values.Get("publisherId"), 10, 64)
	q.Name = values.Get("name")
	q.Type = values.Get("type")
	q.Public = values.Get("public")
	q.Q = values.Get("q")
	q.Ids = values.Get("ids")
}

func (q QueryPublishItemRequest) ToValues(values url.Values) {
	values.Set("pageSize", strconv.FormatInt(q.PageSize, 10))
	values.Set("pageNo", strconv.FormatInt(q.PageNo, 10))
	values.Set("publisherId", strconv.FormatInt(q.PublisherId, 10))
	values.Set("name", q.Name)
	values.Set("type", q.Type)
	values.Set("public", q.Public)
	values.Set("q", q.Q)
	values.Set("ids", q.Ids)
}

// PublishItemResponse 查询单个发布内容响应
type PublishItemResponse struct {
	Header
	Data PublishItem `json:"data"`
}

// QueryPublishItemResponse 查询发布内容响应
type QueryPublishItemResponse struct {
	Header
	Data QueryPublishItemData `json:"data"`
}

// QueryPublishItemData 发布内容列表数据结构
type QueryPublishItemData struct {
	List  []*PublishItem `json:"list"`
	Total int            `json:"total"`
}

// CreatePublishItemRequest 创建发布内容请求
type CreatePublishItemRequest struct {
	Name             string   `json:"name"`
	DisplayName      string   `json:"displayName"`
	PublisherID      int64    `json:"publisherId"`
	Type             string   `json:"type"`
	Logo             string   `json:"logo"`
	Public           bool     `json:"public"`
	Desc             string   `json:"desc"`
	OrgID            int64    `json:"-"`
	Creator          string   `json:"-"`
	NoJailbreak      bool     `json:"noJailbreak"`      // 越狱控制
	GeofenceLon      float64  `json:"geofenceLon"`      // 地理围栏，坐标经度
	GeofenceLat      float64  `json:"geofenceLat"`      // 地理围栏，坐标纬度
	GeofenceRadius   float64  `json:"geofenceRadius"`   // 地理围栏，合理半径
	GrayLevelPercent int      `json:"grayLevelPercent"` // 灰度百分比，0-100
	PreviewImages    []string `json:"previewImages"`    // 预览图
	BackgroundImage  string   `json:"backgroundImage"`  // 背景图
}

// CreatePublishItemResponse 创建发布内容响应
type CreatePublishItemResponse struct {
	Header
	Data PublishItem `json:"data"`
}

// DeletePublishItemResponse 创建发布内容响应
type DeletePublishItemResponse struct {
	Header
	Data PublishItem `json:"data"`
}

// UpdatePublishItemRequest 更新发布内容请求
type UpdatePublishItemRequest struct {
	ID               int64    `json:"-"`
	DisplayName      string   `json:"displayName"`
	Logo             string   `json:"logo"`
	Public           bool     `json:"public"`
	Desc             string   `json:"desc"`
	NoJailbreak      bool     `json:"noJailbreak"`      // 越狱控制
	GeofenceLon      float64  `json:"geofenceLon"`      // 地理围栏，坐标经度
	GeofenceLat      float64  `json:"geofenceLat"`      // 地理围栏，坐标纬度
	GeofenceRadius   float64  `json:"geofenceRadius"`   // 地理围栏，合理半径
	GrayLevelPercent int      `json:"grayLevelPercent"` // 灰度百分比，0-100
	PreviewImages    []string `json:"previewImages"`    // 预览图
	BackgroundImage  string   `json:"backgroundImage"`  // 背景图
}

// UpdatePublishItemResponse 更新发布内容响应
type UpdatePublishItemResponse struct {
	Header
	Data PublishItem `json:"data"`
}

// QueryPublishItemVersionRequest 查询发布版本请求
type CreatePublishItemVersionRequest struct {
	Version       string        `json:"version"`
	BuildID       string        `json:"buildID"`
	PackageName   string        `json:"package_name"`
	Public        bool          `json:"public"`
	IsDefault     bool          `json:"is_default"`
	Logo          string        `json:"logo"`
	Desc          string        `json:"desc"`
	Readme        string        `json:"readme"`
	Spec          string        `json:"spec"`
	Swagger       string        `json:"swagger"`
	ReleaseID     string        `json:"releaseId"`
	MobileType    ResourceType  `json:"mobileType"`
	H5VersionInfo H5VersionInfo `json:"h5VersionInfo"`
	PublishItemID int64         `json:"-"`
	OrgID         int64         `json:"-"`
	AppID         uint64        `json:"appID"`
	Creator       string        `json:"-"`
}

// CreateOffLinePublishItemVersionRequest 创建离线包发布版本请求
type CreateOffLinePublishItemVersionRequest struct {
	Desc          string                `json:"desc"`
	FormFile      multipart.File        `json:"-"`
	FileHeader    *multipart.FileHeader `json:"-"`
	PublishItemID int64                 `json:"-"`
	IdentityInfo  IdentityInfo          `json:"-"`
	OrgID         int64                 `json:"-"`
}

// H5VersionInfo H5包的版本信息
type H5VersionInfo struct {
	VersionInfo
	TargetMobiles map[string][]string // H5对应的移动应用版本, key是应用类型，value是版本号
}

// VersionInfo 版本信息
type VersionInfo struct {
	PackageName string `json:"packageName"` // 包名
	Version     string `json:"version"`     // 版本
	BuildID     string `json:"buildId"`     // 用来校验同version时的版本新旧，默认是pipelineID
}

type CreatePublishItemVersionResponse struct {
	Header
	Data        PublishItemVersion `json:"data"`
	PublishItem PublishItem        `json:"publishItem"`
}

// QueryPublishItemVersionRequest 查询发布版本请求
type QueryPublishItemVersionRequest struct {
	Public      string       `query:"public"`
	PageNo      int64        `query:"pageNo"`
	PageSize    int64        `query:"pageSize"`
	MobileType  ResourceType `query:"mobileType"`
	PackageName string       `query:"packageName"`
	ItemID      int64        `json:"-"`
	OrgID       int64        `json:"-"`
	IsDefault   string       `json:"-"`
}

// QueryPublishItemVersionResponse 查询发布版本响应
type QueryPublishItemVersionResponse struct {
	Header
	Data QueryPublishItemVersionData `json:"data"`
}

// QueryPublishItemVersionData 发布版本数据结构
type QueryPublishItemVersionData struct {
	List  []*PublishItemVersion `json:"list"`
	Total int                   `json:"total"`
}

// GetPublishItemLatestVersionRequest 查询发布内容的最新版本信息请求
type GetPublishItemLatestVersionRequest struct {
	AK             string        `json:"ak"`
	AI             string        `json:"ai"`
	CurrentAppInfo VersionInfo   `json:"currentAppInfo"`
	CurrentH5Info  []VersionInfo `json:"currentH5Info"`
	MobileType     ResourceType  `json:"mobileType"`
	ForceBetaH5    bool          `json:"forceBetaH5"`
	Check          bool          `json:"check"`
}

// GetPublishItemLatestVersionResponse 查询发布内容的最新版本信息响应
type GetPublishItemLatestVersionResponse struct {
	Header
	Data GetPublishItemLatestVersionData `json:"data"`
}

// GetPublishItemLatestVersionData 发布内容的最新版本信息数据
type GetPublishItemLatestVersionData struct {
	AppVersion *PublishItemVersion            `json:"appVerison"`
	H5Versions map[string]*PublishItemVersion `json:"h5Versions"`
}

// PublishItemVersionStates 版本状态 release或者beta
type PublishItemVersionStates string

const (
	PublishItemReleaseVersion PublishItemVersionStates = "release"
	PublishItemBetaVersion    PublishItemVersionStates = "beta"
)

// UpdatePublishItemVersionStatesRequset 上架下架版本请求
type UpdatePublishItemVersionStatesRequset struct {
	PublishItemID        int64                    `json:"publishItemID"`
	PublishItemVersionID int64                    `json:"publishItemVersionID"`
	PackageName          string                   `json:"packageName"`
	VersionStates        PublishItemVersionStates `json:"versionStates"`
	GrayLevelPercent     int                      `json:"grayLevelPercent"` // 灰度百分比，0-100
	Public               bool                     `json:"-"`
}

type PublishItemDistributionResponse struct {
	Header
	Data PublishItemDistributionData `json:"data"`
}

type PublishItemDistributionData struct {
	Default         *PublishItemVersion          `json:"default"`
	Versions        *QueryPublishItemVersionData `json:"versions"`
	Name            string                       `json:"name"`
	DisplayName     string                       `json:"displayName"`
	Desc            string                       `json:"desc"`
	Logo            string                       `json:"logo"`
	CreatedAt       time.Time                    `json:"createdAt"`
	PreviewImages   []string                     `json:"previewImages"`   // 预览图
	BackgroundImage string                       `json:"backgroundImage"` // 背景图
}

// PublishItemCertificationListRequest 认证列表request
type PublishItemCertificationListRequest struct {
	PageNo        uint64 `query:"pageNo"`
	PageSize      uint64 `query:"pageSize"`
	UserID        uint64 `query:"userId"`
	DeviceNo      string `query:"deviceNo"`
	StartTime     uint64 `query:"start"`
	EndTime       uint64 `query:"end"`
	PublishItemID uint64 `query:"publishItemId"`
}

// PublishItemUserlistRequest 添加黑、白名单
type PublishItemUserlistRequest struct {
	PageNo        uint64 `json:"pageNo"`
	PageSize      uint64 `json:"pageSize"`
	UserID        string `json:"userId"`
	UserName      string `json:"userName"`
	DeviceNo      string `json:"deviceNo"`
	PublishItemID uint64 `json:"publishItemId"`
	Operator      string `json:"operator"`
}

// PublishItemSecurityStatusRequest 用户设备安全状态
type PublishItemSecurityStatusRequest struct {
	Ak       string  `query:"ak"`
	Ai       string  `query:"ai"`
	UserID   string  `query:"userId"`
	DeviceNo string  `query:"deviceNo"`
	Lon      float64 `query:"lon"`
	Lat      float64 `query:"lat"`
}

// PublishItemSecuritySetRequest 设置用户设备安全状态
type PublishItemSecuritySetRequest struct {
	NoJailbreak    string `query:"noJailbreak"`
	GeofenceLon    string `query:"geofenceLon"`    // 地理围栏，坐标经度
	GeofenceLat    string `query:"geofenceLat"`    // 地理围栏，坐标纬度
	GeofenceRadius string `query:"geofenceRadius"` // 地理围栏，合理半径
}

// PublishItemSecurityStatusResponse 用户设备安全状态返回
type PublishItemSecurityStatusResponse struct {
	InBlacklist    bool   `json:"inBlacklist"`
	InEraseList    bool   `json:"inEraselist"`
	EraseStatus    string `json:"eraseStatus"`
	NoJailbreak    bool   `json:"noJailbreak"`
	WithinGeofence bool   `json:"withinGeofence"`
}

// PublishItemEraseRequest 更新数据擦除状态
type PublishItemEraseRequest struct {
	DeviceNo    string `json:"deviceNo"`
	EraseStatus string `json:"status"`
	Ak          string `json:"ak"`
	Ai          string `json:"ai"`
}

// PublishItemUserlistData 添加黑、白名单返回结构
type PublishItemUserlistData struct {
	List  []*PublishItemUserListResponse `json:"list"`
	Total uint64                         `json:"total"`
}

type PublishItemAddBlacklistResponse struct {
	Header
	Data PublishItem `json:"data"`
}

type PublicItemAddEraseData struct {
	Data     PublishItem `json:"data"`
	DeviceNo string      `json:"deviceNo"`
}

type PublicItemAddEraseResponse struct {
	Header
	Data PublicItemAddEraseData `json:"data"`
}

type PublishItemDeleteBlacklistResponse struct {
	Header
	Data PublishItemUserListResponse `json:"data"`
}

// PublishItemUserlistResponse 添加黑、白名单返回
type PublishItemUserListResponse struct {
	ID              uint64    `json:"id"`
	UserID          string    `json:"userId"`
	UserName        string    `json:"userName"`
	EraseStatus     string    `json:"eraseStatus"`
	DeviceNo        string    `json:"deviceNo"`
	PublishItemID   uint64    `json:"publishItemId"`
	CreatedAt       time.Time `json:"createdAt"`
	PublishItemName string    `json:"publishItemName"`
}

type PublishItemCertificationResponse struct {
	UserID        string    `json:"userId"`
	UserName      string    `json:"userName"`
	DeviceNo      string    `json:"deviceNo"`
	LastLoginTime time.Time `json:"lastLoginTime"`
}

// PublishItemStatisticsTrendData 统计大盘，整体趋势接口返回
type PublishItemStatisticsTrendData struct {
	Header
	Data PublishItemStatisticsTrendResponse `json:"data"`
}

// PublishItemStatisticsTrendResponse 统计大盘，整体趋势接口返回
type PublishItemStatisticsTrendResponse struct {
	// SevenDayAvgNewUsers 七日平均新用户
	SevenDayAvgNewUsers uint64 `json:"7dAvgNewUsers"`
	// SevenDayAvgNewUsersGrowth 七日平均新用户同比增长率
	SevenDayAvgNewUsersGrowth float64 `json:"7dAvgNewUsersGrowth"`
	// SevenDayAvgActiveUsers 七日平均活跃用户
	SevenDayAvgActiveUsers uint64 `json:"7dAvgActiveUsers"`
	// SevenDayAvgActiveUsersGrowth 七日平均活跃用户同比增长率
	SevenDayAvgActiveUsersGrowth float64 `json:"7dAvgActiveUsersGrowth"`
	// SevenDayAvgNewUsersRetention 七日平均新用户次日留存率
	SevenDayAvgNewUsersRetention string `json:"7dAvgNewUsersRetention"`
	// SevenDayAvgNewUsersRetentionGrowth 七日平均新用户次日留存率同比增长率
	SevenDayAvgNewUsersRetentionGrowth float64 `json:"7dAvgNewUsersRetentionGrowth"`
	// SevenDayAvgDuration 七日平均使用时长
	SevenDayAvgDuration string `json:"7dAvgDuration"`
	// SevenDayAvgDurationGrowth 七日平均使用时长同比增长率
	SevenDayAvgDurationGrowth float64 `json:"7dAvgDurationGrowth"`
	// SevenDayTotalActiveUsers 七日总活跃用户
	SevenDayTotalActiveUsers uint64 `json:"7dTotalActiveUsers"`
	// SevenDayTotalActiveUsersGrowth 七日总活跃用户同比增长率
	SevenDayTotalActiveUsersGrowth float64 `json:"7dTotalActiveUsersGrowth"`
	// MonthTotalActiveUsers 30日总活跃用户
	MonthTotalActiveUsers uint64 `json:"monthTotalActiveUsers"`
	// MonthTotalActiveUsersGrowth 30日总活跃用户同比增长率
	MonthTotalActiveUsersGrowth float64 `json:"monthTotalActiveUsersGrowth"`
	// TotalUsers 总用户数
	TotalUsers uint64 `json:"totalUsers"`
	// TotalCrashRate 总崩溃率
	TotalCrashRate string `json:"totalCrashRate"`
}

// PublishItemStatisticsDetailRequest 版本\渠道详情，明细数据接口返回
type PublishItemStatisticsDetailRequest struct {
	// EndTime 截止时间
	EndTime uint64 `query:"endTime"`
}

// PublishItemStatisticsDetailData 版本\渠道详情，明细数据接口返回
type PublishItemStatisticsDetailData struct {
	Header
	Data []PublishItemStatisticsDetailResponse `json:"data"`
}

// PublishItemStatisticsVersionDetailResponse 版本\渠道详情，明细数据接口返回
type PublishItemStatisticsDetailResponse struct {
	// Key 版本、渠道信息
	Key string `json:"versionOrChannel"`
	// totalUsers 截止今日累计用户
	TotalUsers uint64 `json:"totalUsers"`
	// TotalUsersGrowth 截止今日累计用户占比
	TotalUsersGrowth string `json:"totalUsersGrowth"`
	// NewUsers 新增用户
	NewUsers uint64 `json:"newUsers"`
	// ActiveUsers 活跃用户
	ActiveUsers uint64 `json:"activeUsers"`
	// ActiveUsersGrowth 活跃用户占比
	ActiveUsersGrowth string `json:"activeUsersGrowth"`
	// Launches 启动次数
	Launches uint64 `json:"launches"`
	// UpgradeUser 升级用户
	UpgradeUser uint64 `json:"upgradeUser"`
}

// PublishItemStatisticsErrTrendData 错误报告、错误趋势
type PublishItemStatisticsErrTrendData struct {
	Header
	Data PublishItemStatisticsErrTrendResponse `json:"data"`
}

// PublishItemStatisticsErrTrendResponse 错误报告、错误趋势
type PublishItemStatisticsErrTrendResponse struct {
	// CrashTimes 崩溃次数
	CrashTimes uint64 `json:"crashTimes"`
	// CrashRate 崩溃率
	CrashRate string `json:"crashRate"`
	// CrashRateGrowth 崩溃率同比增长率
	CrashRateGrowth float64 `json:"crashRateGrowth"`
	// AffectUsers 影响用户数
	AffectUsers uint64 `json:"affectUsers"`
	// AffectUsersProportion 影响用户占比
	AffectUsersProportion string `json:"affectUsersProportion"`
	// AffectUsersProportionGrowth 影响用户占比同比增长率
	AffectUsersProportionGrowth float64 `json:"affectUsersProportionGrowth"`
}

// PublishItemStatisticsErrListData 错误报告、错误列表
type PublishItemStatisticsErrListData struct {
	Header
	Data []PublishItemStatisticsErrListResponse `json:"data"`
}

// PublishItemStatisticsErrListResponse 错误报告、错误列表
type PublishItemStatisticsErrListResponse struct {
	// errSummary 错误摘要
	ErrSummary string `json:"errSummary"`
	// AppVersion 版本信息
	AppVersion string `json:"appVersion"`
	// TimeOfFirst 首次发生时间
	TimeOfFirst time.Time `json:"timeOfFirst"`
	// TimeOfRecent 最近发生时间
	TimeOfRecent time.Time `json:"timeOfRecent"`
	// TotalErr 累计错误计数
	TotalErr uint64 `json:"totalErr"`
	// AffectUsers 影响用户数
	AffectUsers uint64 `json:"affectUsers"`
}

// PublishItemMetricsCardinalityResp 监控数据返回结构
type PublishItemMetricsCardinalityResp struct {
	Header
	Data CardinalityResults `json:"data"`
}

type CardinalityResults struct {
	Times   []uint64                `json:"time"`
	Title   string                  `json:"title"`
	Total   uint64                  `json:"total"`
	Results []CardinalityResultItem `json:"results"`
}

type CardinalityResultItem struct {
	Name string                                      `json:"name"`
	Data []map[string]*CardinalityResultDataMapValue `json:"data"`
}

type CardinalityResultDataMapValue struct {
	Agg  string   `json:"agg"`
	Data []uint64 `json:"data"`
	Name string   `json:"name"`
	Tag  string   `json:"tag"`
}

// PublishItemMetricsCardinalitySingleResp 监控数据返回结构，data返回单个
type PublishItemMetricsCardinalitySingleResp struct {
	Header
	Data CardinalityResultsSingle `json:"data"`
}

type CardinalityResultsSingle struct {
	Times   []uint64                      `json:"time"`
	Title   string                        `json:"title"`
	Total   uint64                        `json:"total"`
	Results []CardinalityResultSingleItem `json:"results"`
}

type CardinalityResultSingleItem struct {
	Name string                                           `json:"name"`
	Data []map[string]CardinalityResultDataMapSingleValue `json:"data"`
}

type CardinalityResultDataMapSingleValue struct {
	Agg  string  `json:"agg"`
	Data float64 `json:"data"`
	Name string  `json:"name"`
	Tag  string  `json:"tag"`
}

// PublishItemMetricsCardinalityInterfaceResp 监控数据返回结构，data返回单个
type PublishItemMetricsCardinalityInterfaceResp struct {
	Header
	Data CardinalityResultsInterface `json:"data"`
}

type CardinalityResultsInterface struct {
	Times   []uint64                         `json:"time"`
	Title   string                           `json:"title"`
	Total   uint64                           `json:"total"`
	Results []CardinalityResultInterfaceItem `json:"results"`
}

type CardinalityResultInterfaceItem struct {
	Name string                                              `json:"name"`
	Data []map[string]CardinalityResultDataMapInterfaceValue `json:"data"`
}

type CardinalityResultDataMapInterfaceValue struct {
	Agg  string      `json:"agg"`
	Data interface{} `json:"data"`
	Name string      `json:"name"`
	Unit string      `json:"unit"`
	Tag  string      `json:"tag"`
}

// AppStoreResponse 通过bundleId搜索app store里的链接返回
type AppStoreResponse struct {
	ResultCount int64             `json:"resultCount"`
	Results     []AppStoreResults `json:"results"`
}

// AppStoreResults 通过bundleId搜索app store里的链接数据
type AppStoreResults struct {
	TrackViewURL string `json:"trackViewUrl"`
}
