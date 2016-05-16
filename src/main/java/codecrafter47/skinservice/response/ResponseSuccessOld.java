package codecrafter47.skinservice.response;

import codecrafter47.skinservice.SkinInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ResponseSuccessOld {
    private final String state = "SUCCESS";
    private final String skin;
    private final String signature;

    public ResponseSuccessOld(SkinInfo skinInfo) {
        this.skin = skinInfo.getTexturePropertyValue();
        this.signature = skinInfo.getTexturePropertySignature();
    }
}
