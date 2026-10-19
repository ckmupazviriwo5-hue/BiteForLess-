package com.example.biteforlessfoodbridge.data.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ActivityDao {
    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC")
    List<ActivityEventEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ActivityEventEntity event);
}
