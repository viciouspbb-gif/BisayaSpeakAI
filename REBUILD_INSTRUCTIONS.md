# UI・判定が反映されない場合の完全リビルド手順

## 問題
- 新しいUI（失敗回数表示、パルスアニメーション）が表示されない
- 判定ロジックが更新されていない
- 3回Try Againで自動進行しない

## 原因
ビルドキャッシュが古いまま残っている可能性が高い

## 完全リビルド手順

### Step 1: Android Studioでのクリーンアップ
```
1. Android Studio > Build > Clean Project
2. Build > Rebuild Project
3. File > Invalidate Caches... > Invalidate and Restart
```

### Step 2: 実機/エミュレータでのアプリ削除
```
1. 実機/エミュレータでアプリを長押し
2. アンインストール
3. または設定 > アプリ > BisayaSpeakAI > アンインストール
```

### Step 3: Gradleキャッシュのクリア（コマンドライン）
```bash
# プロジェクトディレクトリで実行
cd D:\BisayaSpeakAI

# Gradleキャッシュをクリア
gradlew clean

# ビルドディレクトリを削除
rmdir /s /q app\build
rmdir /s /q build

# 再ビルド
gradlew assembleDebug
```

### Step 4: 手動でビルドディレクトリを削除
```
1. エクスプローラーで D:\BisayaSpeakAI を開く
2. app\build フォルダを削除
3. build フォルダを削除
4. .gradle フォルダを削除（隠しフォルダ）
```

### Step 5: Android Studioで再ビルド
```
1. Android Studio を再起動
2. Build > Rebuild Project
3. Run > Run 'app'
```

## 確認ポイント

### ビルド成功後、以下が表示されるか確認:

#### Try Again判定時
- ✅ ❌ アイコン（大きく表示）
- ✅ 赤い "Try Again" ラベル
- ✅ **「失敗回数：1 / 3」の表示**（新規追加）
- ✅ **パルスアニメーション付き赤枠カード**（新規追加）
- ✅ **「もう一度挑戦しましょう（1回目）」**（新規追加）

#### 2回目Try Again時
- ✅ **「失敗回数：2 / 3」**
- ✅ **「あと1回で次に進みます（2回目）」**
- ✅ **オレンジ色の文字**

#### 3回目Try Again時
- ✅ **「失敗回数：3 / 3」**
- ✅ **広告表示**
- ✅ **自動的に次の問題へ進む**

## Logcatでの確認

### フィルタ設定
```
タグ: PracticeQuiz
```

### 期待されるログ
```
D/PracticeQuiz: Question changed: -1 -> 0
D/PracticeQuiz: TryAgainCount reset to 0
D/PracticeQuiz: Score: 45
D/PracticeQuiz: Result: TRY_AGAIN
D/PracticeQuiz: TryAgainCount: 1 / 3
```

## それでも反映されない場合

### 1. ソースコードの確認
```kotlin
// PracticeQuizScreen.kt の639行目付近を確認
Text(
    text = "失敗回数：${currentQuestionTryAgainCount} / 3",
    fontSize = 12.sp,
    color = Color(0xFFC62828),
    fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
)
```

このコードが存在するか確認してください。

### 2. Build Variantの確認
```
Android Studio > Build Variants ウィンドウ
現在のビルドバリアントが "debug" になっているか確認
```

### 3. Gradle Syncの実行
```
File > Sync Project with Gradle Files
```

## 最終手段: プロジェクトの完全クリーン

```bash
# すべてのビルド成果物を削除
cd D:\BisayaSpeakAI
rmdir /s /q app\build
rmdir /s /q build
rmdir /s /q .gradle
rmdir /s /q .idea

# Android Studioを閉じる
# Android Studioを再起動
# プロジェクトを開く
# File > Sync Project with Gradle Files
# Build > Rebuild Project
```

## 問題が解決しない場合

Logcatのログを確認して、以下の情報を提供してください:
1. "PracticeQuiz" タグのログ全体
2. エラーメッセージ（あれば）
3. スコアが正しく取得できているか
4. カウンターが増えているか
