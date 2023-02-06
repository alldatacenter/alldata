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

declare namespace CP_FORM {
  interface Spec {
    type: 'Form';
    operations: Obj<CP_COMMON.Operation>;
    props: IProps;
    state: IState;
  }

  interface IState {
    formData: Obj | undefined;
  }

  interface IProps {
    width?: number;
    name?: string;
    title?: string;
    visible?: boolean;
    fields: CP_COMMON.FormField[];
    formData?: Obj;
    readOnly?: boolean;
  }

  type Props = MakeProps<Spec> & {
    props: {
      formRef?: any;
      modalProps?: Obj;
      onCancel?(): void;
      onFailed?(res?: object, isEdit?: boolean): void;
      onFieldsChange?(v: Obj): void;
      onOk?(result: object, isEdit: boolean): Promise<any> | void;
    };
  };
}
