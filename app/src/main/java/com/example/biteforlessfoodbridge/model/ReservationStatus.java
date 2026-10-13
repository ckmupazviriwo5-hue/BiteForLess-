package com.example.biteforlessfoodbridge.model;

/** Reservation lifecycle: Available → Reserved → Completed / Cancelled / Expired. */
public enum ReservationStatus {
    RESERVED,
    COMPLETED,
    CANCELLED,
    EXPIRED
}
