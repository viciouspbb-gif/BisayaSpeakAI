# 音声入力機能の実装

## 概要
AI会話機能に音声入力機能を追加しました。ユーザーは日本語、英語、タガログ語で音声入力でき、AIはビサヤ語と翻訳で応答します。

## コンセプト

### ユーザー側（音声入力）
- 🇯🇵 **日本語音声認識** - 日本人学習者向け
- 🇺🇸 **英語音声認識** - 国際的なユーザー向け
- 🇵🇭 **タガログ語音声認識** - フィリピン人学習者向け
- ❌ **ビサヤ語音声認識は不使用** - 認識精度が低いため

### AI側（応答）
- ✅ **ビサヤ語で応答** - 学習の主目的
- ✅ **翻訳を付ける** - ユーザーの言語で理解をサポート

## 実装内容

### 1. 音声認識の言語設定

```kotlin
// 音声認識の言語を取得
val voiceInputLanguage = remember {
    when (Locale.getDefault().language) {
        "ja" -> "ja-JP"      // 日本語
        "ceb" -> "en-US"     // ビサヤ語話者は英語で代用
        "fil", "tl" -> "fil-PH"  // タガログ語
        else -> "en-US"      // デフォルトは英語
    }
}
```

### 2. 2種類の音声入力

#### A. 翻訳用音声入力（推奨）
ユーザーの母語で話して、自動的にビサヤ語に翻訳

```kotlin
// 音声入力ランチャー（翻訳用）
val voiceInputLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
        if (!spokenText.isNullOrBlank()) {
            japaneseInput = spokenText
            // 自動的に翻訳
            viewModel.translateToVisayan(spokenText) { translated ->
                translatedText = translated
                userInput = translated
            }
        }
    }
}
```

**使用例：**
1. マイクボタンをタップ
2. 「こんにちは」と話す
3. 自動的に「Maayong adlaw」に翻訳
4. 翻訳結果が入力欄に表示

#### B. 直接入力用音声入力（上級者向け）
ビサヤ語で直接話す（認識精度は低い）

```kotlin
// 音声入力ランチャー（直接入力用）
val directVoiceInputLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
        if (!spokenText.isNullOrBlank()) {
            userInput = spokenText
        }
    }
}
```

**使用例：**
1. メイン入力欄のマイクボタンをタップ
2. ビサヤ語で話す
3. 認識されたテキストが入力欄に表示

### 3. UI配置

```
┌─────────────────────────────────────┐
│  クイックフレーズ                    │
├─────────────────────────────────────┤
│ ┌─────────────────────────┬────┐   │
│ │ ビサヤ語を入力          │ 🎤 │   │
│ │                         │送信│   │
│ └─────────────────────────┴────┘   │
│                                     │
│ [日本語から翻訳 ▼]                  │
│                                     │
│ ┌─────────────────────────┬────┐   │
│ │ 日本語を入力            │ 🎤 │   │
│ │                         │翻訳│   │
│ └─────────────────────────┴────┘   │
│ → Maayong adlaw                    │
└─────────────────────────────────────┘
```

## 言語別の動作

### 🇯🇵 日本語ユーザー

**音声入力：**
- 翻訳エリアのマイク：日本語認識（ja-JP）
- メイン入力のマイク：ビサヤ語認識（ceb-PH）

**AI応答：**
- ビサヤ語 + 日本語訳

**使用例：**
```
ユーザー（音声）: 「おはようございます」
↓ 自動翻訳
入力欄: "Maayong buntag"
↓ 送信
AI: "Maayong buntag! Kumusta ka?"
翻訳: "おはようございます！元気ですか？"
```

### 🇺🇸 英語ユーザー

**音声入力：**
- 翻訳エリアのマイク：英語認識（en-US）
- メイン入力のマイク：ビサヤ語認識（ceb-PH）

**AI応答：**
- ビサヤ語 + 英語訳

**使用例：**
```
User (voice): "Good morning"
↓ Auto-translate
Input: "Maayong buntag"
↓ Send
AI: "Maayong buntag! Kumusta ka?"
Translation: "Good morning! How are you?"
```

### 🇵🇭 タガログ語ユーザー

**音声入力：**
- 翻訳エリアのマイク：タガログ語認識（fil-PH）
- メイン入力のマイク：ビサヤ語認識（ceb-PH）

**AI応答：**
- ビサヤ語 + タガログ語訳

**使用例：**
```
User (voice): "Magandang umaga"
↓ Auto-translate
Input: "Maayong buntag"
↓ Send
AI: "Maayong buntag! Kumusta ka?"
Salin: "Magandang umaga! Kumusta ka?"
```

### 🇵🇭 ビサヤ語ユーザー

**音声入力：**
- 翻訳エリアのマイク：英語認識（en-US）※代用
- メイン入力のマイク：ビサヤ語認識（ceb-PH）

**AI応答：**
- ビサヤ語 + ビサヤ語の説明

## 技術的な詳細

### Android音声認識API

```kotlin
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP") // 言語コード
    putExtra(RecognizerIntent.EXTRA_PROMPT, "話してください") // プロンプト
}
launcher.launch(intent)
```

### サポートされる言語コード

| 言語 | コード | サポート状況 |
|------|--------|--------------|
| 日本語 | ja-JP | ✅ 高精度 |
| 英語 | en-US | ✅ 高精度 |
| タガログ語 | fil-PH | ✅ 良好 |
| ビサヤ語 | ceb-PH | ⚠️ 低精度 |

### パーミッション

既に`AndroidManifest.xml`に設定済み：
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## 利点

### 1. 学習効率の向上
- 母語で考えて、ビサヤ語に変換
- 発音を気にせず、内容に集中できる

### 2. 初心者に優しい
- ビサヤ語のスペルを知らなくても会話できる
- 音声認識の精度が高い言語を使用

### 3. 多様な学習スタイル
- テキスト入力派
- 音声入力派
- 両方を組み合わせる派

### 4. アクセシビリティ
- タイピングが苦手な人でも使える
- 移動中でも使いやすい

## 使用シナリオ

### シナリオ1: 初級者の日本人
```
1. 「日本語から翻訳」を開く
2. マイクボタンをタップ
3. 「ありがとう」と話す
4. 自動的に「Salamat」に翻訳
5. 送信ボタンをタップ
6. AIが「Walay sapayan!」と応答
7. 翻訳「どういたしまして！」を確認
```

### シナリオ2: 中級者のフィリピン人（タガログ語話者）
```
1. 「日本語から翻訳」を開く
2. マイクボタンをタップ
3. 「Kumusta ka?」と話す（タガログ語）
4. 自動的にビサヤ語に翻訳
5. 送信
6. AIがビサヤ語で応答
7. タガログ語訳を確認
```

### シナリオ3: 上級者
```
1. メイン入力欄のマイクボタンをタップ
2. ビサヤ語で直接話す
3. 認識されたビサヤ語を確認・修正
4. 送信
5. AIがビサヤ語で応答
```

## 注意事項

### ビサヤ語音声認識の精度
- Google音声認識でもビサヤ語の精度は低い
- 誤認識が多い可能性がある
- 上級者向けの機能として提供

### ネットワーク要件
- 音声認識にはインターネット接続が必要
- オフライン時は使用不可

### プライバシー
- 音声データはGoogleのサーバーに送信される
- プライバシーポリシーに明記が必要

## テスト方法

### 1. 日本語音声入力のテスト
```
1. 端末の言語を日本語に設定
2. AI会話を開始
3. 「日本語から翻訳」を開く
4. マイクボタンをタップ
5. 「こんにちは」と話す
6. 「Maayong adlaw」に翻訳されることを確認
```

### 2. 英語音声入力のテスト
```
1. 端末の言語を英語に設定
2. AI会話を開始
3. 「Translate from English」を開く
4. マイクボタンをタップ
5. "Hello"と話す
6. "Maayong adlaw"に翻訳されることを確認
```

### 3. タガログ語音声入力のテスト
```
1. 端末の言語をタガログ語に設定
2. AI会話を開始
3. 翻訳エリアを開く
4. マイクボタンをタップ
5. "Kumusta"と話す
6. ビサヤ語に翻訳されることを確認
```

### 4. ビサヤ語直接入力のテスト
```
1. メイン入力欄のマイクボタンをタップ
2. ビサヤ語で話す
3. 認識されたテキストを確認
4. 必要に応じて修正
5. 送信
```

## トラブルシューティング

### 問題: 音声認識が起動しない
**解決策:**
1. マイクのパーミッションを確認
2. Google音声認識アプリがインストールされているか確認
3. インターネット接続を確認

### 問題: 認識精度が低い
**解決策:**
1. 静かな場所で試す
2. はっきりと話す
3. 端末のマイクに近づける
4. 言語設定を確認

### 問題: ビサヤ語が認識されない
**解決策:**
1. これは正常な動作です
2. ビサヤ語の音声認識精度は低いため
3. 翻訳機能を使用することを推奨

## 今後の改善案

### 1. オフライン音声認識
- Whisperなどのオフラインモデルを使用
- プライバシーの向上
- ネットワーク不要

### 2. ビサヤ語専用音声認識モデル
- カスタムモデルのトレーニング
- 認識精度の向上
- 現地の方言にも対応

### 3. 音声フィードバック
- AIの応答を音声で読み上げ
- ビサヤ語のTTS（Text-to-Speech）
- 発音の学習に役立つ

### 4. 音声会話モード
- 完全ハンズフリーの会話
- 自動的に音声認識を開始
- リアルタイム会話の実現

## まとめ

- ✅ 日本語、英語、タガログ語の音声入力に対応
- ✅ 自動翻訳機能と連携
- ✅ 初心者から上級者まで対応
- ✅ 端末の言語設定に応じて自動調整
- ⚠️ ビサヤ語直接入力は精度が低い（上級者向け）
- ✅ アクセシビリティの向上

この機能により、より多くのユーザーが快適にビサヤ語を学習できるようになりました！
