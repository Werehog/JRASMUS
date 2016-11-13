package hu.rkoszegi.jrasmus;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Created by rkoszegi on 09/11/2016.
 */
public class DatabaseManager {

    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;

    public static void initDB() {
        entityManagerFactory = Persistence.createEntityManagerFactory("JrasmusPersistenceUnit");
        entityManager = entityManagerFactory.createEntityManager();
    }

    public static void closeDB() {
        entityManager.close();
    }

    public static EntityManager getEntityManager() {
        return entityManager;
    }
}
