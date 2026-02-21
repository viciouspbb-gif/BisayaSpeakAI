# In-App Updates 実装ドキュメント

## 概要

Google Play In-App Updates APIを使用して、アプリ内でのアップデート機能を実装しました。

## 実装内容

### 1. **依存関係**

**ファイル**: `app/build.gradle.kts`

```kotlin
implementation("com.google.android.play:app-update:2.1.0")
implementation("com.google.android.play:app-update-ktx:2.1.0")
```

### 2. **UpdateManager.kt**

**ファイル**: `app/src/main/java/com/bisayaspeak/ai/update/UpdateManager.kt`

**主要機能**:
- アップデート可否チェック
- Flexible更新（通常更新）
- Immediate更新（強制更新）
- 最小サポートバージョン制御
- ダウンロード進捗監視
- エラーハンドリング

### 3. **更新タイプ**

#### Flexible Update（通常更新）
- ユーザーがアプリを使用しながらバックグラウンドでダウンロード
- ダウンロード完了後、ユーザーに再起動を促す
- ユーザーは更新を延期可能

#### Immediate Update（強制更新）
- 更新が完了するまでアプリの使用を制限
- 最小サポートバージョン未満の場合に発動
- ユーザーは更新をスキップ不可

### 4. **最小サポートバージョン制御**

**設定**: `UpdateManager.MIN_SUPPORTED_VERSION = 1`

**動作**:
```kotlin
if (currentVersionCode < MIN_SUPPORTED_VERSION) {
    // 強制更新（Immediate Update）を発動
    startImmediateUpdate()
}
```

**変更方法**:
```kotlin
// UpdateManager.kt
companion object {
    const val MIN_SUPPORTED_VERSION = 1  // ← この値を変更
}
```

### 5. **更新フロー**

#### アプリ起動時
```
MainActivity.onCreate()
  ↓
checkForAppUpdate()
  ↓
UpdateManager.checkForUpdate()
  ↓
┌─────────────────────────┐
│ UpdateCheckResult       │
├─────────────────────────┤
│ NoUpdateAvailable       │ → ログ出力のみ
│ FlexibleUpdateAvailable │ → Flexible更新開始
│ ImmediateUpdateAvailable│ → Immediate更新開始
│ ImmediateUpdateRequired │ → 強制更新開始
│ UpdateNotAllowed        │ → 警告ログ
│ Error                   │ → エラーログ
└─────────────────────────┘
```

#### Flexible更新フロー
```
1. startFlexibleUpdate()
   ↓
2. バックグラウンドでダウンロード
   ↓
3. onDownloadProgress(progress)
   ↓
4. onDownloadCompleted()
   ↓
5. Toast表示「Update downloaded. Restart to install.」
   ↓
6. completeUpdate() → アプリ再起動
```

#### Immediate更新フロー
```
1. startImmediateUpdate()
   ↓
2. 全画面更新UIを表示
   ↓
3. ダウンロード＆インストール
   ↓
4. 完了後、アプリ再起動
```

### 6. **MainActivity統合**

**実装箇所**:
- `onCreate()`: 初期化とアップデートチェック
- `onResume()`: 中断された更新の再開
- `onDestroy()`: リソースクリーンアップ

**コールバック**:
```kotlin
updateManager.onDownloadProgress = { progress ->
    // ダウンロード進捗（0-100%）
}

updateManager.onDownloadCompleted = {
    // ダウンロード完了
    // ユーザーに再起動を促す
}

updateManager.onUpdateFailed = { error ->
    // 更新失敗
    // エラーメッセージ表示
}
```

## 使用方法

### 最小サポートバージョンの変更

重大なバグ修正や破壊的変更がある場合、最小サポートバージョンを引き上げます。

```kotlin
// UpdateManager.kt
companion object {
    // 例: バージョン30未満を強制更新
    const val MIN_SUPPORTED_VERSION = 30
}
```

### テスト方法

#### 内部テストトラック
1. Google Play Consoleで内部テストトラックに新バージョンをアップロード
2. テストデバイスで古いバージョンをインストール
3. アプリを起動
4. アップデート通知が表示されることを確認

#### ローカルテスト
```kotlin
// デバッグ用に強制的にアップデート可能状態にする
if (BuildConfig.DEBUG) {
    // テストコード
}
```

## ログ出力

### 正常系
```
D/MainActivity: No update available
D/MainActivity: Flexible update available
D/MainActivity: Immediate update available
D/MainActivity: Immediate update REQUIRED (force update)
D/UpdateManager: Downloading: 50%
D/UpdateManager: Download completed, ready to install
```

### エラー系
```
W/MainActivity: Update not allowed: Immediate update not allowed
E/MainActivity: Update check error: Network error
E/UpdateManager: Update failed
```

## 注意事項

### 1. **Google Play配信のみ**
- In-App Updates APIはGoogle Play経由の配信でのみ動作
- APKの直接インストールでは動作しない

### 2. **テスト環境**
- 内部テストトラックまたはクローズドテストトラックで検証
- 本番環境にリリース前に必ずテスト

### 3. **ユーザー体験**
- Flexible更新: ユーザーの操作を妨げない
- Immediate更新: 緊急時のみ使用（重大なバグ、セキュリティ問題）

### 4. **更新頻度**
- 頻繁な強制更新はユーザー体験を損なう
- 計画的なバージョン管理が重要

## トラブルシューティング

### 更新が検出されない
- Google Playで新バージョンが公開されているか確認
- テストトラックに正しく配信されているか確認
- キャッシュクリアして再試行

### 更新が失敗する
- ネットワーク接続を確認
- ストレージ容量を確認
- Google Playサービスが最新か確認

### 強制更新が発動しない
- `MIN_SUPPORTED_VERSION`の設定を確認
- 現在のバージョンコードを確認
- ログで`shouldForceUpdate()`の結果を確認

## 今後の拡張

### 1. **UI改善**
- ダウンロード進捗バーの表示
- カスタムダイアログでの更新案内
- リトライボタンの追加

### 2. **通知機能**
- ダウンロード完了時の通知
- 更新失敗時の通知

### 3. **分析**
- 更新成功率の追跡
- 更新所要時間の測定
- エラー発生率の監視

## 参考資料

- [In-App Updates API - Android Developers](https://developer.android.com/guide/playcore/in-app-updates)
- [Google Play Core Library](https://developer.android.com/guide/playcore)
- [アプリ内アップデートのベストプラクティス](https://developer.android.com/guide/playcore/in-app-updates/best-practices)

---

**実装日**: 2024年12月7日  
**バージョン**: 2.1.3 (31)  
**最小サポートバージョン**: 1
