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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import {
  get,
  set,
  cloneDeep,
  merge,
  map,
  isEmpty,
  isNumber,
  isString,
  isPlainObject,
  isArray,
  find,
  has,
  keys,
} from 'lodash';
import { isPromise, useMount } from './utils';
import { Submit, Reset } from './form-button';
import { Context } from './context';

export const componentMap = {} as any;
export const registComponent = ({ name, Component, ...rest }: any) => {
  componentMap[name] = Component(rest);
};
// registComponent('select', FormSelect);

export interface RegisterProps {
  fixIn: (v: any) => any;
  fixOut: (v: any) => any;
  requiredCheck: (v: any) => any;
}

// 1. 原始组件肯定要包装一下，调整value和onChange的参数，适配Form传入的配置
// 2. 注册的应该是包装后的组件，可以直接接受Form传来的配置
// 3. 这种注册写法，应该是对FormItem的一个封装
// registComponent({
//   name: "select",
//   Component: FormSelect, // 某React组件，props中必须有value、onChange
//   requiredCheck: value => {
//     // 必填校验时，特殊的校验规则
//     return [value !== undefined, "不能为空"];
//   },
//   fixOut: (value, options) => {
//     // 在获取表单数据时，将React组件的value格式化成需要的格式
//     return value;
//   },
//   fixIn: (value = "", options) => {
//     // 从schema到React组件映射时，修正传入React组件的value
//     return value;
//   },
//   extensionFix: (data, options) => {
//     // 从schema到React组件映射时，修正传入React组件的配置项
//     return data;
//   }
//   // event: { // 表单事件机制的eventName，所对应的React组件的事件名
//   //   eventName: {
//   //     handleName: 'onFocus',
//   //   },
//   // },
// });
// registComponent({
//   name: "input",
//   Component: FormInput, // 某React组件，props中必须有value、onChange
//   requiredCheck: value => {
//     // 必填校验时，特殊的校验规则
//     return [value !== undefined && value !== "", "不能为空"];
//   },
//   fixOut: (value, options) => {
//     // 在获取表单数据时，将React组件的value格式化成需要的格式
//     return value;
//   },
//   fixIn: (value, options) => {
//     // 从schema到React组件映射时，修正传入React组件的value
//     return value;
//   },
//   extensionFix: (data, options) => {
//     // 从schema到React组件映射时，修正传入React组件的配置项
//     return data;
//   }
// });

const noop = (a: any) => a;

// 中间层，接入antd或其他form，把field上的配置映射到组件的字段上
export const defaultRenderField =
  (compMap: any) =>
  ({ fields, form, ...rest }: any) => {
    return (
      <div>
        {fields.map((f: any, index: number) => {
          const Component = compMap[f.component];
          return <Component key={f.key || `${index}`} fieldConfig={f} form={form} {...rest} />;
        })}
      </div>
    );
  };

type Operator = '=' | '!=' | '>' | '>=' | '<' | '<=' | 'includes' | 'contains' | 'not_contains' | 'empty' | 'not_empty';
type CheckItem = XOR<
  {
    field: string;
    operator: Operator;
    value: string | number | boolean;
    valueType?: 'string' | 'number' | 'boolean';
  },
  {
    mode: Mode;
  }
>;
const genOrList = (originCheckList: CheckItem[][], mode: string, cb: (con: CheckItem) => void) => {
  return originCheckList.map((andList: CheckItem[]) => {
    const checkAndList = [] as Function[];
    andList.forEach((con: CheckItem) => {
      if (con.mode) {
        checkAndList.push(() => con.mode === mode);
        return;
      } else if (!con.field) {
        return;
      }

      const value =
        con.valueType === 'number' ? Number(con.value) : con.valueType === 'boolean' ? Boolean(con.value) : con.value;
      switch (con.operator) {
        case '=':
          checkAndList.push((data: any) => get(data, con.field) === value);
          break;
        case '!=':
          checkAndList.push((data: any) => get(data, con.field) !== value);
          break;
        case '>':
          checkAndList.push((data: any) => get(data, con.field) >= value);
          break;
        case '>=':
          checkAndList.push((data: any) => get(data, con.field) >= value);
          break;
        case '<':
          checkAndList.push((data: any) => get(data, con.field) < value);
          break;
        case '<=':
          checkAndList.push((data: any) => get(data, con.field) <= value);
          break;
        case 'includes':
          isArray(value) && checkAndList.push((data: any) => value.includes(get(data, con.field)));
          break;
        case 'contains':
          checkAndList.push((data: any) => get(data, con.field).includes(value));
          break;
        case 'not_contains':
          checkAndList.push((data: any) => !get(data, con.field).includes(value));
          break;
        case 'empty':
          checkAndList.push(() => value === undefined);
          break;
        case 'not_empty':
          checkAndList.push(() => value !== undefined);
          break;
        default:
          break;
      }
      cb(con);
    });
    return checkAndList;
  });
};

const genCheckFn = (orList: Function[][]) => {
  return (data: any) =>
    orList.reduce((res, andList) => {
      return andList.reduce((cur, fn) => fn(data) && cur, true) || res;
    }, false);
};

// { pattern: '', type: 'string | number | email', whitespace, enum, len, min: 1, max: 12, equal: 'string:sss' | 'number:123' | 'boolean:false', not_equal: '', msg: '' },
const rules = {
  pattern(v: any, rule: Rule, data: any) {
    if (rule.pattern) {
      const strArr = String(rule.pattern).trim().split('') as string[];
      const firstStr = strArr.shift();
      const lastStr = strArr.pop();
      let pattern = '';
      let attr;
      if (firstStr === '/') {
        pattern = strArr.join('');
      } else {
        pattern = `${firstStr}${strArr.join('')}`;
      }
      if (lastStr !== '/') {
        attr = lastStr;
        strArr.pop();
        pattern = strArr.join('');
      }
      let reg = null;
      try {
        reg = new RegExp(pattern, attr);
        return [reg.test(v), rule.msg || rule.message || 'not match pattern'];
      } catch (e) {
        return [false, '非法的正则表达式，请检查'];
      }
    }
    return [true, ''];
  },
  enum(v: any, rule: Rule, data: any) {
    if (rule.enum) {
      return [rule.enum.includes(v), rule.msg || 'not in enum'];
    }
    return [true, ''];
  },
  string(v: any, rule: Rule, data: any) {
    return [typeof v === 'string', rule.msg || 'not a string'];
  },
  number(v: any, rule: Rule, data: any) {
    return [typeof v === 'number', rule.msg || 'not a number'];
  },
  len(v: any, rule: Rule, data: any) {
    return [(v ? v.length : 0) === rule.len, rule.msg || `length is not:${rule.len}`];
  },
  min(v: any, rule: Rule, data: any) {
    if (rule.min) {
      return [(v ? v.length : 0) >= rule.min, rule.msg || `length is smaller than:${rule.min}`];
    }
    return [true, ''];
  },
  max(v: any, rule: Rule, data: any) {
    if (rule.max) {
      return [(v ? v.length : 0) <= rule.max, rule.msg || `length is bigger than:${rule.max}`];
    }
    return [true, ''];
  },
  equalWith(v: any, rule: Rule, data: any) {
    if (rule.equalWith) {
      const _v = get(data, rule.equalWith);
      return [v === _v, rule.msg || `not equal with:${_v}`];
    }
    return [true, ''];
  },
  // whitespace(v: any, rule: Rule, data: any) {
  //   return [true, ''];
  // },
  validator(v: any, rule: Rule, data: any) {
    if (typeof rule.validator === 'function') {
      return rule.validator(v, rule, data);
    }
    return [true, ''];
  },
};

export const getCheckListFromRule = (rule: Rule) => {
  const checkList = [];
  rule.pattern !== undefined && checkList.push(rules.pattern);
  rule.enum !== undefined && checkList.push(rules.enum);
  rule.string !== undefined && checkList.push(rules.string);
  rule.number !== undefined && checkList.push(rules.number);
  rule.len !== undefined && checkList.push(rules.len);
  rule.min !== undefined && checkList.push(rules.min);
  rule.max !== undefined && checkList.push(rules.max);
  rule.equalWith !== undefined && checkList.push(rules.equalWith);
  rule.validator !== undefined && checkList.push(rules.validator);
  return checkList;
};

const genValidateFn =
  (cb = noop) =>
  (item: InnerFormField, data: any) => {
    for (const rule of item.rules) {
      const checkList = getCheckListFromRule(rule);
      for (const check of checkList) {
        const v = get(data, item.key);
        let result = check(v, rule, data); // 返回 [status: 'success' | 'error', msg: '']
        if (!item.required && !isNumber(v) && isEmpty(v) && ['min', 'len'].includes(Object.keys(rule)[0])) {
          result = [true, ''];
        }
        if (isPromise(result)) {
          // @ts-ignore
          result.then(cb);
          return ['validating', '异步校验中...', result];
        } else if (result[0] === false) {
          return ['error', result[1]];
        } else if (typeof result[0] === 'string') {
          return result;
        }
      }
    }
    return ['success'];
  };

interface Rule {
  // (v: any, formData: any): ValidateResult | PromiseLike<ValidateResult>
  msg: string;
  pattern?: string;
  string?: boolean;
  number?: boolean;
  email?: boolean;
  phone?: boolean;
  whitespace?: boolean;
  enum?: any[];
  len?: number;
  min?: number;
  max?: number;
  equalWith?: string; // 和其他字段值一样，用于校验密码时
  validator?: (...v: any) => Promise<any>;
}

type validateTrigger = 'onChange' | 'onBlur';

export interface FormField {
  index: number; // 顺序编号，在动态变化时确保顺序
  label: string; // 标签
  key: string; // 字段名，唯一的key，支持嵌套
  type: string;
  component: string;
  group?: string;
  value?: any;
  labelTip?: string;
  defaultValue?: any; // 重置时不会清掉
  initialValue?: any; // 仅在mount后set，重置会清掉
  rules?: Rule[];
  required?: boolean;
  validateTrigger?: validateTrigger | validateTrigger[];
  componentProps?: {
    [k: string]: any;
  };
  wrapperProps?: {
    [k: string]: any;
  };
  clearWhen?: string[];
  hideWhen?: CheckItem[][];
  disableWhen?: CheckItem[][];
  removeWhen?: CheckItem[][];
  getComp?: (v: any) => React.ReactNode;
  fixData?: (v: any) => any;
}

type ValidateResult = [boolean, string, PromiseLike<ValidateResult>?];
interface InnerFormField extends FormField {
  visible: boolean;
  remove: boolean;
  disabled: boolean;
  valid: [boolean?, string?, PromiseLike<ValidateResult>?];
  rules: Rule[];
  isTouched: boolean;
  validateTrigger: validateTrigger | validateTrigger[];
  _hideWatchers: string[];
  _hideSubscribes: string[];
  _disabledWatchers: string[];
  _disabledSubscribes: string[];
  _removeWatchers: string[];
  _removeSubscribes: string[];
  converted: boolean;
  registerRequiredCheck: (a: any) => any;
  getData: () => any;
  validate: (item: InnerFormField, data: any) => ValidateResult;
  checkHide: (data: any) => boolean;
  checkDisabled: (data: any) => boolean;
  checkRemove: (data: any) => boolean;
}

type Mode = 'create' | 'edit';
interface FormProp {
  formRef: any;
  fields: FormField[];
  value?: {
    [k: string]: any;
  };
  children?: any;
  componentMap?: Obj;
  onFinish?: (val: any, isEdit: boolean) => void;
  onFinishFailed?: (err: any, val: any, isEdit: boolean) => void;
  renderField?: (arg: any) => any;
  onChange: (changeItem: Obj, vs: Obj) => any;
  formRender?: (arg: any) => any;
}
const empty = null;
export const Form = ({
  formRef: _ref,
  fields,
  value,
  onFinish = noop,
  onFinishFailed = noop,
  onChange = noop,
  children = empty,
  renderField = defaultRenderField,
  formRender,
  componentMap: propsComponentMap = componentMap,
}: FormProp) => {
  const [mode, setMode] = React.useState(Object.keys(value || {}).length ? 'edit' : 'create');
  const formDataRef = React.useRef(value || {});
  const defaultDataRef = React.useRef({});
  const fieldMapRef = React.useRef({});
  const [_fields, setFields] = React.useState([] as InnerFormField[]);
  const RenderFields = React.useMemo(() => renderField(propsComponentMap), [renderField]);

  React.useEffect(() => {
    const curMode = Object.keys(value || {}).length ? 'edit' : 'create';
    formDataRef.current = cloneDeep({ ...value });
    setMode(curMode);
    formRef.current.setFields(
      map([..._fields], (item) => ({ ...item, converted: false })),
      curMode,
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  const changeValue = (_val: object) => {
    if (!_val) return;
    setFields((prev) => {
      const fieldMap = {};
      prev.forEach((f) => {
        f.key && (fieldMap[f.key] = f);
      });
      const visibleChangeMap = {};
      const removeChangeMap = {};
      const disabledChangeMap = {};
      const newList = prev.map((item) => {
        if (has(_val, item.key)) {
          const copy = { ...item }; // 改变引用，触发重渲染
          copy.value = get(_val, copy.key);
          copy.isTouched = true;
          if (copy.validateTrigger.includes('onChange')) {
            copy.valid = copy.validate(copy, formDataRef.current);
          }
          // 检查依赖的field是否要移除或隐藏，需要的标记一下，本次循环无法更新，放到后面再循环一次时处理
          const { _removeSubscribes, _hideSubscribes, _disabledSubscribes } = copy;
          _removeSubscribes.forEach((sk) => {
            const sub = fieldMap[sk];
            const thisResult = sub.checkRemove(formDataRef.current);
            if (sub.remove !== thisResult) {
              removeChangeMap[sub.key] = thisResult;
            }
          });
          _hideSubscribes.forEach((sk) => {
            const sub = fieldMap[sk];
            const thisResult = !sub.checkHide(formDataRef.current);
            if (sub.visible !== thisResult) {
              visibleChangeMap[sub.key] = thisResult;
            }
          });
          _disabledSubscribes.forEach((sk) => {
            const sub = fieldMap[sk];
            const thisResult = sub.checkDisabled(formDataRef.current);
            if (sub.disabled !== thisResult) {
              disabledChangeMap[sub.key] = thisResult;
            }
          });
          return copy;
        }
        return item;
      });
      return newList.map((f) => {
        let newF = f;
        if (removeChangeMap[f.key] !== undefined) {
          newF = { ...f, remove: removeChangeMap[f.key] }; // 改变field的引用，触发重渲染
        }
        if (visibleChangeMap[f.key] !== undefined) {
          newF = { ...f, visible: visibleChangeMap[f.key] };
        }
        if (disabledChangeMap[f.key] !== undefined) {
          newF = { ...f, disabled: disabledChangeMap[f.key] };
        }
        return newF;
      });
    });
  };

  // 获取当前值改变后需要清除的field
  const getClearValue = (val: object = {}) => {
    const clearValues = {} as any;
    map(val, (v, k) => {
      const clearArr = get(find(fieldMapRef.current || [], { key: k }), 'clearWhen');
      map(clearArr || [], (item) => {
        if (val[item] === undefined && formDataRef.current[item] !== undefined) clearValues[item] = undefined;
      });
    });
    return clearValues;
  };

  const formRef = React.useRef({
    setFieldsValue: (_val?: object) => {
      const clearValues = getClearValue(_val) || {};
      const curChangeValue = { ...clearValues, ...(_val || {}) };
      map(curChangeValue, (v, k) => {
        set(formDataRef.current, k, v);
      });
      changeValue(_val || {});
    },
    setFieldValue: (k: string | object, v?: any) => {
      let curValue = {};
      if (k && isString(k)) {
        curValue = { [k]: v };
      } else if (isPlainObject(k)) {
        curValue = k;
      }
      const clearValues = getClearValue(curValue) || {};
      const curChangeValue = { ...clearValues, ...(curValue || {}) };
      map(curChangeValue, (cv, ck) => {
        set(formDataRef.current, ck, cv);
      });

      changeValue(curChangeValue);
      onChange(cloneDeep(formDataRef.current), curChangeValue);
    },
    setFieldValid: (k: string, v: any) => {
      setFields((prev) =>
        prev.map((item) => {
          return item.key === k ? { ...item, valid: v } : item;
        }),
      );
    },
    setFields: (arr: any[], _mode: string) => {
      const copyFields = cloneDeep(arr) as InnerFormField[];
      const _initialData = {};
      copyFields.forEach((f) => {
        if (f.defaultValue !== undefined && f.key) {
          set(defaultDataRef.current, f.key, f.defaultValue);
        }
        if (f.initialValue !== undefined && f.key) {
          set(_initialData, f.key, f.initialValue);
        }
      });

      formDataRef.current = merge({}, defaultDataRef.current, _initialData, formDataRef.current);
      // 解析hideWhen、removeWhen，调整_fields

      setFields(() => {
        const fieldMap = {};
        copyFields.forEach((f) => {
          if (!f.converted) {
            f._hideSubscribes = [];
            f._removeSubscribes = [];
            f._disabledSubscribes = [];
            f._hideWatchers = [];
            f._removeWatchers = [];
            f._disabledWatchers = [];
          }
          f.key && (fieldMap[f.key] = f);
        });
        return copyFields.map((item) => {
          // hideWhen/removeWhen/disableWhen需要放在converted之前，因为其他field若关联了converted的field，该部分值需要重置
          if (item.hideWhen) {
            item.checkHide = genCheckFn(
              genOrList(item.hideWhen, _mode, (con: any) => {
                item._hideWatchers.push(con.field); // 依赖于谁，做提示用
                !fieldMap[con.field]._hideSubscribes.includes(item.key) &&
                  fieldMap[con.field]._hideSubscribes.push(item.key); // 被谁依赖，触发依赖的更新
              }),
            );
          } else {
            item.checkHide = () => false;
          }
          item.visible = item.visible === undefined ? !item.checkHide(formDataRef.current) : item.visible;
          if (item.removeWhen) {
            item.checkRemove = genCheckFn(
              genOrList(item.removeWhen, _mode, (con: any) => {
                item._removeWatchers.push(con.field);
                !fieldMap[con.field]._removeSubscribes.includes(item.key) &&
                  fieldMap[con.field]._removeSubscribes.push(item.key);
              }),
            );
          } else {
            item.checkRemove = () => false;
          }
          item.remove = item.checkRemove(formDataRef.current);
          if (item.disabled) {
            item.checkDisabled = () => true;
          } else if (item.disableWhen) {
            item.checkDisabled = genCheckFn(
              genOrList(item.disableWhen, _mode, (con: any) => {
                item._disabledWatchers.push(con.field);
                !fieldMap[con.field]._disabledSubscribes.includes(item.key) &&
                  fieldMap[con.field]._disabledSubscribes.push(item.key);
              }),
            );
          } else {
            item.checkDisabled = () => false;
          }
          item.disabled = item.checkDisabled(formDataRef.current);
          //
          if (item.converted) return item;
          item.converted = true;
          // 初始化value
          item.value = get(formDataRef.current, item.key);

          if (!Array.isArray(item.rules)) {
            item.rules = [];
          }
          item.registerRequiredCheck = (fn) => {
            if (item.required && typeof fn === 'function') {
              item.rules.unshift({ validator: fn } as any);
            }
            item.registerRequiredCheck = noop;
          };
          item.validate = genValidateFn((res) => {
            setFields((prev) => prev.map((p) => (p.key === item.key ? { ...p, valid: res } : p)));
          });

          if (!item.componentProps) {
            item.componentProps = {};
          }
          if (!item.wrapperProps) {
            item.wrapperProps = {};
          }
          if (isEmpty(item.validateTrigger)) {
            item.validateTrigger = ['onChange'];
          } else if (typeof item.validateTrigger === 'string') {
            item.validateTrigger = [item.validateTrigger];
          }
          if (item.validateTrigger.includes('onBlur')) {
            const originOnBlur = item.componentProps.onBlur;
            item.componentProps.onBlur = (...args: any) => {
              (originOnBlur || noop)(...args);
              // 因为onChange肯定在onBlur前触发，data是最新
              setFields((prev) =>
                prev.map((p) => (p.key === item.key ? { ...p, valid: item.validate(item, formDataRef.current) } : p)),
              );
            };
          }

          item.valid = [];
          item.getData = () => (item.fixData || noop)(item.value);
          return item;
        });
      });
      formRef.current.onSubmit = (_onFinish = onFinish, _onFinishFailed = onFinishFailed) => {
        formRef.current
          .validateFields()
          .then((val: any) => {
            _onFinish(val, mode === 'edit');
          })
          .catch((err: any) => {
            _onFinishFailed(
              err.errorFields.map((item: any) => item.errors),
              err.values,
              mode === 'edit',
            );
            formRef.current.scrollToField(err.errorFields[0].name);
          });
      };
    },
    getFields: () => cloneDeep(_fields),
    isFieldTouched(k: string) {
      return !!fieldMapRef.current[k].isTouched; // 这里_fields是空数组，是个闭包
    },
    onSubmit: (_onFinish = onFinish, _onFinishFailed = onFinishFailed) => {
      formRef.current
        .validateFields()
        .then((val: any) => {
          _onFinish(val, mode === 'edit');
        })
        .catch((err: any) => {
          _onFinishFailed(
            err.errorFields.map((item: any) => item.errors),
            err.values,
            mode === 'edit',
          );
          formRef.current.scrollToField(err.errorFields[0].name);
        });
    },
    validateFields() {
      return new Promise((resolve, reject) => {
        formRef.current.validate().then((checkInfo: any) => {
          const error = [] as any[];
          const _formData = formRef.current.getData();
          const validFormData = {};
          map(checkInfo, (v, k) => {
            if (v[0] !== 'success') {
              error.push({
                name: k,
                errors: v,
              });
            }
            if (k in _formData) {
              validFormData[k] = _formData[k];
            }
          });

          if (error.length) {
            reject({ values: validFormData, errorFields: error });
          } else {
            resolve(validFormData);
          }
        });
      });
    },
    scrollToField(key: string) {
      const unsuccessEle = document.getElementById(key) as any;
      unsuccessEle && unsuccessEle.scrollIntoView();
    },
    validate() {
      const asyncKeys = [] as string[];
      const asyncChecks = [] as Array<PromiseLike<ValidateResult>>;
      const checkInfo = {};

      setFields((prev) =>
        prev.map((item) => {
          const prevResult = item.valid;
          const newResult = item.validate(item, formDataRef.current);
          if (!item.remove) {
            if (newResult[2]) {
              asyncKeys.push(item.key);
              asyncChecks.push(newResult[2]);
            } else {
              checkInfo[item.key] = newResult;
            }
          }
          if (prevResult[0] !== newResult[0] || prevResult[1] !== newResult[1]) {
            return { ...item, valid: newResult };
          }
          return item;
        }),
      );
      if (asyncChecks.length) {
        return Promise.all(asyncChecks).then((rs) => {
          const asyncResult = {};
          asyncKeys.forEach((k, i) => {
            asyncResult[k] = rs[i];
          });
          return { ...checkInfo, ...asyncResult };
        });
      }
      return Promise.resolve(checkInfo);
    },
    reset: (k?: string) => {
      if (k) {
        const defaultV = get(defaultDataRef.current, k);
        set(formDataRef.current, k, defaultV);
      } else {
        formDataRef.current = merge({}, defaultDataRef.current);
      }

      setFields((prev) => {
        // 保持当前field，只重置数据等，而不是直接恢复到原始状态
        return prev.map((item) => {
          const copy = { ...item };
          copy.key && (copy.value = get(formDataRef.current, copy.key));
          copy.isTouched = false;
          copy.valid = [];
          copy.visible = copy.visible === undefined ? !copy.checkHide(formDataRef.current) : copy.visible;
          copy.remove = copy.checkRemove(formDataRef.current);
          return copy;
        });
      });
    },
    getData: () => {
      const data = {};
      Object.keys(fieldMapRef.current).forEach((k) => {
        // 数据要从formDataRef取，否则会有延迟
        const v = get(formDataRef.current, k);
        const isRemovedField = get(fieldMapRef.current, [k, 'remove']);
        if (!isRemovedField) {
          const group = get(fieldMapRef.current, [k, 'group']);
          if (!group) {
            set(data, k, v);
          } else if (!get(fieldMapRef.current, [group, 'remove'])) {
            set(data, k, v);
          }
        }
      });
      return data;
    },
  });

  useMount(() => {
    _ref && (_ref.current = formRef.current);
    formRef.current.setFields([...fields], mode);
  });

  React.useEffect(() => {
    _fields.forEach((f) => {
      f.key && (fieldMapRef.current[f.key] = f);
    });
    formRef.current.getFields = () => cloneDeep(_fields);
  }, [_fields]);

  if (!_fields.length) return null;
  // 渲染前排序
  const sortFields = _fields.filter((f) => !f.remove);

  /**
   * field:
   *   value: any
   *   dataSource: static | dynamic
   *   visible: boolean
   *   valid: {msg, status}
   *   validate(): {msg, status}
   *   checkHide(): boolean
   *   checkRemove(): boolean
   */

  return (
    <Context.Provider value={{ form: formRef.current }}>
      {formRender ? (
        formRender({ RenderFields, form: formRef.current, fields: sortFields })
      ) : (
        <RenderFields fields={sortFields} form={formRef.current} />
      )}
      {children}
    </Context.Provider>
  );
};

Form.Submit = Submit;
Form.Reset = Reset;
