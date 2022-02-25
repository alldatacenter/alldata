/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.tools.jce;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import javax.crypto.Cipher;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

/**
 * JcePolicyInfo provides information about the JVM's installed JCE (Java Cryptology Enhancements)
 * policy.
 */
public class JcePolicyInfo {

  public static void main(String[] args) throws Exception {
    try {
      boolean showHelp = true;
      CommandLine cli = new DefaultParser().parse(options(), args);
      if (cli.hasOption("lc")) {
        listCiphers();
        showHelp = false;
      }

      if (cli.hasOption("tu")) {
        testUnlimitedKeyJCEPolicy();
        showHelp = false;
      }

      if (showHelp) {
        printHelp(null);
      }
    } catch (UnrecognizedOptionException e) {
      printHelp(e);
    }

  }

  private static void printHelp(UnrecognizedOptionException exception) {
    HelpFormatter helpFormatter = new HelpFormatter();

    if (exception == null) {
      helpFormatter.printHelp("jcepolicyinfo [options]", options());
    } else {
      helpFormatter.printHelp("jcepolicyinfo [options]", exception.getLocalizedMessage(), options(), null);
    }

    System.exit(1);
  }

  private static Options options() {
    return new Options()
        .addOption(Option.builder("h")
            .longOpt("help")
            .desc("print help")
            .build())
        .addOption(Option.builder("tu")
            .longOpt("test_unlimited")
            .desc("Test's the policy for unlimited key encryption")
            .hasArg(false)
            .argName("tu")
            .build())
        .addOption(Option.builder("lc")
            .longOpt("list_ciphers")
            .desc("List the ciphers allowed by the policy")
            .hasArg(false)
            .argName("lc")
            .build());
  }

  /**
   * Test if the JCE policy supports unlimited keys
   */
  private static void testUnlimitedKeyJCEPolicy() {
    System.out.print("Unlimited Key JCE Policy: ");

    try {
      boolean unlimited = Cipher.getMaxAllowedKeyLength("RC5") >= 256;
      System.out.println(unlimited);

      // If the unlimited key JCE policy is installed exit with a 0 since that indicates a non-error;
      // If the unlimited key JCE policy is not installed exit with a 1
      System.exit(unlimited ? 0 : 1);
    } catch (NoSuchAlgorithmException e) {
      System.out.println("unknown [error]");
      System.exit(-1);
    }
  }

  /**
   * Display the list of available ciphers and their maximum suported key lengths.
   */
  private static void listCiphers() {
    System.out.println("Available ciphers:");

    for (Provider provider : Security.getProviders()) {
      String providerName = provider.getName();

      for (Provider.Service service : provider.getServices()) {
        String algorithmName = service.getAlgorithm();

        if ("Cipher".equalsIgnoreCase(service.getType())) {
          try {
            long keylength = Cipher.getMaxAllowedKeyLength(algorithmName);

            System.out.print('\t');
            System.out.print(providerName.toLowerCase());
            System.out.print('.');
            System.out.print(algorithmName.toLowerCase());
            System.out.print(": ");
            System.out.println(keylength);
          } catch (NoSuchAlgorithmException e) {
            // This is unlikely since we are getting the algorithm names from the service providers.
            // In any case, if a bad algorithm is listed it can be skipped since this is only for
            // informational purposes.
          }
        }
      }
    }
  }
}
