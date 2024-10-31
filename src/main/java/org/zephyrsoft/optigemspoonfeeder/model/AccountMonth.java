package org.zephyrsoft.optigemspoonfeeder.model;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class AccountMonth implements Comparable<AccountMonth> {
    private static final Comparator<AccountMonth> COMPARATOR =
        Comparator.comparing(AccountMonth::getAccount).thenComparing(AccountMonth::getYearMonth, Comparator.reverseOrder());
    private static final DateTimeFormatter YEAR_MONTH_FORMAT_FOR_HUMANS = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Pattern JSON_FILE_NAME = Pattern.compile("^(.+)-(\\d{4})-(\\d{2}).json$");

    private String account;
    private YearMonth yearMonth;

    public String getAccountForFilename() {
        return account.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("__", "_");
    }

    public String getFilename() {
        return account + "-" + YEAR_MONTH_FORMAT.format(yearMonth) + ".json";
    }

    public String getLabel() {
        return account + " (" + YEAR_MONTH_FORMAT_FOR_HUMANS.format(yearMonth) + ")";
    }

    public static boolean matches(String filename) {
        return JSON_FILE_NAME.matcher(filename).matches();
    }

    public static AccountMonth fromFilename(String filename) {
        Matcher matcher = JSON_FILE_NAME.matcher(filename);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("filename " + filename + " does not match the pattern");
        }
        return new AccountMonth(matcher.group(1), YearMonth.of(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))));
    }

    @Override
    public int compareTo(final AccountMonth o) {
        return COMPARATOR.compare(this, o);
    }
}
