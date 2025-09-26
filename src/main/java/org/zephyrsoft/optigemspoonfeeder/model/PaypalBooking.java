package org.zephyrsoft.optigemspoonfeeder.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class PaypalBooking {
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

    public boolean isPositive() {
        return transactionAmount.signum() == 1;
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
