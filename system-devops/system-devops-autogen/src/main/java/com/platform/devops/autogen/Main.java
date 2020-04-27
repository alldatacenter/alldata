package com.platform.devops.autogen;

import com.platform.devops.autogen.impl.ScalaPlayImpl;
import org.apache.commons.cli.*;


/**
 * Created by wulinhao on 2020/04/12.
 */
public class Main {

    public static void main(String[] args) {

        Options options = new Options();

        Option type = new Option("t", "type", true, "type shoule be scala-play java-jar");
        type.setRequired(true);
        options.addOption(type);


        Option dir = new Option("d", "dir", true, "build dir");
        dir.setRequired(true);
        options.addOption(dir);

        Option name = new Option("n", "name", true, "project name");
        name.setRequired(true);
        options.addOption(name);

        Option database = new Option("db", "db_url", true, "the database want to connect");
        database.setRequired(false);
        options.addOption(database);

        Option user = new Option("u", "db_user", true, "the database want to connect");
        user.setRequired(false);
        options.addOption(user);

        Option pass = new Option("p", "db_pass", true, "the database want to connect");
        pass.setRequired(false);
        options.addOption(pass);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        InputParams ip = null;

        try {
            cmd = parser.parse(options, args);
            ip = getParams(cmd);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        try {
            run(ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void run(InputParams ip) throws Exception {
        TmpBuilder builder = null;
        System.out.println(ip.type);
        if (ip.type.equals("scala-play")) {
            builder = new ScalaPlayImpl(ip);
        }
        if (builder != null) {
            builder.build();
        } else {
            throw new Exception("not found builder with type" + ip.type);
        }
    }

    static InputParams getParams(CommandLine cmd) {
        InputParams ip = new InputParams();
        ip.type = cmd.getOptionValue("type");
        ip.dir = cmd.getOptionValue("dir");
        ip.name = cmd.getOptionValue("name");
        ip.dbUrl = cmd.getOptionValue("db_url");
        ip.dbUser = cmd.getOptionValue("db_user");
        ip.dbPass = cmd.getOptionValue("db_pass");
        return ip;
    }

}
