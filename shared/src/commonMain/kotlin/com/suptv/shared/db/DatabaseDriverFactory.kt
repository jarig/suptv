package com.suptv.shared.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): SupTvDatabase {
    val driver = driverFactory.createDriver()
    return SupTvDatabase(driver)
}
