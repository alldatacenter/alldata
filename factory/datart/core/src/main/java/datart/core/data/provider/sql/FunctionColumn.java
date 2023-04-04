package datart.core.data.provider.sql;

import lombok.Data;

@Data
public class FunctionColumn implements Alias {

    private String alias;

    private String snippet;

    @Override
    public String toString() {
        return "FunctionColumn{" +
                "alias='" + alias + '\'' +
                ", snippet='" + snippet + '\'' +
                '}';
    }
}
