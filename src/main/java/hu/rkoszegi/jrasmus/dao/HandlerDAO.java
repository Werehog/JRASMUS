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
        String query = "SELECT h FROM BaseHandler h";
        Query q = em.createQuery(query, BaseHandler.class);
        return q.getResultList();
    }

   public List<BaseHandler> findByName(String label) {
        String query = "SELECT h FROM BaseHandler h WHERE h.label = :pName";
        Query q = em.createQuery(query, StoredFile.class);
        q.setParameter("pName", label);
        return q.getResultList();
    }

    public void deleteByName(String name) {
        List<BaseHandler> handler = this.findByName(name);
        if (handler != null && !handler.isEmpty()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.remove(handler.get(0));
            tx.commit();
        }
    }
}
