package example.Model;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class TransactionService {

    private static final String COLLECTION_NAME = "Transactions";

    public Transaction createTransactionFromUpi(String upiString) throws ExecutionException, InterruptedException {
        String merchantName = extractQueryParam(upiString, "pn");
        String upiId = extractQueryParam(upiString, "pa");
        String amountString = extractQueryParam(upiString, "am");
        double amount = 0.0;
        if (amountString != null && !amountString.isEmpty()) {
            try {
                amount = Double.parseDouble(amountString);
            } catch (NumberFormatException ignored) {
            }
        }

        // Generate readable transaction id like TXN1761557920755
        String transactionId = "TXN" + System.currentTimeMillis();
        long createdTimestamp = System.currentTimeMillis();

        Transaction transaction = new Transaction(
                transactionId,
                merchantName,
                amount,
                "PENDING",
                createdTimestamp
        );
        transaction.setUpiId(upiId);

        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference documentReference = dbFirestore.collection(COLLECTION_NAME).document(transactionId);
        documentReference.set(transaction);

        return transaction;
    }

    public Transaction getTransaction(String transactionId) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference documentReference = dbFirestore.collection(COLLECTION_NAME).document(transactionId);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();
        if (!document.exists()) {
            return null;
        }
        return document.toObject(Transaction.class);
    }

    public String getTransactionStatus(String transactionId) throws ExecutionException, InterruptedException {
        Transaction tx = getTransaction(transactionId);
        return tx != null ? tx.getStatus() : null;
    }

    public void updateTransactionStatus(String transactionId, String status) {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference documentReference = dbFirestore.collection(COLLECTION_NAME).document(transactionId);
        documentReference.update("status", status);
    }

    public List<Transaction> getTransactionsByStatus(String status) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        CollectionReference collectionReference = dbFirestore.collection(COLLECTION_NAME);
        ApiFuture<QuerySnapshot> future = collectionReference.whereEqualTo("status", status).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Transaction> transactions = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            transactions.add(document.toObject(Transaction.class));
        }
        return transactions;
    }

    public static class DashboardStats {
        private int transactions;
        private double totalRevenue;
        private int pendingPayments;

        public int getTransactions() {
            return transactions;
        }

        public void setTransactions(int transactions) {
            this.transactions = transactions;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(double totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public int getPendingPayments() {
            return pendingPayments;
        }

        public void setPendingPayments(int pendingPayments) {
            this.pendingPayments = pendingPayments;
        }
    }

    public DashboardStats getTodayDashboardStats() throws ExecutionException, InterruptedException {
        return getDashboardStatsForDate(null);
    }

    public DashboardStats getDashboardStatsForDate(String yyyyMMdd) throws ExecutionException, InterruptedException {
        long startOfDay;
        long endOfDay;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (yyyyMMdd != null && !yyyyMMdd.isEmpty()) {
            try {
                String[] parts = yyyyMMdd.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1; // Calendar month 0-based
                int day = Integer.parseInt(parts[2]);
                cal.set(java.util.Calendar.YEAR, year);
                cal.set(java.util.Calendar.MONTH, month);
                cal.set(java.util.Calendar.DAY_OF_MONTH, day);
            } catch (Exception ignored) {
            }
        }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        startOfDay = cal.getTimeInMillis();
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        endOfDay = cal.getTimeInMillis();

        Firestore dbFirestore = FirestoreClient.getFirestore();
        CollectionReference collectionReference = dbFirestore.collection(COLLECTION_NAME);
        ApiFuture<QuerySnapshot> future = collectionReference
                .whereGreaterThanOrEqualTo("createdTimestamp", startOfDay)
                .whereLessThan("createdTimestamp", endOfDay)
                .get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        DashboardStats stats = new DashboardStats();
        int txCount = 0;
        double revenue = 0.0;
        int pending = 0;

        for (QueryDocumentSnapshot doc : documents) {
            Transaction t = doc.toObject(Transaction.class);
            txCount++;
            if ("COMPLETED".equalsIgnoreCase(t.getStatus())) {
                revenue += t.getAmount();
            }
            if ("PENDING".equalsIgnoreCase(t.getStatus())) {
                pending++;
            }
        }

        stats.setTransactions(txCount);
        stats.setTotalRevenue(revenue);
        stats.setPendingPayments(pending);
        return stats;
    }

    public void clearCompletedHistory() throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        CollectionReference collectionReference = dbFirestore.collection(COLLECTION_NAME);
        ApiFuture<QuerySnapshot> future = collectionReference.whereEqualTo("status", "COMPLETED").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        for (QueryDocumentSnapshot doc : documents) {
            doc.getReference().delete();
        }
    }

    public Transaction createInvoiceTransaction(String merchantName,
                                                String upiId,
                                                double amount,
                                                java.util.List<InvoiceItem> items) throws ExecutionException, InterruptedException {
        String transactionId = "TXN" + System.currentTimeMillis();
        long createdTimestamp = System.currentTimeMillis();

        Transaction transaction = new Transaction(
                transactionId,
                merchantName,
                amount,
                "PENDING",
                createdTimestamp
        );
        transaction.setUpiId(upiId);
        transaction.setItems(items);

        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference documentReference = dbFirestore.collection(COLLECTION_NAME).document(transactionId);
        documentReference.set(transaction);

        return transaction;
    }

    private String extractQueryParam(String uri, String key) {
        if (uri == null || !uri.contains("?")) {
            return null;
        }
        String[] parts = uri.split("\\?", 2);
        if (parts.length < 2) {
            return null;
        }
        String query = parts[1];
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2 && key.equalsIgnoreCase(keyValue[0])) {
                return decode(keyValue[1]);
            }
        }
        return null;
    }

    private String decode(String value) {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    public boolean isExpired(Transaction transaction) {
        if (transaction == null) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - transaction.getCreatedTimestamp();
        // QR valid for 3 minutes (180000 ms)
        return diff > 180000;
    }
}


