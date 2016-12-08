package hu.rkoszegi.jrasmus.dao;

import hu.rkoszegi.jrasmus.DatabaseManager;
import hu.rkoszegi.jrasmus.handler.BaseHandler;
import hu.rkoszegi.jrasmus.model.StoredFile;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by rkoszegi on 09/11/2016.
 */
public class StoredFileDAO {

    private EntityManager em;

    public StoredFileDAO() {
        this.em = DatabaseManager.getEntityManager();
    }

    public void persist(StoredFile storedFile) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(storedFile);
        tx.commit();
    }

    public List<StoredFile> getAllStoredFile() {
        String query = "SELECT f FROM StoredFile f";
        Query q = em.createQuery(query, StoredFile.class);
        return q.getResultList();
    }

    public void deleteByReference(StoredFile storedFile) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.remove(storedFile);
            tx.commit();
    }

    public List<StoredFile> findByHandler(BaseHandler handler) {
        String query = "SELECT f FROM StoredFile f WHERE f.handler = :handler";
        Query q = em.createQuery(query, StoredFile.class);
        q.setParameter("handler", handler);
        return q.getResultList();
    }
}
