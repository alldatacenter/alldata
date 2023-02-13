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

declare namespace CP_TEXT {
  type IRenderType = 'linkText' | 'text' | 'statusText' | 'copyText' | 'textWithIcon' | 'multipleText' | 'bgProgress';
  interface Spec {
    type: 'Text';
    props: IProps;
    operations?: Obj<CP_COMMON.Operation>;
  }

  interface IProps {
    renderType: IRenderType;
    value: ILinkTextData | string | IStatusText | ICopyText;
    visible?: boolean;
    styleConfig?: IStyleConfig;
    textStyleName?: Obj;
    title?: string;
    titleLevel?: number;
  }

  interface IStatusTextItem {
    text: string;
    status: 'default' | 'success' | 'processing' | 'error';
  }
  type IStatusText = IStatusTextItem | IStatusTextItem[];

  interface ICopyText {
    text: string;
    copyText: string;
  }

  interface IBgProgress {
    text: string;
    percent: number;
  }

  interface IStyleConfig {
    [pro: string]: any;
    bold?: boolean; // 是否加粗,
    lineHeight?: number;
    fontSize?: number;
  }

  interface ILinkTextData {
    text: Array<ILinkTarget | string> | ILinkTarget | string;
    direction?: 'row' | 'column';
  }

  interface ILinkTarget {
    icon?: string;
    iconTip?: string;
    image?: string;
    iconStyleName?: string;
    text: string;
    operationKey: string;
    styleConfig?: IStyleConfig;
    withTag?: boolean;
    tagStyle?: import('react').CSSProperties;
  }

  type Props = MakeProps<Spec>;
}
