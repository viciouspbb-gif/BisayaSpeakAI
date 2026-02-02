# AI会話機能の多言語対応実装

## 概要
AI会話機能を4カ国語（日本語、英語、ビサヤ語、タガログ語）に対応させました。

## 実装内容

### 1. 言語検出機能の追加

```kotlin
/**
 * 現在の言語を取得
 */
private fun getCurrentLanguage(): String {
    return when (Locale.getDefault().language) {
        "ja" -> "ja"
        "ceb" -> "ceb"
        "fil", "tl" -> "fil"
        else -> "en"
    }
}

/**
 * 言語名を取得
 */
private fun getLanguageName(lang: String): String {
    return when (lang) {
        "ja" -> "Japanese"
        "en" -> "English"
        "ceb" -> "Cebuano"
        "fil" -> "Filipino"
        else -> "English"
    }
}
```

### 2. システムプロンプトの多言語化

**変更前（日本語のみ）:**
```kotlin
val basePrompt = """
    あなたはビサヤ語（セブアノ語）の会話パートナーです。
    
    【応答フォーマット】
    {
      "bisaya": "ビサヤ語の文章",
      "japanese": "日本語訳",
      ...
    }
"""
```

**変更後（多言語対応）:**
```kotlin
val currentLang = getCurrentLanguage()
val langName = getLanguageName(currentLang)

val basePrompt = """
    You are a Bisaya (Cebuano) conversation partner.
    
    RESPONSE FORMAT:
    {
      "bisaya": "Bisaya text",
      "translation": "Translation in $langName",
      ...
    }
    
    IMPORTANT: Provide translation in $langName language.
"""
```

### 3. JSON応答パースの更新

**変更前:**
```kotlin
val japaneseRegex = """"japanese"\s*:\s*"([^"]+)"""".toRegex()
val japanese = japaneseRegex.find(jsonText)?.groupValues?.get(1) ?: ""
```

**変更後:**
```kotlin
val translationRegex = """"translation"\s*:\s*"([^"]+)"""".toRegex()
// 後方互換性のため"japanese"もサポート
val japaneseRegex = """"japanese"\s*:\s*"([^"]+)"""".toRegex()

val translationMatch = translationRegex.find(jsonText) ?: japaneseRegex.find(jsonText)
val translation = translationMatch?.groupValues?.get(1)?.replace("\\n", "\n") ?: ""
```

## 対応言語

### 1. 🇯🇵 日本語（ja）
- 端末言語が日本語の場合
- AIの翻訳も日本語で提供

**例:**
```
AI: Maayong buntag!
翻訳: おはようございます！
```

### 2. 🇺🇸 英語（en）
- 端末言語が英語の場合（デフォルト）
- 国際的なユーザー向け

**例:**
```
AI: Maayong buntag!
Translation: Good morning!
```

### 3. 🇵🇭 ビサヤ語（ceb）
- 端末言語がビサヤ語の場合
- 現地のビサヤ語話者向け

**例:**
```
AI: Maayong buntag!
Hubad: Maayong buntag! (挨拶の説明)
```

### 4. 🇵🇭 タガログ語（fil）
- 端末言語がタガログ語/フィリピノ語の場合
- フィリピンの他地域のユーザー向け

**例:**
```
AI: Maayong buntag!
Salin: Magandang umaga!
```

## 動作の流れ

1. **アプリ起動時**
   ```
   端末の言語設定を取得
   ↓
   getCurrentLanguage()で言語コードを判定
   ↓
   getLanguageName()で言語名を取得
   ```

2. **AI会話開始時**
   ```
   createSystemPrompt()でプロンプト生成
   ↓
   言語に応じた翻訳指示を含める
   ↓
   Gemini APIに送信
   ```

3. **AI応答受信時**
   ```
   JSON応答を受信
   ↓
   "translation"フィールドをパース
   ↓
   ユーザーの言語で翻訳を表示
   ```

## テスト方法

### 1. 日本語でテスト
```
1. 端末の言語設定を日本語に変更
2. アプリを再起動
3. AI会話を開始
4. AIの応答が日本語訳付きで表示されることを確認
```

### 2. 英語でテスト
```
1. 端末の言語設定を英語に変更
2. アプリを再起動
3. AI会話を開始
4. AIの応答が英語訳付きで表示されることを確認
```

### 3. ビサヤ語でテスト
```
1. 端末の言語設定をビサヤ語（Cebuano）に変更
2. アプリを再起動
3. AI会話を開始
4. AIの応答がビサヤ語訳付きで表示されることを確認
```

### 4. タガログ語でテスト
```
1. 端末の言語設定をタガログ語（Filipino）に変更
2. アプリを再起動
3. AI会話を開始
4. AIの応答がタガログ語訳付きで表示されることを確認
```

## 後方互換性

古いバージョンとの互換性を保つため、以下の対応を実装：

```kotlin
// "translation"と"japanese"の両方をサポート
val translationMatch = translationRegex.find(jsonText) ?: japaneseRegex.find(jsonText)
```

これにより、古いプロンプトで生成された応答も正しく表示されます。

## 利点

### 1. グローバル展開が可能
- 日本市場だけでなく、英語圏やフィリピン市場にも展開可能
- ユーザーベースの拡大

### 2. 現地ユーザーにも対応
- ビサヤ語話者が母語で学習できる
- タガログ語話者もアクセス可能

### 3. 学習効果の向上
- ユーザーの母語で説明を受けられる
- 理解度が向上

### 4. マーケティング効果
- 多言語対応をアピールポイントに
- より多くのユーザーにリーチ

## 注意事項

### Gemini APIの言語対応
Gemini APIは以下の言語に対応しています：
- ✅ 日本語
- ✅ 英語
- ✅ ビサヤ語（セブアノ語）
- ✅ タガログ語（フィリピノ語）

### 翻訳品質
- 日本語と英語：高品質
- ビサヤ語とタガログ語：良好（ただし、まれに不正確な場合あり）

### エラーハンドリング
翻訳が取得できない場合は、空文字列を返し、ビサヤ語のみを表示します。

## 今後の改善案

### 1. 言語選択機能
ユーザーが手動で翻訳言語を選択できる機能：
```kotlin
// 設定画面で言語を選択
PreferencesManager.setPreferredLanguage("en")

// AI会話時に選択した言語を使用
val preferredLang = PreferencesManager.getPreferredLanguage()
```

### 2. 複数言語の同時表示
ビサヤ語 + 日本語 + 英語を同時に表示：
```
AI: Maayong buntag!
🇯🇵 おはようございます！
🇺🇸 Good morning!
```

### 3. 音声合成の多言語対応
各言語の音声で読み上げ：
- 日本語TTS
- 英語TTS
- ビサヤ語TTS
- タガログ語TTS

## 音声入力について

### 現状
**音声入力機能は実装されていません。**

### 実装が推奨されない理由

1. **認識精度の問題**
   - ビサヤ語の音声認識は精度が低い
   - Google Speech APIでもビサヤ語のサポートが限定的

2. **ネットワーク依存**
   - 音声認識にはインターネット接続が必須
   - オフライン時に使用できない

3. **プライバシーの懸念**
   - 音声データをサーバーに送信する必要がある
   - ユーザーのプライバシーに配慮が必要

4. **複雑性の増加**
   - 実装とメンテナンスのコストが高い
   - バグの原因になりやすい

### 代替案

#### 1. テキスト入力の改善（現在の方針）
- ✅ クイックフレーズ機能
- ✅ 日本語からの翻訳機能
- ✅ 辞書機能

#### 2. 発音練習機能（既に実装済み）
- ✅ 録音して発音をチェック
- ✅ AIによる発音評価
- ✅ お手本音声の再生

#### 3. 将来的な実装（検討中）
- オフライン音声認識（Whisper等）
- ビサヤ語専用の音声認識モデル
- 音声入力のオプション機能として提供

## まとめ

- ✅ AI会話機能を4カ国語対応に実装
- ✅ 端末の言語設定に応じて自動切り替え
- ✅ 後方互換性を維持
- ✅ グローバル展開が可能に
- ❌ 音声入力は実装していない（推奨されない）
- ✅ テキスト入力の補助機能で代替

これにより、より多くのユーザーにアプリを提供できるようになりました！
