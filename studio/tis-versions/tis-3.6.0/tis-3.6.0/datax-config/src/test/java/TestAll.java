import com.qlangtech.tis.plugin.ds.TestDataType;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-19 10:21
 **/
public class TestAll extends TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(TestDataType.class);
        return suite;
    }
}
