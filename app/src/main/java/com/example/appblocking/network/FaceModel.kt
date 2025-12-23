package com.example.appblocking.network

import com.google.gson.annotations.SerializedName

class FaceModel (
    @SerializedName("user_id") val userId: String,

    @SerializedName("embedding_center") val embeddingCenter: String?,
    @SerializedName("embedding_left") val embeddingLeft: String?,
    @SerializedName("embedding_right") val embeddingRight: String?
)