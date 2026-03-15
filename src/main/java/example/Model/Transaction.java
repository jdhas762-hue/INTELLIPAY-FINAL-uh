package example.Model;

public class Transaction {

    private String transactionId;
    private String merchantName;
    private double amount;
    private String status;
    private long createdTimestamp;
    // optional extra fields for invoices / UPI integration
    private String upiId;
    private java.util.List<InvoiceItem> items;

    public Transaction() {
        // Firestore needs a public no-arg constructor
    }

    public Transaction(String transactionId, String merchantName, double amount, String status, long createdTimestamp) {
        this.transactionId = transactionId;
        this.merchantName = merchantName;
        this.amount = amount;
        this.status = status;
        this.createdTimestamp = createdTimestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public java.util.List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(java.util.List<InvoiceItem> items) {
        this.items = items;
    }
}


