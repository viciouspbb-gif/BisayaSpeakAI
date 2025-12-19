# リリースガイド - Version 1.5.0

## ✅ 完了した作業

- [x] バージョン更新: 1.4.0 → 1.5.0
- [x] versionCode更新: 15 → 16
- [x] リスニング機能の改善実装
- [x] リリースノート作成

---

## 🚀 リリース手順

### ステップ1: ビルド前の確認

#### 1-1. コードの確認
```bash
# プロジェクトディレクトリに移動
cd C:\Users\katsunori\CascadeProjects\BisayaSpeakAI

# Gitで変更を確認（オプション）
git status
git diff
```

#### 1-2. バージョン確認
- [x] `app/build.gradle.kts`
  - versionCode: **16**
  - versionName: **"1.5.0"**

#### 1-3. APIキーの確認
```bash
# local.propertiesにGEMINI_API_KEYがあることを確認
notepad local.properties
```

---

### ステップ2: ビルド

#### 2-1. クリーンビルド
```bash
# Windows PowerShell または コマンドプロンプト
cd C:\Users\katsunori\CascadeProjects\BisayaSpeakAI

# クリーン
.\gradlew clean

# リリースビルド
.\gradlew assembleRelease
```

#### 2-2. ビルド成功の確認
ビルドが成功すると、以下の場所にAPKが生成されます：
```
C:\Users\katsunori\CascadeProjects\BisayaSpeakAI\app\build\outputs\apk\release\app-release.apk
```

#### 2-3. APKのサイズ確認
```bash
# ファイルサイズを確認（目安: 10-20MB）
dir app\build\outputs\apk\release\app-release.apk
```

---

### ステップ3: 署名確認

APKは自動的に署名されています（build.gradle.ktsで設定済み）

#### 署名情報
- **Keystore**: `bisaya-speak-ai.jks`
- **Alias**: `bisaya-speak-ai`
- **Password**: `Bisaya2025`

#### 署名確認コマンド（オプション）
```bash
# JDKのkeytoolで確認
keytool -printcert -jarfile app\build\outputs\apk\release\app-release.apk
```

---

### ステップ4: テスト

#### 4-1. 実機でのテスト
```bash
# APKを実機にインストール
adb install -r app\build\outputs\apk\release\app-release.apk

# または、Android Studioから実行
```

#### 4-2. テスト項目
- [ ] アプリが起動する
- [ ] リスニング機能が動作する
- [ ] 音声速度が調整される
- [ ] 問題の重複が減っている
- [ ] 広告が表示される
- [ ] セッション完了時に広告が表示される
- [ ] 既存の機能（発音診断、AI会話）が動作する

---

### ステップ5: Google Play Consoleへのアップロード

#### 5-1. Play Consoleにログイン
1. https://play.google.com/console にアクセス
2. BisayaSpeakAIアプリを選択

#### 5-2. 新しいリリースを作成
1. 左メニュー > **リリース** > **本番環境**
2. **新しいリリースを作成** をクリック

#### 5-3. APKをアップロード
1. **アプリバンドル** セクションで **アップロード** をクリック
2. `app-release.apk` を選択してアップロード
3. アップロード完了を待つ

#### 5-4. リリースノートを入力

**日本語（ja-JP）**
```
🎧 リスニング機能を大幅改善！

✨ 正解率に応じて音声速度が自動調整
- 上達すると速くなり、より実践的な練習が可能に
- 苦手な場合は遅くなり、初心者でも安心

📚 問題の重複を大幅削減
- 1セッション10問に最適化
- 既出問題を記録し、未使用問題を優先出題

🎯 学習体験の向上
- 個人のレベルに合わせた最適な難易度
- より効果的な学習が可能に

バグ修正と安定性の向上
```

**英語（en-US）**
```
🎧 Major Listening Feature Update!

✨ Auto-adjusting speech speed based on accuracy
- Speed increases as you improve for more practical practice
- Speed decreases when struggling for easier learning

📚 Reduced question repetition
- Optimized to 10 questions per session
- Prioritizes unused questions

🎯 Enhanced learning experience
- Difficulty adapts to your level
- More effective learning

Bug fixes and stability improvements
```

#### 5-5. リリース名を入力
```
1.5.0 - リスニング機能改善
```

---

### ステップ6: 段階的公開（推奨）

#### 6-1. 公開割合を設定
1. **段階的公開** を選択
2. 初回: **10%** のユーザーに公開
3. 問題がなければ: **50%** に拡大
4. 最終的に: **100%** に公開

#### 6-2. モニタリング期間
- 10%公開: 24時間モニタリング
- 50%公開: 48時間モニタリング
- 100%公開: 問題なければ完全公開

---

### ステップ7: 公開後のモニタリング

#### 7-1. クラッシュレート確認
1. Play Console > **品質** > **Android Vitals**
2. クラッシュレートを確認（目標: 1%未満）

#### 7-2. ユーザーレビュー確認
1. Play Console > **ユーザーからのフィードバック** > **評価とレビュー**
2. 新しいレビューをチェック
3. 問題報告に迅速に対応

#### 7-3. 広告表示率確認
1. AdMob管理画面にログイン
2. 広告表示回数とeCPMを確認
3. 期待値: 1セッションあたり平均3回の広告表示

---

## 🔧 トラブルシューティング

### ビルドエラーが発生した場合

#### エラー: "GEMINI_API_KEY not found"
```bash
# local.propertiesを確認
notepad local.properties

# 以下の行があることを確認
GEMINI_API_KEY=your_api_key_here
```

#### エラー: "Keystore not found"
```bash
# keystoreファイルがあることを確認
dir bisaya-speak-ai.jks

# なければ、バックアップから復元
```

#### エラー: "Build failed"
```bash
# Gradleキャッシュをクリア
.\gradlew clean --no-daemon

# Android Studioを再起動
# 再度ビルド
.\gradlew assembleRelease
```

### アップロードエラーが発生した場合

#### エラー: "Version code must be higher"
- versionCodeが前回より大きいことを確認
- 現在: 16（前回: 15）

#### エラー: "APK signature invalid"
- keystoreファイルとパスワードを確認
- 必要に応じて再署名

---

## 📊 リリース後の確認事項

### 24時間以内
- [ ] クラッシュレート < 1%
- [ ] 新しいレビューの確認
- [ ] 広告表示率の確認

### 1週間以内
- [ ] ダウンロード数の推移
- [ ] アクティブユーザー数
- [ ] 平均セッション時間
- [ ] 広告収益の変化

### 必要に応じて
- [ ] ホットフィックスの準備
- [ ] 次回アップデートの計画

---

## 📝 チェックリスト

### リリース前
- [x] コード変更の完了
- [x] バージョン番号の更新
- [x] リリースノートの作成
- [ ] ビルドの成功
- [ ] 実機でのテスト
- [ ] 署名の確認

### リリース時
- [ ] Play Consoleへのアップロード
- [ ] リリースノートの入力
- [ ] 段階的公開の設定
- [ ] 公開ボタンのクリック

### リリース後
- [ ] クラッシュレートの確認
- [ ] ユーザーレビューの確認
- [ ] 広告表示率の確認
- [ ] 問題がなければ100%公開

---

## 🆘 緊急時の対応

### 重大なバグが見つかった場合
1. **即座に公開を停止**
   - Play Console > リリース > 公開を停止
2. **ホットフィックスを準備**
   - バグを修正
   - versionCode: 17
   - versionName: 1.5.1
3. **緊急リリース**
   - 段階的公開をスキップして100%公開

### ロールバックが必要な場合
1. Play Console > リリース > 以前のバージョンに戻す
2. ユーザーに通知（必要に応じて）

---

## 📞 連絡先

### サポート
- メール: （あなたのメールアドレス）
- Play Console: https://play.google.com/console

### 参考資料
- `RELEASE_NOTES_v1.5.0.md` - リリースノート
- `README.md` - プロジェクト概要
- `TESTING_GUIDE.md` - テストガイド

---

**作成日**: 2025-10-30
**バージョン**: 1.5.0
**次回予定**: 1.6.0（未定）
