package codecrafter47.skinservice.database;

import java.util.List;

public interface Model {
    Head getHead(byte[] image);
    void saveHead(byte[] image, String skin, String signature);
    List<MinecraftAccount> getAccounts();
    void addAccount(MinecraftAccount account);
    void removeAccount(String email);
}
