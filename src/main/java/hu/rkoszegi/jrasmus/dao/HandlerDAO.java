package hu.rkoszegi.jrasmus.dao;

import hu.rkoszegi.jrasmus.DatabaseManager;
import hu.rkoszegi.jrasmus.handler.BaseHandler;
import hu.rkoszegi.jrasmus.model.StoredFile;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by rkoszegi on 13/11/2016.
 */
public class HandlerDAO {

    //@PersistenceContext(unitName = "JrasmusPersistenceUnit")
    private EntityManager em;

    public HandlerDAO() {
        this.em = DatabaseManager.getEntityManager();
    }

    public void persist(BaseHandler handler) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(handler);
        tx.commit();
    }

    public List<BaseHandler> getAllStoredHandler() {
        String query = "SELECT h FROM Handler h";
        Query q = em.createQuery(query, BaseHandler.class);
        return q.getResultList();
    }

   /* public List<StoredFile> findByName(String name) {
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
    }*/
}
