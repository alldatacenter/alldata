package org.dromara.cloudeon.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class YamlTest {
    @Test
    public void testLoad() {
        String yamlStr = "key: hello yaml";
        Yaml yaml = new Yaml();
        Object ret = yaml.load(yamlStr);
        System.out.println(ret);
    }


    @Test
    public void yaml2POJO() throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(new File("/Volumes/Samsung_T5/opensource/PowerJob/render_templates/customer.yaml"));
        Customer customer = yaml.loadAs(inputStream, Customer.class);
        System.out.println(customer);
    }

    @Test
    public void POJO2yaml() throws IOException {
        Customer customer = new Customer();
        customer.setAge(45);
        customer.setFirstName("Greg");
        customer.setLastName("McDowell");
        customer.setContactDetails(Lists.newArrayList(new Contact("mobile",123123),new Contact("home",8989734)));
        customer.setHomeAddress(new Address("jinju","gz","gd",34));
        Yaml yaml = new Yaml();
        System.out.println(yaml.dumpAs(customer, Tag.MAP, null));
        System.out.println(yaml.dumpAsMap(customer));

    }

    @Test
    public void map2yaml() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("name", "Silenthand Olleander");
        data.put("race", "Human");
        data.put("traits", new String[] { "ONE_HAND", "ONE_EYE" });
        Yaml yaml = new Yaml();
        StringWriter writer = new StringWriter();
        yaml.dump(data, writer);
        System.out.println(writer.toString());

    }

    @Data
    @ToString
    public static class Customer {
        private String firstName;
        private String lastName;
        private int age;
        private List<Contact> contactDetails;
        private Address homeAddress;

    }

    @Data
    @AllArgsConstructor
    @ToString
    public static class Contact {
        private String type;
        private int number;

    }

    @Data
    @ToString
    @AllArgsConstructor
    public static class Address {
        private String line;
        private String city;
        private String state;
        private Integer zip;

    }

}
