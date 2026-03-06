package com.bisayaspeak.ai.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Realtime Database マネージャー
 * リスニング練習データの管理
 */
@Singleton
class FirebaseManager @Inject constructor() {
    
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val listeningPracticeRef: DatabaseReference = database.getReference("listening_practice")
    
    /**
     * データベース参照を取得（外部アクセス用）
     */
    fun getDatabase(): FirebaseDatabase = database
    
    /**
     * リスニング練習データ参照を取得
     */
    fun getListeningPracticeRef(): DatabaseReference = listeningPracticeRef
    
    /**
     * リスニング練習データを追加
     */
    suspend fun addListeningPracticeData(data: List<Map<String, Any>>): Result<Unit> {
        return try {
            // バッチ処理でデータを追加
            val updates = mutableMapOf<String, Any>()
            
            data.forEach { item ->
                val id = item["id"] as Long
                updates["$id"] = item
            }
            
            listeningPracticeRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * リスニング練習データを取得
     */
    suspend fun getListeningPracticeData(): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = listeningPracticeRef.get().await()
            val data = snapshot.children.mapNotNull { 
                it.value as? Map<String, Any>
            }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 特定IDのデータを取得
     */
    suspend fun getListeningPracticeById(id: Long): Result<Map<String, Any>?> {
        return try {
            val snapshot = listeningPracticeRef.child(id.toString()).get().await()
            val data = snapshot.value as? Map<String, Any>
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * データを更新
     */
    suspend fun updateListeningPracticeData(id: Long, data: Map<String, Any>): Result<Unit> {
        return try {
            listeningPracticeRef.child(id.toString()).setValue(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * データを削除
     */
    suspend fun deleteListeningPracticeData(id: Long): Result<Unit> {
        return try {
            listeningPracticeRef.child(id.toString()).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
