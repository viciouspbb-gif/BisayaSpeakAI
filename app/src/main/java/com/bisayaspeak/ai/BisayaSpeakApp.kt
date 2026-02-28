package com.bisayaspeak.ai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BisayaSpeakApp : Application() {
    // 中身は空でいい。Configuration.Providerも、変数もすべて一度消せ（あるいはコメントアウト）
}
