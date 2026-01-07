package com.bisayaspeak.ai.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication マネージャー
 */
class AuthManager(private val context: Context) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * 現在のユーザーを取得
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    /**
     * ログイン状態を確認
     */
    val isLoggedIn: Boolean
        get() = currentUser != null
    
    /**
     * メールアドレスとパスワードでアカウント作成
     */
    suspend fun createAccount(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("アカウント作成に失敗しました"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * メールアドレスとパスワードでログイン
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("ログインに失敗しました"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Googleアカウントでログイン
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Googleログインに失敗しました"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * ログアウト
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * パスワードリセットメールを送信
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * メールアドレスを更新
     */
    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            currentUser?.updateEmail(newEmail)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * パスワードを更新
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            currentUser?.updatePassword(newPassword)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * アカウントを削除
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
