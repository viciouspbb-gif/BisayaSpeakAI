package com.bisayaspeak.ai.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    // テスト用ID
    const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val AD_UNIT_ID_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val AD_UNIT_ID_REWARD = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var playCounter = 0

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
        loadInterstitial(context)
        loadReward(context)
    }

    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, AD_UNIT_ID_INTERSTITIAL, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            })
    }

    fun loadReward(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, AD_UNIT_ID_REWARD, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    // 準備完了トースト（デバッグ用：うざければ消してOK）
                    Toast.makeText(context, "動画広告の準備完了！", Toast.LENGTH_SHORT).show()
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    // ★ここ！失敗したらエラー内容を表示する
                    Toast.makeText(context, "動画読込エラー: ${adError.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    fun checkAndShowInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        playCounter++
        // 2回に1回表示
        if (playCounter % 2 == 0) {
            if (interstitialAd != null) {
                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        loadInterstitial(activity)
                        onAdClosed()
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        interstitialAd = null
                        onAdClosed()
                    }
                }
                interstitialAd?.show(activity)
            } else {
                loadInterstitial(activity)
                onAdClosed()
            }
        } else {
            onAdClosed()
        }
    }

    fun showRewardAd(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadReward(activity)
                    onAdClosed()
                }
            }
            rewardedAd?.show(activity) { _ ->
                onRewardEarned()
            }
        } else {
            // 準備中ならそう伝える
            Toast.makeText(activity, "動画を読み込んでいます…数秒待ってね", Toast.LENGTH_SHORT).show()
            loadReward(activity)
        }
    }
}