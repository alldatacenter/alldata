package com.hw.lineage.server.interfaces.result;

/**
 * @description: ResultMessage
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class ResultMessage {
    private ResultMessage() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SUCCESS = "success";

    public static final String CREATE_SUCCESS = "create success";

    public static final String DELETE_SUCCESS = "delete success";

    public static final String UPDATE_SUCCESS = "update success";

    public static final String QUERY_SUCCESS = "query success";

    public static final String CHECK_SUCCESS = "check success";

    public static final String UPLOAD_SUCCESS = "upload success";

    public static final String QUERY_FAILED = "query failed";

    public static final String DELETE_FAILED = "delete failed";

    public static final String UPDATE_FAILED = "update failed";

    public static final String DETAIL_SUCCESS = "detail success";

    public static final String PARSE_LINEAGE_SUCCESS = "parse lineage success";

    public static final String PARSE_FUNCTION_SUCCESS = "parse function success";

    public static final String LOGIN_SUCCESS = "login success";

    public static final String LOGOUT_SUCCESS = "logout success";

    public static final String NOT_LOGGED_IN = "please log in first";

    public static final String USER_ACCOUNT_EXPIRED= "user account expired";

    public static final String USER_ACCOUNT_OR_PASSWORD_ERROR= "user account or password error";

    public static final String USER_PASSWORD_EXPIRED= "user password expired";

    public static final String USER_ACCOUNT_DISABLE= "user account disable";

    public static final String USER_ACCOUNT_LOCKED= "user account locked";

    public static final String DETAIL_FAILED = "detail failed";

    public static final String NULL_POINTER_EXCEPTION = "null pointer exception";

    public static final String UNIQUE_INDEX_CONFLICT_ERROR = "unique index conflict, please enter again";

    public static final String TOKEN_HAS_NO_PERMISSION = "token has no permission";

}
