# リスニング画面のスクロール問題修正

## 問題
リスニング練習画面で、単語パネルが画面に収まりきらず、小さい画面の機種ではスクロールしないと全ての要素が見えない状態でした。

## 原因
- メインの`Column`が`fillMaxSize()`を使用していたが、スクロール機能がなかった
- 内部の`LazyVerticalGrid`（単語パネル）が高さを指定していなかったため、親のスクロールと競合していた

## 修正内容

### 1. インポートの追加
```kotlin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
```

### 2. メインColumnにスクロール機能を追加

**変更前:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
```

**変更後:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
```

### 3. LazyVerticalGridの高さを固定

**変更前:**
```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
```

**変更後:**
```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier
        .fillMaxWidth()
        .height(400.dp),
    userScrollEnabled = false
) {
```

## 改善点

1. **全画面スクロール対応**
   - メインの`Column`に`verticalScroll`を追加することで、画面全体がスクロール可能に
   - 小さい画面でも全ての要素にアクセス可能

2. **単語パネルの固定高さ**
   - `LazyVerticalGrid`に`height(400.dp)`を設定
   - 単語パネルの高さを固定することで、レイアウトが安定
   - `userScrollEnabled = false`で、親のスクロールのみを使用

3. **レイアウトの安定性**
   - スクロールの競合を解消
   - 様々な画面サイズに対応

## テスト方法

1. **小さい画面でテスト**
   - 小さい画面サイズのエミュレータ（例: Pixel 4a）で動作確認
   - 全ての要素が表示されるか確認

2. **スクロール動作の確認**
   - 画面を上下にスクロールできるか確認
   - 単語パネルが適切に表示されるか確認

3. **様々な画面サイズでテスト**
   - 大きい画面（タブレット）でも正常に動作するか確認
   - 横向きでも問題ないか確認

## 期待される動作

- ✅ 小さい画面でも全ての要素が見える
- ✅ スムーズにスクロールできる
- ✅ 単語パネルが適切なサイズで表示される
- ✅ 音声再生ボタン、選択エリア、結果表示、単語パネルが全て見える

## 注意事項

### 高さの調整
もし単語パネルの高さ（400.dp）が適切でない場合は、以下のように調整できます：

```kotlin
// より多くの単語を表示したい場合
.height(500.dp)

// より少なく表示したい場合
.height(300.dp)
```

### 代替案
もし固定高さではなく、動的に高さを決めたい場合は、以下のアプローチも可能です：

```kotlin
// 単語の数に応じて高さを計算
val gridHeight = remember(shuffledWords.size) {
    val rows = (shuffledWords.size + 1) / 2 // 2列なので
    (rows * 92).dp // 80dp (カードの高さ) + 12dp (間隔)
}

LazyVerticalGrid(
    modifier = Modifier
        .fillMaxWidth()
        .height(gridHeight),
    userScrollEnabled = false
) {
```

ただし、シンプルさと安定性を考慮して、現在は固定高さ（400.dp）を採用しています。
