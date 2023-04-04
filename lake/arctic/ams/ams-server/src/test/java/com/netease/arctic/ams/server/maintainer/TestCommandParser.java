package com.netease.arctic.ams.server.maintainer;


import com.netease.arctic.ams.server.maintainer.command.AnalyzeCall;
import com.netease.arctic.ams.server.maintainer.command.CallCommandTestBase;
import com.netease.arctic.ams.server.maintainer.command.HelpCall;
import com.netease.arctic.ams.server.maintainer.command.IllegalCommandException;
import com.netease.arctic.ams.server.maintainer.command.OptimizeCall;
import com.netease.arctic.ams.server.maintainer.command.RepairCall;
import com.netease.arctic.ams.server.maintainer.command.ShowCall;
import com.netease.arctic.ams.server.maintainer.command.SimpleRegexCommandParser;
import com.netease.arctic.ams.server.maintainer.command.TableCall;
import com.netease.arctic.ams.server.maintainer.command.UseCall;
import org.junit.Assert;
import org.junit.Test;

public class TestCommandParser extends CallCommandTestBase {
  private static SimpleRegexCommandParser simpleRegexCommandParser =
      new SimpleRegexCommandParser(callFactory);

  @Test
  public void testParser() throws Exception {
    //AnalyzeCall test
    Assert.assertEquals(AnalyzeCall.class,
        simpleRegexCommandParser.parse("ANALYZE stock ").getClass());
    Assert.assertEquals(AnalyzeCall.class,
        simpleRegexCommandParser.parse("  analyze order").getClass());
    Assert.assertThrows(IllegalCommandException.class,
        () -> simpleRegexCommandParser.parse("ANALYZE stock order"));

    //RepairCall test
    Assert.assertThrows(IllegalCommandException.class,
        () -> simpleRegexCommandParser.parse("REPAIR stock ROLLBACK 1234567"));
    Assert.assertThrows(IllegalCommandException.class,
        () -> simpleRegexCommandParser.parse("REPAIR stock FIND_BACK 1234567"));
    Assert.assertThrows(IllegalCommandException.class,
        () -> simpleRegexCommandParser.parse("REPAIR stock SYNC_METADATA 1234567"));
    Assert.assertThrows(IllegalCommandException.class,
        () -> simpleRegexCommandParser.parse("REPAIR stock THROUGH FIND_BACK 1234567"));
    Assert.assertThrows(IllegalCommandException.class,
        () -> simpleRegexCommandParser.parse("REPAIR stock THROUGH SYNC_METADATA 1234567"));
    Assert.assertThrows(IllegalCommandException.class,
        () -> simpleRegexCommandParser.parse("REPAIR stock SYNC_METADATA"));
    Assert.assertEquals(RepairCall.class,
        simpleRegexCommandParser.parse("REPAIR  stock  THROUGH  ROLLBACK  123456789").getClass());
    Assert.assertEquals(RepairCall.class,
        simpleRegexCommandParser.parse("repair stock through rollback 123456789").getClass());
    Assert.assertEquals(RepairCall.class,
        simpleRegexCommandParser.parse("repair stock through rollback ").getClass());
    Assert.assertEquals(RepairCall.class,
        simpleRegexCommandParser.parse(" REPAIR stock THROUGH FIND_BACK ").getClass());
    Assert.assertEquals(RepairCall.class,
        simpleRegexCommandParser.parse("repair  stock  through  sync_metadata").getClass());

    //UseCall test
    Assert.assertEquals(UseCall.class,
        simpleRegexCommandParser.parse("USE  my_db").getClass());
    Assert.assertEquals(UseCall.class,
        simpleRegexCommandParser.parse("use my_catalog").getClass());
    Assert.assertThrows(IllegalCommandException.class,
        () -> simpleRegexCommandParser.parse("USE my_catalog my_db"));

    //OptimizeCall test
    Assert.assertEquals(OptimizeCall.class,
        simpleRegexCommandParser.parse("OPTIMIZE START order_line").getClass());
    Assert.assertEquals(OptimizeCall.class,
        simpleRegexCommandParser.parse("optimize stop  stock").getClass());
    Assert.assertThrows(IllegalCommandException.class,
        () -> simpleRegexCommandParser.parse("OPTIMIZE stock"));

    //TableCall test
    Assert.assertEquals(TableCall.class,
        simpleRegexCommandParser.parse("TABLE order_line REFRESH").getClass());

    //ShowCall test
    Assert.assertEquals(ShowCall.class,
        simpleRegexCommandParser.parse("SHOW DATABASES").getClass());
    Assert.assertEquals(ShowCall.class,
        simpleRegexCommandParser.parse("SHOW TABLES").getClass());
    Assert.assertEquals(ShowCall.class,
        simpleRegexCommandParser.parse("show  databases").getClass());
    Assert.assertEquals(ShowCall.class,
        simpleRegexCommandParser.parse("show catalogs").getClass());
    Assert.assertThrows(IllegalCommandException.class,
        () -> simpleRegexCommandParser.parse("show my_db tables"));

    Assert.assertEquals(HelpCall.class,
        simpleRegexCommandParser.parse("FIND_BACK stock").getClass());
    Assert.assertEquals(HelpCall.class,
        simpleRegexCommandParser.parse("help").getClass());
    Assert.assertEquals(HelpCall.class,
        simpleRegexCommandParser.parse("analyze").getClass());

  }

}
