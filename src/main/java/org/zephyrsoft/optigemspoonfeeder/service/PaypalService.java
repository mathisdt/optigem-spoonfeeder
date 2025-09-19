package org.zephyrsoft.optigemspoonfeeder.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.PaypalBooking;
import org.zephyrsoft.optigemspoonfeeder.model.PaypalTransactionType;
import org.zephyrsoft.optigemspoonfeeder.paypal.api.TransactionsApi;
import org.zephyrsoft.optigemspoonfeeder.paypal.client.ApiClient;
import org.zephyrsoft.optigemspoonfeeder.paypal.model.SearchResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.logging.log4j.util.Strings.isBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaypalService {
    private static final DateTimeFormatter DATE_TIME_OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXX");

    public List<PaypalBooking> getBookings(OptigemSpoonfeederProperties.AccountProperties account, YearMonth month) {
        if (isBlank(account.getPaypalClientId()) || isBlank(account.getPaypalClientSecret())) {
            return null;
        }

        RestClient restClient = RestClient.create();

        RestClient.ResponseSpec authResponse = restClient
            .post()
            .uri(account.getPaypalBaseUrl() + account.getPaypalAuthEndpoint())
            .header("Authorization", "Basic " + Base64.encode(account.getPaypalClientId() + ":" + account.getPaypalClientSecret()).toString())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body("grant_type=client_credentials")
            .retrieve();
        String authResponseBody = authResponse.body(String.class);
        Map<?, ?> result;
        try {
            result = new ObjectMapper().readValue(authResponseBody, HashMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("could not parse response from auth endpoint: " + authResponseBody, e);
        }
        if (!result.containsKey("access_token")) {
            throw new RuntimeException("response from auth endpoint did not contain access token: " + authResponseBody);
        }
        String token = result.get("access_token").toString();

        ApiClient client = new ApiClient(restClient);
        client.setBasePath(account.getPaypalBaseUrl());
        client.setAccessToken(token);
        TransactionsApi transactionsApi = new TransactionsApi(client);

        LocalDate firstFetchedDay = month.atDay(1).minusDays(account.getPaypalDaysBefore());
        LocalDate lastFetchedDay = month.atEndOfMonth();
        List<PaypalBooking> endOfLastMonth = getDataFromPaypal(transactionsApi,
            firstFetchedDay,
            month.atDay(1).minusDays(1));
        List<PaypalBooking> thisMonth = getDataFromPaypal(transactionsApi,
            month.atDay(1),
            lastFetchedDay);

        List<PaypalBooking> bookings = new ArrayList<>(endOfLastMonth);
        bookings.addAll(thisMonth);
        log.info("loaded {} Paypal bookings for period {} - {}", bookings.size(), firstFetchedDay, lastFetchedDay);
        return bookings;
    }

    private List<PaypalBooking> getDataFromPaypal(TransactionsApi transactionsApi, LocalDate start, LocalDate end) {
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.plusDays(1).atStartOfDay();
        ZoneOffset startOffset = ZoneId.systemDefault().getRules().getOffset(startTime);
        ZoneOffset endOffset = ZoneId.systemDefault().getRules().getOffset(endTime);

        SearchResponse response = transactionsApi.searchGet(DATE_TIME_OFFSET.format(startTime.atOffset(startOffset)),
            DATE_TIME_OFFSET.format(endTime.atOffset(endOffset)),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "all",
            null,
            100,
            null);
        return response.getTransactionDetails().stream()
            .map(tx -> PaypalBooking.builder()
                .type(PaypalTransactionType.byCode(tx.getTransactionInfo().getTransactionEventCode()))
                .date(OffsetDateTime.from(DATE_TIME_OFFSET.parse(tx.getTransactionInfo().getTransactionInitiationDate()))
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime())
                .currency(tx.getTransactionInfo().getTransactionAmount().getCurrencyCode())
                .transactionAmount(new BigDecimal(tx.getTransactionInfo().getTransactionAmount().getValue()))
                .feeAmount(tx.getTransactionInfo().getFeeAmount() == null
                    ? BigDecimal.ZERO
                    : new BigDecimal(tx.getTransactionInfo().getFeeAmount().getValue()))
                .description(tx.getTransactionInfo().getTransactionNote())
                .name(emptyToNull((nullToEmpty(tx.getPayerInfo().getPayerName().getFullName()) + " " + nullToEmpty(tx.getPayerInfo().getPayerName().getAlternateFullName())).trim()))
                .firstName(tx.getPayerInfo().getPayerName().getGivenName())
                .lastName(tx.getPayerInfo().getPayerName().getSurname())
                .email(tx.getPayerInfo().getEmailAddress())
                .street(tx.getShippingInfo().getAddress() == null
                    ? null
                    : emptyToNull(nullToEmpty(tx.getShippingInfo().getAddress().getLine1()) + " " + nullToEmpty(tx.getShippingInfo().getAddress().getLine2())).trim())
                .zip(tx.getShippingInfo().getAddress() == null
                    ? null
                    : tx.getShippingInfo().getAddress().getPostalCode())
                .city(tx.getShippingInfo().getAddress() == null
                    ? null
                    : tx.getShippingInfo().getAddress().getCity())
                .build())
            .toList();
    }
}
