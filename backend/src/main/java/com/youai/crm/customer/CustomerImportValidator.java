package com.youai.crm.customer;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CustomerImportValidator {
    private static final Pattern DATE = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$");
    private static final Pattern YEAR = Pattern.compile("^\\d{4}$");
    private static final Pattern INCOME = Pattern.compile("^(\\d+(?:\\.\\d+)?)(万)?(?:[-~～至到](\\d+(?:\\.\\d+)?)(万)?)?元?$");

    private CustomerImportValidator() {}

    static void validate(CustomerRequest request) {
        String birthday = trimmed(request.birthday());
        String ageText = trimmed(request.age());
        LocalDate today = LocalDate.now();
        LocalDate date = null;
        int year = 0;
        Matcher dateMatch = DATE.matcher(birthday);
        if (!birthday.isEmpty()) {
            if (dateMatch.matches()) {
                try {
                    date = LocalDate.of(Integer.parseInt(dateMatch.group(1)), Integer.parseInt(dateMatch.group(2)), Integer.parseInt(dateMatch.group(3)));
                    year = date.getYear();
                } catch (DateTimeException exception) {
                    throw new IllegalArgumentException("生日日期无效");
                }
            } else if (YEAR.matcher(birthday).matches()) {
                year = Integer.parseInt(birthday);
            } else {
                throw new IllegalArgumentException("生日格式错误");
            }
            if (year < 1900 || year > today.getYear() || (date != null && date.isAfter(today))) {
                throw new IllegalArgumentException("出生年份或生日无效");
            }
        }
        if (!ageText.isEmpty()) {
            if (!ageText.matches("\\d{1,3}")) throw new IllegalArgumentException("年龄格式错误");
            if (year != 0) {
                int age = Integer.parseInt(ageText);
                int expected = today.getYear() - year;
                if (date != null) {
                    if (today.getMonthValue() < date.getMonthValue()
                            || (today.getMonthValue() == date.getMonthValue() && today.getDayOfMonth() < date.getDayOfMonth())) expected--;
                    if (age != expected) throw new IllegalArgumentException("年龄与生日不一致");
                } else if (age != expected && age != expected - 1) {
                    throw new IllegalArgumentException("年龄与出生年份不一致");
                }
            }
        }
        Range monthly = incomeRange(request.monthlyIncome());
        Range annual = incomeRange(request.annualIncome());
        if (monthly != null && annual != null
                && (annual.max.compareTo(monthly.min) < 0
                    || annual.min.compareTo(monthly.max.multiply(BigDecimal.valueOf(24))) > 0)) {
            throw new IllegalArgumentException("年收入与月收入明显不符");
        }
    }

    private static Range incomeRange(String value) {
        Matcher match = INCOME.matcher(trimmed(value).replaceAll("[,，\\s]", ""));
        if (!match.matches()) return null;
        boolean tenThousand = match.group(2) != null || match.group(4) != null;
        BigDecimal first = new BigDecimal(match.group(1)).multiply(BigDecimal.valueOf(tenThousand ? 10000 : 1));
        BigDecimal second = match.group(3) == null ? first
                : new BigDecimal(match.group(3)).multiply(BigDecimal.valueOf(tenThousand ? 10000 : 1));
        return first.signum() >= 0 && second.compareTo(first) >= 0 ? new Range(first, second) : null;
    }

    private static String trimmed(String value) { return value == null ? "" : value.trim(); }

    private record Range(BigDecimal min, BigDecimal max) {}
}
