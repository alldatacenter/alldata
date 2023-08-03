package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.anchors.TypedKey;
import com.linkedin.feathr.core.utils.ConfigUtils;
import com.typesafe.config.Config;

import static com.linkedin.feathr.core.config.producer.anchors.AnchorConfig.*;

/**
 * Package private class to build {@link TypedKey} from the following config syntax:
 * <pre>
 *{@code
 * key: [key1, key2]
 * }
 * </pre>
 *
 * or
 *
 * <pre>
 *{@code
 * key.sqlExpr: [key1, key2]
 * }
 * </pre>
 *
 * or
 *
 * <pre>
 *{@code
 * key.mvel: [key1, key2]
 * }
 * </pre>
 */
class TypedKeyBuilder {
  // instance initialized when loading the class
  private static final TypedKeyBuilder INSTANCE = new TypedKeyBuilder();

  private TypedKeyBuilder() { }

  public static TypedKeyBuilder getInstance() {
    return INSTANCE;
  }

  TypedKey build(Config config) {
    String keyExprTypeStr;
    ExprType keyExprType;
    if (config.hasPath(KEY_MVEL)) {
      keyExprTypeStr = KEY_MVEL;
      keyExprType = ExprType.MVEL;
    } else if (config.hasPath(KEY_SQL_EXPR)) {
      keyExprTypeStr = KEY_SQL_EXPR;
      keyExprType = ExprType.SQL;
    } else {
      keyExprTypeStr = KEY;
      keyExprType = ExprType.MVEL;
    }
    // get the raw key expr which is in HOCON format
    String rawKeyExpr = ConfigUtils.getHoconString(config, keyExprTypeStr);
    return rawKeyExpr == null ? null : new TypedKey(rawKeyExpr, keyExprType);
  }
}
