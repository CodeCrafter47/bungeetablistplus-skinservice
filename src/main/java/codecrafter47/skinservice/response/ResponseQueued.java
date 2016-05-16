package codecrafter47.skinservice.response;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ResponseQueued {
    private final String state = "QUEUED";
    private final int timeLeft;
}
