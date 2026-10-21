

package com.timbershop.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.timbershop.entity.Bill;
import com.timbershop.entity.BillItem;
import com.timbershop.entity.Customer;
import com.timbershop.entity.Timber;
import com.timbershop.util.HibernateUtil;

public class BillingService {

    private static final double GST_RATE = 10.0;
    private static final double DISCOUNT_RATE = 0.0;

    public static class SaleItem {
        private final int timberId;
        private final double quantity;

        public SaleItem(int timberId, double quantity) {
            this.timberId = timberId;
            this.quantity = quantity;
        }

        public int getTimberId() {
            return timberId;
        }

        public double getQuantity() {
            return quantity;
        }
    }

    private double roundMoney(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    public double calculateItemAmount(double quantity, double pricePerCft) {
        return roundMoney(quantity * pricePerCft);
    }

    public boolean checkStock(double availableStock, double requestedQuantity) {
        return requestedQuantity <= availableStock;
    }

    public double calculateRemainingStock(double availableStock, double soldQuantity) {
        return roundMoney(availableStock - soldQuantity);
    }

    public double calculateDiscount(double subtotal) {
        return roundMoney(subtotal * DISCOUNT_RATE / 100.0);
    }

    public double calculateGST(double subtotal, double discountAmount) {
        double taxableAmount = subtotal - discountAmount;
        return roundMoney(taxableAmount * GST_RATE / 100.0);
    }

    public double calculateFinalAmount(
            double subtotal, double discountAmount, double gstAmount) {
        return roundMoney(subtotal - discountAmount + gstAmount);
    }

    // Original single-timber billing method.
    public void processSale(int customerId, int timberId, double quantity) {
        List<SaleItem> items = new ArrayList<>();
        items.add(new SaleItem(timberId, quantity));
        processMultiItemSale(customerId, items);
    }

    // Create one bill for multiple timber types.
    public void processMultiItemSale(int customerId, List<SaleItem> items) {

        if (items == null || items.isEmpty()) {
            System.out.println("Please add at least one timber item.");
            return;
        }

        Transaction transaction = null;
        int billId = 0;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            Customer customer = session.get(Customer.class, customerId);

            if (customer == null) {
                throw new IllegalArgumentException(
                        "Customer ID not found: " + customerId);
            }

            // Combine repeated timber IDs.
            Map<Integer, Double> quantities = new HashMap<>();

            for (SaleItem item : items) {

                if (item == null || item.getTimberId() <= 0) {
                    throw new IllegalArgumentException("Invalid timber ID.");
                }

                double quantity = item.getQuantity();

                if (!Double.isFinite(quantity) || quantity <= 0) {
                    throw new IllegalArgumentException(
                            "Quantity must be a positive number.");
                }

                quantities.merge(item.getTimberId(), quantity, Double::sum);
            }

            Map<Integer, Timber> timberMap = new HashMap<>();
            double subtotal = 0.0;

            // Validate timber records and stock.
            for (Map.Entry<Integer, Double> entry : quantities.entrySet()) {

                int timberId = entry.getKey();
                double requestedQuantity = entry.getValue();

                Timber timber = session.get(Timber.class, timberId);

                if (timber == null) {
                    throw new IllegalArgumentException(
                            "Timber ID not found: " + timberId);
                }

                double price = timber.getPricePerCft();
                double stock = timber.getStock();

                if (!Double.isFinite(price) || price < 0) {
                    throw new IllegalArgumentException(
                            "Invalid price for " + timber.getWoodName());
                }

                if (!Double.isFinite(stock) || stock < 0
                        || !checkStock(stock, requestedQuantity)) {
                    throw new IllegalArgumentException(
                            "Insufficient stock for " + timber.getWoodName()
                            + ". Available: " + stock + " CFT"
                            + ", requested: " + requestedQuantity + " CFT");
                }

                timberMap.put(timberId, timber);

                double itemAmount =
                        calculateItemAmount(requestedQuantity, price);

                subtotal = roundMoney(subtotal + itemAmount);
            }

            // Calculate bill totals.
            double discountAmount = calculateDiscount(subtotal);
            double gstAmount = calculateGST(subtotal, discountAmount);

            double finalAmount = calculateFinalAmount(
                    subtotal, discountAmount, gstAmount);

            // Save the bill.
            Bill bill = new Bill(
                    customerId,
                    LocalDateTime.now(),
                    subtotal,
                    discountAmount,
                    GST_RATE,
                    gstAmount,
                    finalAmount);

            session.persist(bill);
            session.flush();

            billId = bill.getId();

            // Save bill items and update stock.
            for (Map.Entry<Integer, Double> entry : quantities.entrySet()) {

                int timberId = entry.getKey();
                double quantity = entry.getValue();

                Timber timber = timberMap.get(timberId);
                double price = timber.getPricePerCft();

                double amount = calculateItemAmount(quantity, price);

                BillItem billItem = new BillItem(
                        billId, timberId, quantity, price, amount);

                session.persist(billItem);

                timber.setStock(
                        calculateRemainingStock(timber.getStock(), quantity));
            }

            // Commit bill, bill items and stock changes together.
            transaction.commit();

            // Print the receipt.
            System.out.println();
            System.out.println("================================================");
            System.out.println("             SIVASANKARI TIMBERS");
            System.out.println("                 SALES BILL");
            System.out.println("================================================");
            System.out.println("Bill ID  : " + billId);
            System.out.println("Date     : " + bill.getBillDate());
            System.out.println("Customer : " + customer.getName());
            System.out.println("Phone    : " + customer.getPhone());
            System.out.println("------------------------------------------------");

            System.out.printf(
                    Locale.US,
                    "%-18s %10s %12s %14s%n",
                    "WOOD", "QTY(CFT)", "RATE(Rs.)", "AMOUNT(Rs.)");

            System.out.println("------------------------------------------------");

            // IMPORTANT: Print each timber row here.
            for (Map.Entry<Integer, Double> entry : quantities.entrySet()) {

                Timber timber = timberMap.get(entry.getKey());
                double quantity = entry.getValue();
                double price = timber.getPricePerCft();
                double amount = calculateItemAmount(quantity, price);

                System.out.printf(
                        Locale.US,
                        "%-18s %10.2f %12.2f %14.2f%n",
                        timber.getWoodName(),
                        quantity,
                        price,
                        amount);
            }

            System.out.println("------------------------------------------------");

            System.out.printf(
                    Locale.US, "Subtotal       : Rs. %.2f%n", subtotal);

            System.out.printf(
                    Locale.US, "Discount       : Rs. %.2f%n", discountAmount);

            // Avoid percentage-format conversion errors.
            System.out.printf(
                    Locale.US, "GST (%.2f percent): Rs. %.2f%n",
                    GST_RATE, gstAmount);

            System.out.println("------------------------------------------------");

            System.out.printf(
                    Locale.US, "GRAND TOTAL    : Rs. %.2f%n", finalAmount);

            System.out.println("================================================");
            System.out.println("           Thank you! Visit again.");
            System.out.println("================================================");

        } catch (Exception e) {

            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            System.err.println("Unable to process the bill.");
            System.err.println("Reason: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Generate the PDF after the database transaction succeeds.
        if (billId > 0) {
            System.out.println();
            System.out.println("Generating PDF invoice...");

            try {
                new InvoicePdfGenerator().generateInvoice(billId);
            } catch (Exception e) {
                System.err.println(
                        "Bill saved, but PDF generation failed: "
                        + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

