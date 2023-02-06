// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

declare namespace PUBLISHER {
  interface IAuthenticate {
    userName?: string;
    userId: string;
    deviceNo: string;
    createdAt: string;
  }

  interface IListQuery extends IPagingReq, MonitorKey {
    artifactId: string;
    q?: string;
  }

  interface IBlackList {
    id: string;
    userName: number;
    userId: string;
    deviceNo: string;
    createdAt: string;
  }
  interface IListQuery extends MonitorKey {
    pageNo: number;
    pageSize: number;
  }

  type EraseStatus = 'success' | 'failure' | 'erasing';

  interface IErase {
    deviceNo: string;
    createdAt: string;
    eraseStatus: EraseStatus;
  }

  interface IPublisher {
    id: string;
    name: string;
    logo: string;
    desc: string;
    type: string;
  }

  interface PublisherListQuery {
    orgId: number;
    pageNo: number;
    pageSize: number;
    loadMore?: boolean;
    q?: string;
  }

  interface IArtifacts {
    id: string;
    name: string;
    orgId: string;
    publisherId: number;
    type: string;
    logo: string;
    public: boolean;
    desc: string;
    updatedAt: string;
    createdAt: string;
    grayLevelPercent: string;
    noJailbreak: boolean;
    isGeofence: boolean;
    downloadUrl?: string;
    geofenceLon?: string | number;
    geofenceLat?: string | number;
    geofenceRadius?: string | number;
    versionStates?: versionType;
  }

  interface ArtifactsListQuery {
    publisherId: string;
    pageNo: number;
    pageSize: number;
    public: boolean;
    type: APPLICATION.appMode;
    loadMore?: boolean;
    q?: string;
  }

  interface IMeta {
    appId: string;
    appName: string;
    projectId: string;
    projectName: string;
  }

  interface IResource {
    meta: {
      appStoreURL?: string;
      build: string;
      buildID: string;
      bundleId: string;
      byteSize: number;
      displayName: string;
      fileId: number;
      packageName: string;
      version: string;
    };
    name: string;
    type: MobileType;
    url: string;
  }

  type versionType = 'release' | 'beta';

  interface IVersion {
    id: string;
    buildId: string;
    version: string;
    releaseId: string;
    logo: string;
    desc: string;
    public: boolean;
    isDefault: boolean;
    artifactsId?: string;
    createdAt: string;
    meta: IMeta;
    versionStates: versionType;
    grayLevelPercent?: number;
    targetMobiles: {
      ios: string[];
      android: string[];
    };
    mobileType: MobileType;
    resources: IResource[];
  }

  type MobileType = 'ios' | 'android' | 'h5' | 'aab';

  interface VersionListQuery {
    artifactsId: string;
    mobileType?: MobileType;
    packageName?: string;
    pageNo: number;
    pageSize: number;
  }

  interface IUpdateGrayQuery {
    action: 'publish' | 'unpublish';
    publishItemID: number;
    publishItemVersionID: number;
    versionStates: versionType;
    packageName?: string;
    grayLevelPercent?: number;
  }

  interface IOnlineVersionQuery {
    publishItemId: number;
    mobileType?: MobileType;
    packageName?: string;
  }

  interface IOnlineVersion {
    id: number;
    version: string;
    public: boolean;
    versionStates: versionType;
    grayLevelPercent: string;
  }

  interface AllVersionQuery extends MonitorKey {
    publisherItemId: string;
    group: string;
    count: string;
    start: number;
    end: number;
  }

  interface IStatisticsTrend {
    '7dAvgActiveUsers': number;
    '7dAvgActiveUsersGrowth': number;
    '7dAvgDuration': number;
    '7dAvgDurationGrowth': number;
    '7dAvgNewUsers': number;
    '7dAvgNewUsersGrowth': number;
    '7dAvgNewUsersRetention': number;
    '7dAvgNewUsersRetentionGrowth': number;
    '7dTotalActiveUsers': number;
    '7dTotalActiveUsersGrowth': number;
    monthTotalActiveUsers: number;
    monthTotalActiveUsersGrowth: number;
    totalCrashRate: number;
    totalUsers: number;
  }

  interface IErrorTrend extends MonitorKey {
    affectUsers: number;
    affectUsersProportion: string;
    affectUsersProportionGrowth: string;
    crashRate: string;
    crashRateGrowth: string;
    crashTimes: number;
  }

  interface ErrorListQuery extends MonitorKey {
    artifactsId: string;
    start: number;
    end: number;
    filter_av?: string;
  }

  interface ErrorItem {
    affectUsers: number;
    appVersion: string;
    errSummary: string;
    timeOfFirst: string;
    timeOfRecent: string;
    totalErr: number;
  }
  interface ErrorDetailQuery extends MonitorKey {
    artifactsId: string;
    start: number;
    end: number;
    filter_error: string;
    limit: number;
  }

  interface ErrorDetail {
    '@timestamp': number;
    fields: {
      count: number;
    };
    name: string;
    tags: {
      av: string;
      br: string;
      md: string;
      dh: string;
      channel: string;
      cid: string;
      cpu: string;
      device: string;
      doc_path: string;
      gps: string;
      host: string;
      ip: string;
      jb: string;
      mem: string;
      ns: string;
      os: string;
      osv: string;
      osn: string;
      rom: string;
      tk: string;
      type: string;
      uid: string;
      vid: string;
      stack_trace: string;
      error: string;
    };
    timestamp: number;
  }

  interface IError {
    id: string;
    title: string;
    version: string;
    firstHappen: string;
    lastHappen: string;
    errorCount: number;
    impactUserCount: number;
  }

  interface IChartQuery extends MonitorKey {
    publisherItemId: string;
    start?: number;
    end?: number;
    cardinality?: string | string[];
    align?: boolean;
    points?: number;
    group?: string;
    filter_av?: string;
    filter_ch?: string;
    count?: string;
    limit?: number;
  }

  interface VersionStatisticQuery extends MonitorKey {
    artifactsId: string;
    endTime: string;
  }

  interface VersionStatistic {
    activeUsers: number;
    activeUsersGrowth: string;
    launches: number;
    newUsers: number;
    totalUsers: number;
    totalUsersGrowth: string;
    upgradeUser: number;
    versionOrChannel: string;
  }

  interface QueryLibVersion {
    libID: number;
  }
  interface LibVersion {
    libName: string;
    version: string;
  }

  interface MonitorKey {
    ak?: string;
    ai?: string;
  }

  interface MonitorItem {
    ak: string;
    ai: string;
    env: string;
    appId: number;
  }

  // 离线包只支持android和ios
  type OfflinePackageType = 'android' | 'ios';

  interface OfflinePackage {
    publishItemId: string;
    data: FormData;
  }
}
