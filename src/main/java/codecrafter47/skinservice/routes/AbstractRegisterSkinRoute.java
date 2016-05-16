package codecrafter47.skinservice.routes;

import codecrafter47.skinservice.SkinManager;
import codecrafter47.skinservice.SkinRequest;
import codecrafter47.skinservice.response.ResponseQueued;
import codecrafter47.skinservice.response.ResponseSuccess;
import codecrafter47.skinservice.util.ImageWrapper;
import codecrafter47.skinservice.util.InternalServerException;
import codecrafter47.skinservice.util.RequestException;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;

public abstract class AbstractRegisterSkinRoute implements Route {

    private final SkinManager skinManager;

    protected AbstractRegisterSkinRoute(SkinManager skinManager) {
        this.skinManager = skinManager;
    }

    /**
     * Invoked when a request is made on this route's corresponding path e.g. '/hello'
     *
     * @param request  The request object providing information about the HTTP request
     * @param response The response object providing functionality for modifying the response
     * @return The content to be set in the response
     * @throws Exception
     */
    @Override
    public Object handle(Request request, Response response) throws Exception {
        if (request.raw().getAttribute("org.eclipse.jetty.multipartConfig") == null) {
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
            request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
        }

        Part part = request.raw().getPart("file");

        if (part == null) {
            throw new RequestException("No file uploaded.");
        }

        BufferedImage image = ImageIO.read(part.getInputStream());

        return handle(request, response, new ImageWrapper(image));
    }

    protected abstract Object handle(Request request, Response response, ImageWrapper image) throws Exception;

    protected Object handleRegisterSkin(Request request, Response response, ImageWrapper skin) throws Exception {
        if (skin.getWidth() != 64 || skin.getHeight() != 64) {
            throw new RequestException("Skin must be 64x64 px.");
        }

        SkinRequest skinRequest = skinManager.requestSkin(skin, request.ip());

        if (skinRequest.isError()) {
            throw new InternalServerException("Unknown error");
        }

        if (skinRequest.isFinished()) {
            return new ResponseSuccess(skinRequest.getResult());
        }

        return new ResponseQueued(skinRequest.getTimeLeft());
    }
}
