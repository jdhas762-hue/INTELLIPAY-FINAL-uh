package example.Controller;

import com.google.zxing.WriterException;
import example.Model.QRCodeGenerator;
import example.Model.Transaction;
import example.Model.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RestController
@CrossOrigin
public class QRCodeController {

    @Autowired
    QRCodeGenerator qrCodeGenerator;

    @Autowired
    TransactionService transactionService;

    @GetMapping(value = "/generateAndDownloadQRCode")
    public void download()
            throws Exception {
        String codeText = "sharon";
        QRCodeGenerator qrCodeGenerator = new QRCodeGenerator();
        qrCodeGenerator.generateQRCodeImage(codeText);
    }

    @PostMapping(value = "/generateQR")
    public byte[] generateQR(@RequestBody String paymentDetails) throws IOException, WriterException, ExecutionException, InterruptedException {
        System.out.println("QR generation initiated");
        // Create a PENDING transaction when the QR is generated
        Transaction transaction = transactionService.createTransactionFromUpi(paymentDetails);
        System.out.println("Created transaction with id: " + transaction.getTransactionId());
        // Do not modify QR generation logic or UPI format
        return qrCodeGenerator.getQRCodeImage(paymentDetails);
    }

    /*@GetMapping(value = "/genrateQRCode")
    public ResponseEntity<byte[]> generateQRCode(@RequestBody CodeText codeText) throws IOException, WriterException {

            return ResponseEntity.status(HttpStatus.OK).body(QRCodeGenerator.getQRCodeImage(codeText.getCodeTextString(), 480, 480));
        }
    */
}