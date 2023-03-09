package com.linkedin.feathr.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;


/**
 * The class is used to figure out the input features in a mvel expresion.
 */
public class MvelInputsResolver {
  private static final MvelInputsResolver INSTANCE = new MvelInputsResolver();

  public static MvelInputsResolver getInstance() {
    return INSTANCE;
  }

  private MvelInputsResolver() {
  }

  /**
   * Gets the input features in the mvel expression.
   * It leverages Mvel compiler to compute the input variables. However, Mvel needs to resolve the imports via the
   * classloader. To make this functionality light, we don't want to rely on the class loaders as sometimes we only
   * have a simple config file. Instead, we use a heuristic approach to replace the import with some dummy class that
   * we have and the input variables will still be correctly computed by Mvel.
   * TODO - 7784): Migrate this inline mvel expression to a more structured derived syntax
   * Part of the reason we need to do this is we are not using the more explicit derived syntax where input features
   * are explicitly specified. We should explore if we can migrate the implicit inline derived features to the explicit
   * ones.
   */
  public List<String> getInputFeatures(String mvelExpr) {
    List<String> expressions = Arrays.stream(mvelExpr.split(";"))
        .map(String::trim)
        // normalize spaces
        .map(expression -> expression.replaceAll("\\s{2,}", " "))
        .collect(Collectors.toList());
    Set<String> imports =
        expressions.stream().map(String::trim).filter(x -> x.startsWith("import ")).collect(Collectors.toSet());

    // Use the cleaned expressions for further processing
    String rewrittenExpr = String.join(";", expressions);
    for (String mvelImport : imports) {
      List<String> importSplit = Arrays.asList(mvelImport.split("\\."));
      String className = importSplit.get(importSplit.size() - 1);
      // Use java.lang.Object as the dummy class to replace other classes to get over Mvel's import check.
      // Mvel compiler will check if a class exist in the classpath. In some scenarios, we don't have the classes in
      // the classpath but only the config file but we still want to run the mvel compiler. The approach here is to
      // replace those imported classes with a dummy class and then the mvel compiler will continue to run(Mvel compiler
      // doesn't check if the class has that function). This is a hack as mvel compiler doesn't provide other ways to
      // achieve this.
      // For example: "import come.linkedin.MyClass; MyClass.apply(featureA);" will be converted into
      // "import java.lang.Object; Object.apply(featureA);"
      rewrittenExpr = rewrittenExpr.replace(mvelImport + ";", "import java.lang.Object;");
      rewrittenExpr = rewrittenExpr.replaceAll(className + ".", "Object.");
    }
    // Use MVEL "analysis compiler" to figure out what the inputs are
    ParserContext parserContext = new ParserContext();
    MVEL.analysisCompile(rewrittenExpr, parserContext);

    // MVEL Hack: remove '$' from the inputs, since it's a "special" input used for fold/projection statements
    // For example, typeAndPermissionList = ($.type + ", " + getPermission($) in users). Here $ sign will be considered
    // as an input.
    // Refer to https://iwww.corp.linkedin.com/wiki/cf/pages/viewpage.action?pageId=272932479#FrameMVELUserGuide(go/framemvel)-Dollar($)SignSyntax
    // for more deltails.
    List<String> list = new ArrayList<>();
    for (String featureName : parserContext.getInputs().keySet()) {
      // Filter out com and org since they are imports
      if (!"$".equals(featureName) && !featureName.equals("com") && !featureName.equals("org")) {
        list.add(featureName);
      }
    }
    return list;
  }
}
