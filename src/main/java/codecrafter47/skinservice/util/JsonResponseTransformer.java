package codecrafter47.skinservice.util;

import com.google.gson.Gson;
import lombok.Getter;
import spark.ResponseTransformer;

public class JsonResponseTransformer implements ResponseTransformer {

    @Getter
    private static final JsonResponseTransformer instance = new JsonResponseTransformer();

    private static final ThreadLocal<Gson> gson = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return new Gson();
        }
    };

    private JsonResponseTransformer() {
    }

    @Override
    public String render(Object o) {
        return gson.get().toJson(o);
    }
}
