package app.quranaudio

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import app.quranaudio.shared.quran.SurahCatalog
import javax.inject.Inject

class SkeletonProbe @Inject constructor() {
    fun text() = SurahCatalog.get(1).nameArabic
}

@Entity data class ProbeEntity(@PrimaryKey val id: String)
@Dao interface ProbeDao { @Query("SELECT * FROM ProbeEntity") suspend fun all(): List<ProbeEntity> }
@Database(entities = [ProbeEntity::class], version = 1, exportSchema = false)
abstract class ProbeDb : RoomDatabase() { abstract fun dao(): ProbeDao }
