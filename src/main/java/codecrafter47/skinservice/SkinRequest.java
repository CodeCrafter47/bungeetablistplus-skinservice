package codecrafter47.skinservice;

import codecrafter47.skinservice.util.ImageWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SkinRequest {
    private final ImageWrapper image;
    private final String ip;
    private boolean finished = false;
    private boolean error = false;
    private int timeLeft = 1;
    private SkinInfo result = null;
}
