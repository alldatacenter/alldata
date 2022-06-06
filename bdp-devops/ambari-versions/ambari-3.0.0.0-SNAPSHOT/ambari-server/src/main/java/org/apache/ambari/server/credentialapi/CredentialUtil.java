/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.server.credentialapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.security.alias.CredentialProvider;
import org.apache.hadoop.security.alias.CredentialProviderFactory;
import org.apache.hadoop.security.alias.CredentialShell;
import org.apache.hadoop.security.alias.JavaKeyStoreProvider;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Command line utility that wraps over CredentialShell. Extends the
 * create command to overwrite a credential if it exists. Also
 * provides the ability to get the decrypted password.
 *
 * The CLI structure and code is the same as CredentialShell.
 */
public class CredentialUtil extends Configured implements Tool {
  /**
   * List of supported commands.
   */
  final static private String COMMANDS =
          "   [--help]\n" +
                  "   [" + CreateCommand.USAGE + "]\n" +
                  "   [" + DeleteCommand.USAGE + "]\n" +
                  "   [" + ListCommand.USAGE + "]\n" +
                  "   [" + GetCommand.USAGE + "]\n";

  /**
   * JCEKS provider prefix.
   */
  public static final String jceksPrefix = JavaKeyStoreProvider.SCHEME_NAME + "://file";

  /**
   * Local JCEKS provider prefix.
   */
  public static final String localJceksPrefix = "localjceks://file";

  /**
   * Password alias
   */
  private String alias = null;

  /**
   * Password specified using the -value option
   */
  private String value = null;

  /**
   * Provider specified using the -provider option
   */
  protected CredentialProvider provider;

  /**
   * When creating a credential, overwrite the credential if it exists.
   * If -n option is specified, this will be set to false.
   */
  private boolean overwrite = true;

  /**
   * Prompt for user confirmation before deleting/overwriting a credential.
   * If -f option is specified, it will be set to false. In the case
   * of a create command, it will be set to false if -n or -f is specified.
   */
  private boolean interactive = true;

  /**
   * One of the supported credential commands.
   */
  private Command command = null;

  /**
   * Main program.
   *
   * @param args Command line arguments
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new CredentialUtil(), args);
    System.exit(res);
  }

  /**
   * Called by ToolRunner.run(). This is the entry point to the tool.
   * Parses the command line arguments and executes the appropriate command.
   *
   * @param args - Arguments supplied by the user.
   * @return - 0 if successful. 1 in case of a failure.
   * @throws Exception - If something goes wrong during command execution.
   */
  @Override
  public int run(String[] args) throws Exception {
    int exitCode = 1;

    for (int i = 0; i < args.length; ++i) {
      if (args[i].equals("create")) {
        if (i == args.length - 1) {
          return 1;
        }
        command = new CreateCommand();
        alias = args[++i];
        if (alias.equals("-h") || alias.equals("-help")) {
          printUsage();
          return 0;
        }
      } else if (args[i].equals("get")) {
        if (i == args.length - 1) {
          return 1;
        }
        command = new GetCommand();
        alias = args[++i];
        if (alias.equals("-h") || alias.equals("-help")) {
          printUsage();
          return 0;
        }
      } else if (args[i].equals("delete")) {
        if (i == args.length - 1) {
          printUsage();
          return 1;
        }
        command = new DeleteCommand();
        alias = args[++i];
        if (alias.equals("-help")) {
          printUsage();
          return 0;
        }
      } else if (args[i].equals("list")) {
        if (i < args.length - 1) {
          alias = args[i + 1];
        }
        command = new ListCommand();
        if (alias.equals("-h") || alias.equals("-help")) {
          printUsage();
          return 0;
        }
        alias = "not required";
      } else if (args[i].equals("-provider")) {
        if (i == args.length - 1) {
          return 1;
        }
        String providerPath = getNormalizedPath(args[++i]);
        getConf().set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, providerPath);
        provider = getCredentialProvider();
      } else if (args[i].equals("-f") || args[i].equals("-force")) {
        interactive = false;
        overwrite = true;
      } else if (args[i].equals("-n")) {
        interactive = false;
        overwrite = false;
      } else if (args[i].equals("-v") || args[i].equals("-value")) {
        value = args[++i];
      } else if (args[i].equals("-h") || args[i].equals("-help")) {
        printUsage();
        return 0;
      } else {
        printUsage();
        ToolRunner.printGenericCommandUsage(System.err);
        return 1;
      }
    }

    if (command == null) {
      printUsage();
    }
    else if (command.validate()) {
      exitCode = command.execute();
    }

    return exitCode;
  }

  /**
   * Prints a command specific usage or overall tool usage.
   */
  protected void printUsage() {
    System.out.println(getUsagePrefix() + COMMANDS);
    if (command != null) {
      System.out.println(command.getUsage());
    }
    else {
      System.out.println("=========================================================" +
              "======");
      System.out.println(CreateCommand.USAGE + ":\n\n" + CreateCommand.DESC);
      System.out.println("=========================================================" +
              "======");
      System.out.println(DeleteCommand.USAGE + ":\n\n" + DeleteCommand.DESC);
      System.out.println("=========================================================" +
              "======");
      System.out.println(ListCommand.USAGE + ":\n\n" + ListCommand.DESC);
      System.out.println("=========================================================" +
              "======");
      System.out.println(GetCommand.USAGE + ":\n\n" + GetCommand.DESC);
    }
  }

  /**
   * Overridden by the command line driver to provide name of the tool.
   *
   * @return - CLI specific information, like tool name, year, copyright, etc.
   */
  protected String getUsagePrefix() {
    return "Usage: ";
  }

  /*
   * Normalize the providerPath to jceks://file/<file_path> or localjceks://file/<file_path>
   */
  private static String getNormalizedPath(String providerPath) {
    if (providerPath != null) {
      String jceksPath;

      if (providerPath.startsWith("/")) {
        providerPath = providerPath.substring(1);
      }

      jceksPath = StringUtils.lowerCase(providerPath.trim());

      if (!jceksPath.startsWith(StringUtils.lowerCase(jceksPrefix)) &&
              !jceksPath.startsWith(localJceksPrefix)) {
        providerPath = jceksPrefix + "/" + providerPath;
      }
    }

    return providerPath;
  }

  /**
   * Gets the provider object for the user specified provider.
   *
   * @return - A credential provider.
   */
  private CredentialProvider getCredentialProvider() {
    CredentialProvider provider = null;

    List<CredentialProvider> providers;
    try {
      providers = CredentialProviderFactory.getProviders(getConf());
      provider = providers.get(0);
    } catch (IOException e) {
      e.printStackTrace(System.err);
    }

    return provider;
  }

  /**
   * CredentialCommand base class
   */
  private abstract class Command {
    /**
     * Validates the user input.
     *
     * @return - True if inputs are valid. False otherwise.
     */
    public boolean validate() {
      boolean rc = true;

      if (alias == null || alias.isEmpty()) {
        System.out.println("There is no alias specified. Please provide the" +
                "mandatory <alias>. See the usage description with -help.");
        rc = false;
      }

      if (provider == null) {
        System.out.println("There are no valid CredentialProviders configured." +
                "\nCredential will not be created.\n"
                + "Consider using the -provider option to indicate the provider" +
                " to use.");
        rc = false;
      }

      return rc;
    }

    /**
     * Gets command usage and description.
     *
     * @return
     */
    public abstract String getUsage();

    /**
     * Called by run(). Implemented by the concrete command classes.
     *
     * @return - 0 if successful. 1 on failure.
     * @throws Exception - If something goes wrong.
     */
    public abstract int execute() throws Exception;
  }

  /**
   * Gets the credential for the specified alias from the
   * specified provider.
   */
  private class GetCommand extends Command {
    public static final String USAGE = "get <alias> [-provider provider-path]";
    public static final String DESC =
            "The get subcommand gets the credential for the specified alias\n" +
                    "from the provider specified through the -provider argument.\n";

    /**
     * Executes the get command. Prints the clear text password on the command line.
     *
     * @return - 0 on success; 1 on failure.
     * @throws IOException
     */
    @Override
    public int execute() throws IOException {
      int exitCode = 0;

      try {
        String credential = getCredential();
        if (credential == null) {
          exitCode = 1;
        } else {
          System.out.println(credential);
        }
      } catch (IOException ex) {
        System.out.println("Cannot get the credential for the specified alias."
                + ": " + ex.getMessage());
        throw ex;
      }

      return exitCode;
    }

    /**
     * Gets the clear text password from the credential provider.
     *
     * @return - Decrypted password for the specified alias.
     * @throws IOException
     */
    private String getCredential() throws IOException {
      String credential = null;
      CredentialProvider.CredentialEntry credEntry = provider.getCredentialEntry(alias);

      if (credEntry != null) {
        char[] password = credEntry.getCredential();
        if (password != null) {
          credential = String.valueOf(password);
        }
      }

      return credential;
    }

    /**
     * Usage and description.
     *
     * @return
     */
    @Override
    public String getUsage() {
      return USAGE + ":\n\n" + DESC;
    }
  }

  /**
   * Creates a new credential for the alias specified or overwrites an
   * existing credential
   */
  private class CreateCommand extends Command {
    /**
     * Usage summary
     */
    public static final String USAGE =
            "create <alias> [-value credential] [-provider provider-path] [-f | -n]";

    /**
     * Command description
     */
    public static final String DESC =
            "The create subcommand creates a new credential or overwrites\n" +
                    "an existing credential for the name specified\n" +
                    "as the <alias> argument within the provider indicated through\n" +
                    "the -provider argument. The command asks for confirmation to\n" +
                    "overwrite the existing credential unless the -f option is specified.\n" +
                    "Specify -n to not overwrite if the credential exists.\nThe option specified last wins.";

    /**
     * Creates or updates the specified credential.
     *
     * @return - 0 on success; 1 on failure.
     * @throws Exception
     */
    @Override
    public int execute() throws Exception {
      int exitCode = 0;
      CredentialProvider.CredentialEntry credEntry = provider.getCredentialEntry(alias);

      if (credEntry != null) {
        /*
         * If credential already exists, overwrite if -f flag was specified.
         * overwrite is true if -f was specified.
         * overwrite is false if -n was specified.
         * if neither options were specified, prompt the user.
         */
        if (interactive) {
          // prompt the user to confirm or reject the overwrite
          overwrite = ToolRunner
                  .confirmPrompt("You are about to OVERWRITE the credential " +
                          alias + " from CredentialProvider " + provider +
                          ". Continue? ");
        }

        if (overwrite) {
          // delete the existing credential
          DeleteCommand deleteCommand = new DeleteCommand();
          exitCode = deleteCommand.execute();
        } else {
          // nothing to do
          return 0;
        }
      }

      // create new or overwrite existing credential if delete succeeded
      if (exitCode == 0) {
        exitCode = createCredential();
      }

      return exitCode;
    }

    /**
     * Usage and description.
     * @return
     */
    @Override
    public String getUsage() {
      return USAGE + ":\n\n" + DESC;
    }

    /**
     * Creates the specified credential. A credential with the same alias
     * should not exist. It must be deleted before this method is called.
     *
     * @return - 0 on success; 1 on failure.
     * @throws Exception - If the alias already exists.
     */
    private int createCredential() throws Exception {
      int exitCode;
      List<String> args = new ArrayList<>();

      args.add("create");
      args.add(alias);
      if (value != null) {
        args.add("-value");
        args.add(value);
      }

      String[] toolArgs = args.toArray(new String[args.size()]);

      exitCode = ToolRunner.run(getConf(), new CredentialShell(), toolArgs);

      return exitCode;
    }
  }

  /**
   * Deletes the credential specified by the alias from the
   * specified provider.
   */
  private class DeleteCommand extends Command {
    public static final String USAGE =
            "delete <alias> [-f] [-provider provider-path]";
    public static final String DESC =
            "The delete subcommand deletes the credential specified\n" +
                    "as the <alias> argument from within the provider indicated\n" +
                    "through the -provider argument. The command asks for\n" +
                    "confirmation unless the -f option is specified.";

    /**
     * Deletes the specified alias. Prompts for user confirmation
     * if -f option is not specified.
     * @return
     * @throws Exception
     */
    @Override
    public int execute() throws Exception {
      int exitCode;
      List<String> args = new ArrayList<>();

      args.add("delete");
      args.add(alias);
      if (!interactive) {
        args.add("-f");
      }

      String[] toolArgs = args.toArray(new String[args.size()]);

      exitCode = ToolRunner.run(getConf(), new CredentialShell(), toolArgs);

      return exitCode;
    }

    /**
     * Usage and description.
     *
     * @return
     */
    @Override
    public String getUsage() {
      return USAGE + ":\n\n" + DESC;
    }
  }


  /**
   * Lists all the aliases contained in the specified provider.
   */
  private class ListCommand extends Command {
    /**
     * Command usage
     */
    public static final String USAGE = "list [-provider provider-path]";

    /**
     * Command description
     */
    public static final String DESC =
            "The list subcommand displays the aliases contained within \n" +
                    "a particular provider - as configured in core-site.xml or\n " +
                    "indicated through the -provider argument.";

    /**
     * Executes the list command.
     *
     * @return - 0 if successful; 1 otherwise.
     * @throws Exception - If something goes wrong.
     */
    @Override
    public int execute() throws Exception {
      int exitCode;
      List<String> args = new ArrayList<>();

      args.add("list");

      String[] toolArgs = args.toArray(new String[args.size()]);

      exitCode = ToolRunner.run(getConf(), new CredentialShell(), toolArgs);

      return exitCode;
    }

    /**
     * Usage and description.
     *
     * @return
     */
    @Override
    public String getUsage() {
      return USAGE + ":\n\n" + DESC;
    }
  }
}