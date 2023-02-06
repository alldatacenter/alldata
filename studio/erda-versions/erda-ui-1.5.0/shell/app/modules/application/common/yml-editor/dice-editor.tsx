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

// import React from 'react';
import { IYmlEditorProps } from './index';
// import i18n from 'i18n';
// // @ts-ignore
// import yaml from 'js-yaml';
// import { map, get, omit, isEmpty, cloneDeep, difference, flatten } from 'lodash';
// import { Spin, Button, message } from 'antd';
// import { RenderForm } from 'common';
// import repoStore from 'application/stores/repo';
// import { getInfoFromRefName } from 'application/pages/repo/util';
// import FileContainer from 'application/common/components/file-container';
// import { NodeType, externalKey, CHART_CONFIG, CHART_NODE_SIZE, FileType } from './config';
// import YmlChart from './yml-chart';
// import { isPipelineYml } from './utils';
// import { useMount } from 'react-use';
// import PipelineNodeDrawer from './pipeline-node-drawer';
// import { notify } from 'common/utils';
// import loadingStore from 'core/stores/loading';

// import './index.scss';

// interface IData {
//   alias: string;
//   [externalKey]: any;
// }

// const defaultJson = {
//   services: {},
//   envs: {},
//   addons: {},
//   version: '1.0',
// };
// const emptyObj = {};

// /**
//    * 根据文件类型初始化默认值
//    */
// export const initYmlDefaultFields = (fileType: string, data?: any) => {
//   const initContent: any = data || {};

//   if (fileType === FileType.dice) {
//     if (!initContent.services) {
//       initContent.services = {};
//     }
//     if (!initContent.addons) {
//       initContent.addons = {};
//     }
//     if (!initContent.envs) {
//       initContent.envs = {};
//     }
//   } else {
//     if (!initContent.stages) {
//       initContent.stages = [];
//     }
//     if (!initContent.resources) {
//       initContent.resources = [];
//     }
//     if (!initContent.envs) {
//       initContent.envs = {};
//     }

//     if (!initContent.version) {
//       initContent.version = '1.0';
//     }
//   }
//   return initContent;
// };

// const getItemFromYamlJson = (key: string, jsonContent: any) => {
//   let currentItem = {};
//   if (key.includes('|')) {
//     const splits = key.split('|');
//     splits.forEach((splitKey: string) => {
//       if (jsonContent[splitKey]) {
//         currentItem = jsonContent[splitKey];
//       }
//     });
//   } else {
//     currentItem = jsonContent[key];
//   }
//   return currentItem;
// };

// // const convertDiceYmlData = (jsonContent: Obj) => {
// //   const result: any[] = [];
// //   const addons: any[] = [];
// //   const order = ['envs', 'services', 'addons'];
// //   if (!isEmpty(jsonContent)) {
// //     map(order, (key: string, index: number) => {
// //       const currentItem: any = getItemFromYamlJson(key, jsonContent);
// //       if (index === 0) {
// //         // @ts-ignore
// //         item = {
// //           icon: 'qj',
// //           title: i18n.t('dop:deploy global variables'),
// //           lineTo: ['all'],
// //           data: currentItem,
// //           allowMove: false,
// //           allowDelete: false,
// //           content: () => {
// //             return <PropertyView dataSource={currentItem} />;
// //           },
// //           editView: (editing: boolean) => {
// //             return (
// //               <EditGlobalVariable
// //                 editing={editing}
// //                 globalVariable={currentItem}
// //                 onSubmit={editGlobalVariable}
// //               />
// //             );
// //           },
// //         };
// //         result.push([item]);
// //       } else if (index === 1) {
// //         const groups: any[] = dependsOnDataFilter(currentItem);
// //         groups.forEach((services: any[]) => {
// //           const group: any[] = [];
// //           services.forEach((service: any) => {
// //             // @ts-ignore
// //             group.push({
// //               icon: 'wfw',
// //               title: i18n.t('microService'),
// //               data: currentItem,
// //               name: service.name,
// //               lineTo: service.depends_on,
// //               editView: (editing: boolean) => {
// //                 return (
// //                   <EditService
// //                     jsonContent={jsonContent}
// //                     editing={editing}
// //                     service={service}
// //                     onSubmit={editService}
// //                   />
// //                 );
// //               },
// //               content: () => {
// //                 return <DiceServiceView dataSource={service} />;
// //               },
// //             });
// //           });

// //           result.push(group);
// //         });
// //       } else if (index === 2) {
// //         forEach(currentItem, (value: any, k: string) => {
// //           const addon = { ...value, name: k };
// //           addons.push({
// //             id: `addon-${k}`,
// //             icon: 'addon1',
// //             data: addon,
// //             name: k,
// //             lineTo: [],
// //           });
// //         });
// //       }
// //     });
// //   }
// //   return {
// //     addons,
// //     editData: result,
// //   };
// // };

const DiceEditor = (props: IYmlEditorProps) => {
  //   const { fileName, ops, editing, content } = props;
  //   const [info, tree, propsGroupedAddonList] = repoStore.useStore(s => [s.info, s.tree, s.groupedAddonList]);
  //   const { changeMode } = repoStore.reducers;
  //   const { getAvailableAddonList } = repoStore.effects;
  //   const formRef: any = React.useRef(null);
  //   const form = formRef.current;
  //   const [{ originJsonContent, jsonContent, groupedAddonList, displayData, editData, dataKey, chosenNode, isCreate, drawerVis }, updater, update] = useUpdate({
  //     originJsonContent: defaultJson,
  //     jsonContent: defaultJson,
  //     groupedAddonList: [],
  //     displayData: resetData([], editing) as IData[][],
  //     editData: [],
  //     dataKey: 1,
  //     chosenNode: null as null | IStageTask,
  //     isCreate: false,
  //     drawerVis: false,
  //   });

  //   useMount(() => {
  //     updater.groupedAddonList(propsGroupedAddonList);
  //     getAvailableAddonList();
  //   });

  //   React.useEffect(() => {
  //     try {
  //       let loadedContent = yaml.load(content);
  //       loadedContent = initYmlDefaultFields(loadedContent === 'undefined' ? emptyObj : loadedContent);
  //       updater.originJsonContent(loadedContent);
  //     } catch (e) {
  //       notify('error', `${i18n.t('dop:yml format error')}：${e.message}`);
  //       updater.jsonContent(defaultJson);
  //     }
  //   }, [content, updater]);

  //   React.useEffect(() => {
  //     updater.groupedAddonList(propsGroupedAddonList);
  //   }, [propsGroupedAddonList, updater]);

  //   React.useEffect(() => {
  //     updater.jsonContent(originJsonContent);
  //   }, [originJsonContent, updater]);

  //   React.useEffect(() => {
  //     update({
  //       displayData: resetData(editData, editing),
  //       dataKey: dataKey + 1,
  //     });
  //   // eslint-disable-next-line react-hooks/exhaustive-deps
  //   }, [editData, editing, update]);

  //   const onClickNode = (nodeData: any) => {
  //   };

  //   const onDeleteNode = (nodeData: any) => {
  //   };

  //   const closeDrawer = () => {
  //     update({
  //       chosenNode: null,
  //       drawerVis: false,
  //     });
  //   };

  //   const commitData = (commitContent: string, values: any) => {

  //   };

  //   const handleSubmit = (values: any) => {
  //   };

  //   const checkForm = () => {
  //     form.validateFields((err: any, values: any) => {
  //       if (err) return;
  //       handleSubmit(values);
  //       form.resetFields();
  //     });
  //   };

  //   const cancelEditing = () => {
  //     changeMode({ editFile: false, addFile: false });
  //   };

  //   const onSubmit = (formTaskData: any) => {
  //   };

  //   const renderSaveBtn = () => {
  //     if (!editing) return null;
  //     const getFieldsList = () => {
  //       const { branch } = getInfoFromRefName(tree.refName);
  //       const fieldsList = [
  //         {
  //           name: 'message',
  //           type: 'textArea',
  //           itemProps: {
  //             placeholder: i18n.t('dop:submit information'),
  //             autoSize: { minRows: 3, maxRows: 7 },
  //             maxLength: 200,
  //           },
  //           initialValue: `Update ${fileName}`,
  //         },
  //         {
  //           name: 'branch',
  //           type: 'select',
  //           initialValue: branch,
  //           options: (info.branches || []).map((a: any) => ({ name: a, value: a })),
  //           itemProps: {
  //             placeholder: i18n.t('dop:submit branch'),
  //             disabled: true,
  //           },
  //         },
  //       ];
  //       return fieldsList;
  //     };

  //     return (
  //       <>
  //         <RenderForm
  //           ref={formRef}
  //           className="commit-file-form"
  //           list={getFieldsList()}
  //         />
  //         <div className="p-4">
  //           <Button type="primary" className="mr-3" onClick={checkForm}>
  //             {i18n.t('save')}
  //           </Button>
  //           <Button onClick={cancelEditing}>
  //             {i18n.t('cancel')}
  //           </Button>
  //         </div>
  //       </>
  //     );
  //   };

  //   const chartConfig = {
  //     NODE: CHART_NODE_SIZE,
  //     MARGIN: editing ? CHART_CONFIG.MARGIN.EDITING : CHART_CONFIG.MARGIN.NORMAL,
  //   };

  //   const curNodeData = React.useMemo(() => {
  //     return chosenNode ? omit(chosenNode, externalKey) : chosenNode;
  //   }, [chosenNode]);
  //   const allAlias = map(flatten(get(curStructure, 'stages') || []), 'alias');
  //   const otherTaskAlias = drawerVis ? difference(allAlias, [get(chosenNode, 'alias')]) : [];

  //   return (
  //     <div>
  //       <FileContainer className="new-yml-editor flex flex-col justify-center full-spin-height" name={editing ? `${i18n.t('edit')} ${fileName}` : fileName} ops={ops}>
  //         <Spin spinning={loading.commit}>
  //           <YmlChart
  //             data={displayData}
  //             editing={editing}
  //             chartConfig={chartConfig}
  //             key={dataKey}
  //             onClickNode={onClickNode}
  //             onDeleteNode={onDeleteNode}
  //           />
  //           {renderSaveBtn()}
  //         </Spin>
  //       </FileContainer>
  //       <PipelineNodeDrawer
  //         visible={drawerVis}
  //         isCreate={isCreate}
  //         closeDrawer={closeDrawer}
  //         nodeData={curNodeData as IStageTask}
  //         editing={editing}
  //         otherTaskAlias={otherTaskAlias as string[]}
  //         onSubmit={onSubmit}
  //       />
  //     </div>
  //   );
  return null;
};

export default DiceEditor;

// // 非编辑状态下: 插入开始节点，后面数据不变
// // 编辑状态下：插入开始节点、层与层之间插入添加行节点
// const resetData = (data: any[][] = [], isEditing = false) => {
//   const reData = [
//     [{ [externalKey]: { nodeType: NodeType.startNode } }], // 插入开始节点
//   ] as any[][];
//   if (data.length === 0) {
//     isEditing && reData.push([{ [externalKey]: { nodeType: NodeType.addRow, insertPos: 0 } }]); // 中间追加添加行
//   } else if (isEditing) {
//     reData.push([{ [externalKey]: { nodeType: NodeType.addRow, insertX: 0 } }]); // 中间追加添加行
//     map(data, (item, index) => {
//       reData.push(map(item, (subItem, subIndex) => ({
//         ...subItem,
//         [externalKey]: { nodeType: 'pipeline', name: subItem.alias, xIndex: index, yIndex: subIndex },
//       })));
//       reData.push([{ [externalKey]: { nodeType: NodeType.addRow, insertPos: index + 1 } }]); // 末尾追加添加行
//     });
//   } else {
//     map(data, (item, index) => {
//       reData.push(map(item, (subItem, subIndex) => ({
//         ...subItem,
//         [externalKey]: { nodeType: 'pipeline', name: subItem.alias, xIndex: index, yIndex: subIndex },
//       })));
//     });
//   }
//   return reData;
// };
