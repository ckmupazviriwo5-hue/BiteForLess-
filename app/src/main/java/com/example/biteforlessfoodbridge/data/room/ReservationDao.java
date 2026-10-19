package com.example.biteforlessfoodbridge.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReservationDao {
    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    ReservationEntity getById(String id);

    @Query("SELECT * FROM reservations WHERE userId = :userId")
    List<ReservationEntity> getForUser(String userId);

    @Query("SELECT * FROM reservations WHERE listingId = :listingId")
    List<ReservationEntity> getForListing(String listingId);

    @Query("SELECT * FROM reservations")
    List<ReservationEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReservationEntity reservation);

    @Update
    void update(ReservationEntity reservation);

    @Query("DELETE FROM reservations WHERE id = :id")
    void delete(String id);
}
