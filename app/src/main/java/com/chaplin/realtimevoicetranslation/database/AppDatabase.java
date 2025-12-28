

package com.chaplin.realtimevoicetranslation.database;

import androidx.room.RoomDatabase;
import com.chaplin.realtimevoicetranslation.database.dao.MyDao;
import com.chaplin.realtimevoicetranslation.database.entities.RecentPeerEntity;


@androidx.room.Database(version = 1, entities = {RecentPeerEntity.class})
abstract public class AppDatabase extends RoomDatabase {
    abstract public MyDao myDao();
}
