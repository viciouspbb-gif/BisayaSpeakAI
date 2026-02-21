# 録音データサイズ0問題の診断手順

## 🔍 問題の本質

```
AHAL: AudioStream ... read: Exit: returning size: 0
```

録音バッファがサイズ0で返ってくる → 音声データが取得できていない → 常にTry Again判定

## 📋 診断ログの確認

### 1. ビルドして再インストール

```bash
cd D:\BisayaSpeakAI
.\gradlew clean assembleDebug
adb uninstall com.bisayaspeak.ai
adb install app\build\outputs\apk\debug\app-debug.apk
```

### 2. Logcatで以下のタグをフィルタ

```
AudioRecorder
```

### 3. 発音練習を実行して、以下のログを確認

#### 正常な場合のログ:
```
D/AudioRecorder: Recording started, bufferSize: 3200
D/AudioRecorder: Warmup read #1: 1600 bytes
D/AudioRecorder: Warmup read #2: 1600 bytes
D/AudioRecorder: Warmup completed, total reads: 10
D/AudioRecorder: Sound detected, amplitude: 5234
D/AudioRecorder: Sound detected, amplitude: 6123
D/AudioRecorder: Recording completed: totalBytes=32000, duration=2000 ms, hasSound=true, isSilent=false
```

#### 問題がある場合のログ:
```
D/AudioRecorder: Recording started, bufferSize: 3200
D/AudioRecorder: Warmup completed, total reads: 10
W/AudioRecorder: Zero read count: 10
W/AudioRecorder: Zero read count: 20
E/AudioRecorder: Too many zero reads, aborting
E/AudioRecorder: No data recorded, returning null
```

## 🔧 問題別の対処法

### ケース1: マイク権限が許可されていない

#### 症状
```
D/AudioRecorder: Recording started, bufferSize: 3200
E/AudioRecorder: Too many zero reads, aborting
```

#### 対処法
```
1. 実機の設定 > アプリ > BisayaSpeakAI > 権限
2. マイクが「許可」になっているか確認
3. 「拒否」の場合は「許可」に変更
4. アプリを再起動
```

### ケース2: AudioRecordの初期化失敗

#### 症状
```
（ログが全く出ない）
```

#### 対処法

AudioRecordの初期化パラメータを確認:

```kotlin
val sampleRate = 16000
val bufferSize = AudioRecord.getMinBufferSize(
    sampleRate,
    AudioFormat.CHANNEL_IN_MONO,
    AudioFormat.ENCODING_PCM_16BIT
)

// bufferSizeが負の値の場合は初期化失敗
if (bufferSize < 0) {
    Log.e("AudioRecorder", "getMinBufferSize failed: $bufferSize")
}
```

### ケース3: 端末がマイク入力をサポートしていない

#### 症状
```
D/AudioRecorder: Recording started, bufferSize: 3200
W/AudioRecorder: Zero read count: 10
W/AudioRecorder: Zero read count: 20
（音が出ているのにamplitudeが0）
```

#### 対処法

別の端末で試すか、サンプリングレートを変更:

```kotlin
// 16000 Hz → 44100 Hz に変更
private const val SAMPLE_RATE = 44100
```

### ケース4: ウォームアップ期間が短すぎる

#### 症状
```
D/AudioRecorder: Warmup completed, total reads: 10
（Warmup read のログが1つも出ない）
```

#### 対処法

ウォームアップ期間を延長:

```kotlin
private const val WARMUP_DURATION_MS = 500L  // 200ms → 500ms
```

## ✅ 確認チェックリスト

- [ ] マイク権限が「許可」になっている
- [ ] Logcatに "Recording started" が出力される
- [ ] Logcatに "Warmup read" が出力される
- [ ] Logcatに "Sound detected" が出力される
- [ ] totalBytesRead が 0 より大きい
- [ ] hasSound が true になる

## 🎯 次のステップ

### すべてのログが正常な場合

録音は成功しています。問題は判定ロジックにあります。

### ログが異常な場合

上記の対処法を試してください。

## 📱 実機での確認方法

### 1. マイクテスト

実機の「ボイスレコーダー」アプリで録音できるか確認:
- 録音できる → アプリの問題
- 録音できない → 端末の問題

### 2. 他のアプリでの確認

Google音声入力で音声認識できるか確認:
- 認識できる → マイクは正常
- 認識できない → マイク故障

### 3. 権限の再設定

```
1. 設定 > アプリ > BisayaSpeakAI
2. 権限 > マイク > 拒否
3. アプリを起動（権限リクエストが表示される）
4. 許可を選択
```

## 🔍 詳細デバッグ

### AudioRecordの状態を確認

```kotlin
val state = audioRecord?.state
val recordingState = audioRecord?.recordingState

Log.d("AudioRecorder", "State: $state (INITIALIZED=${AudioRecord.STATE_INITIALIZED})")
Log.d("AudioRecorder", "Recording state: $recordingState (RECORDING=${AudioRecord.RECORDSTATE_RECORDING})")
```

期待される出力:
```
D/AudioRecorder: State: 1 (INITIALIZED=1)
D/AudioRecorder: Recording state: 3 (RECORDING=3)
```

### バッファサイズの確認

```kotlin
Log.d("AudioRecorder", "Min buffer size: $bufferSize")
Log.d("AudioRecorder", "Buffer array size: ${buffer.size}")
```

期待される出力:
```
D/AudioRecorder: Min buffer size: 3200
D/AudioRecorder: Buffer array size: 1600
```

## 📄 ログの保存

問題が解決しない場合、以下のコマンドでログを保存してください:

```bash
adb logcat -d > D:\BisayaSpeakAI\logcat.txt
```

ログファイルを確認して、エラーメッセージを探してください。
