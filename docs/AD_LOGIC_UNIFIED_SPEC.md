# 全練習モード広告ロジック統一仕様

## 概要

全ての練習モード（リスニング、クイズ、発音練習など）の広告表示ロジックを統一しました。

## 統一ルール

### 広告発火タイミング

| トリガー | 広告表示 | 備考 |
|---------|---------|------|
| **1セット終了**（5問終了など） | ✅ 1回必ず表示 | 各モードのセット定義に従う |
| **アプリバック／ホーム／画面遷移による途中離脱** | ✅ 1回必ず表示 | 中断時に発火 |
| **通信エラーで回答送信失敗 → リトライで成功** | ✅ 1回必ず表示 | エラーハンドリング後 |

### 削除された旧仕様

以下のロジックは**完全に削除**されました：

- ❌ 「10回連続正解で広告」
- ❌ 「2回ミスで広告」
- ❌ 「3回Try Againで広告」
- ❌ 「2回Perfectで広告」

**新仕様では、成功でも失敗でも、セット完了基準のみで広告を表示します。**

## 実装詳細

### 1. 共通広告管理クラス

**ファイル**: `PracticeSessionManager.kt`

```kotlin
class PracticeSessionManager(private val isPremium: Boolean)
```

**主要メソッド**:
- `startSession()` - セッション開始を記録
- `onSessionComplete(activity, onAdDismissed)` - セット完了時の広告表示
- `onSessionInterrupted(activity, onAdDismissed)` - 中断時の広告表示
- `onRetrySuccess(activity, onAdDismissed)` - リトライ成功時の広告表示

**特徴**:
- プレミアムユーザーは広告をスキップ
- 1セッションにつき1回のみ広告表示（重複防止）
- ログ出力で動作確認可能

### 2. 対応画面

#### 2.1 ListeningScreen（リスニング練習）

**変更内容**:
- `ListeningViewModel`から旧仕様（2回ミス、5問連続正解）を削除
- `shouldShowAd`フラグでセッション完了時のみ広告表示
- `DisposableEffect`で中断時の広告発火
- `BackHandler`でバックボタン押下時の広告発火

**ファイル**:
- `ListeningViewModel.kt`
- `ListeningScreen.kt`

#### 2.2 PracticeQuizScreen（発音練習クイズ）

**変更内容**:
- 旧仕様（3回Try Again、2回Perfect）を完全削除
- 5問完了時のみ広告表示
- 中断時の広告発火を実装
- Try Againカウンター表示を削除

**ファイル**:
- `PracticeQuizScreen.kt`

#### 2.3 QuizScreen（AIクイズ）

**変更内容**:
- `PracticeSessionManager`を統合
- 5問完了時のみ広告表示
- 中断時の広告発火を実装

**ファイル**:
- `QuizScreen.kt`

#### 2.4 RecordingScreen（発音チェック）

**変更内容**:
- 診断成功時に広告表示（1セット = 1回の発音チェック）
- 中断時の広告発火を実装
- `PracticeSessionManager`を統合

**ファイル**:
- `RecordingScreen.kt`

## テストケース（全モード共通）

| ケース | 条件 | 広告結果 |
|--------|------|---------|
| **通常に5問クリア** | 完了後遷移 | ✅ 1回 |
| **ノーミス** | 完了後遷移 | ✅ 1回 |
| **初回で失敗→クリア** | 完了後遷移 | ✅ 1回 |
| **2問目で戻る/他画面遷移** | 中断時 | ✅ 1回 |
| **通信エラー→リトライ成功** | 成功後遷移 | ✅ 1回 |

## 適用範囲

- **Lite版（無料版）**: 広告表示あり
- **PRO版（プレミアム）**: 広告なし（現状維持）

## コード例

### セッション開始

```kotlin
val sessionManager = remember { PracticeSessionManager(isPremium) }

LaunchedEffect(Unit) {
    sessionManager.startSession()
}
```

### セット完了時

```kotlin
LaunchedEffect(shouldShowAd) {
    if (shouldShowAd) {
        sessionManager.onSessionComplete(activity) {
            // 広告表示後の処理
        }
    }
}
```

### 中断時

```kotlin
DisposableEffect(Unit) {
    onDispose {
        if (sessionStarted && !sessionCompleted) {
            sessionManager.onSessionInterrupted(activity)
        }
    }
}

BackHandler {
    if (sessionStarted && !sessionCompleted) {
        sessionManager.onSessionInterrupted(activity) {
            onNavigateBack()
        }
    } else {
        onNavigateBack()
    }
}
```

## 動作確認方法

### ログ確認

Logcatで以下のタグをフィルタリング：
- `PracticeSessionManager`
- `ListeningScreen`
- `PracticeQuiz`
- `QuizScreen`
- `RecordingScreen`

### 確認ポイント

1. **セッション開始時**: "Session started" ログ
2. **セット完了時**: "Session completed, showing ad" ログ
3. **中断時**: "Session interrupted, showing ad" ログ
4. **広告表示**: AdMobManagerの広告表示ログ

## まとめ

### 一言まとめ

**どの練習でも「1セット＝1広告」「中断でも1広告」で統一。**

### 主な改善点

1. ✅ 広告ロジックの統一化
2. ✅ 複雑な条件分岐の削除
3. ✅ コードの保守性向上
4. ✅ ユーザー体験の一貫性確保
5. ✅ デバッグの容易化

### 今後の拡張

新しい練習モードを追加する場合：

1. `PracticeSessionManager`をインスタンス化
2. `startSession()`でセッション開始
3. `onSessionComplete()`でセット完了時の広告
4. `DisposableEffect`と`BackHandler`で中断時の広告

この3ステップで統一された広告ロジックを実装できます。
