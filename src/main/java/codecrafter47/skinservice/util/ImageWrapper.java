package codecrafter47.skinservice.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

@AllArgsConstructor
public class ImageWrapper {

    @Getter
    private final BufferedImage image;

    public static ImageWrapper createTransparentImage(int w, int h) {
        BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                bufferedImage.setRGB(x, y, 0xffffffff);
            }
        }
        return new ImageWrapper(bufferedImage);
    }

    /**
     * Returns the width of the <code>BufferedImage</code>.
     *
     * @return the width of this <code>BufferedImage</code>
     */
    public int getWidth() {
        return image.getWidth();
    }

    /**
     * Returns the height of the <code>BufferedImage</code>.
     *
     * @return the height of this <code>BufferedImage</code>
     */
    public int getHeight() {
        return image.getHeight();
    }

    public ImageWrapper getSubimage(int x, int y, int w, int h) {
        return new ImageWrapper(image.getSubimage(x, y, w, h));
    }

    public void drawImage(ImageWrapper image, int x, int y) {
        int[] rgb = new int[image.getWidth() * image.getHeight()];
        image.getImage().getRGB(0, 0, image.getWidth(), image.getHeight(), rgb, 0, image.getWidth());
        getImage().setRGB(x, y, image.getWidth(), image.getHeight(), rgb, 0, image.getWidth());
    }

    public void setAreaTransparent(int x, int y, int w, int h) {
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                image.setRGB(x + dx, y + dy, 0xffffffff);
            }
        }
    }

    public byte[] toByteArray() {
        int[] rgb = image.getRGB(0, 0, getWidth(), getHeight(), null, 0, getWidth());
        ByteBuffer byteBuffer = ByteBuffer.allocate(rgb.length * 4);
        byteBuffer.asIntBuffer().put(rgb);
        return byteBuffer.array();
    }

    @SneakyThrows
    public byte[] sha512() {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        return digest.digest(toByteArray());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ImageWrapper)) {
            return false;
        }
        if (super.equals(obj)) {
            return true;
        }
        BufferedImage other = ((ImageWrapper) obj).getImage();
        if (other.getWidth() != image.getWidth()
                || other.getHeight() != image.getHeight()) {
            return false;
        }
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (image.getRGB(x, y) != other.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int value = image.getWidth() * image.getHeight();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                value += (((x + 1) * (y + 1)) % 113) * image.getRGB(x, y);
            }
        }
        return value;
    }
}
