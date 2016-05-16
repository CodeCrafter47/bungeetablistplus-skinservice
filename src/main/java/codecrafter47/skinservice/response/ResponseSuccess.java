package codecrafter47.skinservice.response;

import codecrafter47.skinservice.SkinInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ResponseSuccess {
    private final String state = "SUCCESS";
    private final String texturePropertyValue;
    private final String texturePropertySignature;
    private final String skinUrl;

    public ResponseSuccess(SkinInfo skinInfo) {
        this.texturePropertyValue = skinInfo.getTexturePropertyValue();
        this.texturePropertySignature = skinInfo.getTexturePropertySignature();
        this.skinUrl = skinInfo.getSkinURL();
    }
}
