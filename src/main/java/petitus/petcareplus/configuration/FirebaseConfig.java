package petitus.petcareplus.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account.base64}")
    private String serviceAccountBase64;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        GoogleCredentials googleCredentials;
        // Kiểm tra nếu có Base64 string từ environment variable
        // Decode Base64 string thành JSON
        byte[] decodedBytes = Base64.getDecoder().decode(serviceAccountBase64);
        ByteArrayInputStream credentialsStream = new ByteArrayInputStream(decodedBytes);
        googleCredentials = GoogleCredentials.fromStream(credentialsStream);

        FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                .setCredentials(googleCredentials)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(firebaseOptions);
        }

        return FirebaseMessaging.getInstance();
    }
}