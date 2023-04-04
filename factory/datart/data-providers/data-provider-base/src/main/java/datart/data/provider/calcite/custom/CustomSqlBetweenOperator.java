package datart.data.provider.calcite.custom;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.fun.SqlBetweenOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.calcite.util.Util;

public class CustomSqlBetweenOperator extends SqlBetweenOperator {
    public CustomSqlBetweenOperator(Flag flag, boolean negated) {
        super(flag, negated);
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public void unparse(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
        final SqlWriter.Frame frame =
                writer.startList(SqlWriter.FrameTypeEnum.create("BETWEEN"), "", "");
        call.operand(VALUE_OPERAND).unparse(writer, getLeftPrec(), 0);
        writer.sep(this.toString());
        final SqlNode lower = call.operand(LOWER_OPERAND);
        final SqlNode upper = call.operand(UPPER_OPERAND);
        int lowerPrec = new AndFinder().containsAnd(lower) ? 100 : 0;
        lower.unparse(writer, lowerPrec, lowerPrec);
        writer.sep("AND");
        upper.unparse(writer, 0, getRightPrec());
        writer.endList(frame);
    }

    private static class AndFinder extends SqlBasicVisitor<Void> {
        public Void visit(SqlCall call) {
            final SqlOperator operator = call.getOperator();
            if (operator == SqlStdOperatorTable.AND) {
                throw Util.FoundOne.NULL;
            }
            return super.visit(call);
        }

        boolean containsAnd(SqlNode node) {
            try {
                node.accept(this);
                return false;
            } catch (Util.FoundOne e) {
                return true;
            }
        }
    }

}
