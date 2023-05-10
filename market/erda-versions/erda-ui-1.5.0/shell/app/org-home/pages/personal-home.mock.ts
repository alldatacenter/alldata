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

export const mockSidebar: CONFIG_PAGE.RenderConfig = {
  scenario: {
    scenarioKey: 'home-page-sidebar',
    scenarioType: 'home-page-sidebar', // 后端定义
  },
  protocol: {
    hierarchy: {
      root: 'page',
      structure: {
        page: ['sidebar'],
        sidebar: ['myOrganization', 'myInfo'],
        myInfo: ['myProject', 'myApplication'],
        myOrganization: ['orgImage', 'orgSwitch', 'joinedBrief', 'emptyOrganization'],
        createOrgBtnWrapper: ['createOrgBtn'],
        emptyOrganization: ['emptyOrgTitle', 'createOrgBtnWrapper', 'emptyOrgText', 'orgFormModal', ''],
        myProject: ['myProjectTitle', 'myProjectFilter', 'myProjectList', 'emptyProject'],
        emptyProject: ['projectTipWithoutOrg', 'projectTipWithOrg'],
        projectTipWithOrg: ['createProjectTip', 'createProjectTipWithoutOrg'],
        myApplication: ['myApplicationTitle', 'myApplicationFilter', 'myApplicationList', 'emptyApplication'],
      },
    },
    components: {
      page: {
        type: 'Container',
        props: {
          visible: true,
          whiteBg: true,
          fullHeight: true,
        },
      },
      sidebar: {
        type: 'Container',
        props: {
          whiteBg: true,
          fullHeight: true,
        },
      },
      myOrganization: {
        type: 'Container',
        props: {
          spaceSize: 'big',
        },
      },
      emptyOrganization: {
        type: 'Container',
        props: {
          visible: true,
        },
      },
      createOrgBtnWrapper: {
        type: 'RowContainer',
        props: {
          contentSetting: 'center',
          visible: true,
        },
      },
      createOrgBtn: {
        type: 'Button',
        props: {
          text: 'create organization',
          type: 'primary',
        },
        operations: {
          click: {
            key: 'addOrg',
            reload: false,
            command: { key: 'set', state: { visible: true }, target: 'orgFormModal' },
          },
        },
      },
      emptyOrgTitle: {
        type: 'TextGroup',
        props: {
          visible: true,
          align: 'center',
          value: [
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '未加入任何组织',
              },
            },
          ],
        },
      },
      emptyOrgText: {
        type: 'TextGroup',
        props: {
          visible: true,
          align: 'center',
          value: [
            {
              props: {
                renderType: 'linkText',
                visible: true,
                value: {
                  text: [{ text: '了解如何受邀加入到组织', operationKey: 'toJoinOrgDoc' }],
                },
              },
            },
            {
              props: {
                renderType: 'linkText',
                visible: true,
                value: {
                  text: [{ text: '浏览公开组织信息', operationKey: 'toPublicOrgPage' }],
                },
              },
            },
          ],
        },
        operations: {
          toJoinOrgDoc: {
            command: {
              key: 'goto',
              target: 'https://docs.erda.cloud/',
              jumpOut: true,
              visible: false,
            },
            key: 'click',
            reload: false,
            show: false,
          },
          toPublicOrgPage: {
            command: {
              key: 'goto',
              target: 'orgList',
              jumpOut: true,
              visible: false,
            },
            key: 'click',
            reload: false,
            show: false,
          },
        },
      },
      orgFormModal: {
        name: 'orgFormModal',
        type: 'FormModal',
        operations: {
          submit: {
            key: 'submitOrg',
            reload: true,
          },
        },
        props: {
          title: '添加组织',
          fields: [
            {
              key: 'displayName',
              label: '组织名称',
              component: 'input',
              required: true,
            },
            {
              key: 'name',
              label: '组织标识',
              component: 'input',
              required: true,
              rules: [
                {
                  msg: '由小写字母、数字，-组成',
                  pattern: '/^[a-z0-9-]*$/',
                },
              ],
            },
            {
              key: 'desc',
              label: '组织描述',
              component: 'textarea',
              required: true,
              componentProps: {
                autoSize: {
                  minRows: 2,
                  maxRows: 4,
                },
                maxLength: 500,
              },
            },
            {
              key: 'scope',
              label: '谁可以看到该组织',
              component: 'radio',
              required: true,
              componentProps: {
                radioType: 'radio',
                displayDesc: true,
              },
              dataSource: {
                static: [
                  {
                    name: '私人的',
                    desc: '小组及项目只能由成员查看',
                    value: 'private',
                  },
                  {
                    name: '公开的',
                    desc: '无需任何身份验证即可查看该组织和任何公开项目',
                    value: 'public',
                  },
                ],
              },
            },
            {
              key: 'logo',
              label: '组织 Logo',
              component: 'upload',
              componentProps: {
                uploadText: '上传图片',
                sizeLimit: 2,
                supportFormat: ['png', 'jpg', 'jpeg', 'gif', 'bmp'],
              },
            },
          ],
        },
        state: {
          visible: false,
          formData: {
            logo: 'https://dice.dev.terminus.io:3000/api/files/71b6e5dc8e1f4b08899494a6ac428708',
          },
        },
      },
      orgImage: {
        type: 'Image',
        props: {
          src: 'https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=3314214233,3432671412&fm=26&gp=0.jpg',
          size: 'normal',
          type: 'organization',
        },
      },
      orgSwitch: {
        type: 'DropdownSelect',
        props: {
          visible: true,
          options: [
            {
              label: '组织B',
              value: 'organizeB',
              prefixImgSrc:
                'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQYQY0vUTJwftJ8WqXoLiLeB--2MJkpZLpYOA&usqp=CAU',
              operations: {
                click: {
                  key: 'click',
                  show: false,
                  reload: false,
                  command: {
                    key: 'goto',
                    target: 'orgRoot',
                    jumpOut: false,
                    state: {
                      params: {
                        orgName: 'organizeA',
                      },
                    },
                  },
                },
              },
            },
            {
              label: '组织A',
              value: 'organizeA',
              prefixImgSrc:
                'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTI1EaartvKCwGgDS7FTpu71EyFs1wCl1MsFQ&usqp=CAU',
              operations: {
                click: {
                  key: 'click',
                  show: false,
                  reload: false,
                  command: {
                    key: 'goto',
                    target: 'orgRoot',
                    jumpOut: false,
                    state: {
                      params: {
                        orgName: 'organizeA',
                      },
                    },
                  },
                },
              },
            },
          ],
          quickSelect: [
            {
              value: 'orgList',
              label: '浏览公开组织',
              operations: {
                click: {
                  key: 'click',
                  show: false,
                  reload: false,
                  command: {
                    key: 'goto',
                    target: 'orgList',
                    jumpOut: false,
                  },
                },
              },
            },
          ],
        },
        state: {
          value: 'organizeC',
          label: '组织C',
        },
      },
      joinedBrief: {
        type: 'Table',
        props: {
          visible: false,
          rowKey: 'id',
          columns: [
            { title: '', dataIndex: 'category' },
            { title: '', dataIndex: 'number', width: 42 },
          ],
          showHeader: false,
          pagination: false,
          styleNames: {
            'no-border': true,
            'light-card': true,
          },
        },
        data: {
          list: [
            {
              id: 1,
              category: {
                renderType: 'textWithIcon',
                prefixIcon: 'project-icon',
                value: '参与项目数：',
                colorClassName: 'color-primary',
              },
              number: 5,
            },
            {
              id: 1,
              category: {
                renderType: 'textWithIcon',
                prefixIcon: 'app-icon',
                value: '参与应用数：',
                colorClassName: 'color-primary',
              },
              number: 45,
            },
          ],
        },
      },
      emptyProject: {
        // type: 'EmptyHolder',
        // props: {
        //   tip: '暂无数据，请先加入组织~',
        //   visible: true,
        //   relative: true,
        // }
        type: 'Container',
        props: {
          visible: true,
        },
      },
      projectTipWithoutOrg: {
        type: 'Text',
        props: {
          visible: false,
          renderType: 'linkText',
          value: {
            text: ['请先加入组织或者', { text: '了解更多项目内容', operationKey: 'toJoinOrgDoc' }],
          },
        },
        operations: {
          toJoinOrgDoc: {
            command: {
              key: 'goto',
              target: 'https://docs.erda.cloud/',
              jumpOut: true,
              visible: false,
            },
            key: 'click',
            reload: false,
            show: false,
          },
        },
      },
      projectTipWithOrg: {
        type: 'Container',
        props: {
          visible: true,
        },
      },
      createProjectTipWithoutOrg: {
        type: 'Text',
        props: {
          visible: true,
          renderType: 'linkText',
          value: {
            text: ['请先加入组织', { text: '了解更多项目内容', operationKey: 'toJoinOrgDoc' }],
          },
        },
        operations: {
          toJoinOrgDoc: {
            command: {
              key: 'goto',
              target: 'https://docs.erda.cloud/',
              jumpOut: true,
              visible: false,
            },
            key: 'click',
            reload: false,
            show: false,
          },
        },
      },

      createProjectTip: {
        type: 'Text',
        props: {
          visible: false,
          renderType: 'linkText',
          value: {
            text: [
              { text: '如何创建项目', operationKey: 'createProjectDoc' },
              ' 或 ',
              { text: '通过公开组织浏览公开项目信息', operationKey: 'toPublicOrgPage' },
            ],
          },
        },
        operations: {
          createProjectDoc: {
            command: {
              key: 'goto',
              target: 'https://docs.erda.cloud/',
              jumpOut: true,
              visible: false,
            },
            key: 'click',
            reload: false,
            show: false,
          },
          toPublicOrgPage: {
            command: {
              key: 'goto',
              target: 'orgList',
              jumpOut: true,
              visible: false,
            },
            key: 'click',
            reload: false,
            show: false,
          },
        },
      },

      myInfo: {
        type: 'Container',
        props: {
          fullHeight: true,
          scrollAuto: true,
        },
      },
      myProject: {
        type: 'Container',
        props: {
          spaceSize: 'middle',
        },
      },
      myProjectTitle: {
        type: 'Title',
        props: {
          title: '项目',
          level: 1,
          noMarginBottom: true,
          showDivider: true,
          size: 'normal',
          operations: [
            {
              props: {
                text: '创建',
                visible: false,
                disabled: false,
                disabledTip: '暂无创建项目权限',
                type: 'link',
              },
              operations: {
                click: {
                  command: {
                    key: 'goto',
                    target: 'createProject',
                    jumpOut: false,
                    visible: false,
                  },
                  key: 'click',
                  reload: false,
                  show: false,
                },
              },
            },
          ],
        },
      },
      myProjectFilter: {
        type: 'ContractiveFilter',
        props: {
          visible: false,
          delay: 1000,
          fullWidth: true,
        },
        state: {
          conditions: [
            {
              key: 'title',
              label: '标题',
              emptyText: '全部',
              fixed: true,
              showIndex: 2,
              placeholder: '搜索项目',
              type: 'input',
            },
          ],
          values: {},
        },
        operations: {
          filter: {
            key: 'filter',
            reload: true,
          },
        },
      },
      myProjectList: {
        type: 'List',
        props: {
          visible: false,
          isLoadMore: true,
          alignCenter: true,
          noBorder: true,
          size: 'small',
        },
        data: {
          list: [
            {
              id: '1',
              porjectId: '13',
              title: '测试1测试1测试1测试1',
              titleSize: 'text-base',
              prefixImg: 'https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png',
              prefixImgSize: 'middle',
              prefixImgCircle: true,
              operations: {
                click: {
                  key: 'click',
                  show: false,
                  reload: false,
                  command: {
                    key: 'goto',
                    target: 'projectAllIssue',
                    state: {
                      params: {
                        projectId: '13',
                      },
                    },
                  },
                },
              },
            },
            {
              id: '2',
              porjectId: '13',
              title: '测试2',
              titleSize: 'text-base',
              prefixImg: 'https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png',
              prefixImgSize: 'middle',
              prefixImgCircle: true,
              operations: {
                click: {
                  key: 'click',
                  show: false,
                  reload: false,
                  command: {
                    key: 'goto',
                    target: 'projectAllIssue',
                    state: {
                      params: {
                        projectId: '13',
                      },
                    },
                  },
                },
              },
            },
          ],
        },
        operations: {
          changePageNo: {
            key: 'changePageNo',
            reload: true,
            fillMeta: 'pageNo',
          },
        },
        state: {
          pageNo: 1,
          pageSize: 5,
          total: 5,
        },
      },
      myApplication: {
        type: 'Container',
        props: {
          visible: false,
        },
      },
      myApplicationTitle: {
        type: 'Title',
        props: {
          visible: true,
          title: '应用',
          level: 1,
          noMarginBottom: true,
          showDivider: true,
        },
      },
      myApplicationFilter: {
        type: 'ContractiveFilter',
        props: {
          delay: 1000,
          visible: true,
          fullWidth: true,
        },
        state: {
          conditions: [
            {
              key: 'title',
              label: '标题',
              emptyText: '全部',
              fixed: true,
              showIndex: 2,
              placeholder: '搜索应用',
              type: 'input',
            },
          ],
          values: {},
        },
        operations: {
          filter: {
            key: 'filter',
            reload: true,
          },
        },
      },
      myApplicationList: {
        type: 'List',
        props: {
          visible: false,
          isLoadMore: true,
          alignCenter: true,
          noBorder: true,
          size: 'small',
        },
        data: {
          list: [
            {
              id: '1',
              title: '测试1测试1测试1测试1',
              prefixImgSize: 'middle',
              prefixImgCircle: true,
              prefixImg: '/images/default-app-icon.svg',
              operations: {
                click: {
                  key: 'click',
                  show: false,
                  reload: false,
                  command: {
                    key: 'goto',
                    target: 'app',
                    state: {
                      params: {
                        projectId: '9',
                        appId: '8',
                      },
                    },
                  },
                },
              },
            },
            {
              id: '2',
              title: '测试2',
              prefixImg: 'https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png',
              prefixImgSize: 'middle',
              prefixImgCircle: true,
              operations: {
                click: {
                  key: 'click',
                  show: false,
                  reload: false,
                  command: {
                    key: 'goto',
                    target: 'app',
                    state: {
                      params: {
                        projectId: '9',
                        appId: '8',
                      },
                    },
                  },
                },
              },
            },
          ],
        },
        operations: {
          changePageNo: {
            key: 'changePageNo',
            reload: true,
            fillMeta: 'pageNo',
          },
        },
        state: {
          pageNo: 1,
          pageSize: 5,
          total: 5,
        },
      },
      emptyApplication: {
        type: 'EmptyHolder',
        props: {
          tip: '未加入任何应用',
          visible: true,
          relative: true,
        },
      },
    },
  },
};

export const mockContent: CONFIG_PAGE.RenderConfig = {
  scenario: {
    scenarioKey: 'home-page-content',
    scenarioType: 'home-page-content', // 后端定义
  },
  protocol: {
    hierarchy: {
      root: 'page',
      structure: {
        page: ['content'],
        content: ['title', 'emptyOrgTip', 'emptyPublicOrgTip', 'emptyProjectTip', 'emptyProjectIssue', 'tableGroup'],
        emptyOrgTip: { left: 'erdaLogo', right: 'emptyOrgText' },
        emptyOrgText: ['emptyOrgTitle', 'emptyOrgContent'],
        emptyPublicOrgTip: { left: 'orgLogo', right: 'emptyPublicOrgText' },
        emptyPublicOrgText: ['emptyPublicOrgTitle', 'emptyPublicOrgContent'],
        emptyProjectTip: { left: 'orgLogo', right: 'emptyProjectText' },
        emptyProjectText: ['emptyProjectTitle', 'emptyProjectContent'],
      },
    },
    components: {
      page: { type: 'Container' },
      title: {
        type: 'Title',
        props: {
          visible: true,
          title: '事件',
          level: 1,
          noMarginBottom: true,
          size: 'big',
        },
      },
      emptyOrgTip: {
        type: 'LRContainer',
        props: {
          visible: true,
          whiteBg: true,
          startAlign: true,
          contentSetting: 'start',
        },
      },
      erdaLogo: {
        type: 'Image',
        props: {
          src: '/images/favicon.ico',
          visible: true,
          isCircle: true,
          size: 'small',
          type: 'erda',
        },
      },
      emptyOrgText: {
        type: 'Container',
        props: {
          visible: true,
        },
      },
      emptyOrgTitle: {
        type: 'Title',
        props: {
          visible: true,
          title: '你已经是 Erda Cloud 组织的成员',
          level: 2,
        },
      },
      emptyOrgContent: {
        type: 'TextGroup',
        props: {
          visible: true,
          value: [
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '以下是作为平台新成员的一些快速入门知识：',
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'linkText',
                title: '创建组织',
                visible: true,
                gapSize: 'small',
                value: {
                  text: [
                    { text: '通过左侧' },
                    {
                      text: '创建组织',
                      withTag: true,
                      tagStyle: {
                        backgroundColor: '#6A549E',
                        color: '#ffffff',
                        margin: '0 12px',
                        padding: '4px 15px',
                        borderRadius: '3px',
                      },
                    },
                    { text: '可以快速创建自己的组织' },
                  ],
                },
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'linkText',
                visible: true,
                gapSize: 'small',
                title: '浏览公开组织',
                value: {
                  text: '通过左上角的浏览公开组织信息，选择公开组织可以直接进入浏览该组织公开项目的信息可（包含项目管理、应用运行信息等）',
                },
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'linkText',
                visible: true,
                gapSize: 'small',
                title: '加入组织',
                value: {
                  text: '组织当前都是受邀机制，需要线下联系企业所有者进行邀请加入',
                },
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '当你已经加入到任何组织后，此框将不再显示',
                textStyleName: {
                  'text-xs': true,
                  'color-text-desc': true,
                },
              },
              gapSize: 'normal',
            },
          ],
        },
        operations: {
          toSpecificProject: {
            command: {
              key: 'goto',
              target: 'projectAllIssue',
              jumpOut: true,
              state: {
                query: {
                  issueViewGroup__urlQuery:
                    'eyJ2YWx1ZSI6ImthbmJhbiIsImNoaWxkcmVuVmFsdWUiOnsia2FuYmFuIjoiZGVhZGxpbmUifX0=',
                },
                params: {
                  projectId: '13',
                },
              },
              visible: false,
            },
            key: 'click',
            reload: false,
            show: false,
          },
        },
      },
      emptyPublicOrgTip: {
        type: 'LRContainer',
        props: {
          visible: false,
          whiteBg: true,
          startAlign: true,
          contentSetting: 'start',
        },
      },
      emptyPublicOrgText: {
        type: 'Container',
        props: {
          visible: false,
        },
      },
      emptyPublicOrgTitle: {
        type: 'Title',
        props: {
          visible: true,
          title: '你当前正在浏览公开组织 XXXX',
          level: 2,
        },
      },
      emptyPublicOrgContent: {
        type: 'TextGroup',
        props: {
          visible: true,
          value: [
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '以下是作为平台新成员的一些快速入门知识：',
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '* 切换组织',
                styleConfig: {
                  bold: true,
                },
              },
              gapSize: 'small',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '使用此屏幕上左上角的组织切换，快速进行组织之间切换',
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '* 公开项目浏览',
                styleConfig: {
                  bold: true,
                },
              },
              gapSize: 'small',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '可以在左侧项目下公开项目信息进行浏览',
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '* 加入组织',
                styleConfig: {
                  bold: true,
                },
              },
              gapSize: 'small',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '组织当前都是受邀机制，需要线下联系企业所有者进行邀请加入',
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '当你已经加入到该公开组织后，此框将不再显示',
                textStyleName: {
                  'text-xs': true,
                  'color-text-desc': true,
                },
              },
              gapSize: 'normal',
            },
          ],
        },
      },
      emptyProjectTip: {
        type: 'LRContainer',
        props: {
          visible: false,
          whiteBg: true,
          startAlign: true,
          contentSetting: 'start',
        },
      },
      orgLogo: {
        type: 'Image',
        props: {
          src: '/images/resources/org.png',
          visible: true,
          isCircle: true,
          size: 'small',
        },
      },
      emptyProjectText: {
        type: 'Container',
        props: {
          visible: false,
        },
      },
      emptyProjectTitle: {
        type: 'Title',
        props: {
          visible: true,
          title: '你已经是 XXX 组织的成员',
          level: 2,
        },
      },
      emptyProjectContent: {
        type: 'TextGroup',
        props: {
          visible: false,
          value: [
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '以下是作为组织新成员的一些快速入门知识：',
              },
              gapSize: 'normal',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '* 切换组织',
                styleConfig: {
                  bold: true,
                },
              },
              gapSize: 'small',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '使用此屏幕上左上角的组织切换，快速进行组织之间切换',
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '* 公开组织浏览',
                styleConfig: {
                  bold: true,
                },
              },
              gapSize: 'small',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '可以通过切换组织下拉菜单中选择公开组织进行浏览',
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '* 加入项目',
                styleConfig: {
                  bold: true,
                },
              },
              gapSize: 'small',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '当前都是受邀机制，需要线下联系项目管理员进行邀请加入',
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '* 该组织内公开项目浏览',
                styleConfig: {
                  bold: true,
                },
              },
              gapSize: 'small',
            },
            {
              props: {
                renderType: 'linkText',
                visible: true,
                value: {
                  text: [
                    '点击左上角菜单',
                    {
                      icon: 'application-menu',
                      iconStyleName: 'primary-icon',
                    },
                    '选择 DevOps平台进入，选择我的项目可以查看该组织下公开项目的信息',
                  ],
                },
              },
              gapSize: 'large',
            },
            {
              props: {
                renderType: 'text',
                visible: true,
                value: '当你已经加入到任何项目后，此框将不再显示',
                textStyleName: {
                  'text-xs': true,
                  'color-text-desc': true,
                },
              },
              gapSize: 'normal',
            },
          ],
        },
      },
      emptyProjectIssue: {
        type: 'EmptyHolder',
        props: {
          tip: '已加入的项目中，无待完成事项',
          visible: false,
          relative: true,
          whiteBg: true,
          paddingY: true,
        },
      },
      content: {
        type: 'Container',
        props: {
          visible: true,
        },
      },
      tableGroup: {
        type: 'TableGroup',
        props: {
          visible: false,
        },
        operations: {
          changePageNo: {
            key: 'changePageNo',
            reload: true,
            fillMeta: 'pageNo',
          },
        },
        data: {
          list: [
            {
              title: {
                props: {
                  renderType: 'linkText',
                  value: {
                    text: [
                      {
                        image:
                          'https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=3314214233,3432671412&fm=26&gp=0.jpg',
                      },
                      {
                        text: 'Erda',
                        operationKey: 'toSpecificProject',
                        styleConfig: {
                          color: 'black',
                          fontSize: '16px',
                        },
                      },
                    ],
                    isPureText: false,
                  },
                },
                operations: {
                  toSpecificProject: {
                    command: {
                      key: 'goto',
                      target: 'projectAllIssue',
                      jumpOut: true,
                      state: {
                        query: {
                          issueViewGroup__urlQuery:
                            'eyJ2YWx1ZSI6ImthbmJhbiIsImNoaWxkcmVuVmFsdWUiOnsia2FuYmFuIjoiZGVhZGxpbmUifX0=',
                        },
                        params: {
                          projectId: '13',
                          orgName: 'terminus',
                        },
                      },
                    },
                    key: 'click',
                    reload: false,
                    show: false,
                  },
                },
              },
              subtitle: {
                title: '你未完成的事项',
                level: 3,
                size: 'small',
              },
              description: {
                renderType: 'linkText',
                visible: true,
                value: {
                  text: [
                    '当前你还有',
                    {
                      text: ' 120 ',
                      styleConfig: {
                        bold: true,
                      },
                    },
                    '个事项待完成，已过期:',
                    {
                      text: ' 40 ',
                      styleConfig: {
                        bold: true,
                      },
                    },
                    '，本日到期:',
                    {
                      text: ' 40 ',
                      styleConfig: {
                        bold: true,
                      },
                    },
                    '，7日内到期:',
                    {
                      text: ' 36 ',
                      styleConfig: {
                        bold: true,
                      },
                    },
                    '，30日内到期:',
                    {
                      text: ' 44 ',
                      styleConfig: {
                        bold: true,
                      },
                    },
                  ],
                },
                textStyleName: { 'color-text-light-desc': true },
              },
              table: {
                props: {
                  rowKey: 'key',
                  columns: [
                    { title: '', dataIndex: 'name', width: 600 },
                    { title: '', dataIndex: 'planFinishedAt' },
                  ],
                  showHeader: false,
                  pagination: false,
                  size: 'small',
                  styleNames: {
                    'no-border': true,
                  },
                },
                data: {
                  list: [
                    {
                      id: '153',
                      projectId: '13',
                      type: 'requirement',
                      name: {
                        renderType: 'textWithIcon',
                        prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                        value: '222运行速度没得说，完全不卡，打游戏体验极佳',
                        hoverActive: 'hover-active',
                      },
                      planFinishedAt: '2022-03-02',
                      orgName: 'terminus',
                    },
                    {
                      id: '150',
                      projectId: '13',
                      type: 'requirement',
                      name: {
                        renderType: 'textWithIcon',
                        prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                        value: '111运行速度没得说，完全不卡，打游戏体验极佳',
                        hoverActive: 'hover-active',
                      },
                      planFinishedAt: '2022-03-02',
                      orgName: 'terminus',
                    },
                    {
                      id: '153',
                      projectId: '13',
                      type: 'requirement',
                      name: {
                        renderType: 'textWithIcon',
                        prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                        value: '111运行速度没得说，完全不卡，打游戏体验极佳',
                        hoverActive: 'hover-active',
                      },
                      planFinishedAt: '2022-03-02',
                      orgName: 'terminus',
                    },
                    {
                      id: '150',
                      projectId: '13',
                      type: 'requirement',
                      name: {
                        renderType: 'textWithIcon',
                        prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                        value: '111运行速度没得说，完全不卡，打游戏体验极佳',
                        hoverActive: 'hover-active',
                      },
                      planFinishedAt: '2022-03-02',
                      orgName: 'terminus',
                    },
                    {
                      id: '153',
                      projectId: '13',
                      type: 'requirement',
                      name: {
                        renderType: 'textWithIcon',
                        prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                        value: '111运行速度没得说，完全不卡，打游戏体验极佳',
                        hoverActive: 'hover-active',
                      },
                      planFinishedAt: '2022-03-02',
                      orgName: 'terminus',
                    },
                  ],
                },
                operations: {
                  clickRow: {
                    key: 'clickRow',
                    reload: false,
                    command: {
                      key: 'goto',
                      target: 'projectIssueDetail',
                      jumpOut: true,
                    },
                  },
                },
              },
              extraInfo: {
                props: {
                  renderType: 'linkText',
                  value: {
                    text: [{ text: '查看剩余112条事件', operationKey: 'toSpecificProject', icon: 'double-right' }],
                  },
                },
                operations: {
                  toSpecificProject: {
                    command: {
                      key: 'goto',
                      target: 'projectAllIssue',
                      jumpOut: true,
                      state: {
                        query: {
                          issueViewGroup__urlQuery:
                            'eyJ2YWx1ZSI6ImthbmJhbiIsImNoaWxkcmVuVmFsdWUiOnsia2FuYmFuIjoiZGVhZGxpbmUifX0=',
                        },
                        params: {
                          projectId: '13',
                          orgName: 'terminus',
                        },
                      },
                      visible: false,
                    },
                    key: 'click',
                    reload: false,
                    show: false,
                  },
                },
              },
            },
            {
              title: {
                props: {
                  renderType: 'linkText',
                  value: {
                    text: [
                      {
                        image:
                          'https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=3314214233,3432671412&fm=26&gp=0.jpg',
                      },
                      {
                        text: 'Erda',
                        operationKey: 'toSpecificProject',
                        styleConfig: {
                          bold: true,
                          color: 'black',
                          fontSize: '16px',
                        },
                      },
                    ],
                    isPureText: false,
                  },
                },
                operations: {
                  toSpecificProject: {
                    command: {
                      key: 'goto',
                      target: 'projectAllIssue',
                      jumpOut: true,
                      state: {
                        query: {
                          issueViewGroup__urlQuery:
                            'eyJ2YWx1ZSI6ImthbmJhbiIsImNoaWxkcmVuVmFsdWUiOnsia2FuYmFuIjoiZGVhZGxpbmUifX0=',
                        },
                        params: {
                          projectId: '13',
                          orgName: 'terminus',
                        },
                      },
                    },
                    key: 'click',
                    reload: false,
                    show: false,
                  },
                },
              },
              subtitle: {
                title: '你未完成的事项',
                level: 3,
                size: 'small',
              },
              description: {
                renderType: 'linkText',
                visible: true,
                value: {
                  text: [
                    '当前你还有',
                    {
                      text: ' 120 ',
                      styleConfig: {
                        bold: true,
                        fontSize: '16px',
                      },
                    },
                    '个事项待完成，已过期:',
                    {
                      text: ' 40 ',
                      styleConfig: {
                        bold: true,
                        fontSize: '16px',
                      },
                    },
                    '，本日到期:',
                    {
                      text: ' 40 ',
                      styleConfig: {
                        bold: true,
                        fontSize: '16px',
                      },
                    },
                    '，7日内到期:',
                    {
                      text: ' 36 ',
                      styleConfig: {
                        bold: true,
                        fontSize: '16px',
                      },
                    },
                    '，30日内到期:',
                    {
                      text: ' 44 ',
                      styleConfig: {
                        bold: true,
                        fontSize: '16px',
                      },
                    },
                  ],
                },
                textStyleName: { 'color-text-light-desc': true },
              },
              table: {
                props: {
                  rowKey: 'key',
                  columns: [
                    { title: '', dataIndex: 'name', width: 600 },
                    { title: '', dataIndex: 'planFinishedAt' },
                  ],
                  showHeader: false,
                  pagination: false,
                  styleNames: {
                    'no-border': true,
                  },
                },
                data: {
                  list: [
                    {
                      id: '153',
                      projectId: '13',
                      type: 'requirement',
                      name: {
                        renderType: 'textWithIcon',
                        prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                        value: '222运行速度没得说，完全不卡，打游戏体验极佳',
                        hoverActive: 'hover-active',
                      },
                      planFinishedAt: '2022-03-02',
                    },
                    {
                      id: '150',
                      projectId: '13',
                      type: 'requirement',
                      name: {
                        renderType: 'textWithIcon',
                        prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                        value: '111运行速度没得说，完全不卡，打游戏体验极佳',
                        hoverActive: 'hover-active',
                      },
                      planFinishedAt: '2022-03-02',
                    },
                    {
                      id: '153',
                      projectId: '13',
                      type: 'requirement',
                      name: {
                        renderType: 'textWithIcon',
                        prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                        value: '111运行速度没得说，完全不卡，打游戏体验极佳',
                        hoverActive: 'hover-active',
                      },
                      planFinishedAt: '2022-03-02',
                    },
                    {
                      id: '150',
                      projectId: '13',
                      type: 'requirement',
                      name: {
                        renderType: 'textWithIcon',
                        prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                        value: '111运行速度没得说，完全不卡，打游戏体验极佳',
                        hoverActive: 'hover-active',
                      },
                      planFinishedAt: '2022-03-02',
                    },
                    {
                      id: '153',
                      projectId: '13',
                      type: 'requirement',
                      name: {
                        renderType: 'textWithIcon',
                        prefixIcon: 'ISSUE_ICON.issue.REQUIREMENT',
                        value: '111运行速度没得说，完全不卡，打游戏体验极佳',
                        hoverActive: 'hover-active',
                      },
                      planFinishedAt: '2022-03-02',
                    },
                  ],
                },
                operations: {
                  clickRow: {
                    key: 'clickRow',
                    reload: false,
                    command: {
                      key: 'goto',
                      target: 'projectIssueDetail',
                      jumpOut: true,
                    },
                  },
                },
              },
              extraInfo: {
                props: {
                  renderType: 'linkText',
                  value: {
                    text: [{ text: '查看剩余112条事件', operationKey: 'toSpecificProject', icon: 'double-right' }],
                  },
                },
                operations: {
                  toSpecificProject: {
                    command: {
                      key: 'goto',
                      target: 'projectAllIssue',
                      jumpOut: true,
                      state: {
                        query: {
                          issueViewGroup__urlQuery:
                            'eyJ2YWx1ZSI6ImthbmJhbiIsImNoaWxkcmVuVmFsdWUiOnsia2FuYmFuIjoiZGVhZGxpbmUifX0=',
                        },
                        params: {
                          projectId: '13',
                        },
                      },
                      visible: false,
                    },
                    key: 'click',
                    reload: false,
                    show: false,
                  },
                },
              },
            },
          ],
        },
        state: {
          pageNo: 1,
          pageSize: 1,
          total: 5,
        },
      },
    },
  },
};
