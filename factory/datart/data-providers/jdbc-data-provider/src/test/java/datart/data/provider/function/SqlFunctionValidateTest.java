package datart.data.provider.function;

import datart.core.base.exception.Exceptions;
import datart.data.provider.calcite.SqlParserUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SqlFunctionValidateTest {

    @Test
    public void testFunctionValidate() {
        List<String> errorStr = new ArrayList<>();
        for (String s : SqlFunctionExamples.functionList) {
            try {
                SqlParserUtils.parseSnippet(s);
            } catch (Exception e) {
                errorStr.add(s);
            }
        }
        if (errorStr.size()>0){
            Exceptions.msg("Functions validate failed: "+ StringUtils.join(errorStr, ","));
        }
        log.info("Functions validate passed");
    }
}
