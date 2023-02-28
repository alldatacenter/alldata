export type TRouterItem = {
    path: string,
    exact?: boolean,
    component?: any,
    children?: TRouterItem[],
    key?: string,
    icon?: React.ReactNode
    label?: string,
    menuHide?: boolean,
    [key: string]: any,
}

export type MenuItem = TRouterItem;
export type TRouter = {
    [key: string]: TRouterItem
}
