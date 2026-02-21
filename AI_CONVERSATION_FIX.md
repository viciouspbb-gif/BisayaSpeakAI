# AI会話機能の修正内容

## 問題
AI会話機能がモックデータのような応答になっていて、正常に会話ができない状態でした。

## 原因分析
1. **JSONパース処理の不備**: Gemini APIからの応答が様々な形式（```json ブロック、生のJSON、テキスト混在など）で返される可能性があるが、パース処理が単純すぎた
2. **エラーハンドリングの不足**: API呼び出しやパース処理でエラーが発生しても詳細なログが出力されず、問題の特定が困難だった
3. **フォールバック処理**: パースに失敗した場合、生のレスポンステキストをそのまま返していたため、期待する形式でない応答が表示されていた

## 修正内容

### 1. JSON応答パースの改善 (`GeminiConversationEngine.kt`)

#### 変更前
```kotlin
private fun parseAIResponse(responseText: String): AIResponse {
    return try {
        val bisayaRegex = """"bisaya":\s*"([^"]+)"""".toRegex()
        val japaneseRegex = """"japanese":\s*"([^"]+)"""".toRegex()
        
        val bisaya = bisayaRegex.find(responseText)?.groupValues?.get(1) ?: ""
        val japanese = japaneseRegex.find(responseText)?.groupValues?.get(1) ?: ""
        
        AIResponse(
            text = bisaya.ifEmpty { responseText },
            translation = japanese.ifEmpty { "" },
            hints = emptyList(),
            isConversationEnd = false
        )
    } catch (e: Exception) {
        AIResponse(
            text = responseText,
            translation = "",
            hints = emptyList(),
            isConversationEnd = false
        )
    }
}
```

#### 変更後
```kotlin
private fun parseAIResponse(responseText: String): AIResponse {
    Log.d("GeminiEngine", "Parsing response: ${responseText.take(200)}...")
    
    return try {
        // JSONブロックを抽出（```json ... ``` や { ... } の形式に対応）
        val jsonText = when {
            responseText.contains("```json") -> {
                val startIndex = responseText.indexOf("```json") + 7
                val endIndex = responseText.indexOf("```", startIndex)
                if (endIndex > startIndex) {
                    responseText.substring(startIndex, endIndex).trim()
                } else {
                    responseText
                }
            }
            responseText.trim().startsWith("{") -> {
                responseText.trim()
            }
            else -> {
                // JSONブロックが見つからない場合、{ } で囲まれた部分を探す
                val startIndex = responseText.indexOf("{")
                val endIndex = responseText.lastIndexOf("}")
                if (startIndex >= 0 && endIndex > startIndex) {
                    responseText.substring(startIndex, endIndex + 1)
                } else {
                    responseText
                }
            }
        }
        
        Log.d("GeminiEngine", "Extracted JSON: ${jsonText.take(200)}...")
        
        // JSONパース処理（改善版）
        val bisayaRegex = """"bisaya"\s*:\s*"([^"]+)"""".toRegex()
        val japaneseRegex = """"japanese"\s*:\s*"([^"]+)"""".toRegex()
        
        val bisayaMatch = bisayaRegex.find(jsonText)
        val japaneseMatch = japaneseRegex.find(jsonText)
        
        val bisaya = bisayaMatch?.groupValues?.get(1)?.replace("\\n", "\n") ?: ""
        val japanese = japaneseMatch?.groupValues?.get(1)?.replace("\\n", "\n") ?: ""
        
        Log.d("GeminiEngine", "Parsed - Bisaya: $bisaya, Japanese: $japanese")
        
        if (bisaya.isNotEmpty()) {
            AIResponse(
                text = bisaya,
                translation = japanese,
                hints = emptyList(),
                isConversationEnd = false
            )
        } else {
            // JSONパースに失敗した場合、生のテキストを使用
            Log.w("GeminiEngine", "JSON parse failed, using raw text")
            AIResponse(
                text = responseText.trim(),
                translation = "",
                hints = emptyList(),
                isConversationEnd = false
            )
        }
    } catch (e: Exception) {
        Log.e("GeminiEngine", "Error parsing response", e)
        AIResponse(
            text = responseText.trim(),
            translation = "",
            hints = emptyList(),
            isConversationEnd = false
        )
    }
}
```

**改善点:**
- 複数のJSON形式に対応（```json ブロック、生のJSON、テキスト混在）
- エスケープシーケンス（`\n`など）を正しく処理
- 詳細なログ出力で問題の特定が容易に
- パース失敗時の適切なフォールバック処理

### 2. エラーハンドリングの強化

#### `startConversation`メソッド
```kotlin
try {
    val response = model.generateContent(prompt)
    val responseText = response.text ?: ""
    Log.d("GeminiEngine", "Received response (length: ${responseText.length})")
    
    if (responseText.isEmpty()) {
        Log.e("GeminiEngine", "Empty response from Gemini API")
        throw Exception("Gemini APIから空の応答が返されました")
    }
    
    return parseAIResponse(responseText)
} catch (e: Exception) {
    Log.e("GeminiEngine", "Error in startConversation", e)
    throw Exception("会話の開始に失敗しました: ${e.message}", e)
}
```

#### `respondToUser`メソッド
```kotlin
try {
    val response = model.generateContent(prompt)
    val responseText = response.text ?: ""
    Log.d("GeminiEngine", "Received response (length: ${responseText.length})")
    
    if (responseText.isEmpty()) {
        Log.e("GeminiEngine", "Empty response from Gemini API")
        throw Exception("Gemini APIから空の応答が返されました")
    }
    
    return parseAIResponse(responseText)
} catch (e: Exception) {
    Log.e("GeminiEngine", "Error in respondToUser", e)
    throw Exception("応答の生成に失敗しました: ${e.message}", e)
}
```

**改善点:**
- API呼び出しの各段階で詳細なログを出力
- 空の応答を検出して適切なエラーメッセージを表示
- エラーメッセージに具体的な原因を含める

## テスト方法

1. **アプリをビルドして実行**
   ```bash
   cd C:\Users\katsunori\CascadeProjects\BisayaSpeakAI
   .\gradlew assembleDebug
   ```

2. **Logcatでログを確認**
   - Android Studioのログキャットで以下のタグをフィルタ:
     - `GeminiEngine`
     - `ConversationVM`
     - `ConversationRepo`

3. **確認項目**
   - [ ] AI会話が開始できる
   - [ ] ビサヤ語と日本語訳が正しく表示される
   - [ ] ユーザーの発言に対してAIが応答する
   - [ ] エラーが発生した場合、詳細なログが出力される

## 期待される動作

1. **正常な会話フロー**
   ```
   [GeminiEngine] Starting conversation: mode=FREE_TALK, level=BEGINNER
   [GeminiEngine] Sending prompt (length: 1234)
   [GeminiEngine] Received response (length: 567)
   [GeminiEngine] Parsing response: {"bisaya": "Maayong buntag!", ...
   [GeminiEngine] Extracted JSON: {"bisaya": "Maayong buntag!", ...
   [GeminiEngine] Parsed - Bisaya: Maayong buntag!, Japanese: おはようございます
   ```

2. **エラー時の動作**
   ```
   [GeminiEngine] Error in startConversation: API key invalid
   [ConversationVM] Failed to start session: 会話の開始に失敗しました: API key invalid
   ```

## 追加の注意事項

### APIキーの確認
`local.properties`ファイルに正しいGemini APIキーが設定されているか確認してください:
```properties
GEMINI_API_KEY=your_actual_api_key_here
```

### APIキーの取得
https://makersuite.google.com/app/apikey

### トラブルシューティング

1. **空の応答が返される場合**
   - APIキーが有効か確認
   - APIの利用制限に達していないか確認
   - ネットワーク接続を確認

2. **JSONパースエラーが発生する場合**
   - ログでGemini APIからの生の応答を確認
   - システムプロンプトが正しく設定されているか確認

3. **会話が開始できない場合**
   - Logcatでエラーメッセージを確認
   - インターネット接続を確認
   - アプリの権限設定を確認

## 今後の改善案

1. **より堅牢なJSONパース**: Gson/Kotlinx.serializationライブラリを使用
2. **リトライ機構**: API呼び出し失敗時の自動リトライ
3. **キャッシング**: 同じ質問に対する応答をキャッシュ
4. **オフライン対応**: 基本的なフレーズはローカルで応答
