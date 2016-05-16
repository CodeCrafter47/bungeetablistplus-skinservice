package codecrafter47.skinservice.response;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ResponseError {
    private final String state = "ERROR";
    private final String errorMessage;
}
