package com.ecommerce.customer.domain;

import com.ecommerce.customer.domain.exception.InvalidCustomerDataException;
import com.ecommerce.customer.domain.model.Complaint;
import com.ecommerce.customer.domain.model.ComplaintStatus;
import com.ecommerce.customer.domain.model.Customer;
import com.ecommerce.customer.domain.model.Email;
import com.ecommerce.customer.domain.model.Feedback;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerDomainTest {

    @Test
    void emailIsNormalisedAndComparedByValue() {
        assertThat(new Email("  Jane@Example.COM ")).isEqualTo(new Email("jane@example.com"));
        assertThat(new Email("Jane@Example.COM").address()).isEqualTo("jane@example.com");
        assertThat(new Email("a@b.co").hashCode()).isEqualTo(new Email("A@B.CO").hashCode());
    }

    @Test
    void malformedEmailsAreRefused() {
        for (String bad : new String[]{null, "", "plain", "no@tld", "two@@x.com", "sp ace@x.com"}) {
            assertThatThrownBy(() -> new Email(bad)).as("email '" + bad + "'")
                    .isInstanceOf(InvalidCustomerDataException.class);
        }
    }

    @Test
    void registerBuildsACustomerWithATrimmedNameAndNormalisedEmail() {
        Customer customer = Customer.register("  Jane Doe ", "JANE@X.com");

        assertThat(customer.getName()).isEqualTo("Jane Doe");
        assertThat(customer.getEmail().address()).isEqualTo("jane@x.com");
        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getCreatedAt()).isNotNull();
    }

    @Test
    void registerNeedsANameAndAValidEmail() {
        assertThatThrownBy(() -> Customer.register(" ", "a@b.co")).isInstanceOf(InvalidCustomerDataException.class);
        assertThatThrownBy(() -> Customer.register(null, "a@b.co")).isInstanceOf(InvalidCustomerDataException.class);
        assertThatThrownBy(() -> Customer.register("Jane", "nope")).isInstanceOf(InvalidCustomerDataException.class);
    }

    @Test
    void updateProfileChangesOnlyTheProvidedFields() {
        Customer customer = Customer.register("Jane", "jane@x.com");

        customer.updateProfile(null, "new@x.com");
        assertThat(customer.getName()).isEqualTo("Jane");
        assertThat(customer.getEmail().address()).isEqualTo("new@x.com");

        customer.updateProfile("Janet", null);
        assertThat(customer.getName()).isEqualTo("Janet");
        assertThat(customer.getEmail().address()).isEqualTo("new@x.com");

        assertThatThrownBy(() -> customer.updateProfile("", null)).isInstanceOf(InvalidCustomerDataException.class);
        assertThatThrownBy(() -> customer.updateProfile(null, "bad")).isInstanceOf(InvalidCustomerDataException.class);
    }

    @Test
    void feedbackAcceptsAnOptionalRatingBetweenOneAndFive() {
        UUID customerId = UUID.randomUUID();

        assertThat(Feedback.submit(customerId, "Great", null).getRating()).isNull();
        assertThat(Feedback.submit(customerId, "Great", 1).getRating()).isEqualTo(1);
        assertThat(Feedback.submit(customerId, "Great", 5).getRating()).isEqualTo(5);
        assertThatThrownBy(() -> Feedback.submit(customerId, "Great", 0))
                .isInstanceOf(InvalidCustomerDataException.class);
        assertThatThrownBy(() -> Feedback.submit(customerId, "Great", 6))
                .isInstanceOf(InvalidCustomerDataException.class);
        assertThatThrownBy(() -> Feedback.submit(customerId, " ", 3))
                .isInstanceOf(InvalidCustomerDataException.class);
    }

    @Test
    void aComplaintStartsOpenAndNeedsContent() {
        UUID customerId = UUID.randomUUID();

        Complaint complaint = Complaint.raise(customerId, " Parcel was late ");

        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.OPEN);
        assertThat(complaint.getContent()).isEqualTo("Parcel was late");
        assertThat(complaint.getCustomerId()).isEqualTo(customerId);
        assertThatThrownBy(() -> Complaint.raise(customerId, "")).isInstanceOf(InvalidCustomerDataException.class);
    }
}
