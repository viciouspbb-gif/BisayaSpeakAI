# Bisaya Speak AI - ビルド手順

## 🚀 クイックスタート

### 前提条件
- Android Studio Hedgehog (2023.1.1) 以上
- JDK 17
- Android SDK 34

### ビルド手順

#### 方法1: Android Studioでビルド（推奨）

1. **Android Studioを起動**

2. **プロジェクトを開く**
   ```
   File → Open
   → C:\Users\katsunori\CascadeProjects\BisayaSpeakAI を選択
   ```

3. **Gradle Syncを待つ**
   - 初回は依存関係のダウンロードに数分かかります
   - 下部のステータスバーで進捗を確認

4. **ビルド**
   ```
   Build → Make Project
   または
   Ctrl + F9 (Windows)
   ```

5. **実行**
   ```
   Run → Run 'app'
   または
   Shift + F10 (Windows)
   ```

#### 方法2: コマンドラインでビルド

```powershell
# プロジェクトディレクトリに移動
cd C:\Users\katsunori\CascadeProjects\BisayaSpeakAI

# デバッグビルド
.\gradlew assembleDebug

# リリースビルド（署名が必要）
.\gradlew assembleRelease

# APKの場所
# app\build\outputs\apk\debug\app-debug.apk
# app\build\outputs\apk\release\app-release.apk
```

## ⚠️ 現在の状況

### 実装済み
✅ プロジェクト構造
✅ build.gradle.kts
✅ AndroidManifest.xml
✅ データモデル
✅ API定義
✅ リポジトリ
✅ ViewModel
✅ UI画面（Compose）
✅ AdManager
✅ アイコン定義

### 注意事項

**重要**: このプロジェクトは完全なコード実装を含んでいますが、実際にビルドして実行するには以下が必要です：

1. **Pythonサーバーの起動**
   ```powershell
   cd C:\Users\katsunori\CascadeProjects\bisaya-pronunciation-server
   python main.py
   ```

2. **サーバーURL設定**
   - エミュレータ: `http://10.0.2.2:8000/`
   - 実機: PCのIPアドレスに変更

3. **AdMob設定**
   - 現在はテスト用広告ID
   - 本番環境では実際の広告IDに変更

## 🔧 トラブルシューティング

### Gradle Sync失敗
```powershell
# Gradleキャッシュをクリア
.\gradlew clean

# または Android Studio で
File → Invalidate Caches → Invalidate and Restart
```

### ビルドエラー
```
Build → Clean Project
Build → Rebuild Project
```

### 依存関係エラー
```
File → Sync Project with Gradle Files
```

## 📱 実行オプション

### エミュレータで実行
1. AVD Managerでエミュレータを作成
2. API Level 24以上を選択
3. Run → Run 'app'

### 実機で実行
1. デバイスで開発者オプションを有効化
2. USBデバッグを有効化
3. PCに接続
4. Run → Run 'app'

## 🎯 次のステップ

ビルド成功後：
1. ✅ アプリが起動する
2. ✅ レベル選択画面が表示される
3. ✅ 練習画面でバナー広告が表示される
4. ✅ 録音・診断機能が動作する
5. ✅ 結果画面でスコアが表示される

## 📝 ビルド成果物

### デバッグビルド
```
app/build/outputs/apk/debug/app-debug.apk
```

### リリースビルド
```
app/build/outputs/apk/release/app-release.apk
app/build/outputs/bundle/release/app-release.aab
```

## 🚀 リリース準備

リリースビルドには署名が必要です：

```powershell
# 署名鍵の生成
keytool -genkey -v -keystore bisaya-speak-ai.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bisaya-speak-ai

# build.gradle.kts に署名設定を追加
# または Android Studio の Generate Signed Bundle を使用
```

詳細は `FINAL_LAUNCH_GUIDE.md` を参照してください。
