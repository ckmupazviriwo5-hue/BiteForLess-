package com.example.biteforlessfoodbridge.data.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        UserEntity.class,
        ShopEntity.class,
        FoodListingEntity.class,
        ReservationEntity.class,
        ReviewEntity.class,
        ActivityEventEntity.class
}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "bite_for_less.db";
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract ShopDao shopDao();
    public abstract FoodListingDao foodListingDao();
    public abstract ReservationDao reservationDao();
    public abstract ReviewDao reviewDao();
    public abstract ActivityDao activityDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
