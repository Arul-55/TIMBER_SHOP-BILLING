
package com.timbershop.service;

import com.timbershop.entity.Bill;
import com.timbershop.entity.BillItem;
import com.timbershop.entity.Customer;
import com.timbershop.entity.Timber;
import com.timbershop.util.HibernateUtil;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.hibernate.Session;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InvoicePdfGenerator {

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();

    private static final float LEFT = 28;
    private static final float RIGHT = PAGE_W - 28;
    private static final float CONTENT_W = RIGHT - LEFT;

    private static final Color NAVY = new Color(17, 47, 81);
    private static final Color TEAL = new Color(0, 148, 145);
    private static final Color GOLD = new Color(255, 190, 48);
    private static final Color PALE_BLUE = new Color(231, 243, 253);
    private static final Color ROW_BLUE = new Color(231, 243, 252);
    private static final Color DARK = new Color(25, 43, 68);
    private static final Color GREY = new Color(105, 122, 142);
    private static final Color BORDER = new Color(202, 218, 232);
    private static final Color WHITE = Color.WHITE;
    private static final Color GREEN = new Color(42, 143, 65);

    private static final Path RESOURCES =
            Path.of("src", "main", "resources");

    private static final Path HEADER_PATH =
            RESOURCES.resolve("timber-header.png");

    private static final Path BOTTOM_PATH =
            RESOURCES.resolve("timber-bottom.png");

    private static final Path REGULAR_FONT_PATH =
            RESOURCES.resolve("DejaVuSansCondensed.ttf");

    private static final Path BOLD_FONT_PATH =
            RESOURCES.resolve("DejaVuSansCondensed-Bold.ttf");

    private PDType0Font regular;
    private PDType0Font bold;

    private PDImageXObject headerImage;
    private PDImageXObject bottomImage;

    private static class InvoiceRow {
        String woodName;
        double quantity;
        double rate;
        double amount;

        InvoiceRow(String woodName, double quantity,
                   double rate, double amount) {
            this.woodName = woodName;
            this.quantity = quantity;
            this.rate = rate;
            this.amount = amount;
        }
    }

    public void generateInvoice(int billId) {

        try {
            Files.createDirectories(Path.of("invoices"));

            try (PDDocument document = new PDDocument();
                 Session session =
                         HibernateUtil.getSessionFactory().openSession()) {

                Bill bill = session.get(Bill.class, billId);

                if (bill == null) {
                    System.out.println("Bill not found: " + billId);
                    return;
                }

                Customer customer =
                        session.get(Customer.class, bill.getCustomerId());

                if (customer == null) {
                    System.out.println(
                            "Customer not found for bill " + billId);
                    return;
                }

                List<BillItem> billItems = session.createQuery(
                        "FROM BillItem WHERE billId = :id ORDER BY id",
                        BillItem.class)
                        .setParameter("id", billId)
                        .getResultList();

                List<InvoiceRow> rows = new ArrayList<>();

                for (BillItem item : billItems) {
                    Timber timber =
                            session.get(Timber.class, item.getTimberId());

                    String woodName = timber == null
                            ? "Unknown Wood"
                            : timber.getWoodName();

                    rows.add(new InvoiceRow(
                            woodName,
                            item.getQuantity(),
                            item.getPricePerCft(),
                            item.getAmount()));
                }

                // Embed the actual font files into the PDF.
                regular = PDType0Font.load(
                        document, requireFile(REGULAR_FONT_PATH));

                bold = PDType0Font.load(
                        document, requireFile(BOLD_FONT_PATH));

                // Load the two decorative timber images.
                headerImage = loadImage(document, HEADER_PATH);
                bottomImage = loadImage(document, BOTTOM_PATH);

                int rowIndex = 0;
                int pageNumber = 1;
                boolean totalsOnlyPage = false;

                do {
                    PDPage page = new PDPage(PDRectangle.A4);
                    document.addPage(page);

                    try (PDPageContentStream cs =
                                 new PDPageContentStream(document, page)) {

                        drawPageHeader(cs, bill, pageNumber);

                        if (totalsOnlyPage) {
                            drawTotals(cs, bill, 475);
                            drawBottomDecoration(cs);
                            drawFooter(cs);
                            break;
                        }

                        float tableY =
                                drawCustomerPanel(cs, bill, customer);

                        tableY = drawTableHeader(cs, tableY);

                        // Leave sufficient space for totals and decoration.
                        final float rowHeight = 27;
                        final float minimumRowBottom = 326;

                        while (rowIndex < rows.size()
                                && tableY - rowHeight
                                >= minimumRowBottom) {

                            drawItemRow(
                                    cs,
                                    rows.get(rowIndex),
                                    rowIndex,
                                    tableY,
                                    rowHeight);

                            tableY -= rowHeight;
                            rowIndex++;
                        }

                        if (rowIndex < rows.size()) {
                            text(cs, bold, 10, NAVY,
                                    LEFT, 300,
                                    "Continued on next page...");

                            drawFooter(cs);
                            pageNumber++;
                            continue;
                        }

                        // If the remaining rows leave insufficient space,
                        // place the totals on a clean separate page.
                        if (tableY - 12 - 125 < 155) {
                            drawFooter(cs);
                            pageNumber++;
                            totalsOnlyPage = true;
                            continue;
                        }

                        drawTotals(cs, bill, tableY - 12);
                        drawBottomDecoration(cs);
                        drawFooter(cs);
                        break;
                    }
                } while (true);

                String filename =
                        "SIVASANKARI_TIMBERS_Bill_" + billId + ".pdf";

                Path output =
                        Path.of("invoices", filename).toAbsolutePath();

                document.save(output.toFile());

                System.out.println();
                System.out.println("======================================");
                System.out.println("INVOICE PDF CREATED SUCCESSFULLY");
                System.out.println("Shop: SIVASANKARI TIMBERS");
                System.out.println("Bill ID: " + billId);
                System.out.println("File: " + output);
                System.out.println("======================================");

            }

        } catch (Exception e) {
            System.err.println("Invoice generation failed: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================
    // HEADER
    // =========================================================

    private void drawPageHeader(
            PDPageContentStream cs,
            Bill bill,
            int pageNumber) throws IOException {

        float headerHeight = 153;
        float headerBottom = PAGE_H - headerHeight;

        rect(cs, 0, headerBottom, PAGE_W, headerHeight, NAVY);

        // Draw the image first so the title and invoice badge remain visible.
        if (headerImage != null) {
            cs.drawImage(
                    headerImage,
                    PAGE_W - 225,
                    headerBottom + 5,
                    225,
                    headerHeight - 5);
        }

        // Dark left panel behind the shop title.
        rect(cs, 0, headerBottom, 335, headerHeight, NAVY);
        rect(cs, 0, headerBottom - 6, PAGE_W, 6, TEAL);

        rect(cs, 28, PAGE_H - 74, 5, 38, GOLD);

        text(cs, bold, 23, WHITE,
                42, PAGE_H - 54,
                "SIVASANKARI TIMBERS");

        text(cs, bold, 8, new Color(181, 219, 242),
                43, PAGE_H - 72,
                "QUALITY TIMBER  |  TRUSTED SERVICE");

        text(cs, bold, 17, WHITE,
                28, PAGE_H - 108,
                "SALES INVOICE");

        rect(cs, 28, PAGE_H - 117, 190, 3, GOLD);

        // Right-aligned invoice number and date box.
        float badgeWidth = 215;
        float badgeHeight = 48;
        float badgeX = RIGHT - badgeWidth;
        float badgeY = PAGE_H - 126;

        rect(cs, badgeX, badgeY,
                badgeWidth, badgeHeight, TEAL);

        text(cs, bold, 12, WHITE,
                badgeX + 13, badgeY + 29,
                "INVOICE NO. " + bill.getId());

        String date = formatDate(bill);

        text(cs, regular, 9, WHITE,
                badgeX + 13, badgeY + 12,
                "Date: " + date);

        textRight(cs, regular, 8, GREY,
                RIGHT, headerBottom - 18,
                "Page " + pageNumber);
    }

    // =========================================================
    // CUSTOMER PANEL
    // =========================================================

    private float drawCustomerPanel(
            PDPageContentStream cs,
            Bill bill,
            Customer customer) throws IOException {

        float x = LEFT;
        float y = PAGE_H - 316;
        float h = 112;

        rect(cs, x, y, CONTENT_W, h, PALE_BLUE);
        rect(cs, x, y, 5, h, TEAL);

        line(cs, 298, y + 14,
                298, y + h - 14,
                new Color(120, 166, 195), 0.8f);

        text(cs, bold, 13, NAVY,
                48, y + 86, "BILL TO");

        text(cs, regular, 10, DARK,
                48, y + 64, "Customer:");

        text(cs, bold, 10, DARK,
                112, y + 64, fit(customer.getName(), 25));

        text(cs, regular, 10, DARK,
                48, y + 42, "Phone:");

        text(cs, regular, 10, DARK,
                112, y + 42, fit(customer.getPhone(), 23));

        text(cs, regular, 10, DARK,
                48, y + 20, "Address:");

        text(cs, regular, 10, DARK,
                112, y + 20, fit(customer.getAddress(), 23));

        text(cs, bold, 12, NAVY,
                316, y + 86, "INVOICE DETAILS");

        text(cs, regular, 10, DARK,
                316, y + 64,
                "Invoice No:   " + bill.getId());

        text(cs, regular, 9, DARK,
                316, y + 44,
                "Date: " + formatDate(bill));

        text(cs, regular, 9, DARK,
                316, y + 25,
                "Payment: Not recorded");

        rect(cs, 316, y + 6, 150, 16, GREEN);

        text(cs, bold, 8, WHITE,
                324, y + 11,
                "INVOICE GENERATED");

        return y - 15;
    }

    // =========================================================
    // TABLE
    // =========================================================

    private float drawTableHeader(
            PDPageContentStream cs,
            float y) throws IOException {

        float h = 30;

        rect(cs, LEFT, y - h, CONTENT_W, h, NAVY);

        text(cs, bold, 9, WHITE,
                39, y - 19, "S.No");

        text(cs, bold, 9, WHITE,
                78, y - 19, "WOOD NAME");

        text(cs, bold, 9, WHITE,
                260, y - 19, "QTY (CFT)");

        text(cs, bold, 9, WHITE,
                345, y - 19, "RATE (Rs.)");

        text(cs, bold, 9, WHITE,
                438, y - 19, "AMOUNT (Rs.)");

        return y - h;
    }

    private void drawItemRow(
            PDPageContentStream cs,
            InvoiceRow item,
            int index,
            float y,
            float h) throws IOException {

        float bottom = y - h;

        rect(cs, LEFT, bottom, CONTENT_W, h,
                index % 2 == 0 ? ROW_BLUE : WHITE);

        rect(cs, LEFT, bottom, 3, h, TEAL);

        text(cs, regular, 9, DARK,
                39, bottom + 10,
                String.valueOf(index + 1));

        text(cs, bold, 9, DARK,
                78, bottom + 10,
                fit(item.woodName, 25));

        textRight(cs, regular, 9, DARK,
                307, bottom + 10,
                money(item.quantity));

        textRight(cs, regular, 9, DARK,
                411, bottom + 10,
                money(item.rate));

        textRight(cs, bold, 9, NAVY,
                520, bottom + 10,
                money(item.amount));

        line(cs, 68, bottom, 68, y, BORDER, 0.5f);
        line(cs, 250, bottom, 250, y, BORDER, 0.5f);
        line(cs, 335, bottom, 335, y, BORDER, 0.5f);
        line(cs, 424, bottom, 424, y, BORDER, 0.5f);
        line(cs, LEFT, bottom, RIGHT, bottom, BORDER, 0.6f);
    }

    // =========================================================
    // TOTALS PANEL
    // =========================================================

    private void drawTotals(
            PDPageContentStream cs,
            Bill bill,
            float top) throws IOException {

        float width = 278;
        float height = 125;
        float x = RIGHT - width;
        float bottom = top - height;

        rect(cs, x, bottom, width, height, PALE_BLUE);
        rect(cs, x, bottom, 5, height, TEAL);

        text(cs, regular, 10, DARK,
                x + 17, top - 23, "Subtotal");

        textRight(cs, bold, 10, DARK,
                RIGHT - 14, top - 23,
                "Rs. " + money(bill.getSubtotal()));

        text(cs, regular, 10, GREY,
                x + 17, top - 47, "Discount");

        textRight(cs, regular, 10, DARK,
                RIGHT - 14, top - 47,
                "Rs. " + money(bill.getDiscountAmount()));

        text(cs, regular, 10, GREY,
                x + 17, top - 71,
                "GST (" + money(bill.getGstRate()) + "%)");

        textRight(cs, regular, 10, DARK,
                RIGHT - 14, top - 71,
                "Rs. " + money(bill.getGstAmount()));

        line(cs, x + 15, top - 84,
                RIGHT - 14, top - 84,
                BORDER, 0.8f);

        rect(cs, x, bottom, width, 32, TEAL);

        text(cs, bold, 10, WHITE,
                x + 13, bottom + 11, "GRAND TOTAL");

        textRight(cs, bold, 12, WHITE,
                RIGHT - 13, bottom + 10,
                "Rs. " + money(bill.getTotalAmount()));
    }

    // =========================================================
    // LOWER TIMBER IMAGE
    // =========================================================

    private void drawBottomDecoration(
            PDPageContentStream cs) throws IOException {

        if (bottomImage == null) {
            return;
        }

        // Confined to the lower-left area. It does not cover
        // the totals panel, footer text, or signature line.
        cs.drawImage(
                bottomImage,
                LEFT,
                130,
                205,
                58);
    }

    // =========================================================
    // FOOTER
    // =========================================================

    private void drawFooter(
            PDPageContentStream cs) throws IOException {

        // The thank-you section is above the bottom branding strip.
        line(cs, LEFT, 116,
                RIGHT, 116, TEAL, 1.2f);

        text(cs, bold, 11, NAVY,
                LEFT, 96,
                "THANK YOU FOR YOUR BUSINESS!");

        text(cs, regular, 8, GREY,
                LEFT, 81,
                "Thank you for choosing SIVASANKARI TIMBERS.");

        text(cs, regular, 8, GREY,
                LEFT, 68,
                "Please retain this invoice for your records.");

        text(cs, bold, 8, DARK,
                RIGHT - 142, 96,
                "Authorized Signature");

        line(cs, RIGHT - 145, 72,
                RIGHT, 72, GREY, 0.7f);

        // Bottom navy and gold brand strip.
        rect(cs, 0, 0, PAGE_W, 10, NAVY);
        rect(cs, 0, 10, PAGE_W, 4, GOLD);
        rect(cs, 0, 14, PAGE_W, 30, NAVY);

        text(cs, bold, 9, WHITE,
                115, 26,
                "SIVASANKARI TIMBERS");

        text(cs, regular, 8, WHITE,
                253, 26,
                "| QUALITY WOOD FOR A BETTER TOMORROW");
    }

    // =========================================================
    // GENERAL HELPERS
    // =========================================================

    private File requireFile(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException(
                    "Required file not found: "
                            + path.toAbsolutePath());
        }

        return path.toFile();
    }

    private PDImageXObject loadImage(
            PDDocument document,
            Path path) throws IOException {

        if (!Files.isRegularFile(path)) {
            System.out.println(
                    "Optional image not found: "
                            + path.toAbsolutePath());
            return null;
        }

        return PDImageXObject.createFromFileByContent(
                path.toFile(), document);
    }

    private String formatDate(Bill bill) {
        if (bill.getBillDate() == null) {
            return "-";
        }

        return bill.getBillDate().format(
                DateTimeFormatter.ofPattern(
                        "dd-MM-yyyy hh:mm a",
                        Locale.ENGLISH));
    }

    private String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private String fit(String value, int maxLength) {
        String output = safe(value);

        if (output.length() <= maxLength) {
            return output;
        }

        return output.substring(0, maxLength - 3) + "...";
    }

    private String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value.replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
    }

    private void rect(
            PDPageContentStream cs,
            float x,
            float y,
            float width,
            float height,
            Color color) throws IOException {

        cs.setNonStrokingColor(color);
        cs.addRect(x, y, width, height);
        cs.fill();
    }

    private void line(
            PDPageContentStream cs,
            float x1,
            float y1,
            float x2,
            float y2,
            Color color,
            float thickness) throws IOException {

        cs.setStrokingColor(color);
        cs.setLineWidth(thickness);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private void text(
            PDPageContentStream cs,
            PDType0Font font,
            float size,
            Color color,
            float x,
            float y,
            String value) throws IOException {

        String output = safe(value);

        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(output);
        cs.endText();
    }

    private void textRight(
            PDPageContentStream cs,
            PDType0Font font,
            float size,
            Color color,
            float rightX,
            float y,
            String value) throws IOException {

        String output = safe(value);

        float textWidth =
                font.getStringWidth(output) / 1000 * size;

        text(cs, font, size, color,
                rightX - textWidth, y, output);
    }
}
