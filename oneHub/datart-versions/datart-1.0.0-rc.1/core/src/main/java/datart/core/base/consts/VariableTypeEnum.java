package datart.core.base.consts;

public enum VariableTypeEnum {

    /**
     * 查询变量，在VIZ中通过控制器传值。
     * 以达到动态条件的作用。
     */
    QUERY,

    /**
     * 权限变量，为不同角色配置不同的值。在脚本解析时用当前用户所关联的角色所对应的变量值替换。
     * 达到数据的行权限控制。
     */
    PERMISSION,

}