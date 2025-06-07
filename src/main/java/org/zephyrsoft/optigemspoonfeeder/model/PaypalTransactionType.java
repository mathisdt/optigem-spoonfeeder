package org.zephyrsoft.optigemspoonfeeder.model;

import lombok.RequiredArgsConstructor;

/**
 * see https://developer.paypal.com/docs/transaction-search/transaction-event-codes/
 */
@RequiredArgsConstructor
public enum PaypalTransactionType {
    PAYPAL_ACCOUNT_TO_PAYPAL_ACCOUNT_PAYMENT("T00"),
    NON_PAYMENT_RELATED_FEES("T01"),
    CURRENCY_CONVERSION("T02"),
    BANK_DEPOSIT_INTO_PAYPAL_ACCOUNT("T03"),
    BANK_WITHDRAWAL_FROM_PAYPAL_ACCOUNT("T04"),
    DEBIT_CARD("T05"),
    CREDIT_CARD_WITHDRAWAL("T06"),
    CREDIT_CARD_DEPOSIT("T07"),
    BONUS("T08"),
    INCENTIVE("T09"),
    BILL_PAY("T10"),
    REVERSAL("T11"),
    ADJUSTMENT("T12"),
    AUTHORIZATION("T13"),
    DIVIDEND("T14"),
    HOLD_FOR_DISPUTE_OR_OTHER_INVESTIGATION("T15"),
    BUYER_CREDIT_DEPOSIT("T16"),
    NON_BANK_WITHDRAWAL("T17"),
    BUYER_CREDIT_WITHDRAWAL("T18"),
    ACCOUNT_CORRECTION("T19"),
    FUNDS_TRANSFER_FROM_PAY_PAL_ACCOUNT_TO_ANOTHER("T20"),
    RESERVES_AND_RELEASES("T21"),
    TRANSFERS("T22"),
    GENERIC_INSTRUMENT_AND_OPEN_WALLET("T30"),
    COLLECTIONS_AND_DISBURSEMENTS("T50"),
    PAYABLES_AND_RECEIVABLES("T97"),
    DISPLAY_ONLY_TRANSACTION("T98"),
    OTHER("T99");

    private final String prefix;

    public static PaypalTransactionType byCode(String code) {
        for (PaypalTransactionType type : values()) {
            if (code.startsWith(type.prefix)) {
                return type;
            }
        }
        return null;
    }
}
