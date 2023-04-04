package datart.server.common;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import datart.core.base.exception.Exceptions;
import datart.core.common.JavascriptUtils;
import datart.server.base.params.DownloadCreateParam;

import javax.script.Invocable;
import javax.script.ScriptException;

public class JsParserUtils {

    private static Invocable parser;

    public static DownloadCreateParam parseExecuteParam(String type, String json) throws ScriptException, NoSuchMethodException, JsonProcessingException {
        Invocable parser = getParser();
        if (parser == null) {
            Exceptions.msg("param parser load error");
        }
        Object result = parser.invokeFunction("getQueryData", type, json);
        return JSON.parseObject(result.toString(), DownloadCreateParam.class);
    }

    private static synchronized Invocable getParser() {
        if (parser == null) {
            try {
                parser = JavascriptUtils.load("javascript/parser.js");
            } catch (Exception e) {
                Exceptions.e(e);
            }
        }
        return parser;
    }
}
