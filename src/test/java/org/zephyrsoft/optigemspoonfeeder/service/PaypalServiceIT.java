package org.zephyrsoft.optigemspoonfeeder.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.PaypalBooking;
import org.zephyrsoft.optigemspoonfeeder.model.PaypalTransactionType;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WireMockTest(httpPort = 9970)
@SpringBootTest(properties = {
    "org.zephyrsoft.optigem-spoonfeeder.dir=src/test/resources/basedata",
    "org.zephyrsoft.optigem-spoonfeeder.bank-account.AAA.paypal-base-url=http://localhost:9970",
    "org.zephyrsoft.optigem-spoonfeeder.bank-account.AAA.paypal-client-id=AAAAAAAAAAAaAAaaaAAAA",
    "org.zephyrsoft.optigem-spoonfeeder.bank-account.AAA.paypal-client-secret=BBBbBBBbbbBBBBbbb"
})
public class PaypalServiceIT {
    @Autowired
    private OptigemSpoonfeederProperties properties;
    @Autowired
    private PaypalService paypalService;

    @BeforeEach
    void setup() throws IOException {
        try (InputStream content = getClass().getResourceAsStream("/wiremock-paypal/response_auth.json")) {
            assertNotNull(content);
            String body = new String(content.readAllBytes());
            assertNotNull(body);
            stubFor(post("/v1/oauth2/token")
                .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE, body)));
        }
        try (InputStream content = getClass().getResourceAsStream("/wiremock-paypal/response_payload_2025_04.json")) {
            assertNotNull(content);
            String body = new String(content.readAllBytes());
            assertNotNull(body);
            stubFor(get(urlMatching("/v1/reporting/transactions.*end_date=2025-05-01.*"))
                .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE, body)));
        }
        try (InputStream content = getClass().getResourceAsStream("/wiremock-paypal/response_payload_2025_05.json")) {
            assertNotNull(content);
            String body = new String(content.readAllBytes());
            assertNotNull(body);
            stubFor(get(urlMatching("/v1/reporting/transactions.*start_date=2025-05-01.*"))
                .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE, body)));
        }
    }

    @Test
    void getBookings() {
        List<PaypalBooking> result = paypalService.getBookings(properties.getBankAccount().get(properties.getBankAccount().keySet().stream().findFirst().orElseThrow()),
            YearMonth.of(2025, 5));

        assertThat(result)
            .hasSize(4)
            .anyMatch(pb -> pb.getType() == PaypalTransactionType.PAYPAL_ACCOUNT_TO_PAYPAL_ACCOUNT_PAYMENT
                && Objects.equals(pb.getFirstName(), "Test")
                && Objects.equals(pb.getLastName(), "Person")
                && Objects.equals(pb.getDate(), LocalDateTime.of(2025, 4, 27, 20, 30)))
            .anyMatch(pb -> pb.getType() == PaypalTransactionType.BANK_WITHDRAWAL_FROM_PAYPAL_ACCOUNT
                && pb.getFirstName() == null
                && pb.getLastName() == null
                && Objects.equals(pb.getDate(), LocalDateTime.of(2025, 4, 28, 1, 00)))
            .anyMatch(pb -> pb.getType() == PaypalTransactionType.PAYPAL_ACCOUNT_TO_PAYPAL_ACCOUNT_PAYMENT
                && Objects.equals(pb.getFirstName(), "Test")
                && Objects.equals(pb.getLastName(), "Person2")
                && Objects.equals(pb.getDate(), LocalDateTime.of(2025, 5, 27, 20, 30)))
            .anyMatch(pb -> pb.getType() == PaypalTransactionType.BANK_WITHDRAWAL_FROM_PAYPAL_ACCOUNT
                && pb.getFirstName() == null
                && pb.getLastName() == null
                && Objects.equals(pb.getDate(), LocalDateTime.of(2025, 5, 28, 1, 00)));

    }
}
