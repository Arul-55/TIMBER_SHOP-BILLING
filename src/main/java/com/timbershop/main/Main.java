
package com.timbershop.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.timbershop.dao.BillDAO;
import com.timbershop.dao.BillItemDAO;
import com.timbershop.dao.CustomerDAO;
import com.timbershop.dao.TimberDAO;
import com.timbershop.entity.Customer;
import com.timbershop.entity.Timber;
import com.timbershop.service.BillingService;
import com.timbershop.service.BillingService.SaleItem;
import com.timbershop.service.InvoicePdfGenerator;
import com.timbershop.util.HibernateUtil;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static final TimberDAO timberDAO = new TimberDAO();
    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final BillDAO billDAO = new BillDAO();
    private static final BillItemDAO billItemDAO = new BillItemDAO();

    private static final BillingService billingService = new BillingService();
    private static final InvoicePdfGenerator pdfGenerator =
            new InvoicePdfGenerator();

    public static void main(String[] args) {
        try {
            boolean running = true;

            while (running) {
                System.out.println("\n========================================");
                System.out.println("          SIVASANKARI TIMBERS");
                System.out.println("   BILLING AND INVENTORY MANAGEMENT");
                System.out.println("========================================");
                System.out.println("1. Timber Management");
                System.out.println("2. Customer Management");
                System.out.println("3. Create New Bill");
                System.out.println("4. Bill Management");
                System.out.println("5. Sales Reports");
                System.out.println("6. Stock Reports");
                System.out.println("7. Exit");
                System.out.println("========================================");

                int choice = readInt("Enter your choice: ");

                switch (choice) {
                    case 1:
                        timberManagementMenu();
                        break;
                    case 2:
                        customerManagementMenu();
                        break;
                    case 3:
                        createNewBillMenu();
                        break;
                    case 4:
                        billManagementMenu();
                        break;
                    case 5:
                        salesReportsMenu();
                        break;
                    case 6:
                        stockReportsMenu();
                        break;
                    case 7:
                        running = false;
                        System.out.println("Thank you for using SIVASANKARI TIMBERS!");
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            }
        } catch (Exception e) {
            System.err.println("Application error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
            HibernateUtil.shutdown();
        }
    }

    // ---------------- TIMBER MANAGEMENT ----------------

    private static void timberManagementMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- TIMBER MANAGEMENT ---");
            System.out.println("1. Add Timber");
            System.out.println("2. View All Timbers");
            System.out.println("3. Search Timber by Name");
            System.out.println("4. Update Timber Price and Stock");
            System.out.println("5. Delete Timber");
            System.out.println("6. Low Stock Report");
            System.out.println("7. Expensive Timber Report");
            System.out.println("8. Back");

            int choice = readInt("Enter your choice: ");

            switch (choice) {
                case 1:
                    String name = readText("Wood name: ");
                    String type = readText("Wood type: ");
                    double price = readDouble("Price per CFT: ");
                    double stock = readDouble("Available stock in CFT: ");

                    if (name.isEmpty() || type.isEmpty()
                            || price < 0 || stock < 0) {
                        System.out.println("Invalid timber details.");
                        break;
                    }

                    timberDAO.addTimber(new Timber(name, type, price, stock));
                    break;

                case 2:
                    timberDAO.viewAllTimbers();
                    break;

                case 3:
                    String searchName = readText("Enter wood name: ");
                    timberDAO.searchTimberByName(searchName);
                    break;

                case 4:
                    int updateId = readInt("Timber ID: ");
                    double newPrice = readDouble("New price per CFT: ");
                    double newStock = readDouble("New stock in CFT: ");

                    if (newPrice < 0 || newStock < 0) {
                        System.out.println("Price and stock cannot be negative.");
                        break;
                    }

                    timberDAO.updateTimber(updateId, newPrice, newStock);
                    break;

                case 5:
                    timberDAO.deleteTimber(
                            readInt("Timber ID to delete: "));
                    break;

                case 6:
                    timberDAO.viewLowStockTimbers(
                            readDouble("Enter stock threshold: "));
                    break;

                case 7:
                    timberDAO.viewExpensiveTimbers(
                            readDouble("Enter minimum price: "));
                    break;

                case 8:
                    back = true;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

            if (!back) {
                pause();
            }
        }
    }

    // ---------------- CUSTOMER MANAGEMENT ----------------

    private static void customerManagementMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- CUSTOMER MANAGEMENT ---");
            System.out.println("1. Add Customer");
            System.out.println("2. View All Customers");
            System.out.println("3. Search Customer by Phone");
            System.out.println("4. Update Customer");
            System.out.println("5. Delete Customer");
            System.out.println("6. Back");

            int choice = readInt("Enter your choice: ");

            switch (choice) {
                case 1:
                    String name = readText("Customer name: ");
                    String phone = readText("Phone number: ");
                    String address = readText("Address: ");

                    if (name.isEmpty()) {
                        System.out.println("Customer name cannot be empty.");
                        break;
                    }

                    customerDAO.addCustomer(
                            new Customer(name, phone, address));
                    break;

                case 2:
                    customerDAO.viewAllCustomers();
                    break;

                case 3:
                    String searchPhone = readText("Phone number: ");
                    customerDAO.searchCustomer(searchPhone);
                    break;

                case 4:
                    int customerId = readInt("Customer ID: ");
                    String newName = readText("New name: ");
                    String newPhone = readText("New phone: ");
                    String newAddress = readText("New address: ");

                    customerDAO.updateCustomer(
                            customerId, newName, newPhone, newAddress);
                    break;

                case 5:
                    customerDAO.deleteCustomer(
                            readInt("Customer ID to delete: "));
                    break;

                case 6:
                    back = true;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

            if (!back) {
                pause();
            }
        }
    }

    // ---------------- MULTI-TIMBER BILLING ----------------

    private static void createNewBillMenu() {
        System.out.println("\n--- CREATE NEW TIMBER BILL ---");

        System.out.println("\nAvailable timbers:");
        timberDAO.viewAllTimbers();

        System.out.println("\nAvailable customers:");
        customerDAO.viewAllCustomers();

        int customerId = readInt("Enter customer ID: ");
        List<SaleItem> items = new ArrayList<>();

        while (true) {
            int timberId = readInt("Enter timber ID: ");
            double quantity = readDouble("Enter quantity in CFT: ");

            if (timberId <= 0 || quantity <= 0) {
                System.out.println("Timber ID and quantity must be positive.");
                continue;
            }

            items.add(new SaleItem(timberId, quantity));
            System.out.println("Item added. Total entries: " + items.size());

            String answer = readText("Add another timber? (Y/N): ");

            while (!answer.equalsIgnoreCase("Y")
                    && !answer.equalsIgnoreCase("N")) {
                answer = readText("Please enter Y or N: ");
            }

            if (answer.equalsIgnoreCase("N")) {
                break;
            }
        }

        String confirm = readText("Confirm and create bill? (Y/N): ");

        if (confirm.equalsIgnoreCase("Y")) {
            billingService.processMultiItemSale(customerId, items);
        } else {
            System.out.println("Bill creation cancelled.");
        }

        pause();
    }

    // ---------------- BILL MANAGEMENT ----------------

    private static void billManagementMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- BILL MANAGEMENT ---");
            System.out.println("1. View All Bills");
            System.out.println("2. Search Bills by Customer");
            System.out.println("3. View Bill Items");
            System.out.println("4. Sales History");
            System.out.println("5. Search Bill by ID");
            System.out.println("6. Reprint PDF Invoice");
            System.out.println("7. Back");

            int choice = readInt("Enter your choice: ");

            switch (choice) {
                case 1:
                    billDAO.viewAllBills();
                    break;

                case 2:
                    billDAO.searchBillsByCustomerId(
                            readInt("Enter customer ID: "));
                    break;

                case 3:
                    billItemDAO.searchBillItemsByBillId(
                            readInt("Enter bill ID: "));
                    break;

                case 4:
                    billDAO.viewSalesHistory();
                    break;

                case 5:
                    billDAO.searchBillById(
                            readInt("Enter bill ID: "));
                    break;

                case 6:
                    int billId = readInt("Enter existing bill ID: ");
                    pdfGenerator.generateInvoice(billId);
                    break;

                case 7:
                    back = true;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

            if (!back) {
                pause();
            }
        }
    }

    // ---------------- SALES REPORTS ----------------

    private static void salesReportsMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- SALES REPORTS ---");
            System.out.println("1. Total Sales");
            System.out.println("2. Customer Total Sales");
            System.out.println("3. Sales History");
            System.out.println("4. Search Bill by ID");
            System.out.println("5. Back");

            int choice = readInt("Enter your choice: ");

            switch (choice) {
                case 1:
                    billDAO.viewTotalSales();
                    break;

                case 2:
                    billDAO.viewCustomerTotalSales(
                            readInt("Enter customer ID: "));
                    break;

                case 3:
                    billDAO.viewSalesHistory();
                    break;

                case 4:
                    billDAO.searchBillById(
                            readInt("Enter bill ID: "));
                    break;

                case 5:
                    back = true;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

            if (!back) {
                pause();
            }
        }
    }

    // ---------------- STOCK REPORTS ----------------

    private static void stockReportsMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- STOCK REPORTS ---");
            System.out.println("1. View All Stock");
            System.out.println("2. Low Stock Report");
            System.out.println("3. Expensive Timber Report");
            System.out.println("4. Back");

            int choice = readInt("Enter your choice: ");

            switch (choice) {
                case 1:
                    timberDAO.viewAllTimbers();
                    break;

                case 2:
                    timberDAO.viewLowStockTimbers(
                            readDouble("Enter stock threshold: "));
                    break;

                case 3:
                    timberDAO.viewExpensiveTimbers(
                            readDouble("Enter minimum price: "));
                    break;

                case 4:
                    back = true;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

            if (!back) {
                pause();
            }
        }
    }

    // ---------------- INPUT HELPERS ----------------

    private static int readInt(String message) {
        while (true) {
            System.out.print(message);

            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid whole number.");
            }
        }
    }

    private static double readDouble(String message) {
        while (true) {
            System.out.print(message);

            try {
                double value = Double.parseDouble(scanner.nextLine().trim());

                if (Double.isFinite(value)) {
                    return value;
                }

                System.out.println("Enter a valid number.");
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid number.");
            }
        }
    }

    private static String readText(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    private static void pause() {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
