package com.example.biteforlessfoodbridge.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    ReviewEntity getById(String id);

    @Query("SELECT * FROM reviews WHERE listingId = :listingId")
    List<ReviewEntity> getForListing(String listingId);

    @Query("SELECT * FROM reviews")
    List<ReviewEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReviewEntity review);

    @Update
    void update(ReviewEntity review);

    @Query("DELETE FROM reviews WHERE id = :id")
    void delete(String id);
}
