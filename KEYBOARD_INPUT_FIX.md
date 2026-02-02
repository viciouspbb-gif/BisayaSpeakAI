# キーボード入力時の表示問題修正

## 問題
AI会話画面でテキスト入力時にキーボードが表示されると、入力欄が隠れてしまい、ユーザーが入力内容を確認できない状態でした。

## 原因
- キーボード（IME）の高さを考慮したレイアウト調整がされていなかった
- 会話リストと入力エリアがキーボードの表示に応じて適切に調整されていなかった

## 修正内容

### 1. インポートの追加
```kotlin
import androidx.compose.ui.platform.LocalDensity
```

### 2. Scaffoldの設定変更

**変更前:**
```kotlin
Scaffold(
    topBar = { ... }
) { paddingValues ->
```

**変更後:**
```kotlin
Scaffold(
    topBar = { ... },
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
) { paddingValues ->
```

**理由:**
- デフォルトのWindowInsetsを無効化し、手動でキーボードの高さを制御

### 3. 会話リストにIMEパディングを追加

**変更前:**
```kotlin
LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
```

**変更後:**
```kotlin
val imeInsets = WindowInsets.ime
val density = LocalDensity.current
val imeBottomPadding = with(density) { imeInsets.getBottom(density).toDp() }

LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 16.dp,
        bottom = 16.dp + imeBottomPadding
    ),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
```

**理由:**
- キーボードの高さ分だけ下部パディングを追加
- 会話リストがキーボードに隠れないようにする

### 4. 入力エリアにimePadding()を追加

**変更前:**
```kotlin
Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 8.dp
) {
```

**変更後:**
```kotlin
val imeInsets = WindowInsets.ime
val density = LocalDensity.current
val imeBottomPadding = with(density) { imeInsets.getBottom(density).toDp() }

Surface(
    modifier = Modifier
        .fillMaxWidth()
        .imePadding(),
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 8.dp
) {
```

**理由:**
- `.imePadding()`でキーボードの高さ分だけ自動的に上に移動
- 入力エリアが常に見える位置に配置される

### 5. AndroidManifest.xmlの確認

既に正しく設定されています：
```xml
<activity
    android:name=".MainActivity"
    android:windowSoftInputMode="adjustResize">
```

**`adjustResize`の効果:**
- キーボードが表示されたときにアクティビティのサイズを調整
- Composeの`WindowInsets.ime`と連携して動作

## 改善点

1. **入力エリアが常に見える**
   - キーボード表示時も入力欄が隠れない
   - ユーザーが入力内容を確認できる

2. **会話リストの自動調整**
   - キーボード表示時に会話リストが適切にスクロール
   - 最新のメッセージが見える

3. **全機種対応**
   - 様々な画面サイズとキーボードの高さに対応
   - 小さい画面でも大きい画面でも正常に動作

4. **スムーズなアニメーション**
   - キーボードの表示/非表示時にスムーズに移動
   - ユーザー体験の向上

## テスト方法

### 1. 基本的な動作確認
```
1. AI会話画面を開く
2. テキスト入力欄をタップ
3. キーボードが表示される
4. 入力欄が見えることを確認
5. テキストを入力
6. 入力内容が見えることを確認
```

### 2. 翻訳機能の確認
```
1. 「日本語から翻訳」ボタンをタップ
2. 翻訳エリアが展開される
3. 日本語入力欄をタップ
4. キーボードが表示される
5. 入力欄が見えることを確認
```

### 3. 様々な画面サイズでテスト
```
- 小さい画面（Pixel 4a）
- 中くらいの画面（Pixel 6）
- 大きい画面（Pixel 7 Pro）
- タブレット（Pixel Tablet）
```

### 4. 横向きでのテスト
```
1. 画面を横向きにする
2. テキスト入力欄をタップ
3. キーボードが表示される
4. 入力欄が見えることを確認
```

## 期待される動作

### キーボード表示前
```
┌─────────────────┐
│   TopBar        │
├─────────────────┤
│                 │
│  会話リスト      │
│                 │
│                 │
├─────────────────┤
│  入力エリア      │
└─────────────────┘
```

### キーボード表示後
```
┌─────────────────┐
│   TopBar        │
├─────────────────┤
│  会話リスト      │
│  (スクロール可)  │
├─────────────────┤
│  入力エリア      │ ← 常に見える
├─────────────────┤
│                 │
│   キーボード     │
│                 │
└─────────────────┘
```

## 技術的な詳細

### WindowInsets.ime
- IME（Input Method Editor）= キーボード
- `WindowInsets.ime`でキーボードの高さを取得
- `getBottom()`でキーボードの下部からの高さを取得

### imePadding()
- Composeの便利なModifier
- 自動的にキーボードの高さ分だけパディングを追加
- キーボードの表示/非表示に応じて動的に変化

### adjustResize vs adjustPan
- **adjustResize**: アクティビティ全体をリサイズ（今回採用）
  - 利点: 全体が見える、スクロール可能
  - 欠点: レイアウトの再計算が必要
  
- **adjustPan**: 画面をパン（移動）
  - 利点: レイアウトの再計算不要
  - 欠点: 一部が隠れる可能性

## トラブルシューティング

### 問題: 入力欄がまだ隠れる
**解決策:**
1. AndroidManifest.xmlの`windowSoftInputMode`を確認
2. `adjustResize`が設定されているか確認
3. アプリを完全に再起動

### 問題: キーボードが表示されない
**解決策:**
1. エミュレータの設定を確認
2. 「Show virtual keyboard」が有効か確認
3. 実機でテスト

### 問題: アニメーションがカクカクする
**解決策:**
1. エミュレータのパフォーマンス設定を確認
2. 実機でテスト（エミュレータより滑らか）

## 今後の改善案

1. **キーボードの高さに応じた最適化**
   - 小さいキーボードと大きいキーボードで異なる調整

2. **横向きモードの最適化**
   - 横向き時は入力エリアを小さくする

3. **フローティングキーボード対応**
   - タブレットのフローティングキーボードに対応

4. **音声入力との連携**
   - 音声入力時もレイアウトを適切に調整
