package com.youai.crm.customer;

import com.youai.crm.account.AccessPolicy;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerPhoneCheckControllerTest {
    CustomerRepository repository = mock(CustomerRepository.class);
    AccessPolicy policy = mock(AccessPolicy.class);
    Authentication auth = mock(Authentication.class);
    CustomerPhoneCheckController controller = new CustomerPhoneCheckController(repository, policy);
    @Test void rejectsInvalidPhoneWithoutQuery() {
        assertThrows(ResponseStatusException.class, () -> controller.check(new CustomerPhoneCheckController.PhoneCheckRequest("123", null), auth));
        verifyNoInteractions(repository);
    }
    @Test void reportsDuplicateWithoutExposingDetails() {
        Customer customer = mock(Customer.class);
        when(customer.getCustomerNo()).thenReturn("existing");
        when(repository.findByPhone("13132196202")).thenReturn(Optional.of(customer));
        assertTrue(controller.check(new CustomerPhoneCheckController.PhoneCheckRequest("13132196202", null), auth).duplicate());
        verify(customer, never()).getPhone();
    }
    @Test void acceptsUnusedPhone() {
        when(repository.findByPhone("13132196202")).thenReturn(Optional.empty());
        assertFalse(controller.check(new CustomerPhoneCheckController.PhoneCheckRequest("13132196202", null), auth).duplicate());
    }
    @Test void excludesEditableCurrentRecord() {
        Customer customer = mock(Customer.class);
        when(customer.getCustomerNo()).thenReturn("self");
        when(repository.findByCustomerNo("self")).thenReturn(Optional.of(customer));
        when(repository.findByPhone("13132196202")).thenReturn(Optional.of(customer));
        assertFalse(controller.check(new CustomerPhoneCheckController.PhoneCheckRequest("13132196202", "self"), auth).duplicate());
        verify(policy).requireUserId(customer.getOwnerId(), auth);
    }
    @Test void rejectsUnauthorizedExclusion() {
        Customer customer = mock(Customer.class);
        when(customer.getOwnerId()).thenReturn(42L);
        when(repository.findByCustomerNo("other")).thenReturn(Optional.of(customer));
        doThrow(new AccessDeniedException("denied")).when(policy).requireUserId(42L, auth);
        assertThrows(AccessDeniedException.class, () -> controller.check(new CustomerPhoneCheckController.PhoneCheckRequest("13132196202", "other"), auth));
        verify(repository, never()).findByPhone(anyString());
    }
}
