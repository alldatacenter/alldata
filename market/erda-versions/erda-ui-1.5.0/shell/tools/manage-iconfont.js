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

/* eslint-disable no-console */
const fs = require('fs');
const path = require('path');
const { walker } = require('./file-walker');
const http = require('superagent');

// 无法检测到的存储动态iconfont信息，后期也需要手动更新
const tempVariableIconPath = path.resolve(__dirname, './temp-variable-iconfont.json');
const relatedProjects = ['terminus-dice', 'dice-layout'];

// 该接口用于撤销删除操作
const restoreDeletedIconUrl = 'https://www.iconfont.cn/api/icon/restoreProjectDeletedIcons.json';
// 该接口用于获取iconfont中项目数据
const projectsFetchUrl = 'https://www.iconfont.cn/api/user/myprojects.json';
// 该接口用于获取指定项目下的iconfont数据
const iconsFetchUrl = 'https://www.iconfont.cn/api/project/detail.json';
// 该接口用于删除制定id的iconfont
const iconsDeleteUrl = 'https://www.iconfont.cn/api/icon/deleteProjectIcon.json';
// 该接口用于删除制定id的iconfont
const deletedIconUrl = 'https://www.iconfont.cn/api/project/getProjectDeletedIcons.json';

// 登陆https://www.iconfont.cn/manage/index，通过f12手动获取接口的入参信息，包括下面的cookie值
const requestParams = {
  t: '1603171812351',
  ctoken: '_vrr27wzht3OR4NYzvQ8RU8A',
};

const cookie =
  'ctoken=_vrr27wzht3OR4NYzvQ8RU8A; EGG_SESS_ICONFONT=U8AXvqwdm-42-umGXGwgKq_Emj2wuVCkA87TjZ3dn6xm2T4whio3sIKoy4kjkuBSusLMQ-0MhcjWBE1FwhfGmMbpO9xPCEANAHIhoET_7kJ_pbscGV6FmfCh8QTWcmCiTv5lhhXEW-AxLfe1otCy-eI-zPgODc0D5EZxlVSk4mqOdEz-94IZi5OAcsu3pRkTAQs9KRTgwyfMtp67P9YXwDeVNoXPHTR1XHpaQgBHgWZxIoXczyxCXVtKz5kL3XUgvwp6JLe2wev9xkYzghiHai8qD-IW7Y1geGJ1t7DZpRojCu45sjyYzfUi8Z4C92GiKnM_HBWtGZSNh9fPZKzi9PPy1119sjt4-z9m3nAw8FTZ8oh_b93eiXLSeGwju8N4OynopeUV81az5imoZmTUfooAFW9T2bxrX8GUMxq5IiWUiv2FgC-7e_ShRA6jNBjYvcgTZPG_EDpHuhv7HpLsEpnwrjUZESIX_aXFhZ72jg_h-SR5bhK38ur8Vs0naHOB2heHAcQpLut_xmyHKOORn1le-9ByNEL98Oxllv5K0oD3-0a9Y-BjgYryPoMTL66fxEWWU_ub4ptIb3N9Fcty4Th3X2Bn7TnbozNlEqXeqAqJGg6ZfLChCr94DvayKNNRtHB2sL-tgTfvlPl2kj1pB9R9fZ4PGOxB0RUA_TvD0wVhrikVVgE0kNkAWqwTPu2zOqmHXx-U2SY-6AlTnc0wIK-e3UosG5YpKB5Hivw1oAes7s6_1vQUN2lFRaCTyoTBZo9e5ZYyiYRIr2oM5ZXQFOjUHXlD4wqAq0x-RSpz2EVnaRY7fPcnEbo30LifQSJ1agJ7wqKhEWeG_4D9kWRusQ9xNSV6kyjG395-bcTPeirkUxMWB6Afql569eRjon8ZjFltFfFi3-UexFctdRn5lPAEAuu1gKSby7GymKEpU3Ta3NSVO-PPzD9mH2R4Qkwg2KOplTe2i8SeForTMAh7tTOEqy8XHzrat7q69D9x8RE=';

let iconNameList = [];
const iconMap = {};
const uselessIconMap = {};
const nowTime = new Date().valueOf();

// 匹配html文件中的icon-* ，icon* ，icon: '*'
const iconReg1 = /['"\s*](icon:\s+'([\w]+)')|(icon-([\w-]+))|(icon[0-9a-zA-Z][\w-]+)/g;
// 匹配打包后文件中的iconfont
const iconReg2 = /"Icon(\\"],\s{.+?type:\s[\\"']+([\w-]+))|(font\s(icon[\w-]+))[\\"']+/g;

const extractIconFromFile = (content, filePath, isEnd, resolve) => {
  // 只处理代码文件
  if (!['.tsx', '.ts', '.js', '.jsx', '.html'].includes(path.extname(filePath)) && !isEnd) {
    return;
  }

  let match1 = iconReg1.exec(content);
  let match2 = iconReg2.exec(content);

  // 抠出当前文件所有iconfont标签
  if (filePath.indexOf('/app/') >= 0) {
    while (match1) {
      if (match1) {
        const [, , iconName, , _iconName, iconSuffix] = match1;
        const iconfont = iconName || iconSuffix || _iconName;

        if (!iconNameList.includes(iconfont)) {
          iconNameList.push(iconfont);
        }
      }
      match1 = iconReg1.exec(content);
    }
  } else if (filePath.indexOf('/public/scripts') >= 0) {
    while (match2) {
      if (match2) {
        const [, , iconName, _iconName, iconSuffix] = match2;
        const iconfont = iconName || iconSuffix || _iconName;

        if (!iconNameList.includes(iconfont)) {
          iconNameList.push(iconfont);
        }
      }
      match2 = iconReg2.exec(content);
    }
  }

  if (!isEnd && iconNameList.length === 0) {
    return;
  }
  isEnd && resolve();
};

// 获取iconfont项目数据
const getProjectsData = () => {
  return http
    .get(projectsFetchUrl)
    .query(requestParams)
    .set('cookie', cookie)
    .then((response) => response.body.data.corpProjects)
    .catch((err) => {
      console.log('获取iconfont项目数据失败：', err);
      return false;
    });
};
// 获取deleted iconfont项目数据
const getDeletedIconData = (pid) => {
  return http
    .get(deletedIconUrl)
    .query({ requestParams, limit: 1000, page: 1, pid })
    .set('cookie', cookie)
    .then((res) => res.body.data.icons)
    .catch((err) => {
      console.log('获取回收站中iconfont数据失败：', err);
      return false;
    });
};

// 根据项目id获取iconfont数据
const getIconData = (pid) => {
  return http
    .get(iconsFetchUrl)
    .query({ ...requestParams, pid })
    .set('cookie', cookie)
    .then((response) => response.body.data.icons)
    .catch((err) => {
      console.log('获取iconfont数据失败：', err);
      return false;
    });
};

// 根据iconfont的id删除对应的iconfont
const deleteIconByProject = async (pid, pName) => {
  const idList = Object.keys(uselessIconMap[pid].icons);
  const ids = idList.join(',');

  return new Promise((resolve, reject) => {
    return http
      .post(iconsDeleteUrl)
      .send({ ...requestParams, type: 'project', ids, pid })
      .set('cookie', cookie)
      .then(() => {
        console.log(`清除了${pName}项目中${idList.length}个无用的iconfont`);
        resolve(idList.length);
      })
      .catch((err) => {
        console.error('删除iconfont失败:', err);
        reject();
      });
  });
};

// 计算出需要清除掉的iconfont
const findUselessIcon = (pid, projectName) => {
  return new Promise(async (resolve) => {
    uselessIconMap[pid] = { projectName, icons: {} };

    const iconData = await getIconData(pid);
    iconData &&
      iconData.forEach(({ id, font_class }) => {
        if (!iconNameList.includes(font_class)) {
          uselessIconMap[pid].icons[id] = font_class;
        }
      });
    resolve();
  }).catch((err) => {
    return err;
  });
};

const manageIconFont = async () => {
  await new Promise((resolve) => {
    // 第一步，找出使用了 iconfont 的内容
    walker({
      root: path.resolve(__dirname, '../public/scripts'),
      dealFile: (content, filePath, isEnd) => {
        extractIconFromFile.apply(null, [content, filePath, isEnd, resolve]);
      },
    });
  });
  await new Promise((resolve) => {
    // 第一步，找出使用了 iconfont 的内容
    walker({
      root: path.resolve(__dirname, '../app'),
      dealFile: (content, filePath, isEnd) => {
        extractIconFromFile.apply(null, [content, filePath, isEnd, resolve]);
      },
    });
  });

  if (!iconNameList.length) {
    console.error('未发现需要需要清理的iconfont，程序退出');
    return;
  }

  iconNameList.forEach((iconfont) => {
    iconMap[iconfont] = iconfont;
  });

  // 更新使用到的iconfont
  const varIconMap = JSON.parse(fs.readFileSync(tempVariableIconPath, { encoding: 'utf-8' }));
  iconNameList = Object.values({ ...iconMap, ...varIconMap });

  // 第二步，比对iconfont库的icon，找出需要删除的iconfont
  const projectData = await getProjectsData();
  if (!projectData || !projectData.length) {
    console.error('获取iconfont project信息失败');
    return;
  }

  const promiseArr = [];
  projectData.forEach(({ id, name }) => {
    if (relatedProjects.includes(name)) {
      const findPromise = findUselessIcon(id, name);
      promiseArr.push(findPromise);
    }
  });

  const getIdsSuccess = await Promise.all(promiseArr)
    .then(() => true)
    .catch(() => false);

  if (!getIdsSuccess) {
    console.log('查找无用iconfont失败');
    return;
  }

  // 第三步，获取需要删除的iconfont的ids
  const delPromiseArr = [];
  Object.keys(uselessIconMap).forEach((pid) => {
    const deletePromise = deleteIconByProject(pid, uselessIconMap[pid].projectName);
    delPromiseArr.push(deletePromise);
  });

  await Promise.all(delPromiseArr)
    .then((res) => {
      const uselessLen = res.reduce((t, item) => {
        return t + item;
      }, 0);
      console.log(`共清除了${uselessLen}个无用的iconfont，再见👋`);
      return true;
    })
    .catch(() => {
      return false;
    });
};

const restoreDeletedIcon = async (pid) => {
  const deletedIcons = await getDeletedIconData(pid);

  const justDeletedIcons = deletedIcons.filter(({ deleted_at }) => {
    const time = new Date(deleted_at).valueOf();
    // 撤销十分钟内被删除的iconfont
    return nowTime - time < 1000 * 60 * 10;
  });

  const ids = justDeletedIcons.map(({ id }) => id).join(',');

  return new Promise((resolve, reject) => {
    return http
      .post(restoreDeletedIconUrl)
      .send({ ...requestParams, ids, pid })
      .set('cookie', cookie)
      .then(() => {
        resolve();
      })
      .catch((err) => {
        console.error('撤销删除失败:', err);
        reject();
      });
  });
};

// 撤销十分钟内被删除的iconfont
const restoreIconFont = async () => {
  const projectData = await getProjectsData();
  if (!projectData || !projectData.length) {
    console.error('获取iconfont project信息失败');
    return;
  }

  // 第三步，获取需要删除的iconfont的ids
  const restorePromiseArr = [];
  projectData.forEach(({ id }) => {
    const restorePromise = restoreDeletedIcon(id);
    restorePromiseArr.push(restorePromise);
  });

  await Promise.all(restorePromiseArr)
    .then(() => {
      console.log('完成撤销删除工作');
      return true;
    })
    .catch(() => {
      return false;
    });

  console.log('iconfont已撤销删除，再见👋');
};

module.exports = {
  manageIconFont,
  restoreIconFont,
};
