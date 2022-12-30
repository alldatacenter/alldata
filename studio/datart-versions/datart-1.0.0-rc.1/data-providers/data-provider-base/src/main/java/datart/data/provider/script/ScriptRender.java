package datart.data.provider.script;


import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.QueryScript;
import lombok.extern.slf4j.Slf4j;

/**
 * 实现Script的变量替换和表达式替换。
 * 输出一个可由对应执行器直接执行的字符串。
 * 各种类型的变量处理方式如下:
 * : 系统变量,值为null,将整个表达式替换成1=1。有值时直接替换成值。
 * : 查询变量,单值直接替换。多值按照执行器格式拼接成数组格式字符串。
 * : 权限变量,值为@ALL_PERMISSION@，代表无权限控制，替换整个表达式为1=1。值为null代表无权限，替换整个表达式为1=0。其它值，根据实际情况替换。
 * Note:由于权限变量最终取值是根据当前登录用户的角色计算得来的,在编写View时无法确定变量的最终取值，从而无法按照值的类型编写条件表达式。因此需要根据最终的变量值修改整个条件表达式。
 * <p>
 * Realize the Script variable replacement and expression replacement.
 * Outputs a string that can be executed directly by the corresponding executor.
 * The various types of variables are handled as follows:
 * : System variable, with a value of null, replaces the entire expression with 1=1.If there is a value, replace it with a value.
 * : Query variables, single value direct replacement.Multiple values are concatenated into an array format string in the executor format.
 * : permission variable with a value of @all_permission@, which represents no permission control, replacing the entire expression with 1=1.
 * A value of NULL indicates no permission, and replaces the entire expression with 1=0.Other values, replace according to the actual situation.
 * Note: Since the final value of the permission variable is calculated according to the role of the current logger,
 * </> the final value of the variable cannot be determined when writing View,
 * </> so it is impossible to write a conditional expression according to the type of the value.
 * </> Therefore,the entire conditional expression needs to be modified based on the final variable value.
 */
@Slf4j
public class ScriptRender {

    protected QueryScript queryScript;

    protected ExecuteParam executeParam;

    public ScriptRender(QueryScript queryScript, ExecuteParam executeParam) {
        this.queryScript = queryScript;
        this.executeParam = executeParam;
    }

}