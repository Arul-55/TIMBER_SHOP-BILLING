package com.timbershop.dao;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.timbershop.entity.BillItem;
import com.timbershop.util.HibernateUtil;

public class BillItemDAO {

    // CREATE - Add a bill item
    public void addBillItem(BillItem billItem) {

        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            session.persist(billItem);

            transaction.commit();

            System.out.println("Bill item added successfully!");

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            e.printStackTrace();
        }
    }
 // READ - Display all bill items
    public void viewAllBillItems() {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            java.util.List<BillItem> billItems =
                    session.createSelectionQuery("from BillItem", BillItem.class)
                           .getResultList();

            for (BillItem billItem : billItems) {
                System.out.println(billItem);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 // SEARCH - Find bill items by bill ID
    public void searchBillItemsByBillId(int billId) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            java.util.List<BillItem> billItems =
                    session.createSelectionQuery(
                            "from BillItem where billId = :billId",
                            BillItem.class)
                           .setParameter("billId", billId)
                           .getResultList();

            if (billItems.isEmpty()) {

                System.out.println("No bill items found!");

            } else {

                System.out.println("Bill items found:");

                for (BillItem billItem : billItems) {
                    System.out.println(billItem);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}