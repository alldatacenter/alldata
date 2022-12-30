package datart.core.data.provider.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FunArg {

    private FunArgType type;

    private Object value;

}
