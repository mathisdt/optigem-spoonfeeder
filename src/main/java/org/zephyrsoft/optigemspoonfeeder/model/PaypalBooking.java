package org.zephyrsoft.optigemspoonfeeder.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class PaypalBooking {
    private static final Set<PaypalTransactionType> INCOMING_TYPES = Set.of(PaypalTransactionType.PAYPAL_ACCOUNT_TO_PAYPAL_ACCOUNT_PAYMENT,
        PaypalTransactionType.BUYER_CREDIT_DEPOSIT, PaypalTransactionType.BILL_PAY, PaypalTransactionType.CREDIT_CARD_DEPOSIT);

    private PaypalTransactionType type;
    private LocalDateTime date;
    /** including fee */
    private BigDecimal transactionAmount;
    private BigDecimal feeAmount;
    private String currency;
    private String description;
    private String name;
    private String firstName;
    private String lastName;
    private String street;
    private String zip;
    private String city;
    private String email;

    public boolean isIncomingPayment() {
        return transactionAmount.signum() == 1 && INCOMING_TYPES.contains(type);
    }

    /** without fee */
    public BigDecimal getNetAmount() {
        if (transactionAmount == null) {
            return BigDecimal.ZERO;
        } else {
            // "add" because the fee is always negative
            return transactionAmount.add(feeAmount == null ? BigDecimal.ZERO : feeAmount);
        }
    }
}
