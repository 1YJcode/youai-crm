package com.youai.crm.customer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CustomerImportValidatorTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private CustomerRequest request(String birthday, String age, String monthly, String annual) {
        return mapper.convertValue(Map.of(
                "name", "测试客户", "phone", "13800009999", "source", "批量导入", "owner", "白板",
                "birthday", birthday, "age", age, "monthlyIncome", monthly, "annualIncome", annual), CustomerRequest.class);
    }

    @Test
    void validatesFullBirthdayAroundThisYearsBirthday() {
        int year = LocalDate.now().getYear() - 30;
        assertDoesNotThrow(() -> CustomerImportValidator.validate(request(year + "-01-01", "30", "", "")));
        assertThrows(IllegalArgumentException.class, () -> CustomerImportValidator.validate(request(year + "-01-01", "29", "", "")));
        assertDoesNotThrow(() -> CustomerImportValidator.validate(request(year + "-12-31", "29", "", "")));
        assertThrows(IllegalArgumentException.class, () -> CustomerImportValidator.validate(request(year + "-12-31", "30", "", "")));
    }

    @Test
    void acceptsBothPossibleAgesForBirthYear() {
        String year = String.valueOf(LocalDate.now().getYear() - 30);
        assertDoesNotThrow(() -> CustomerImportValidator.validate(request(year, "29", "", "")));
        assertDoesNotThrow(() -> CustomerImportValidator.validate(request(year, "30", "", "")));
        assertThrows(IllegalArgumentException.class, () -> CustomerImportValidator.validate(request(year, "28", "", "")));
    }

    @Test
    void flagsOnlyObviousIncomeDiscrepancies() {
        assertDoesNotThrow(() -> CustomerImportValidator.validate(request("", "", "10000", "180000")));
        assertDoesNotThrow(() -> CustomerImportValidator.validate(request("", "", "8001-12000元", "150000")));
        assertDoesNotThrow(() -> CustomerImportValidator.validate(request("", "", "面议", "150000")));
        assertThrows(IllegalArgumentException.class, () -> CustomerImportValidator.validate(request("", "", "10000", "9000")));
        assertThrows(IllegalArgumentException.class, () -> CustomerImportValidator.validate(request("", "", "1-2万", "50万")));
    }
}
