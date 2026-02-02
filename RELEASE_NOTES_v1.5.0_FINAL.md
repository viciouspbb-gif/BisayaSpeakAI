# Release Notes - Version 1.5.0

## Google Play Console用リリースノート（日本語・英語併記）

**新機能とユーザー体験の向上 / New Features and Enhanced User Experience**

【新機能 / New Features】
• AI会話に中級・上級レベルを追加（プレミアム限定）
  Added intermediate and advanced levels for AI conversation (Premium only)

• リスニング練習の音声速度が学習進度に応じて自動調整
  Listening practice speed now adjusts automatically based on your progress

• 会話メッセージに音声再生ボタンを追加
  Added audio playback button for conversation messages

【改善 / Improvements】
• リスニング問題の表示を改善
  Improved listening question display

• アプリの安定性を向上
  Enhanced app stability

• ユーザーインターフェースを最適化
  Optimized user interface

引き続きビサヤ語学習をお楽しみください！
Continue enjoying your Bisaya language learning journey!

---

## リリース手順

### 1. ビルド
```powershell
cd C:\Users\katsunori\CascadeProjects\BisayaSpeakAI
.\gradlew clean
.\gradlew assembleRelease
```

### 2. APKの場所
```
C:\Users\katsunori\CascadeProjects\BisayaSpeakAI\app\build\outputs\apk\release\app-release.apk
```

### 3. Google Play Consoleにアップロード
1. Google Play Console にログイン
2. BisayaSpeakAI アプリを選択
3. リリース → 本番環境 → 新しいリリースを作成
4. APKをアップロード
5. リリースノートを貼り付け（上記の日英併記版）
6. 審査に送信

### 4. バージョン情報
- Version Code: 16
- Version Name: 1.5.0

---

## 内部変更ログ（開発者用）

### リスニング機能
- 音声速度の動的調整（0.6〜1.0倍速）
- 既出問題の管理
- 広告の自動表示（ダイアログ削除）
- バナー広告追加（上部）
- 大文字小文字の統一

### 発音診断機能
- 診断完了時にCM自動表示
- バナー広告（下部）

### AI会話機能
- バナー広告追加（上部）
- 5ターンごとのCM自動表示
- ユーザーメッセージにもスピーカーボタン
- セッション開始時にターンカウントリセット
- 中級・上級を有効化（プレミアム限定）

### Billing
- 開発者アカウント（vicious.pbb@gmail.com）は常にプレミアム扱い
- デバッグビルドは常にプレミアム扱い
