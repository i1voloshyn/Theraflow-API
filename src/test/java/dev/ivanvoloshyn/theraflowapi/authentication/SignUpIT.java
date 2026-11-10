package dev.ivanvoloshyn.theraflowapi.authentication;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import dev.ivanvoloshyn.theraflowapi.TestContainerConfiguration;
import dev.ivanvoloshyn.theraflowapi.account.AccountRepository;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpResponse;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorResponse;
import dev.ivanvoloshyn.theraflowapi.fixtures.Constants;
import dev.ivanvoloshyn.theraflowapi.fixtures.SignUpTestBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestContainerConfiguration.class)
public class SignUpIT {

    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private AccountRepository accountRepository;

    @RegisterExtension
    private static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("spring", "root"));

    @AfterEach
    void tearDown() {
        accountRepository.deleteAll();
    }

    @DisplayName("""
            signUp
            SHOULD persist Account with Identity
            SEND verification email
            RETURN status CREATED with SignUpResponse
            """)
    @Test
    void signUp_success() throws MessagingException {
        PasswordSignUpRequest request = SignUpTestBuilder.defaultTherapistSignUpRequest();

        SignUpResponse response = signUp(request)
                .expectStatus().isCreated()
                .returnResult(SignUpResponse.class).getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION);


        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        assertThat(greenMail.getReceivedMessages()).hasSize(1);

        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];

        assertThat(receivedMessage.getSubject()).isEqualTo("Account Verification");
        assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo(request.email());
        assertThat(receivedMessage.getFrom()[0].toString()).isEqualTo("no-reply@theraflow.com");
    }

    @Test
    @DisplayName("""
            signUp
            SHOULD return CONFLICT AND
            DO NOT send verification email
            WHEN there is an attempt to create Account with existed email
            """)
    void signUp_duplicateEmail_returnsConflictAndDoesNotSendSecondEmail() {
        PasswordSignUpRequest request = SignUpTestBuilder.defaultTherapistSignUpRequest();

        signUp(request)
                .expectStatus().isCreated();
        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();

        signUp(request)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorResponse.class)
                .value(error -> {
                    assertThat(error.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(error.errorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
                });

        assertThat(accountRepository.count()).isOne();
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    private RestTestClient.ResponseSpec signUp(PasswordSignUpRequest request) {
        return restTestClient.post()
                .uri(Constants.SIGN_UP_ENDPOINT)
                .body(request)
                .exchange();
    }
}
