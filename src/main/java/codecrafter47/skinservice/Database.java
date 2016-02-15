package codecrafter47.skinservice;

import codecrafter47.skinservice.database.Head;
import codecrafter47.skinservice.database.MinecraftAccount;
import codecrafter47.skinservice.database.Model;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class Database implements Model{
    private final Sql2o sql2o;

    @Inject
    public Database(Sql2o sql2o) {
        this.sql2o = sql2o;
        createTables();
    }

    private void createTables() {
        try(Connection connection = sql2o.open()) {
            connection.createQuery("CREATE TABLE IF NOT EXISTS skins (image BLOB NOT NULL, skin TEXT NOT NULL, signature TEXT NOT NULL, PRIMARY KEY (image(256)))").executeUpdate();
            connection.createQuery("CREATE TABLE IF NOT EXISTS accounts (email VARCHAR(255) NOT NULL UNIQUE PRIMARY KEY, password TEXT NOT NULL, uuid TEXT NOT NULL)").executeUpdate();
        }
    }

    @Override
    public Head getHead(byte[] image) {
        try(Connection connection = sql2o.open()) {
            return connection.createQuery("SELECT * FROM skins WHERE image = :image")
                    .addParameter("image", image)
                    .executeAndFetch(Head.class)
                    .stream().findAny().orElse(null);
        }
    }

    @Override
    public void saveHead(byte[] image, String skin, String signature) {
        try(Connection connection = sql2o.open()) {
            connection.createQuery("INSERT INTO skins (image, skin, signature) VALUES (:image, :skin, :signature)")
                    .addParameter("image", image)
                    .addParameter("skin", skin)
                    .addParameter("signature", signature)
                    .executeUpdate();
        }
    }

    @Override
    public List<MinecraftAccount> getAccounts() {
        try(Connection connection = sql2o.open()) {
            return connection.createQuery("SELECT * FROM accounts")
                    .executeAndFetch(MinecraftAccount.class);
        }
    }

    @Override
    public void addAccount(MinecraftAccount account) {
        try(Connection connection = sql2o.open()) {
            connection.createQuery("INSERT INTO accounts (email, password, uuid) VALUES (:email, :password, :uuid)")
                    .addParameter("email", account.getEmail())
                    .addParameter("password", account.getPassword())
                    .addParameter("uuid", account.getUuid())
                    .executeUpdate();
        }
    }

    @Override
    public void removeAccount(String email) {
        try(Connection connection = sql2o.open()) {
            connection.createQuery("DELETE FROM accounts WHERE email = :email")
                    .addParameter("email", email)
                    .executeUpdate();
        }
    }
}
