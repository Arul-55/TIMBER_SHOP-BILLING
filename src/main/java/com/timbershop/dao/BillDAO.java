package com.timbershop.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.timbershop.entity.Bill;
import com.timbershop.util.HibernateUtil;

public class BillDAO {

    // CREATE - Add a new bill
    public void addBill(Bill bill) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            session.persist(bill);

            transaction.commit();
            System.out.println("Bill added successfully!");

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            System.out.println("Error while adding bill.");
            e.printStackTrace();
        }
    }

    // READ - View all bills
    public void viewAllBills() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            List<Bill> bills = session.createSelectionQuery(
                    "from Bill order by billDate desc",
                    Bill.class
            ).getResultList();

            if (bills.isEmpty()) {
                System.out.println("No bills found!");
                return;
            }

            System.out.println("\n========== ALL BILLS ==========");

            for (Bill bill : bills) {
                System.out.println(bill);
            }

        } catch (Exception e) {
            System.out.println("Error while viewing bills.");
            e.printStackTrace();
        }
    }

    // SEARCH - Find a bill by ID
    public void searchBillById(int billId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Bill bill = session.get(Bill.class, billId);

            if (bill == null) {
                System.out.println("Bill not found!");
                return;
            }

            System.out.println("\n======================================");
            System.out.println("              BILL DETAILS");
            System.out.println("======================================");
            System.out.println("Bill ID        : " + bill.getId());
            System.out.println("Customer ID    : " + bill.getCustomerId());
            System.out.println("Bill Date      : " + bill.getBillDate());
            System.out.println("--------------------------------------");

            System.out.printf("Subtotal       : Rs. %.2f%n",
                    bill.getSubtotal());

            System.out.printf("Discount       : Rs. %.2f%n",
                    bill.getDiscountAmount());

            System.out.printf("GST Rate       : %.2f%%%n",
                    bill.getGstRate());

            System.out.printf("GST Amount     : Rs. %.2f%n",
                    bill.getGstAmount());

            System.out.println("--------------------------------------");

            System.out.printf("FINAL TOTAL    : Rs. %.2f%n",
                    bill.getTotalAmount());

            System.out.println("======================================");

        } catch (Exception e) {
            System.out.println("Error while searching for bill.");
            e.printStackTrace();
        }
    }

    // SEARCH - Find bills by customer ID
    public void searchBillsByCustomerId(int customerId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            List<Bill> bills = session.createSelectionQuery(
                    "from Bill where customerId = :customerId "
                            + "order by billDate desc",
                    Bill.class
            ).setParameter("customerId", customerId)
             .getResultList();

            if (bills.isEmpty()) {
                System.out.println("No bills found for this customer!");
                return;
            }

            System.out.println("\n========== CUSTOMER BILLS ==========");

            for (Bill bill : bills) {
                System.out.println(bill);
            }

        } catch (Exception e) {
            System.out.println("Error while searching customer bills.");
            e.printStackTrace();
        }
    }

    // REPORT - View sales history
    public void viewSalesHistory() {
        System.out.println("\n========== SALES HISTORY ==========");
        viewAllBills();
    }

    // REPORT - Calculate total sales
    public void viewTotalSales() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Double total = session.createSelectionQuery(
                    "select sum(totalAmount) from Bill",
                    Double.class
            ).getSingleResult();

            double totalSales = (total == null) ? 0.0 : total;

            System.out.println("\n========== TOTAL SALES ==========");
            System.out.printf("Total Sales Amount: Rs. %.2f%n", totalSales);

        } catch (Exception e) {
            System.out.println("Error while calculating total sales.");
            e.printStackTrace();
        }
    }

    // REPORT - Calculate total sales for one customer
    public void viewCustomerTotalSales(int customerId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Double total = session.createSelectionQuery(
                    "select sum(totalAmount) from Bill "
                            + "where customerId = :customerId",
                    Double.class
            ).setParameter("customerId", customerId)
             .getSingleResult();

            double customerSales = (total == null) ? 0.0 : total;

            System.out.println("\n========== CUSTOMER SALES ==========");
            System.out.println("Customer ID: " + customerId);

            System.out.printf("Total Sales: Rs. %.2f%n", customerSales);

        } catch (Exception e) {
            System.out.println("Error while calculating customer sales.");
            e.printStackTrace();
        }
    }
}