package com.example.wehomework.adapter.`in`.web

data class ErrorResponse(
    val success: Boolean,
    val message: String,
    val errorCode: String,
    val data: Any? = null
)
