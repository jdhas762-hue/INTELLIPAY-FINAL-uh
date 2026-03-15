package example.Model;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;


@Service
public class FBInitialize {

    @PostConstruct
    public void initialize() {
        try {
            System.out.println("FireStore Boot has started");
            String initialString = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"intellipay-jh\",\n" +
                    "  \"private_key_id\": \"a78038507c1faa2d8164af094058c7b4bdc67fe5\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC6hoyebXF57u+V\\nII2siPPA3lW9c8/VuxEkNxpZgPsBItu12u/kIXDoKyOP6y/w13gGnophA+RbidhL\\nIFBWex95XHojgPjR2LAIrZKYKtFczUFwgr5vAxa1bNcAmfTwAdSBHBgwZGkZ08c8\\nZRfScWQzcm5Vuj4jKmZhDCppHY6UKRDbh10t3bhQjPPPzM/W88BA+OXSVL8KrDGy\\nmrTnCAzEIEvYoMhDIlvGrkCmJV5kWx/wdXGv/k9BFXxmPlb/rvyYJj0lu1cTPViv\\nrXYHkEHnISZb1wk8SnuvyUnbeDpsr2a0mbkNPw6PX6HkR5b9AG7AdsOYAO0ctWaz\\ntFPiY/krAgMBAAECggEAJlQdszR0HAVAUBmXYzvlMt29KqgQri+5jwKPtJKvgYVD\\nNrRdlThV9i9fKGTAvjHYpSuQ7eyZ0UGbI5zpRUwZpJufMYqAFsb7LV1VgVVTHknD\\nR7ZmIXO2B+Psrl/0mBkYLMu0IMWRX1BIYZ24bnMEjxnVmGIkAJ/m18xjLqR3NvR7\\na77XhgEg5MuomzobdFbAeW8etGGFsbJrBbhwJwo8NhUMkgwIxJsvfhlgGwvpPa5L\\n5+ymJnuRgONi+FJp/lZhXbd3TwlS/Gxz8hFy3ilUUOWI7XWKCY/x1YK0r0CWToit\\nulzqcpg21Y9M7XipcO+eDGLPm1AbpcbDkLJ/7HrNeQKBgQDlXZgMkjP6u0AeW6Z1\\nE9/jN11PIiPYbkTjRC2C5yxVjMnPrre4cp6uEyRXEra6pdrmOM02IOM8+wsKS7ic\\n7/ccaaCencgSJ6HXCkUGcTMSRGJE4sx4k5OqU3vLco6ceSIr2xkhAOcl9A0T8Ka5\\nzMpnSKJ37H71NNvbGqb5ya/qfQKBgQDQL3CcphFc5xZTfQhdHFeKYYi6h8+IeNqQ\\n3d69ta0UrfOkf/k+CIRcaV3Of5rjW4Iw+AgTp9yC+MnflZ7BmAcc1scReLYxBbL1\\nASPVApS4+X0N75z8O0IXphZJZK/zZX6Wb67/4xSRrk9yWrSPOCMJ/wvIxMbSQAu0\\nZv/EdGQaxwKBgGpwZZgn3OFVNisgYv3f7D0YOz/22uWcPnGs+OXPe75zqE+th+qb\\nnlGv5mRV9eBmCVBfObQNzQZGhdgQTarenTFdP0F8fRUXuT3+sQuNSqMGgwfuq+6k\\npZkcRs2h6tZoTFhHw8CSF7KAL/V57xU4GRGXHnZClKQcFI74Llaqpu1NAoGAJtFx\\nsgPVIPmF1DNwJ8xtFkRxdjZ0oAHI31I/gigqCb0VvQqPst1rsL+E2kg43UPm1rKo\\n7uFR4Kn7GlGPtJFgYiQ6Iivb7PAfGkgIImPEz2jVxI91OiLig/5YEX9Qv5WhMPPK\\nMbRp1XAnKC95k2roPajszZbDa9i9VfoOntcFA2kCgYEAw1SLTlj2MpzZWLMux1Ye\\nuRSr9u9MveNx1ZtmjwUjYnVgxSH6HnhDBfPgnc2TkXGSk/Ksf3iESF1wfqO3ec5X\\nmyW+2S1J3lIwpkUAtp8vlIRjRgSN/pdvan+DRWq7ARuXeaVd2Th1D2Xjd72ggTHt\\nDOhqCBM98Xdss+67TzpD45A=\\n-----END PRIVATE KEY-----\\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-fbsvc@intellipay-jh.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"103331823091338416421\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-fbsvc%40intellipay-jh.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}\n";
            InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
            GoogleCredentials googleCred = GoogleCredentials.fromStream(targetStream);
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(googleCred)
                    .setDatabaseUrl("https://intellipay-jh.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}