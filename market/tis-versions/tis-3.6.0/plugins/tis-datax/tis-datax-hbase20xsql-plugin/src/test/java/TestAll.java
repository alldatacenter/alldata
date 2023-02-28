
import com.qlangtech.tis.plugin.datax.TestDataXHbase20xsqlReader;
import com.qlangtech.tis.plugin.datax.TestDataXHbase20xsqlWriter;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;
/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-09 09:22
 **/
public class TestAll extends TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(TestDataXHbase20xsqlReader.class);
        suite.addTestSuite(TestDataXHbase20xsqlWriter.class);
        return suite;
    }
}
