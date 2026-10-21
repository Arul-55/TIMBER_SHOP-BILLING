package com.timbershop.dao;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.timbershop.entity.Customer;
import com.timbershop.util.HibernateUtil;

public class CustomerDAO {

    // CREATE
    public void addCustomer(Customer customer) {

        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            session.persist(customer);

            transaction.commit();

            System.out.println("Customer added successfully!");

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            e.printStackTrace();
        }
    }
 // READ - Display all customers
    public void viewAllCustomers() {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            java.util.List<Customer> customers =
                    session.createSelectionQuery("from Customer", Customer.class)
                           .getResultList();

            for (Customer customer : customers) {
                System.out.println(customer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 // SEARCH - Find customer by phone number
    public void searchCustomer(String phone) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Customer customer = session.createSelectionQuery(
                    "from Customer where phone = :phone",
                    Customer.class)
                    .setParameter("phone", phone)
                    .getSingleResultOrNull();

            if (customer != null) {
                System.out.println("Customer found:");
                System.out.println(customer);
            } else {
                System.out.println("Customer not found!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 // UPDATE - Update customer details
    public void updateCustomer(int id, String newName, String newPhone, String newAddress) {

        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            Customer customer = session.get(Customer.class, id);

            if (customer != null) {

                customer.setName(newName);
                customer.setPhone(newPhone);
                customer.setAddress(newAddress);

                transaction.commit();

                System.out.println("Customer updated successfully!");

            } else {

                System.out.println("Customer not found!");

                transaction.rollback();
            }

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            e.printStackTrace();
        }
    }
 // DELETE - Delete customer by ID
    public void deleteCustomer(int id) {

        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            Customer customer = session.get(Customer.class, id);

            if (customer != null) {

                session.remove(customer);

                transaction.commit();

                System.out.println("Customer deleted successfully!");

            } else {

                System.out.println("Customer not found!");

                transaction.rollback();
            }

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            e.printStackTrace();
        }
    }
}