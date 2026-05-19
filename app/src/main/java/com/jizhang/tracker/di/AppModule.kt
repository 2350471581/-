package com.jizhang.tracker.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.jizhang.tracker.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "bill_tracker.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
}

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("plan_prefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideDeepSeekService(deepSeekClient: DeepSeekClient): DeepSeekService =
        DeepSeekService(deepSeekClient)

    @Provides
    @Singleton
    fun provideDeepSeekClient(client: OkHttpClient): DeepSeekClient =
        DeepSeekClient(client)

    @Provides
    @Singleton
    fun provideAIBillService(deepSeekClient: DeepSeekClient, planStorage: PlanStorage): AIBillService =
        AIBillService(deepSeekClient, planStorage)
}
