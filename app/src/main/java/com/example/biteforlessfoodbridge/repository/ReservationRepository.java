package com.example.biteforlessfoodbridge.repository;

import com.example.biteforlessfoodbridge.model.Reservation;

import java.util.List;

/**
 * Single source of truth contract for reservations (Room impl lands in Phase 6).
 *
 * <p>Note (Phase 3): {@code getAll reservations} / {@code getReservationById}
 * are named to avoid erasure clashes with {@link ListingRepository}, since one
 * shared {@code DemoRepository} implements both contracts.
 */
public interface ReservationRepository {
    List<Reservation> getAllReservations();
    List<Reservation> getForUser(String userId);
    List<Reservation> getForListing(String listingId);
    Reservation getReservationById(String id);
    Reservation create(String listingId, String userId, int quantity) throws IllegalStateException;
    void cancel(String reservationId) throws IllegalStateException;
    void complete(String reservationId) throws IllegalStateException;
}
