package com.alibaba.tesla.appmanager.server.factory;

import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateSyntaxException;
import com.hubspot.jinjava.lib.filter.Filter;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Jinja Client Factory
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class JinjaFactory {

    /**
     * Jinja Client
     */
    private static Jinjava jinjava;

    private JinjaFactory() {
    }

    /**
     * 获取一个普通的 Http Client (无代理)
     *
     * @return OkHttpClient
     */
    public static Jinjava getJinjava() {
        if (jinjava == null) {
            synchronized (JinjaFactory.class) {
                if (jinjava == null) {
                    jinjava = new Jinjava();
                    jinjava.getGlobalContext().registerFilter(new DictRenderFilter());
                    jinjava.getGlobalContext().registerFilter(new YamlRenderFilter());
                }
            }
        }
        return jinjava;
    }

    public static class DictRenderFilter implements Filter {

        private final Yaml yaml;

        public DictRenderFilter() {
            yaml = SchemaUtil.createYaml(Object.class);
        }

        @Override
        public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
            if (args.length < 1) {
                throw new TemplateSyntaxException(interpreter, getName(), "requires 1 argument (indent)");
            }
            int indent = Integer.parseInt(args[0]);

            if (var == null) {
                return null;
            }
            String preSpace = String.join("", Collections.nCopies(indent, " "));
            String yamlContent = yaml.dump(var);
            return "\n" + Arrays.stream(yamlContent.split("\n"))
                    .map(line -> preSpace + line)
                    .collect(Collectors.joining("\n"));
        }

        @Override
        public String getName() {
            return "dict_render";
        }
    }

    public static class YamlRenderFilter implements Filter {

        private final Yaml yaml;

        public YamlRenderFilter() {
            yaml = SchemaUtil.createYaml(Object.class);
        }

        @Override
        public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
            if (args.length < 1) {
                throw new TemplateSyntaxException(interpreter, getName(), "requires 1 argument (indent)");
            }
            int indent = Integer.parseInt(args[0]);

            if (var == null) {
                return null;
            }
            String preSpace = String.join("", Collections.nCopies(indent, " "));
            String yamlContent = yaml.dump(var);
            return Arrays.stream(yamlContent.split("\n"))
                    .map(line -> preSpace + line)
                    .collect(Collectors.joining("\n"))
                    .substring(indent);
        }

        @Override
        public String getName() {
            return "yaml_render";
        }
    }
}
