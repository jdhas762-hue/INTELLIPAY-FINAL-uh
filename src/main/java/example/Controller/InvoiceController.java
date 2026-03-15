package example.Controller;

import com.google.zxing.WriterException;
import example.Model.InvoiceImageService;
import example.Model.InvoiceItem;
import example.Model.Item;
import example.Model.ItemService;
import example.Model.QRCodeGenerator;
import example.Model.Transaction;
import example.Model.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
@CrossOrigin
public class InvoiceController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private QRCodeGenerator qrCodeGenerator;

    @Autowired
    private InvoiceImageService invoiceImageService;

    public static class InvoiceItemRequest {
        private String itemId;
        private int quantity;
        private double price; // optional override per invoice

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    public static class CreateInvoiceRequest {
        private String merchantName;
        private String upiId;
        private List<InvoiceItemRequest> items;

        public String getMerchantName() {
            return merchantName;
        }

        public void setMerchantName(String merchantName) {
            this.merchantName = merchantName;
        }

        public String getUpiId() {
            return upiId;
        }

        public void setUpiId(String upiId) {
            this.upiId = upiId;
        }

        public List<InvoiceItemRequest> getItems() {
            return items;
        }

        public void setItems(List<InvoiceItemRequest> items) {
            this.items = items;
        }
    }

    @GetMapping("/create-invoice")
    public String createInvoicePage() {
        return "create-invoice";
    }

    @PostMapping("/invoice")
    @ResponseBody
    public String createInvoice(@RequestBody CreateInvoiceRequest request) throws ExecutionException, InterruptedException {
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        double total = 0.0;

        if (request.getItems() != null) {
            for (InvoiceItemRequest itemReq : request.getItems()) {
                if (itemReq.getQuantity() <= 0) {
                    continue;
                }
                Item item = itemService.findById(itemReq.getItemId());
                if (item == null) {
                    continue;
                }
                // allow overriding price per invoice; fall back to catalog price
                double effectivePrice = itemReq.getPrice() > 0 ? itemReq.getPrice() : item.getPrice();
                double lineTotal = effectivePrice * itemReq.getQuantity();
                total += lineTotal;
                invoiceItems.add(new InvoiceItem(item.getName(), effectivePrice, itemReq.getQuantity()));
            }
        }

        if (total <= 0) {
            return "";
        }

        Transaction transaction = transactionService.createInvoiceTransaction(
                request.getMerchantName(),
                request.getUpiId(),
                total,
                invoiceItems
        );

        return transaction.getTransactionId();
    }

    @GetMapping("/invoice/{transactionId}")
    public String viewInvoice(@PathVariable String transactionId, Model model) throws ExecutionException, InterruptedException {
        Transaction transaction = transactionService.getTransaction(transactionId);
        if (transaction == null) {
            return "invoice-not-found";
        }
        model.addAttribute("transaction", transaction);
        // base URL can be adjusted if deployed elsewhere
        String invoiceLink = "http://localhost:8080/invoice/" + transactionId;
        model.addAttribute("invoiceLink", invoiceLink);
        return "invoice";
    }

    @GetMapping("/qr/{transactionId}")
    public ResponseEntity<byte[]> qrForTransaction(@PathVariable String transactionId) throws ExecutionException, InterruptedException, IOException, WriterException {
        Transaction transaction = transactionService.getTransaction(transactionId);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        String upiString =
                "upi://pay?pa=" + encode(transaction.getUpiId()) +
                        "&pn=" + encode(transaction.getMerchantName()) +
                        "&am=" + encode(String.valueOf(transaction.getAmount())) +
                        "&cu=" + encode("INR");

        byte[] png = QRCodeGenerator.getQRCodeImage(upiString);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return ResponseEntity.ok().headers(headers).body(png);
    }

    @GetMapping("/invoice-image/{transactionId}")
    public ResponseEntity<byte[]> invoiceImage(@PathVariable String transactionId) throws ExecutionException, InterruptedException, IOException {
        Transaction transaction = transactionService.getTransaction(transactionId);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        // Generate invoice image with embedded QR using existing UPI format and QR generator
        java.awt.image.BufferedImage bill = invoiceImageService.generateInvoiceImage(
                transaction.getTransactionId(),
                transaction.getMerchantName(),
                transaction.getUpiId(),
                transaction.getItems(),
                transaction.getAmount()
        );

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(bill, "png", baos);
        byte[] png = baos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + transactionId + ".png");
        return ResponseEntity.ok().headers(headers).body(png);
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


