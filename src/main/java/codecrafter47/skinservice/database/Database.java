package codecrafter47.skinservice.database;

import codecrafter47.skinservice.SkinInfo;
import codecrafter47.skinservice.util.ImageWrapper;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class Database {
    private final Sql2o sql2o;

    @Inject
    public Database(Sql2o sql2o) {
        this.sql2o = sql2o;
        createTables();
    }

    private void createTables() {
        try(Connection connection = sql2o.open()) {
            connection.createQuery("CREATE TABLE IF NOT EXISTS skins (id INTEGER AUTO_INCREMENT NOT NULL PRIMARY KEY, hashFace VARBINARY(64) UNIQUE NOT NULL, hashHead VARBINARY(64) UNIQUE NOT NULL, hash VARBINARY(64) UNIQUE NOT NULL, skinUrl TEXT NOT NULL, texturePropertyValue TEXT NOT NULL, texturePropertySignature TEXT NOT NULL)").executeUpdate();
            connection.createQuery("CREATE TABLE IF NOT EXISTS accounts (email VARCHAR(255) NOT NULL UNIQUE PRIMARY KEY, password TEXT NOT NULL, uuid TEXT NOT NULL)").executeUpdate();
        }
    }

    public SkinInfo getSkinWithFace(ImageWrapper image) {
        try(Connection connection = sql2o.open()) {
            return connection.createQuery("SELECT skinUrl, texturePropertyValue, texturePropertySignature FROM skins WHERE hashFace = :image")
                    .addParameter("image", image.sha512())
                    .executeAndFetch(SkinInfo.class)
                    .stream().findAny().orElse(null);
        }
    }

    public SkinInfo getSkinWithHead(ImageWrapper image) {
        try(Connection connection = sql2o.open()) {
            return connection.createQuery("SELECT skinUrl, texturePropertyValue, texturePropertySignature FROM skins WHERE hashHead = :image")
                    .addParameter("image", image.sha512())
                    .executeAndFetch(SkinInfo.class)
                    .stream().findAny().orElse(null);
        }
    }

    public SkinInfo getSkin(ImageWrapper image) {
        try(Connection connection = sql2o.open()) {
            return connection.createQuery("SELECT skinUrl, texturePropertyValue, texturePropertySignature FROM skins WHERE hash = :image")
                    .addParameter("image", image.sha512())
                    .executeAndFetch(SkinInfo.class)
                    .stream().findAny().orElse(null);
        }
    }

    public void saveSkin(ImageWrapper skin, String skinURL, String texturePropertyValue, String texturePropertySignature) {
        try(Connection connection = sql2o.open()) {
            connection.createQuery("INSERT INTO skins (hashFace, hashHead, hash, skinUrl, texturePropertyValue, texturePropertySignature) VALUES (:hashFace, :hashHead, :hash, :skinUrl, :texturePropertyValue, :texturePropertySignature)")
                    .addParameter("hashFace", skin.getSubimage(8, 8, 8, 8).sha512())
                    .addParameter("hashHead", skin.getSubimage(0, 0, 64, 16).sha512())
                    .addParameter("hash", skin.sha512())
                    .addParameter("skinUrl", skinURL)
                    .addParameter("texturePropertyValue", texturePropertyValue)
                    .addParameter("texturePropertySignature", texturePropertySignature)
                    .executeUpdate();
        }
    }

    public List<MinecraftAccount> getAccounts() {
        try(Connection connection = sql2o.open()) {
            return connection.createQuery("SELECT * FROM accounts")
                    .executeAndFetch(MinecraftAccount.class);
        }
    }

    public void addAccount(MinecraftAccount account) {
        try(Connection connection = sql2o.open()) {
            connection.createQuery("INSERT INTO accounts (email, password, uuid) VALUES (:email, :password, :uuid)")
                    .addParameter("email", account.getEmail())
                    .addParameter("password", account.getPassword())
                    .addParameter("uuid", account.getUuid())
                    .executeUpdate();
        }
    }

    public void removeAccount(String email) {
        try(Connection connection = sql2o.open()) {
            connection.createQuery("DELETE FROM accounts WHERE email = :email")
                    .addParameter("email", email)
                    .executeUpdate();
        }
    }
}
