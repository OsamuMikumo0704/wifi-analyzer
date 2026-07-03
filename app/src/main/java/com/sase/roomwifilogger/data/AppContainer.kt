package com.sase.roomwifilogger.data

import android.content.Context
import com.sase.roomwifilogger.data.db.AppDatabase

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.create(context)
}
