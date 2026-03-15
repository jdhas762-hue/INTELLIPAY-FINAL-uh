package example.Model;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class InvoiceImageService {

    /**
     * Generate an invoice image with merchant, items, total, transaction id and embedded QR.
     * This method does NOT change QR generation logic or UPI format – it only consumes
     * the existing QR service.
     */
    public BufferedImage generateInvoiceImage(String transactionId,
                                              String merchantName,
                                              String upiId,
                                              List<InvoiceItem> items,
                                              double totalAmount) throws IOException {

        // Base bill image
        BufferedImage bill = new BufferedImage(400, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bill.createGraphics();

        // Smooth text
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 400, 600);

        // Merchant title
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString(merchantName != null ? merchantName : "Merchant", 80, 40);

        // Separator line
        g.drawLine(20, 50, 380, 50);

        // Items header
        int y = 80;
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Items", 20, y);
        y += 20;

        g.setFont(new Font("Arial", Font.PLAIN, 12));
        if (items != null) {
            for (InvoiceItem item : items) {
                String line = String.format("%s x%d  ₹%.2f",
                        item.getName(),
                        item.getQuantity(),
                        item.getPrice() * item.getQuantity());
                g.drawString(line, 20, y);
                y += 18;
                if (y > 300) {
                    break; // avoid overflowing into QR area
                }
            }
        }

        // Total amount
        y += 10;
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(String.format("Total: ₹%.2f", totalAmount), 20, y);

        // Transaction ID
        y += 20;
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("TXN ID: " + transactionId, 20, y);

        // Generate QR content using existing UPI format
        String upiString =
                "upi://pay?pa=" + encode(upiId) +
                        "&pn=" + encode(merchantName) +
                        "&am=" + encode(String.valueOf(totalAmount)) +
                        "&cu=" + encode("INR");

        try {
            // Use existing QR generator logic (ZXing) via its byte[] method
            byte[] qrBytes = QRCodeGenerator.getQRCodeImage(upiString);
            BufferedImage qr = ImageIO.read(new ByteArrayInputStream(qrBytes));
            // Draw QR on invoice
            g.drawImage(qr, 125, 350, 150, 150, null);
        } catch (Exception e) {
            // If QR generation fails, we still return the bill without QR
            e.printStackTrace();
        } finally {
            g.dispose();
        }

        // Also save bill image under /invoices/
        saveInvoiceImageToDisk(bill, transactionId);

        return bill;
    }

    private void saveInvoiceImageToDisk(BufferedImage bill, String transactionId) throws IOException {
        Path invoicesDir = Paths.get("invoices");
        if (!Files.exists(invoicesDir)) {
            Files.createDirectories(invoicesDir);
        }
        File outFile = invoicesDir.resolve("invoice_" + transactionId + ".png").toFile();
        ImageIO.write(bill, "png", outFile);
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}


