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

import React from 'react';
import { Spin, message, Button } from 'antd';
import { withRouter } from 'react-router-dom';
import QRCode from 'qrcode.react';
import downloadBg_2x from '../images/download/download-bg@2x.png';
import downloadR1_2x from '../images/download/download-r1@2x.png';
import downloadC_2x from '../images/download/download-c@2x.png';
import downloadS1_2x from '../images/download/download-s1@2x.png';
import downloadY1_2x from '../images/download/download-y1@2x.png';
import downloadY2_2x from '../images/download/download-y2@2x.png';

import agent from 'superagent';
import dayjs from 'dayjs';
import './download.scss';

interface IObj {
  [key: string]: any;
}

type ClientType = 'iOS' | 'Android' | 'PC';
/**
 * 判断客户端
 */
export const judgeClient = (): ClientType => {
  const { userAgent } = navigator;
  let client: ClientType;
  // Android机中，userAgent字段中也包含safari，因此要先判断是否是安卓
  if (/(Android)/i.test(userAgent)) {
    client = 'Android';
    // XXX 2020/4/23 IOS 13 之后，在Ipad中，safari默认请求桌面网站，导致userAgent和MAC中safari的userAgent一样
  } else if (/(iPhone|iPad|iPod|iOS)/i.test(userAgent) || (/safari/i.test(userAgent) && 'ontouchend' in document)) {
    client = 'iOS';
  } else {
    client = 'PC';
  }
  return client;
};

const getOrgFromPath = () => {
  return window.location.pathname.split('/')[1] || '-';
};

const isEmptyObj = (obj: IObj) => {
  return obj === null || obj === undefined || Object.keys(obj).length === 0;
};

const DownloadPage = ({ match }: any) => {
  const [isLoading, setIsLoading] = React.useState(false);
  const [hasDefault, setHasDefault] = React.useState(false);
  const [versionList, setVersionList] = React.useState([] as any[]);
  const [current, setCurrent] = React.useState({ activeKey: '', pkg: {} } as IObj);
  const [logo, setLogo] = React.useState('');
  const [showDownload, setShowDownload] = React.useState(false);
  const [name, setName] = React.useState('');
  const client = judgeClient().toLowerCase();
  const curOrg = getOrgFromPath();
  React.useEffect(() => {
    setIsLoading(true);
    agent
      .get(`/api/${curOrg}/publish-items/${match.params.publishItemId}/distribution`)
      .set('org', curOrg)
      .query({ mobileType: client })
      .then((response: any) => {
        const { success, data, err } = response.body;
        if (success) {
          const { default: defaultVersion } = data as { default: any; versions: { list: any[]; total: number } };
          const vList = isEmptyObj(defaultVersion) ? [] : [defaultVersion];
          let has_default = false;
          if (defaultVersion) {
            const { id, updatedAt } = defaultVersion;
            const resources = defaultVersion.resources || [];
            let pkg = resources[0] || {};
            if (client !== 'pc') {
              pkg = resources.filter((item: IObj) => item.type === client)[0] || {};
            }
            const { meta = {}, type } = pkg;
            const activeKey = `${id}-${type}-${meta.fileId}`;
            setCurrent({ activeKey, pkg, updatedAt });
            has_default = resources.some((t: any) => t.type === client || (t.type === 'data' && client === 'pc'));
            setHasDefault(has_default);
          }
          const logStr = (defaultVersion || {}).logo;
          const reg = /^https?:\/\/.*?(?=\/)/i;
          const logoUrl = logStr ? `${logStr.replace(reg, '')}` : '';
          setLogo(logoUrl);
          setName(data.name);
          setVersionList(vList);
          if (client === 'pc') {
            const { resources = [] } = defaultVersion || {};
            const type = resources?.[0]?.type;
            setShowDownload(has_default && type === 'data');
          } else {
            setShowDownload(has_default);
          }
        } else {
          message.error(err.msg || '很抱歉，当前请求遇到问题，我们将尽快修复！');
        }
        setIsLoading(false);
      })
      .catch(() => {
        setIsLoading(false);
      });
  }, [client, match.params.publishItemId]);
  const handleChangePkg = (activeKey: string, pkg: IObj, updatedAt: string) => {
    const { type } = pkg;
    let download = false;
    if (type === 'data') {
      download = client === 'pc' && hasDefault;
    } else {
      download = client !== 'pc' && hasDefault;
    }
    setShowDownload(download);
    setCurrent({ activeKey, pkg, updatedAt });
  };
  const handleDownload = () => {
    if (isEmptyObj(current)) {
      message.error('请选择要下载的版本');
      return;
    }
    const { meta = {}, url, type }: IObj = current.pkg || {};
    if (client === 'pc' && type !== 'data') {
      message.info('请在移动端下载');
      return;
    }
    const isInWeChat: boolean = /micromessenger/i.test(navigator.userAgent);
    if (isInWeChat) {
      message.info(`请在${client === 'ios' ? 'Safari' : ''}浏览器中打开当前页面`);
      return;
    }
    let downloadUrl = url;
    if (client === 'ios') {
      const { installPlist = '' } = meta;
      if (!installPlist) {
        message.info('下载链接不存在，请稍后刷新重试');
        return;
      }
      if (installPlist.indexOf('https://') === -1) {
        message.info('请使用HTTPS协议链接');
        return;
      }
      downloadUrl = `itms-services://?action=download-manifest&url=${installPlist}`;
    }
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.click();
    link.remove();
  };
  const byteToM = ({ meta }: IObj) => {
    const { byteSize = 0 } = meta || {};
    return byteSize ? `${(byteSize / 1024 / 1024).toFixed(2)}M` : '';
  };
  const versions = [...versionList].map((item) => {
    const { resources = [], id, updatedAt } = item;
    let packages = resources || [];
    if (client !== 'pc') {
      packages = packages.filter((pkg: IObj) => pkg.type === client);
    }
    return packages.map((pkg: IObj) => {
      const { meta, name: vName, type } = pkg;
      const { version, fileId } = meta;
      const displayname = version ? `${version}-${vName}` : vName;
      const key = `${id}-${type}-${fileId}`;
      const isActive = key === current.activeKey;
      return (
        <li
          className={`version-item ${isActive ? 'active' : ''}`}
          data-a={type}
          key={key}
          onClick={() => {
            handleChangePkg(key, pkg, updatedAt);
          }}
        >
          {displayname}
        </li>
      );
    });
  });

  const appStoreURL = current?.pkg?.meta?.appStoreURL;
  return (
    <Spin spinning={isLoading}>
      <div className="download-page bg-gray">
        <div className="content">
          {client === 'ios' && appStoreURL ? (
            <div className="jump-app-store">
              <a href={appStoreURL} target="_blank" rel="noopener noreferrer">
                跳转至App Store
              </a>
            </div>
          ) : null}
          <div className="card-container">
            <div className="qrcode-wrap">
              {client !== 'pc' && logo ? (
                <img className="logo" src={logo} alt="" />
              ) : (
                <QRCode className="qrcode" value={window.location.href} level="H" bgColor="rgba(0,0,0,0)" />
              )}
            </div>
            <p className="app-name">{name}</p>
            <p className="tips download-notice">扫描二维码下载</p>
            <p className="tips download-notice">或用手机浏览器输入网址: {window.location.href}</p>
            <div className="line" />
            {React.Children.count(versions) ? (
              <>
                <ul className="version-list">{versions}</ul>
                <p className="tips version-notice">{byteToM(current.pkg)}</p>
                <p className="tips version-notice">
                  更新于: {current.updatedAt ? dayjs(current.updatedAt).format('YYYY/MM/DD HH:mm') : '--'}
                </p>
                <div className="button-wrap">
                  {showDownload ? (
                    <Button type="primary" onClick={handleDownload}>
                      下载{client === 'pc' ? '' : '安装'}
                    </Button>
                  ) : null}
                </div>
              </>
            ) : (
              <p className="tips">暂时没有符合该机型的安装包</p>
            )}
          </div>
        </div>
        <img className="bg-img" src={downloadBg_2x} alt="" />
        <div className="bg-wrap">
          <img className="bg-img" src={downloadBg_2x} alt="" />
          <img className="people" src={downloadR1_2x} alt="" />
          <img className="water-mark" src={downloadC_2x} alt="" />
          <img className="s1" src={downloadS1_2x} alt="" />
          <img className="y1" src={downloadY1_2x} alt="" />
          <img className="y2" src={downloadY2_2x} alt="" />
        </div>
      </div>
    </Spin>
  );
};

export default withRouter(DownloadPage);
