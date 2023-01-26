package com.example.pitjarusproject.api.response

import com.google.gson.annotations.SerializedName

data class Response(

    @field:SerializedName("stores")
    val stores: List<StoresItem>,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("status")
    val status: String? = null
)

data class StoresItem(

    @field:SerializedName("store_id")
    val storeId: String? = null,

    @field:SerializedName("store_code")
    val storeCode: String? = null,

    @field:SerializedName("channel_name")
    val channelName: String? = null,

    @field:SerializedName("area_name")
    val areaName: String? = null,

    @field:SerializedName("address")
    val address: String? = null,

    @field:SerializedName("dc_name")
    val dcName: String? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("region_id")
    val regionId: String? = null,

    @field:SerializedName("area_id")
    val areaId: String? = null,

    @field:SerializedName("account_id")
    val accountId: String? = null,

    @field:SerializedName("dc_id")
    val dcId: String? = null,

    @field:SerializedName("subchannel_id")
    val subchannelId: String? = null,

    @field:SerializedName("account_name")
    val accountName: String? = null,

    @field:SerializedName("store_name")
    val storeName: String? = null,

    @field:SerializedName("subchannel_name")
    val subchannelName: String? = null,

    @field:SerializedName("region_name")
    val regionName: String? = null,

    @field:SerializedName("channel_id")
    val channelId: String? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null
)
