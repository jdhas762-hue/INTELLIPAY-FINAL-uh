package example.Controller;

import example.Model.Transaction;
import example.Model.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@CrossOrigin
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    public static class ConfirmPaymentRequest {
        private String transactionId;

        public ConfirmPaymentRequest() {
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }
    }

    @PostMapping("/confirmPayment")
    public String confirmPayment(@RequestBody ConfirmPaymentRequest request) throws ExecutionException, InterruptedException {
        String transactionId = request.getTransactionId();
        if (transactionId == null || transactionId.isEmpty()) {
            return "Invalid transactionId";
        }

        Transaction transaction = transactionService.getTransaction(transactionId);
        if (transaction == null) {
            return "Transaction not found";
        }

        if (!"PENDING".equals(transaction.getStatus())) {
            return "Transaction already processed";
        }

        if (transactionService.isExpired(transaction)) {
            transactionService.updateTransactionStatus(transactionId, "EXPIRED");
            return "QR Expired";
        }

        transactionService.updateTransactionStatus(transactionId, "COMPLETED");
        return "Payment Confirmed";
    }

    @GetMapping("/transactions/pending")
    public List<Transaction> getPendingTransactions() throws ExecutionException, InterruptedException {
        return transactionService.getTransactionsByStatus("PENDING");
    }

    @GetMapping("/transactions/history")
    public List<Transaction> getCompletedTransactions() throws ExecutionException, InterruptedException {
        return transactionService.getTransactionsByStatus("COMPLETED");
    }

    @GetMapping("/payment-status/{transactionId}")
    public String getPaymentStatus(@PathVariable String transactionId) throws ExecutionException, InterruptedException {
        String status = transactionService.getTransactionStatus(transactionId);
        return status != null ? status : "";
    }

    @DeleteMapping("/transactions/history")
    public String clearHistory() throws ExecutionException, InterruptedException {
        transactionService.clearCompletedHistory();
        return "History cleared";
    }
}


