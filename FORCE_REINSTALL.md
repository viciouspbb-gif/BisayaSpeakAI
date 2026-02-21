# 強制再インストール手順

## 問題
ビルドは成功しているが、UIが更新されない

## 原因
- 古いAPKが完全に削除されていない
- データキャッシュが残っている
- 複数のビルドバリアントが混在している

## 完全削除＆再インストール手順

### 方法1: ADBコマンドで完全削除（推奨）

```bash
# 1. 現在インストールされているアプリを確認
adb shell pm list packages | findstr bisayaspeak

# 2. アプリとデータを完全に削除
adb uninstall com.bisayaspeak.ai

# 3. キャッシュも削除
adb shell pm clear com.bisayaspeak.ai

# 4. 新しいAPKをインストール
adb install D:\BisayaSpeakAI\app\build\outputs\apk\debug\app-debug.apk

# 5. アプリを起動
adb shell am start -n com.bisayaspeak.ai/.MainActivity
```

### 方法2: 実機で手動削除

```
1. 設定 > アプリ > BisayaSpeakAI
2. ストレージとキャッシュ > ストレージを消去
3. ストレージとキャッシュ > キャッシュを削除
4. アンインストール
5. Android Studioから Run > Run 'app'
```

### 方法3: Android Studioから完全再インストール

```
1. Run > Edit Configurations...
2. Installation Options セクション
3. "Deploy" を "APK from app bundle" に変更
4. "Always install with package manager" にチェック
5. OK
6. Run > Run 'app'
```

## 確認方法

### 1. インストールされているAPKのバージョンを確認

```bash
adb shell dumpsys package com.bisayaspeak.ai | findstr versionName
```

現在のバージョンは `1.8.1` のはずです。

### 2. APKのビルド日時を確認

```bash
# APKファイルの更新日時を確認
dir D:\BisayaSpeakAI\app\build\outputs\apk\debug\app-debug.apk
```

最新のビルド時刻と一致しているか確認してください。

### 3. Logcatでログを確認

```
1. Android Studio > Logcat
2. フィルタ: PracticeQuiz
3. アプリを起動して発音練習を実行
4. 以下のログが出力されるか確認:

D/PracticeQuiz: Question changed: -1 -> 0
D/PracticeQuiz: TryAgainCount reset to 0
D/PracticeQuiz: Score: XX
D/PracticeQuiz: Result: TRY_AGAIN
D/PracticeQuiz: TryAgainCount: 1 / 3
```

## トラブルシューティング

### ログが全く出ない場合

古いコードが実行されています。以下を試してください:

```bash
# 1. すべてのGradleプロセスを停止
taskkill /F /IM java.exe

# 2. ビルドディレクトリを完全削除
cd D:\BisayaSpeakAI
Remove-Item -Recurse -Force app\build
Remove-Item -Recurse -Force build
Remove-Item -Recurse -Force .gradle

# 3. Android Studioを再起動

# 4. Gradle Syncを実行
# File > Sync Project with Gradle Files

# 5. クリーンビルド
.\gradlew clean assembleDebug

# 6. アプリを完全削除
adb uninstall com.bisayaspeak.ai

# 7. 新しいAPKをインストール
adb install app\build\outputs\apk\debug\app-debug.apk
```

### 複数のビルドバリアントがある場合

```
1. Android Studio > Build Variants ウィンドウを開く
2. "debug" が選択されているか確認
3. 他のバリアントがある場合は "debug" に切り替え
4. Build > Rebuild Project
```

### それでも反映されない場合

APKを直接確認します:

```bash
# APKの内容を確認
cd D:\BisayaSpeakAI\app\build\outputs\apk\debug
jar -tf app-debug.apk | findstr PracticeQuizScreen

# 出力例:
# com/bisayaspeak/ai/ui/screens/PracticeQuizScreen.class
# com/bisayaspeak/ai/ui/screens/PracticeQuizScreenKt.class
```

クラスファイルが存在すれば、コードは正しくコンパイルされています。

## 最終確認

新しいUIが表示されているか確認:

### Try Again判定時に表示されるべきもの:
- ✅ ❌ アイコン
- ✅ 赤い "Try Again" ラベル
- ✅ **「失敗回数：1 / 3」**（赤枠の小さいカード）
- ✅ **パルスアニメーション付き赤枠カード**（ゆっくり拡大縮小）
- ✅ **「もう一度挑戦しましょう（1回目）」**

### Logcatに表示されるべきログ:
```
D/PracticeQuiz: Score: XX
D/PracticeQuiz: Result: TRY_AGAIN
D/PracticeQuiz: TryAgainCount: 1 / 3
```

これらが表示されない場合は、まだ古いコードが実行されています。
