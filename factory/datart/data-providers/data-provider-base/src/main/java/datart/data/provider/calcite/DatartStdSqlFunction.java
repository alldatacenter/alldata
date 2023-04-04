package datart.data.provider.calcite;

import datart.core.data.provider.sql.FunArg;
import datart.core.data.provider.sql.FunctionOperator;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlOperandTypeChecker;
import org.apache.calcite.sql.type.SqlOperandTypeInference;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class DatartStdSqlFunction extends SqlFunction {

    private List<FunArg> args;

    private String funName;

    private String tableName;

    private DatartStdSqlFunction(String name, SqlKind kind, SqlReturnTypeInference returnTypeInference, SqlOperandTypeInference operandTypeInference, SqlOperandTypeChecker operandTypeChecker, SqlFunctionCategory category) {
        super(name, kind, returnTypeInference, operandTypeInference, operandTypeChecker, category);
    }

    public DatartStdSqlFunction(String funName, List<FunArg> args) {
        this(funName,
                SqlKind.OTHER_FUNCTION,
                null,
                null,
                null,
                SqlFunctionCategory.USER_DEFINED_CONSTRUCTOR);
        this.args = args;
        this.funName = funName;
    }

    public DatartStdSqlFunction(FunctionOperator functionOperator, String tableName) {
        this(functionOperator.getFunction().name(), functionOperator.getArgs());
        this.tableName = tableName;
    }

//    public SqlNode parseSqlNode() {
//
//        if (CollectionUtils.isEmpty(args)) {
//            return SqlNodeUtils.createSqlIdentifier(funName);
//        }
//
//        SqlNode[] operands = new SqlNode[args.size()];
//
//        for (int i = 0; i < args.size(); i++) {
//            FunArg arg = args.get(i);
//            switch (arg.getType()) {
//                case CONST:
//                    operands[i] = SqlNodeUtils.createSqlLiteral(arg.getValue());
//                case FUNCTION:
//                    operands[i] = new DatartStdSqlFunction((FunctionOperator) arg.getValue(), tableName).parseSqlNode();
//                case FIELD:
//                    operands[i] = SqlNodeUtils.createSqlIdentifier(arg.getValue().toString(), tableName);
//            }
//        }
//        return new SqlBasicCall(this, operands, SqlParserPos.ZERO);
//    }

}