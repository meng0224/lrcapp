package com.example.lrcapp.model

data class AppSettings(
    var smartNaming: Boolean = true,      // 智能命名清理
    var timePrecision: Boolean = true     // 時間精度優化
)
