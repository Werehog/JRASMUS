package hu.rkoszegi.jrasmus.dao;

import hu.rkoszegi.jrasmus.DatabaseManager;
import hu.rkoszegi.jrasmus.model.StoredFile;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by rkoszegi on 09/11/2016.
 */
public class StoredFileDAO {

    //@PersistenceContext(unitName = "JrasmusPersistenceUnit")
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

    public List<StoredFile> findByName(String name) {
        String query = "SELECT f FROM StoredFile f WHERE f.name = :pName";
        Query q = em.createQuery(query, StoredFile.class);
        q.setParameter("pName", name);
        return q.getResultList();
    }

    public void deleteByName(String name) {
        List<StoredFile> customers = this.findByName(name);
        if (customers != null && !customers.isEmpty()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.remove(customers.get(0));
            tx.commit();
        }
    }

    public void deleteByReference(StoredFile storedFile) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.remove(storedFile);
            tx.commit();
    }
}
