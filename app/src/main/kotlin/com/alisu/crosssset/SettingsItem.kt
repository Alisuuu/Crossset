package com.alisu.crosssset

data class SettingsItem(
    val key: String,
    val value: String,
    val table: SettingsTable,
    val description: String? = null,
    val riskLevel: RiskLevel = RiskLevel.SAFE
)

enum class SettingsTable {
    SYSTEM, SECURE, GLOBAL
}

enum class RiskLevel {
    SAFE, MODERATE, DANGEROUS
}
