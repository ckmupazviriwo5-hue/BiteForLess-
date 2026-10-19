package com.example.biteforlessfoodbridge.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FoodListingDao {
    @Query("SELECT * FROM listings WHERE id = :id LIMIT 1")
    FoodListingEntity getById(String id);

    @Query("SELECT * FROM listings WHERE vendorId = :vendorId")
    List<FoodListingEntity> getForVendor(String vendorId);

    @Query("SELECT * FROM listings")
    List<FoodListingEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FoodListingEntity listing);

    @Update
    void update(FoodListingEntity listing);

    @Query("DELETE FROM listings WHERE id = :id")
    void delete(String id);
}
