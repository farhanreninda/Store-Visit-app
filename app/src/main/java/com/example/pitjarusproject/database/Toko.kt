package com.example.pitjarusproject.database

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Suppress("SpellCheckingInspection")
@Entity(tableName = "toko")
@Parcelize
class Toko(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    var store_id: String = "",
    var store_code: String = "",
    var store_name: String = "",
    var address: String = "",
    var dc_id: String = "",
    var dc_name: String = "",
    var account_id: String = "",
    var account_name: String = "",
    var subchannel_id: String = "",
    var subchannel_name: String = "",
    var channel_id: String = "",
    var channel_name: String = "",
    var area_id: String = "",
    var area_name: String = "",
    var region_id: String = "",
    var region_name: String = "",
    var latitude: String = "",
    var longitude: String = "",
    var last_visit: String = ""
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        return if (other is Toko) {
            other.id == id
        } else false
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (id.hashCode())
        return result
    }
}