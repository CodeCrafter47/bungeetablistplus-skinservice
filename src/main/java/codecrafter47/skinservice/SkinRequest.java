package codecrafter47.skinservice;

import codecrafter47.skinservice.util.ImageWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SkinRequest {
    private final ImageWrapper image;
    private final String ip;
    private volatile boolean finished = false;
    private volatile boolean error = false;
    private int timeLeft = 1;
    private volatile SkinInfo result = null;
}
