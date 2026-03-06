package com.bisayaspeak.ai.data.repository

import com.bisayaspeak.ai.data.remote.FirebaseManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebaseデータ削除リポジトリ
 * LV32-35データの緊急削除用
 */
@Singleton
class FirebaseDataCleanupRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {
    
    /**
     * LV32-35のデータを削除（ロールバック）
     */
    suspend fun rollbackLevel32To35Data(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val database = firebaseManager.getDatabase()
                val listeningRef = database.getReference("listening_practice")
                
                // 削除対象のID範囲：311-350
                val idsToDelete = (311L..350L).toList()
                
                idsToDelete.forEach { id ->
                    listeningRef.child(id.toString()).removeValue().await()
                }
                
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 現在のFirebaseデータ状態を確認
     */
    suspend fun checkCurrentDataStatus(): Result<List<Long>> {
        return withContext(Dispatchers.IO) {
            try {
                val database = firebaseManager.getDatabase()
                val listeningRef = database.getReference("listening_practice")
                
                val snapshot = listeningRef.get().await()
                val existingIds = mutableListOf<Long>()
                
                snapshot.children.forEach { childSnapshot ->
                    val id = childSnapshot.key?.toLongOrNull()
                    if (id != null) {
                        existingIds.add(id)
                    }
                }
                
                Result.success(existingIds.sorted())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * LV31のみが存在することを確認
     */
    suspend fun verifyOnlyLevel31Exists(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val statusResult = checkCurrentDataStatus()
                if (statusResult.isFailure) {
                    return@withContext Result.failure(
                        statusResult.exceptionOrNull() ?: Exception("データ取得失敗")
                    )
                }
                
                val existingIds = statusResult.getOrNull() ?: emptyList()
                
                // LV31のID範囲：301-310
                val level31Ids = (301L..310L).toSet()
                val existingIdSet = existingIds.toSet()
                
                // LV31のみが存在し、他のレベルが存在しないことを確認
                val onlyLevel31Exists = existingIdSet.all { it in level31Ids } &&
                                      level31Ids.all { it in existingIdSet }
                
                Result.success(onlyLevel31Exists)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
