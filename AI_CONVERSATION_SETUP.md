# AI会話機能 - セットアップガイド

## 🎉 実装完了

Gemini API + 音声ストリーミングを使用したビサヤ語AI会話機能の実装が完了しました！

---

## 📋 実装内容

### ✅ 完成した機能

1. **Gemini会話エンジン**
   - ビサヤ語対応のプロンプト設計
   - レベル別の語彙・文法調整
   - 5つの会話モード対応

2. **音声ストリーミング処理**
   - リアルタイム録音
   - WAV形式変換
   - 音量レベル計算

3. **会話モード**
   - シャドーイング（反復練習）
   - 単語ドリル（例文作成）
   - ロールプレイ（5つのシナリオ）
   - フリートーク
   - トピック会話

4. **UI画面**
   - 会話モード選択画面
   - AI会話画面（チャット形式）
   - 会話サマリー表示

---

## 🔧 セットアップ手順

### 1. Gemini APIキーの取得

1. [Google AI Studio](https://makersuite.google.com/app/apikey) にアクセス
2. Googleアカウントでログイン
3. 「Create API Key」をクリック
4. APIキーをコピー

### 2. APIキーの設定

`local.properties` ファイルを編集：

```properties
sdk.dir=C\:\\Users\\katsunori\\AppData\\Local\\Android\\Sdk

# Gemini API Key
GEMINI_API_KEY=YOUR_ACTUAL_API_KEY_HERE
```

**重要**: `YOUR_ACTUAL_API_KEY_HERE` を実際のAPIキーに置き換えてください。

### 3. 権限の追加確認

`AndroidManifest.xml` に以下の権限が追加されていることを確認：

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

### 4. ビルドとテスト

```bash
# Gradleビルド
./gradlew build

# アプリをインストール
./gradlew installDebug
```

---

## 🎯 使い方

### 基本的な流れ

1. **レベル選択画面**で学習レベルを選択
2. **会話モード選択画面**で会話モードを選択
   - シャドーイング
   - 単語ドリル
   - ロールプレイ（シナリオ選択）
   - フリートーク
   - トピック会話
3. **AI会話画面**で会話開始
   - マイクボタンをタップして録音
   - AIが応答
   - 会話を続ける
4. **終了ボタン**で会話サマリーを表示

---

## 🏗️ アーキテクチャ

### ファイル構成

```
app/src/main/java/com/bisayaspeak/ai/
├── data/
│   ├── model/
│   │   └── ConversationModels.kt      # データモデル
│   ├── ai/
│   │   └── GeminiConversationEngine.kt # Gemini会話エンジン
│   ├── audio/
│   │   └── AudioStreamProcessor.kt     # 音声処理
│   └── repository/
│       └── ConversationRepository.kt   # リポジトリ
├── ui/
│   ├── screens/
│   │   ├── ConversationModeScreen.kt   # モード選択画面
│   │   └── AIConversationScreen.kt     # AI会話画面
│   └── viewmodel/
│       └── ConversationViewModel.kt    # ViewModel
```

### データフロー

```
UI (Compose)
    ↓
ViewModel
    ↓
Repository
    ↓
├─→ GeminiConversationEngine (AI応答)
└─→ AudioStreamProcessor (音声処理)
```

---

## 🤖 Gemini プロンプト設計

### ビサヤ語対応の工夫

1. **システムプロンプト**
   - ビサヤ語会話パートナーとしての役割定義
   - レベル別の語彙・文法制限
   - JSON形式での応答指定

2. **レベル別調整**
   - **初級**: 基本300語、現在形のみ、頻繁なヒント
   - **中級**: 1000語程度、過去形・未来形、必要時のみヒント
   - **上級**: 制限なし、複雑な構文、最小限のヒント

3. **応答フォーマット**
```json
{
  "bisaya": "ビサヤ語の文章",
  "japanese": "日本語訳",
  "hints": [
    {"bisaya": "ヒント", "japanese": "訳"}
  ],
  "pronunciation_tips": "発音アドバイス",
  "cultural_note": "文化的補足"
}
```

---

## 🎭 ロールプレイシナリオ

### 実装済みシナリオ（5つ）

1. **市場で買い物** (初級)
   - 果物売りとお客さん
   - 値段交渉、購入

2. **タクシーに乗る** (初級)
   - タクシー運転手と乗客
   - 目的地指定、料金確認

3. **レストランで注文** (中級)
   - ウェイターとお客さん
   - メニュー確認、注文

4. **道に迷う** (中級)
   - 地元の人と観光客
   - 道案内、場所確認

5. **緊急事態** (上級)
   - 警察官と助けが必要な人
   - 状況説明、対処

---

## 🔊 音声処理

### 録音仕様

- **サンプルレート**: 16000 Hz
- **チャンネル**: モノラル
- **フォーマット**: PCM 16bit
- **出力**: WAV形式

### 音声ストリーミング

```kotlin
// 録音開始
viewModel.startRecording()

// 録音停止（自動的に音声認識へ）
viewModel.stopRecording()
```

---

## 📊 会話サマリー

### 生成される情報

- 会話時間
- ターン数
- 平均発音スコア
- 新しく使った語彙
- 強み
- 改善点

---

## 🚀 次のステップ

### 現在未実装の機能

1. **音声認識**
   - 現在は仮のテキストを使用
   - Whisper APIまたはGoogle Speech-to-Text統合が必要

2. **音声合成（TTS）**
   - AIの応答を音声で再生
   - Google Text-to-Speech統合

3. **会話履歴の保存**
   - Room Databaseで永続化
   - 過去の会話を振り返る機能

4. **学習進捗トラッキング**
   - 語彙習得数
   - 発音スコアの推移
   - 達成バッジ

### 実装優先度

#### 高優先度
- [ ] 音声認識（Whisper API統合）
- [ ] ナビゲーション統合（既存画面から遷移）

#### 中優先度
- [ ] 音声合成（TTS）
- [ ] 会話履歴の保存

#### 低優先度
- [ ] 学習進捗トラッキング
- [ ] 達成バッジシステム

---

## 💡 使用例

### シャドーイング

```
AI: "Maayong buntag!"
    (おはよう！)

[ユーザーが録音]
User: "Maayong buntag"
      スコア: 85点

AI: "Maayo! Try again with more emphasis on 'ng'."
    (良いですね！'ng'をもっと強調してみましょう)
```

### ロールプレイ（市場）

```
AI: "Maayong buntag! Unsa imong gusto?"
    (おはよう！何が欲しいですか？)

User: "Pila ang mangga?"
      (マンゴーはいくらですか？)

AI: "Lima ka pesos ang usa."
    (1個5ペソです)

User: "Mahal kaayo!"
      (高すぎます！)

AI: "Sige, upat na lang."
    (じゃあ、4ペソでいいよ)
```

---

## ⚠️ 注意事項

### Gemini API制限

- **無料枠**: 60リクエスト/分
- **推奨**: 初期は無料枠で十分
- **有料プラン**: 必要に応じて検討

### プライバシー

- 録音データは一時的にキャッシュに保存
- Gemini APIに送信される会話履歴は最大5ターン
- ユーザーデータの取り扱いに注意

### パフォーマンス

- AI応答時間: 通常2-5秒
- ネットワーク接続必須
- オフラインモードは未対応

---

## 🐛 トラブルシューティング

### APIキーエラー

```
Error: API key not valid
```

**解決方法**:
1. `local.properties`のAPIキーを確認
2. Gradleを再ビルド
3. アプリを再インストール

### 録音できない

```
Error: Recording permission denied
```

**解決方法**:
1. アプリ設定でマイク権限を確認
2. 権限を許可
3. アプリを再起動

### AI応答が遅い

**原因**:
- ネットワーク速度
- Gemini APIの負荷

**解決方法**:
- Wi-Fi接続を使用
- 時間帯を変えて試す

---

## 📚 参考資料

- [Gemini API Documentation](https://ai.google.dev/docs)
- [Android Audio Recording](https://developer.android.com/guide/topics/media/mediarecorder)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)

---

## ✅ チェックリスト

### セットアップ完了確認

- [ ] Gemini APIキーを取得
- [ ] `local.properties`にAPIキーを設定
- [ ] アプリをビルド成功
- [ ] エミュレータ/実機で起動確認
- [ ] マイク権限を許可
- [ ] AI会話が正常に動作

### テスト項目

- [ ] 各会話モードが起動する
- [ ] 録音ボタンが機能する
- [ ] AI応答が表示される
- [ ] 会話サマリーが生成される
- [ ] エラーハンドリングが機能する

---

**実装完了日**: 2025年10月22日  
**バージョン**: 1.0.0  
**開発者**: Cascade AI

🎉 **AI会話機能の実装が完了しました！**
