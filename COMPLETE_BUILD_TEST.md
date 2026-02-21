# 完全ビルド＆テスト手順（Lite版安定化）

## 📋 実装済み機能の確認

### ✅ 1. 音処理の修正
- [x] `RECORD_AUDIO`権限あり
- [x] `AudioRecord`初期化（`getMinBufferSize()`使用）
- [x] ウォームアップ処理（200ms）
- [x] `read()`戻り値チェック
- [x] 0サイズ100回で録音失敗判定
- [x] 診断ログ追加

### ✅ 2. 判定ロジックの改善
- [x] 無音検知強化
- [x] しきい値調整（80%/50%）
- [x] 3回Try Againで自動進行

### ✅ 3. UI反映
- [x] 判定ラベル（✅/🟡/❌）
- [x] 「失敗回数：X / 3」表示
- [x] パルスアニメーション
- [x] 「次の問題へ」ボタン制御

### ✅ 4. Play Services更新
- [x] play-services-ads: 22.6.0 → 23.5.0

## 🔧 ビルド手順

### Step 1: Gradle Sync

```
1. Android Studio > File > Sync Project with Gradle Files
2. 同期が完了するまで待つ
```

### Step 2: クリーンビルド

```bash
cd D:\BisayaSpeakAI
.\gradlew clean
.\gradlew assembleDebug
```

または Android Studio で:
```
Build > Clean Project
Build > Rebuild Project
```

### Step 3: APKの確認

```bash
# APKが生成されたか確認
dir app\build\outputs\apk\debug\app-debug.apk

# APKのサイズと日時を確認
```

## 📱 インストール手順

### 方法1: ADBコマンド（推奨）

```bash
# 1. 既存アプリを完全削除
adb uninstall com.bisayaspeak.ai

# 2. 新しいAPKをインストール
adb install app\build\outputs\apk\debug\app-debug.apk

# 3. アプリを起動
adb shell am start -n com.bisayaspeak.ai/.MainActivity
```

### 方法2: Android Studioから

```
1. Run > Run 'app'
2. デバイスを選択
3. OK
```

## 🔍 テスト手順

### Test 1: マイク権限の確認

```
1. アプリを起動
2. 発音練習を選択
3. マイク権限のダイアログが表示される
4. 「許可」を選択
```

**期待される結果**: 権限が許可される

### Test 2: 録音処理の確認

```
1. Logcat > フィルタ: "AudioRecorder"
2. 発音練習でマイクボタンをタップ
3. ログを確認
```

**期待されるログ**:
```
D/AudioRecorder: Recording started, bufferSize: 3200
D/AudioRecorder: Warmup read #1: 1600 bytes
D/AudioRecorder: Warmup read #2: 1600 bytes
D/AudioRecorder: Warmup completed, total reads: 10
D/AudioRecorder: Sound detected, amplitude: 5234
D/AudioRecorder: Recording completed: totalBytes=32000, duration=2000 ms, hasSound=true, isSilent=false
```

**問題がある場合のログ**:
```
D/AudioRecorder: Recording started, bufferSize: 3200
W/AudioRecorder: Zero read count: 10
E/AudioRecorder: Too many zero reads, aborting
E/AudioRecorder: No data recorded, returning null
```

### Test 3: 判定結果の確認

#### 3-1. Perfect判定
```
1. 正しい発音で録音
2. スコアが80%以上
```

**期待される表示**:
- ✅ アイコン
- 緑色の "Perfect!" ラベル
- 「素晴らしい！完璧な発音です！🎉」
- スケールアニメーション
- 「次の問題へ」ボタン表示（緑色）

#### 3-2. Okay判定
```
1. やや不正確な発音で録音
2. スコアが50-79%
```

**期待される表示**:
- 🟡 アイコン
- 黄色の "Okay" ラベル
- 「良い発音です！もう少しで完璧！👍」
- 「次の問題へ」ボタン表示（黄色）

#### 3-3. Try Again判定（1回目）
```
1. 不正確な発音で録音
2. スコアが50%未満
```

**期待される表示**:
- ❌ アイコン
- 赤色の "Try Again" ラベル
- 「惜しい！もう一度挑戦してみよう💪」
- **「失敗回数：1 / 3」**（赤枠の小さいカード）
- **パルスアニメーション付き赤枠カード**
- **「もう一度挑戦しましょう（1回目）」**
- 「次の問題へ」ボタンなし

#### 3-4. Try Again判定（2回目）
```
1. 再度不正確な発音で録音
2. スコアが50%未満
```

**期待される表示**:
- ❌ アイコン
- 「失敗回数：2 / 3」
- **「あと1回で次に進みます（2回目）」**（オレンジ色）
- パルスアニメーション
- 「次の問題へ」ボタンなし

#### 3-5. Try Again判定（3回目）
```
1. 3回目も不正確な発音で録音
2. スコアが50%未満
```

**期待される動作**:
- ❌ アイコン
- 「失敗回数：3 / 3」
- **広告表示**
- **1秒後に自動的に次の問題へ進む**

### Test 4: 5問完了時の確認

```
1. 5問すべて完了
2. 広告表示
3. 前の画面に戻る
```

**期待される動作**:
- インタースティシャル広告表示
- 広告閉じた後、前の画面に戻る

## 🐛 トラブルシューティング

### 問題1: UIが更新されない

#### 症状
- 「失敗回数」が表示されない
- パルスアニメーションがない
- 古いUIのまま

#### 対処法
```bash
# 1. ビルドキャッシュを完全削除
cd D:\BisayaSpeakAI
Remove-Item -Recurse -Force app\build
Remove-Item -Recurse -Force build
Remove-Item -Recurse -Force .gradle

# 2. Android Studioを再起動

# 3. Gradle Sync
File > Sync Project with Gradle Files

# 4. クリーンビルド
.\gradlew clean assembleDebug

# 5. アプリを完全削除
adb uninstall com.bisayaspeak.ai

# 6. 新しいAPKをインストール
adb install app\build\outputs\apk\debug\app-debug.apk
```

### 問題2: 録音データがサイズ0

#### 症状
```
W/AudioRecorder: Zero read count: 10
E/AudioRecorder: Too many zero reads, aborting
```

#### 対処法1: マイク権限の確認
```
設定 > アプリ > BisayaSpeakAI > 権限 > マイク > 許可
```

#### 対処法2: ウォームアップ期間の延長
```kotlin
// AudioRecorder.kt
private const val WARMUP_DURATION_MS = 500L  // 200ms → 500ms
```

#### 対処法3: サンプリングレートの変更
```kotlin
// AudioRecorder.kt
private const val SAMPLE_RATE = 44100  // 16000 → 44100
```

### 問題3: 広告が表示されない

#### 症状
- 3回Try Againでも広告が出ない
- 5問完了時に広告が出ない

#### 対処法
```
1. AdMobの設定を確認
2. テスト広告IDを使用しているか確認
3. 実機のGoogle Play Servicesを最新化
```

## ✅ 完了チェックリスト

### ビルド
- [ ] Gradle Syncが成功
- [ ] クリーンビルドが成功
- [ ] APKが生成された

### インストール
- [ ] 既存アプリを削除
- [ ] 新しいAPKをインストール
- [ ] アプリが起動

### 録音処理
- [ ] マイク権限が許可された
- [ ] Logcatに "Recording started" が出力
- [ ] Logcatに "Warmup read" が出力
- [ ] Logcatに "Sound detected" が出力
- [ ] totalBytesRead > 0

### 判定結果
- [ ] Perfect判定が表示される
- [ ] Okay判定が表示される
- [ ] Try Again判定が表示される
- [ ] 「失敗回数：X / 3」が表示される

### UI表示
- [ ] パルスアニメーションが動作
- [ ] 「もう一度挑戦しましょう（X回目）」が表示
- [ ] 2回目は「あと1回で次に進みます」が表示
- [ ] 3回目で自動進行

### 広告
- [ ] 3回Try Againで広告表示
- [ ] 5問完了時に広告表示

## 📊 テスト結果の記録

### 録音処理
- [ ] 正常に録音できた
- [ ] 音声データが取得できた
- [ ] 無音検知が動作した

### 判定精度
- [ ] Perfect判定が適切
- [ ] Okay判定が適切
- [ ] Try Again判定が適切

### UI表示
- [ ] すべてのUI要素が表示された
- [ ] アニメーションが動作した
- [ ] ボタン制御が正しい

### 自動進行
- [ ] 3回Try Againで自動進行した
- [ ] 広告が表示された
- [ ] 次の問題に進んだ

## 🎯 次のステップ

すべてのテストが成功したら:

1. **内部テストトラックにアップロード**
   ```
   Build > Generate Signed Bundle / APK
   → Android App Bundle
   → Release
   → 署名情報を入力
   → Build
   ```

2. **Google Play Consoleで配布**
   ```
   Play Console > リリース > テスト > 内部テスト
   → 新しいリリースを作成
   → App Bundleをアップロード
   → リリースノートを入力
   → 確認 > リリース開始
   ```

3. **テスターに通知**
   ```
   テスト用URLを共有
   フィードバックを収集
   ```
