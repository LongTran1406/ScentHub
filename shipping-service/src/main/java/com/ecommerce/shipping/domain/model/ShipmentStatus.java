package com.ecommerce.shipping.domain.model;

import com.ecommerce.shipping.domain.exception.InvalidShipmentStatusException;

/** Value object (enumeration): where a shipment is in its journey. */
public enum ShipmentStatus {
    CREATED,
    DISPATCHED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED;

    /** @throws InvalidShipmentStatusException if the text is not one of the known statuses */
    public static ShipmentStatus parse(String text) {
        if (text != null) {
            for (ShipmentStatus status : values()) {
                if (status.name().equals(text)) {
                    return status;
                }
            }
        }
        throw new InvalidShipmentStatusException("Unknown status: " + text);
    }

    /** True once the parcel has left (or is being handed to the customer): it can no longer be cancelled. */
    public boolean hasLeftWarehouse() {
        return this == DISPATCHED || this == IN_TRANSIT || this == DELIVERED;
    }
}
