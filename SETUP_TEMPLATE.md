# BisayaSpeakAI プロジェクト設定テンプレート
# ダバオのサブノートPCで作業を開始する際、この内容を local.properties に貼り付けてください

# ======================
# Android SDK Path (自動的に設定される場合あり)
# ======================
# Android Studioが自動的に設定するため、通常は手動設定不要
# sdk.dir=C:/Users/[ユーザー名]/AppData/Local/Android/Sdk

# ======================
# 署名鍵関連 (必須)
# ======================
# 署名鍵ファイルの絶対パスを指定
RELEASE_STORE_FILE=C:/BisayaSpeakAI/bisaya-speak-ai.jks
# 署名鍵のパスワード
RELEASE_STORE_PASSWORD=Bisaya2025
# キーのエイリアス
RELEASE_KEY_ALIAS=bisaya-speak-ai
# キーのパスワード
RELEASE_KEY_PASSWORD=Bisaya2025

# ======================
# APIキー類 (必須)
# ======================
# Gemini AI APIキー
GEMINI_API_KEY=ここにGemini_API_KEYを貼り付け
# OpenAI APIキー
OPENAI_API_KEY=ここにOpenAI_API_KEYを貼り付け

# ======================
# サーバー設定 (任意、デフォルト値あり)
# ======================
# バックエンドサーバーのURL (デフォルト: https://bisaya-speak-ai-server-1.onrender.com)
SERVER_BASE_URL=https://bisaya-speak-ai-server-1.onrender.com

# ======================
# AdMob設定 (リリース用 - 本番環境)
# ======================
# AdMobアプリID (リリース用)
ADMOB_APP_ID_RELEASE=ca-app-pub-2676999942952051~8841279040
# 広告ユニットID (リリース用)
AD_UNIT_ID_RELEASE=ca-app-pub-2676999942952051/3507292281
# バナー広告ユニットID (リリース用)
BANNER_AD_UNIT_ID_RELEASE=ca-app-pub-2676999942952051/3507292281
# インタースティシャル広告ユニットID (リリース用)
INTERSTITIAL_AD_UNIT_ID_RELEASE=ca-app-pub-2676999942952051/2023179674
# リワード広告ユニットID (リリース用)
REWARDED_AD_UNIT_ID_RELEASE=ca-app-pub-2676999942952051/7348562506

# ======================
# 開発モード設定 (任意)
# ======================
# DEBUG_MODE=true の場合、デバッグ機能が有効になる
DEBUG_MODE=false

# ======================
# 開発者ホワイトリスト (任意、デバッグ時のみ有効)
# ======================
# 特定のメールアドレスを開発者として扱う場合に設定
# DEBUG_WHITELIST_EMAIL=your-email@example.com

# ======================
# 重要な注意事項
# ======================
# 1. このファイルは .gitignore に含まれており、GitHubにはアップロードされません
# 2. APIキーなどの機密情報は安全に管理してください
# 3. 署名鍵ファイル (.jks) も .gitignore に含まれています
# 4. google-services.json ファイルも手動コピーが必要です (app/ ディレクトリに配置)
# 5. 設定完了後、プロジェクトルートで ./gradlew :app:bundleProRelease を実行してビルドテストしてください
