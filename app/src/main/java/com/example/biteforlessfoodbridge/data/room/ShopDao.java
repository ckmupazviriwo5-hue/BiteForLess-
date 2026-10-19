package com.example.biteforlessfoodbridge.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ShopDao {
    @Query("SELECT * FROM shops WHERE id = :id LIMIT 1")
    ShopEntity getById(String id);

    @Query("SELECT * FROM shops WHERE ownerId = :ownerId LIMIT 1")
    ShopEntity getByOwnerId(String ownerId);

    @Query("SELECT * FROM shops")
    List<ShopEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ShopEntity shop);

    @Update
    void update(ShopEntity shop);

    @Query("DELETE FROM shops WHERE id = :id")
    void delete(String id);
}
