package datart.data.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"datart.core.mappers","datart.core.common"})
public class DataProviderTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataProviderTestApplication.class);
    }
}
