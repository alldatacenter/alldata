package datart;


import datart.core.common.ClassTransformer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"datart"})
public class DatartServerApplication {

    public static void main(String[] args) {
        ClassTransformer.transform();
        SpringApplication.run(DatartServerApplication.class, args);
    }

}