package com.ecommerce.customer.domain.repository;

import com.ecommerce.customer.domain.model.Feedback;

/** Port: storage of {@link Feedback} aggregates. */
public interface FeedbackRepository {

    Feedback save(Feedback feedback);
}
