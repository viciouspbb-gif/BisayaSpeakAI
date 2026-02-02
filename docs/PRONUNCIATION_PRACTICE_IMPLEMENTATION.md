# BisayaSpeakAI Lite版 発音練習機能 実装ドキュメント

## 概要
無音検知ベースの自然な録音処理と、学習体験を邪魔しない広告連動を実装した発音練習機能。

---

## 1. 録音処理の安定化

### AudioRecordの使用
- **サンプリングレート**: 16kHz（統一）
- **チャンネル**: MONO
- **フォーマット**: PCM16
- **バッファサイズ**: `getMinBufferSize()`で機種依存を吸収

### ウォームアップ処理
- 録音開始直後の **200ms** は破棄
- 空データ対策として実装
- マイクの初期化時間を考慮

### 録音時間
- **最大録音時間**: 5秒
- **短文**: 無音検知で自動終了（2〜3秒程度）
- **長文**: 最大5秒まで録音可能

---

## 2. UI/UX改善

### マイクアイコン
- **統一デザイン**: FloatingActionButton（100dp）
- **常に表示**: ボタンが消えることはない
- **アイコンサイズ**: 48dp

### 録音中の視覚フィードバック
- **通常時**: 青緑 (`#03DAC5`)
- **録音中**: 紫ピンク (`#E91E63`) + 脈動アニメーション
- **評価中**: グレー (`#9E9E9E`)

### 無音検知による自動終了
- **無音判定閾値**: 振幅 500
- **無音継続時間**: 3秒
- **動作**: 3秒間無音が続いたら自動的に録音終了

### 判定結果の表示
- **表示位置**: マイクアイコンの下
- **アニメーション**: フェードイン（500ms）
- **表示内容**:
  - 絵文字（🎉/👍/💪）
  - ラベル（Perfect/Okay/Try Again）
  - 説明文

---

## 3. 判定ロジック

### 3段階判定
| 判定 | スコア | 色 | 説明 |
|------|--------|-----|------|
| **Perfect** | 85以上 | 緑 (`#4CAF50`) | 明瞭で基準に近い発音 |
| **Okay** | 60〜84 | 黄 (`#FFC107`) | 誤差ありだが許容範囲 |
| **Try Again** | 60未満 | 赤 (`#F44336`) | 大きく外れている |

### 無音判定
- **条件**: 開始直後から3秒間無音
- **判定**: 自動的に「Try Again」
- **理由**: ユーザーが発音しなかったことを明確化

### データなし判定
- **条件**: 録音失敗またはファイル生成失敗
- **判定**: 自動的に「Try Again」
- **エラーメッセージ**: 「録音に失敗しました」

---

## 4. 広告連動（Lite版のみ）

### インタースティシャル広告の表示条件

#### 1. 無音Try Againが3回連続
```kotlin
if (recordingResult.isSilent) {
    result = PronunciationResult.TRY_AGAIN
    silentTryAgainCount++
    
    if (!isPremium && silentTryAgainCount >= 3) {
        // インタースティシャル広告を表示
        AdMobManager.showInterstitial(activity)
        silentTryAgainCount = 0
    }
}
```

#### 2. 通常のTry Againが3回連続
```kotlin
when (result) {
    PronunciationResult.TRY_AGAIN -> {
        consecutiveTryAgainCount++
        if (consecutiveTryAgainCount >= 3) {
            // インタースティシャル広告を表示
            AdMobManager.showInterstitial(activity)
            consecutiveTryAgainCount = 0
        }
    }
}
```

#### 3. Perfectが2回成功
```kotlin
when (result) {
    PronunciationResult.PERFECT -> {
        perfectCount++
        if (perfectCount >= 2) {
            // インタースティシャル広告を表示
            AdMobManager.showInterstitial(activity)
            perfectCount = 0
        }
    }
}
```

#### 4. 5問終了時（PracticeQuizScreenのみ）
```kotlin
LaunchedEffect(currentQuestionIndex) {
    if (currentQuestionIndex >= questions.size) {
        // 5問終了時にインタースティシャル広告
        if (!isPremium) {
            AdMobManager.showInterstitial(activity)
        }
        onNavigateBack()
    }
}
```

### バナー広告
- **表示位置**: 画面下部（常時表示）
- **実装**: `BannerScreenContainer`で自動表示
- **条件**: Lite版のみ（`isPremium = false`）

---

## 5. ユーザー体験

### 自然な録音終了
- **固定時間ではなく無音検知**
- 短文・長文どちらも自然に対応
- ユーザーは話し終わったら自動的に終了

### わかりやすい失敗理由
- 無音判定で「Try Again」
- 「発音しなかった」と理解しやすい
- 再トライを促す

### 学習体験を邪魔しない広告
- **失敗が続いた時のみ**: 3回連続Try Again
- **成功時にも表示**: 2回Perfect（達成感を損なわない）
- **区切りの良いタイミング**: 5問終了時
- **バナー広告**: 控えめに下部に常時表示

---

## 6. 実装ファイル

### 新規作成
1. **`AudioRecorder.kt`**
   - AudioRecordを使用した録音処理
   - 無音検知と自動終了
   - ウォームアップ処理

2. **`PronunciationResult.kt`**
   - 共通の判定ラベル enum
   - PERFECT, OKAY, TRY_AGAIN

### 更新
1. **`PracticeQuizScreen.kt`**
   - AudioRecorderを使用
   - 無音Try Againカウンター
   - 広告連動ロジック

2. **`PracticeWordDetailScreen.kt`**
   - AudioRecorderを使用
   - 無音Try Againカウンター
   - 広告連動ロジック

3. **`PracticeItem.kt`**
   - 各ジャンルに5問以上を追加
   - `getRandomQuestions()` 関数

4. **`AppNavGraph.kt`**
   - `PracticeQuizScreen` の import

---

## 7. 録音フロー

```
[マイクタップ]
    ↓
[権限確認]
    ↓
[ウォームアップ 200ms]
    ↓
[録音開始（最大5秒）]
    ├─ 音声検出 → 録音継続
    │   └─ 振幅 > 500 → 音声あり
    ├─ 3秒無音 → 自動終了
    │   └─ 振幅 ≤ 500 が3秒継続
    └─ 5秒経過 → 自動終了
    ↓
[無音判定]
    ├─ 最初から3秒無音 → Try Again
    │   └─ silentTryAgainCount++
    └─ 音声あり → API判定
    ↓
[API判定]
    ├─ スコア ≥ 85 → Perfect
    ├─ スコア 60〜84 → Okay
    └─ スコア < 60 → Try Again
    ↓
[判定結果表示]
    ├─ Perfect (緑) + 🎉
    ├─ Okay (黄) + 👍
    └─ Try Again (赤) + 💪
    ↓
[広告連動]
    ├─ 無音Try Again × 3 → 広告
    ├─ 通常Try Again × 3 → 広告
    ├─ Perfect × 2 → 広告
    └─ 5問終了 → 広告
```

---

## 8. テスト方法

### 録音テスト
1. マイクをタップして録音開始
2. 何か話す → 無音で3秒待つ → 自動終了
3. 判定結果が表示されることを確認

### 無音判定テスト
1. マイクをタップ
2. 何も話さずに3秒待つ
3. 「Try Again」が表示されることを確認

### 広告テスト
1. **無音Try Again × 3**: 何も話さずに3回連続 → 広告表示
2. **通常Try Again × 3**: 発音が悪い状態で3回連続 → 広告表示
3. **Perfect × 2**: 良い発音で2回連続 → 広告表示
4. **5問終了**: カテゴリ選択 → 5問完了 → 広告表示

### 有料版テスト
1. `isPremium = true` に設定
2. 広告が表示されないことを確認
3. バナー広告も非表示になることを確認

---

## 9. パフォーマンス最適化

### メモリ管理
- 録音終了後に `AudioRecorder.stopRecording()` を呼び出し
- `DisposableEffect` でリソースを確実に解放
- 一時ファイルは `cacheDir` に保存

### バッテリー消費
- 録音は最大5秒で自動終了
- 無音検知で早期終了
- バックグラウンド処理なし

### ネットワーク
- 録音終了後にのみAPI呼び出し
- 無音判定時はAPI呼び出しをスキップ
- エラー時は適切にハンドリング

---

## 10. 今後の改善案

### 短期
- [ ] 音量レベルのリアルタイム表示
- [ ] 録音時間の視覚的なインジケーター
- [ ] より詳細なエラーメッセージ

### 中期
- [ ] オフライン判定機能（簡易版）
- [ ] 録音データのローカルキャッシュ
- [ ] 発音の詳細フィードバック

### 長期
- [ ] AI音声認識の精度向上
- [ ] ユーザーの発音履歴分析
- [ ] パーソナライズされた練習問題

---

## 11. トラブルシューティング

### 録音が開始されない
- **原因**: マイク権限がない
- **対処**: 権限リクエストダイアログを表示

### 録音が途中で止まる
- **原因**: 3秒間無音が検知された
- **対処**: 正常動作（仕様通り）

### 判定結果が表示されない
- **原因**: API呼び出しエラー
- **対処**: エラーメッセージを表示し、「Try Again」判定

### 広告が表示されない
- **原因**: 有料版または広告ロード失敗
- **対処**: AdMobの設定を確認

---

## 12. まとめ

### 実装完了項目
✅ AudioRecordを使用した安定した録音処理  
✅ ウォームアップ処理（200ms）  
✅ 機種依存のバッファサイズ吸収  
✅ マイクアイコンの統一とUI改善  
✅ 録音中の色変更とアニメーション  
✅ 無音検知による自動終了  
✅ Perfect/Okay/Try Againの3段階判定  
✅ 無音時の自動Try Again判定  
✅ 無音Try Again × 3で広告表示  
✅ 通常Try Again × 3で広告表示  
✅ Perfect × 2で広告表示  
✅ 5問終了時に広告表示  
✅ バナー広告の常時表示（Lite版）  

### ユーザーメリット
- 自然な録音体験（無音検知）
- わかりやすい判定結果
- 学習を邪魔しない広告タイミング
- 短文・長文どちらも対応

### 技術的メリット
- 安定した録音処理
- 機種依存の問題を解消
- メモリとバッテリーの最適化
- 保守性の高いコード構造
