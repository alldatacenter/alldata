package cn.datax.common.core;

public class DataConstant {

    /**
     * Oauth2安全相关常量
     */
    public enum Security {
        //请求头TOKEN名称
        TOKENHEADER("gatewayToken"),
        //请求头TOKEN值
        TOKENVALUE("datax:gateway:123456"),
        //OAUTH2请求头
        AUTHORIZATION("Authorization"),
        //OAUTH2令牌类型
        TOKENTYPE("bearer "),
        //security授权角色前缀
        ROLEPREFIX("ROLE_");

        Security(String val){
            this.val = val;
        }

        private final String val;

        public String getVal() {
            return val;
        }
    }

    /**
     * 通用的是否
     */
    public enum TrueOrFalse {
        FALSE("0",false),
        TRUE("1",true);

        TrueOrFalse(String key, boolean val){
            this.key = key;
            this.val = val;
        }

        private final String key;
        private final boolean val;

        public String getKey() {
            return key;
        }

        public boolean getVal() {
            return val;
        }
    }

    /**
     * 用户认证返回额外信息
     */
    public enum UserAdditionalInfo {
        LICENSE("license", "datax"),
        USER("user", "用户"),
        USERID("user_id", "用户ID"),
        USERNAME("username", "用户名"),
        NICKNAME("nickname", "用户昵称"),
        DEPT("user_dept", "用户部门"),
        ROLE("user_role", "用户角色"),
        POST("user_post", "用户岗位");

        UserAdditionalInfo(String key, String val){
            this.key = key;
            this.val = val;
        }

        private final String key;
        private final String val;

        public String getKey() {
            return key;
        }

        public String getVal() {
            return val;
        }
    }

    /**
     * 通用的启用禁用状态
     */
    public enum EnableState {
        DISABLE("0","禁用"),
        ENABLE("1","启用");

        EnableState(String key, String val){
            this.key = key;
            this.val = val;
        }

        private final String key;
        private final String val;

        public String getKey() {
            return key;
        }

        public String getVal() {
            return val;
        }
    }

    /**
     * 流程审核状态
     */
    public enum AuditState{
        WAIT("1","待提交"),
        BACK("2", "已退回"),
        AUDIT("3","审核中"),
        AGREE("4","通过"),
        REJECT("5","不通过"),
        CANCEL("6", "已撤销");

        AuditState(String key, String val){
            this.key = key;
            this.val = val;
        }

        private final String key;
        private final String val;

        public String getKey() {
            return key;
        }

        public String getVal() {
            return val;
        }
    }

    /**
     * 菜单类型
     */
    public enum MenuType{
        MODULE("0","模块"),
        MENU("1","菜单"),
        BUTTON("2","按钮");

        MenuType(String key, String val){
            this.key = key;
            this.val = val;
        }

        private final String key;
        private final String val;

        public String getKey() {
            return key;
        }

        public String getVal() {
            return val;
        }
    }

    /**
     * 数据范围
     */
    public enum DataScope{
        ALL("1","全部数据权限"),
        CUSTOM("2","自定义数据权限"),
        DEPT("3","本部门数据权限"),
        DEPTANDCHILD("4","本部门及以下数据权限"),
        SELF("5","仅本人数据权限");
        DataScope(String key, String val){
            this.key = key;
            this.val = val;
        }

        private final String key;
        private final String val;

        public String getKey() {
            return key;
        }

        public String getVal() {
            return val;
        }
    }

    /**
     * Api状态
     */
    public enum ApiState{
        WAIT("1","待发布"),
        RELEASE("2","已发布"),
        CANCEL("3","已下线");
        ApiState(String key, String val){
            this.key = key;
            this.val = val;
        }

        private final String key;
        private final String val;

        public String getKey() {
            return key;
        }

        public String getVal() {
            return val;
        }
    }
}
