/* eslint-disable react/jsx-no-constructed-context-values */
/* eslint-disable no-shadow */
import ReactDOM from 'react-dom';
import {
    equals, isNil, map, filter, propEq, findIndex,
} from 'ramda';
import { useUpdate } from 'ahooks';
import React, {
    JSXElementConstructor,
    memo,
    ReactElement,
    RefObject,
    useEffect,
    useRef,
    useState,
    createContext,
} from 'react';
import { useLocation } from 'react-router';

type Children = ReactElement<any, string | JSXElementConstructor<any>> | null
interface context {
    destroy: (params: string, render?: boolean) => void,
    isActive: boolean
}
export const KeepAliveContext = createContext<context>({ destroy: () => { }, isActive: false });
interface Props {
    // eslint-disable-next-line react/no-unused-prop-types
    activeName?: string
    include?: Array<string>
    exclude?: Array<string>
    maxLen?: number
    children: Children
}
function KeepAlive({
    children, exclude, include, maxLen = 5,
}: Props) {
    const containerRef = useRef<HTMLDivElement>(null);
    const components = useRef<Array<{ name: string; ele: Children }>>([]);
    const { pathname } = useLocation();
    const update = useUpdate();
    const isActive = findIndex(propEq('name', pathname))(components.current);
    // 如果没有配置include，exclude 则不缓存
    if (isNil(exclude) && isNil(include)) {
        components.current = [
            {
                name: pathname,
                ele: children,
            },
        ];
    } else {
        // 缓存超过上限的 干掉第一个缓存
        if (components.current.length >= maxLen) {
            components.current = components.current.slice(1);
        }
        components.current = filter(({ name }) => {
            if (exclude && exclude.includes(name)) {
                return false;
            }
            if (include) {
                return include.includes(name);
            }
            return true;
        }, components.current);
        const component = components.current.find((res) => equals(res.name, pathname));
        if (isNil(component)) {
            components.current = [
                ...components.current,
                {
                    name: pathname,
                    ele: children,
                },
            ];
        }
    }
    // 销毁缓存的路由
    function destroy(params: string, render = false) {
        components.current = filter(({ name }) => {
            if (params === name) {
                return false;
            }
            return true;
        }, components.current);
        // 是否需要立即刷新 一般是不需要的
        if (render) {
            update();
        }
    }
    const context = {
        destroy,
        isActive: isActive !== -1,
    };
    const isEquslsPath:(name: string, pathname: string) => boolean = (name:string, pathname:string) => {
        // name
        console.log('pathname', pathname, name);
        return false;
    };
    return (
        <>
            <div ref={containerRef} className="keep-alive" />
            <KeepAliveContext.Provider value={context}>
                {map(
                    ({ name, ele }) => (
                        <Component active={equals(name, pathname) || isEquslsPath(name, pathname)} renderDiv={containerRef} name={name} key={name}>
                            {ele}
                        </Component>
                    ),
                    components.current,
                )}
            </KeepAliveContext.Provider>
        </>
    );
}
export default memo(KeepAlive);
interface ComponentProps {
    active: boolean
    children: Children
    name: string
    renderDiv: RefObject<HTMLDivElement>
}
// 渲染 当前匹配的路由 不匹配的 利用createPortal 移动到 document.createElement('div') 里面
function Component({
    active, children, name, renderDiv,
}: ComponentProps) {
    const [targetElement] = useState(() => document.createElement('div'));
    const activatedRef = useRef(false);
    activatedRef.current = activatedRef.current || active;
    useEffect(() => {
        if (active) { // 渲染匹配的组件
            if (renderDiv.current?.firstChild) {
                renderDiv.current?.replaceChild(targetElement, renderDiv.current?.firstChild);
            } else {
                renderDiv.current?.appendChild(targetElement);
            }
        }
    }, [active]);
    useEffect(() => { // 添加一个id 作为标识 并没有什么太多作用
        targetElement.setAttribute('id', name);
    }, [name]);
    // 把vnode 渲染到document.createElement('div') 里面
    return <>{activatedRef.current && ReactDOM.createPortal(children, targetElement)}</>;
}
export const KeepAliveComponent = memo(Component);
