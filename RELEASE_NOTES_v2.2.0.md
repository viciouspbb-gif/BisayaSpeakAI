# Release Notes - Version 2.2.0 (Build 32)

**リリース日**: 2024年12月7日

## 🎉 新機能

### 💰 収益化機能の実装
- **Google Play Billing統合**
  - Pro版アンロック（`pro_unlock`）
  - Premium AI月額プラン（`premium_ai_monthly`）
  - Premium AI年額プラン（`premium_ai_yearly`）
- **購入状態の永続化**
  - DataStoreによる購入状態の保存
  - アプリ起動時の自動リストア
- **UpgradeScreen連動**
  - 購入ボタンと購入状態の同期
  - 購入成功時のSnackbar表示

### 🔒 学習コンテンツのロック制御
- **発音練習ジャンル画面**
  - Premium対象ジャンルに鍵アイコン表示
  - ロックタップ時に「Proで解放」ダイアログ表示
  - UpgradeScreenへの直接遷移
- **ロールプレイ画面**
  - 無料で使える3シナリオ（空港チェックイン、ホテルチェックイン、レストラン注文）
  - それ以外のシナリオはPremium限定
  - ロックタップ時に「Premiumで解放」ダイアログ表示

### 📢 広告誘導の強化
- **発音練習**
  - 5セット（25問）完了ごとにUpgrade提案ダイアログ表示
  - 無料ユーザーのみ対象
- **ロールプレイ**
  - セッション終了時にUpgrade提案ダイアログ表示
  - 無料ユーザーのみ対象

### 🔄 In-App Updates機能
- **自動アップデートチェック**
  - アプリ起動時に自動チェック
  - Flexible更新（通常更新）とImmediate更新（強制更新）に対応
- **最小サポートバージョン制御**
  - 設定バージョン未満の場合、強制更新を発動
  - 現在の設定: `MIN_SUPPORTED_VERSION = 1`
- **ダウンロード進捗監視**
  - バックグラウンドでのダウンロード
  - 完了時にToast通知
- **中断された更新の再開**
  - アプリ再開時に自動チェック

## 🌐 多言語対応

### 追加された文言（日本語・英語）
- `unlock_pro_btn`: Pro解放ボタン
- `premium_monthly_btn`: Premium月額ボタン
- `premium_yearly_btn`: Premium年額ボタン
- `locked_toast_pro`: Proロック時のトースト
- `locked_toast_premium`: Premiumロック時のトースト
- `upgrade_suggestion_title`: アップグレード提案タイトル
- `upgrade_suggestion_message`: アップグレード提案メッセージ
- `upgrade_now`: 今すぐアップグレード
- `maybe_later`: 後で

## 🛠️ 技術的な改善

### 新規追加ファイル
- `app/src/main/java/com/bisayaspeak/ai/data/PurchaseStore.kt`
- `app/src/main/java/com/bisayaspeak/ai/ui/upgrade/UpgradeViewModel.kt`
- `app/src/main/java/com/bisayaspeak/ai/update/UpdateManager.kt`
- `docs/IN_APP_UPDATES.md`

### 更新されたファイル
- `app/src/main/java/com/bisayaspeak/ai/billing/BillingManager.kt`
- `app/src/main/java/com/bisayaspeak/ai/ui/upgrade/UpgradeScreen.kt`
- `app/src/main/java/com/bisayaspeak/ai/MainActivity.kt`
- `app/src/main/java/com/bisayaspeak/ai/ui/screens/PracticeCategoryScreen.kt`
- `app/src/main/java/com/bisayaspeak/ai/ui/screens/PracticeQuizScreen.kt`
- `app/src/main/java/com/bisayaspeak/ai/ui/screens/MockRolePlayScreens.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ja/strings.xml`

### 依存関係の追加
```kotlin
// Google Play Billing
implementation("com.android.billingclient:billing-ktx:7.1.1")

// In-App Updates
implementation("com.google.android.play:app-update:2.1.0")
implementation("com.google.android.play:app-update-ktx:2.1.0")
```

## 📊 収益化戦略

### プラン構成

| 機能 | 無料 | Pro | Premium AI |
|------|------|-----|------------|
| **発音練習** | 一部ジャンル | 全ジャンル | 全ジャンル |
| **ロールプレイ** | 3シナリオ | 3シナリオ | 全シナリオ |
| **広告** | あり | なし | なし |
| **AI機能** | - | - | 利用可能 |

### 広告表示タイミング
1. 発音練習: 25問（5セット）ごと
2. ロールプレイ: セッション終了時
3. ロック機能タップ: 即座にUpgrade提案

## 🔐 セキュリティ

- 購入状態はDataStoreで暗号化保存
- Google Play Billingの公式ライブラリ使用
- 購入検証はサーバーサイドで実施（推奨）

## 📱 動作環境

- **最小SDKバージョン**: 24 (Android 7.0)
- **ターゲットSDKバージョン**: 35 (Android 15)
- **コンパイルSDKバージョン**: 35

## 🐛 既知の問題

- In-App Updates機能はGoogle Play経由の配信でのみ動作
- APKの直接インストールではアップデートチェックは動作しません

## 📝 テスト推奨項目

### 収益化機能
- [ ] Pro購入フロー
- [ ] Premium月額購入フロー
- [ ] Premium年額購入フロー
- [ ] 購入状態の復元
- [ ] ロック解除の動作確認

### In-App Updates
- [ ] 内部テストトラックでのアップデート検証
- [ ] Flexible更新の動作確認
- [ ] Immediate更新の動作確認
- [ ] 中断された更新の再開

### UI/UX
- [ ] ロックアイコンの表示
- [ ] Upgradeダイアログの表示
- [ ] Toast通知の表示
- [ ] 広告誘導のタイミング

## 🚀 次回リリース予定

- サーバーサイド購入検証の実装
- ダウンロード進捗バーのUI表示
- 購入履歴画面の追加
- アナリティクスの統合

---

**重要**: このバージョンをリリースする前に、Google Play Consoleで以下の設定を完了してください：
1. アプリ内商品（Pro unlock, Premium AI Monthly, Premium AI Yearly）の登録
2. 内部テストトラックでの動作確認
3. プライバシーポリシーの更新（収益化に関する記載）

**ビルドコマンド**:
```bash
./gradlew assembleRelease
```

**リリース手順**:
1. リリースビルドの作成
2. 内部テストトラックへのアップロード
3. テスト実施
4. 本番環境へのロールアウト
