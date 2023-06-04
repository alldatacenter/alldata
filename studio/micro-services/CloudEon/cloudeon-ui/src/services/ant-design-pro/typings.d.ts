// @ts-ignore
/* eslint-disable */

declare namespace API {
  type CurrentUser = {
    name?: string;
    avatar?: string;
    userid?: string;
    email?: string;
    signature?: string;
    title?: string;
    group?: string;
    tags?: { key?: string; label?: string }[];
    notifyCount?: number;
    unreadCount?: number;
    country?: string;
    access?: string;
    geographic?: {
      province?: { label?: string; key?: string };
      city?: { label?: string; key?: string };
    };
    address?: string;
    phone?: string;
  };

  type LoginResult = {
    status?: string;
    type?: string;
    currentAuthority?: string;
  };

  type PageParams = {
    current?: number;
    pageSize?: number;
  };

  type RuleListItem = {
    key?: number;
    disabled?: boolean;
    href?: string;
    avatar?: string;
    name?: string;
    owner?: string;
    desc?: string;
    callNo?: number;
    status?: number;
    updatedAt?: string;
    createdAt?: string;
    progress?: number;
  };

  type RuleList = {
    data?: RuleListItem[];
    /** 列表的内容总数 */
    total?: number;
    success?: boolean;
  };

  type FakeCaptcha = {
    code?: number;
    status?: string;
  };

  type LoginParams = {
    username?: string;
    password?: string;
    autoLogin?: boolean;
    type?: string;
  };

  type ErrorResponse = {
    /** 业务约定的错误码 */
    errorCode: string;
    /** 业务上的错误信息 */
    errorMessage?: string;
    /** 业务上的请求是否成功 */
    success?: boolean;
  };

  type NoticeIconList = {
    data?: NoticeIconItem[];
    /** 列表的内容总数 */
    total?: number;
    success?: boolean;
  };

  type NoticeIconItemType = 'notification' | 'message' | 'event';

  type NoticeIconItem = {
    id?: string;
    extra?: string;
    key?: string;
    read?: boolean;
    avatar?: string;
    title?: string;
    status?: string;
    datetime?: string;
    description?: string;
    type?: NoticeIconItemType;
  };

  type ServiceItem = {
    label?: string;
    name?: string;
    id: number;
    version?: string;
    description?: string;
    dockerImage?: string;
    roles?: Array<string>;
  };

  type ServiceList = {
    success?: boolean;
    message?: string;
    data?: ServiceItem[];
  };

  type ColonyData = {
    clusterId?: number,
  }

  type ColonyItem = {
    serviceCnt?: ReactNode;
    nodeCnt?: ReactNode;
    id?: number,
    createBy?: string,
    createTime?: string,
    clusterName?: string,
    clusterCode?: string,
    stackId?: number,
    kubeConfig?: string
  }

  type ColonyList = {
    success?: boolean;
    message?: string;
    data?: ColonyItem[];
  };

  type NodeItem = {
    id?: number,
    createTime?: string,
    hostname?: string,
    ip?: string,
    rack?: string,
    coreNum?: number,
    totalMem?: number,
    totalDisk?: number,
    sshUser?: string,
    sshPort?: number,
    clusterId?: number,
    cpuArchitecture?: string,
    nodeLabel?: string,
    serviceRoleNum?: string
  }

  type NodeList = {
    success?: boolean;
    message?: string;
    data?: NodeItem[];
  };

  type StackItem = {
    id?: number,
    stackCode?: string
  }

  type StackList = {
    success?: boolean;
    message?: string;
    data?: StackItem[];
  };

  type ConfItem = {
    value: string;
    isCustomConf: any;
    name?: string,
    description?: string,
    label?: string,
    recommendExpression?: string,
    valueType?: string,
    configurableInWizard?: boolean,
    groups?: string[]
  }

  type ConfList = {
    success?: boolean;
    message?: string;
    data?: {
      confs?: ConfItem[],
      customFileNames?: string[],
      fileGroupMap?: object,
    };
  };

  type validRuleItem = {
    fixedNum?: null,
    minNum?:number,
    needOdd?:boolean,
    nodeIds?:number[],
    stackRoleName?:string
  }

  type RolesItem = {
    validRule?: validRuleItem;
    stackRoleName?: string,
    nodeIds?: number[]
  }

  type PresetConfListItem = {
    name?: string,
    value?: string,
    recommendedValue?: string
  }

  type ServiceInfosItem = {
    customConfList: any;
    stackServiceId?: number,
    stackServiceName?: string,
    stackServiceLabel?: string,
    roles?: RolesItem[],
    presetConfList?: PresetConfListItem[]
  }

  type SubmitServicesParams = {
    stackId?: number,
    enableKerberos?: boolean,
    clusterId?: number,
    serviceInfos?: ServiceInfosItem[]
  }

  type normalResult = {
    success?: boolean;
    message?: string;
    data?: any[];
  }

  type stringResult = {
    success?: boolean;
    message?: string;
    data?: string;
  }

  type numberResult = {
    success?: boolean;
    message?: string;
    data?: number;
  }

  type commandResult = {
    success?: boolean;
    message?: string;
    data?: commandType;
  }

  // type commandType = {
  //   serviceProgresses: any;
  //   id: number;
  //   name: string;
  //   type: string,
  //   commandState: string,
  //   submitTime: string,
  //   startTime: string,
  //   endTime: string,
  //   currentProgress: number,
  //   operateUserId: number,
  //   clusterId: number,
  //   // tasksMap:
  // }

  type taskDetailType = {
    id?: number,
    taskShowSortNum?: number,
    taskName?: string,
    taskParam?: string,
    processorClassName?: string,
    commandState?: string,
    taskLogPath?: string,
    startTime?: string,
    endTime?: string,
    commandTaskGroupId?: number,
    commandId?: number,
    serviceInstanceId?: number,
    serviceInstanceName?: string,
    progress?: number,
    powerjobInstanceId?: number
  }

  type progressItem = {
    currentState?: string,
    serviceInstanceName?: string,
    taskDetails?: taskDetailType[],
    successCnt: number,
    totalCnt: number
  }

  type commandType = {
    id?: number,
    name?: string,
    type?: string,
    commandState?: string,
    submitTime?: string,
    startTime?: string,
    endTime?: string,
    currentProgress?: number,
    operateUserId?: number,
    clusterId?: number,
    serviceProgresses?: progressItem[]
  }

  type logResult = {
    success?: boolean;
    message?: string;
    data?: string;
  }

  type serviceInfos = {
    name?: string,
    id?: number,
    dockerImage?: string,
    stackServiceName?: string,
    stackServiceId?: number,
    version?: string,
    stackServiceDesc?: string,
    serviceState?: string,
    serviceStateValue?: number,
  }

  type serviceInfosResult = {
    success?: boolean;
    message?: string;
    data?: serviceInfos;
  }

  type rolesInfos = {
    alertMsgName?: any;
    alertMsgCnt?: any;
    name?: string,
    id?: number,
    roleStatus?: string,
    nodeId?: number,
    nodeHostname?: string,
    nodeHostIp?: string,
    uiUrls?: string[]
  }

  type serviceRolesResult = {
    success?: boolean;
    message?: string;
    data?: rolesInfos[];
  }

  type rolesValid = {
    stackRoleName?: string,
    minNum?: number,
    fixedNum?: number,
    needOdd?: boolean,
    nodeIds?: number[]
  }

  type rolesValidResult = {
    success?: boolean;
    message?: string;
    data?: {[key:string]: rolesValid[]};
  }

  type nodeIpItem = {
    hostname?: string,
    ip?: string
  }

  type nodeIpListResult = {
    success?: boolean;
    message?: string;
    data?: nodeIpItem[];
  }

  type webUrlsItem = {
    hostnameUrl?: string,
    ipUrl?: string,
    name?: string
  }

  type webUrlsListResult = {
    success?: boolean;
    message?: string;
    data?: webUrlsItem[];
  }

  type alertItem = {
    alertName?: string,
    alertId?: number,
    createTime?: string,
    alertLevelMsg?: string,
    serviceInstanceId?: number,
    serviceRoleInstanceId?: number,
    serviceInstanceName?: string,
    serviceRoleLabel?: string,
    hostname?: string,
    info?: string,
    advice?: string
  }

  type alertListResult = {
    success?: boolean;
    message?: string;
    data?: alertItem[];
  }

  type alertRulesItem = {
    ruleName?: string,
    id?: number,
    alertLevel?: string,
    promql?: string,
    alertInfo?: string,
    alertAdvice?: string,
    stackServiceName?: string,
    stackRoleName?: string,
    updateTime?: string,
    createTime?: string,
  }

  type alertRulesListResult = {
    success?: boolean;
    message?: string;
    data?: alertRulesItem[];
  }

  type anyResult = {
    success?: boolean;
    message?: string;
    data?: any;
  }

}
