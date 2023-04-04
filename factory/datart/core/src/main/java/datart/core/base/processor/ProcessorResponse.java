package datart.core.base.processor;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProcessorResponse {

    private boolean success;
    private int errCode;
    private String message;

    public static ProcessorResponse success() {
        return ProcessorResponse.builder()
                .success(true)
                .build();
    }

    public static ProcessorResponse failure(int errCode,String message) {
        return ProcessorResponse.builder()
                .success(false)
                .errCode(errCode)
                .message(message)
                .build();
    }
}
