package com.example.billtracker

import android.app.Application
import com.example.billtracker.data.AppDatabase

class BillTrackerApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
