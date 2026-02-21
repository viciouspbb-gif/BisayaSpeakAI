# Bisaya Speak AI - Androidアプリ

AI搭載のビサヤ語発音学習アプリです。レベル別の学習コンテンツと、Pythonサーバーによる発音診断機能を提供します。

## 🎯 主な機能

### 1. レベル別学習システム
- **初級**: 基本的な単語とフレーズ（挨拶、基本単語など）
- **中級**: 日常会話（市場、交通、レストランなど）
- **上級**: 複雑な会話と高度な表現

### 2. AI発音診断
- **リアルタイム録音**: マイクで発音を録音
- **DTWベース評価**: Python APIによる高精度な類似度計算
- **レベル別フィードバック**: ユーザーのレベルに応じた評価基準
- **詳細分析**: ピッチ、タイミング、音量の比較

### 3. 視覚的な結果表示
- **スコアゲージ**: 0-100点をアニメーション付きで表示
- **評価バッジ**: Excellent / Good / Fair / Needs Improvement
- **詳細フィードバック**: 改善点を具体的に提示
- **比較データ**: ユーザーと参照音声の特徴量比較

### 4. 収益化（AdMob統合）
- **バナー広告**: 学習画面下部に配置
- **インタースティシャル広告**: 診断処理中に表示

## 📱 画面構成

### 1. レベル選択画面
- 初級/中級/上級から選択
- 各レベルの説明と特徴を表示

### 2. 練習画面
- レベル別の学習コンテンツ一覧
- カテゴリ別にグループ化
- ビサヤ語、発音、日本語訳、英語訳を表示
- バナー広告を下部に配置

### 3. 録音画面
- 学習コンテンツの表示
- マイクボタンで録音開始/停止
- 録音完了後、診断ボタンを表示
- 診断中にインタースティシャル広告を表示

### 4. 結果画面
- アニメーション付きスコアゲージ
- 評価バッジ（色分け）
- 総合フィードバック
- 詳細分析（ピッチ、タイミング、音量）
- アドバイス
- 再挑戦ボタン

## 🏗️ アーキテクチャ

### 技術スタック
- **言語**: Kotlin
- **UI**: Jetpack Compose
- **アーキテクチャ**: MVVM
- **ナビゲーション**: Navigation Compose
- **ネットワーク**: Retrofit + OkHttp
- **非同期処理**: Kotlin Coroutines + Flow
- **広告**: Google AdMob
- **権限**: Accompanist Permissions

### プロジェクト構造
```
app/src/main/java/com/bisayaspeak/ai/
├── MainActivity.kt                    # メインアクティビティ
├── data/
│   ├── model/
│   │   ├── LearningLevel.kt          # レベル定義
│   │   ├── LearningContent.kt        # 学習コンテンツ
│   │   └── PronunciationResult.kt    # 診断結果
│   ├── api/
│   │   ├── BisayaSpeakApiService.kt  # API定義
│   │   └── RetrofitClient.kt         # Retrofit設定
│   └── repository/
│       ├── LearningContentRepository.kt  # コンテンツ管理
│       └── PronunciationRepository.kt    # 診断API
├── ui/
│   ├── screens/
│   │   ├── LevelSelectionScreen.kt   # レベル選択
│   │   ├── PracticeScreen.kt         # 練習画面
│   │   ├── RecordingScreen.kt        # 録音画面
│   │   └── ResultScreen.kt           # 結果画面
│   ├── viewmodel/
│   │   ├── PracticeViewModel.kt      # 練習VM
│   │   └── RecordingViewModel.kt     # 録音・診断VM
│   └── theme/
│       ├── Theme.kt                  # テーマ設定
│       └── Type.kt                   # タイポグラフィ
└── ads/
    └── AdManager.kt                  # 広告管理
```

## 🚀 セットアップ

### 前提条件
- Android Studio Hedgehog | 2023.1.1 以上
- JDK 17
- Android SDK 34
- Pythonサーバーが起動していること

### 1. プロジェクトを開く
```bash
cd C:\Users\katsunori\CascadeProjects\BisayaSpeakAI
```

Android Studioでプロジェクトを開きます。

### 2. サーバーURLの設定

`RetrofitClient.kt`でサーバーのURLを設定：

```kotlin
// エミュレータの場合
private const val BASE_URL = "http://10.0.2.2:8000/"

// 実機の場合
private const val BASE_URL = "http://YOUR_SERVER_IP:8000/"
```

### 3. AdMob設定（本番環境）

`AndroidManifest.xml`と`AdManager.kt`で広告ユニットIDを実際のIDに置き換え：

```xml
<!-- AndroidManifest.xml -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="YOUR_ADMOB_APP_ID"/>
```

```kotlin
// AdManager.kt
const val BANNER_AD_UNIT_ID = "YOUR_BANNER_AD_UNIT_ID"
const val INTERSTITIAL_AD_UNIT_ID = "YOUR_INTERSTITIAL_AD_UNIT_ID"
```

### 4. ビルドと実行

```bash
# Gradleビルド
./gradlew build

# アプリをインストール
./gradlew installDebug
```

または、Android Studioの「Run」ボタンをクリック。

## 📡 APIとの連携

### サーバー要件
- Pythonサーバーが起動していること
- エンドポイント: `POST /api/pronounce/check`
- パラメータ:
  - `audio`: 音声ファイル（M4A形式）
  - `word`: 単語
  - `level`: beginner/intermediate/advanced

### レスポンス形式
```json
{
  "status": "success",
  "data": {
    "pronunciation_score": 82.5,
    "level": "beginner",
    "feedback": {
      "rating": "Good",
      "overall": "Good pronunciation!",
      "details": [...],
      "tips": "..."
    },
    "comparison_details": {...}
  }
}
```

## 🎨 UI/UX特徴

### デザイン原則
- **Material Design 3**: 最新のデザインガイドライン
- **アニメーション**: スムーズな画面遷移とフィードバック
- **色分け**: レベルや評価に応じた視覚的な区別
- **アクセシビリティ**: 大きなタップターゲット、明確なラベル

### カラースキーム
- **初級**: 緑系（成長、始まり）
- **中級**: 青系（安定、進歩）
- **上級**: 紫系（高級、達成）
- **評価**:
  - Excellent: 緑
  - Good: 青
  - Fair: 黄
  - Needs Improvement: 赤

## 💰 収益化戦略

### バナー広告
- **配置**: 練習画面の下部
- **サイズ**: 標準バナー（320x50）
- **表示頻度**: 常時表示

### インタースティシャル広告
- **タイミング**: 診断ボタン押下後、結果表示前
- **理由**: 診断処理の待ち時間を活用
- **頻度**: 各診断ごとに1回
- **プリロード**: 前回の広告表示後に次の広告をロード

### 広告最適化
```kotlin
// 広告のプリロード
adManager.loadInterstitialAd()

// 診断時に表示
adManager.showInterstitialAd(activity) {
    // 広告終了後、次の広告をプリロード
    adManager.loadInterstitialAd()
}
```

## 🧪 テスト

### 手動テスト手順
1. レベル選択画面で「初級」を選択
2. 練習画面で「Maayong buntag」を選択
3. 録音画面でマイク権限を許可
4. マイクボタンを押して録音開始
5. 発音後、停止ボタンを押す
6. 診断ボタンを押す
7. インタースティシャル広告が表示される
8. 結果画面でスコアとフィードバックを確認

### テスト用広告ID
現在の実装ではGoogleのテスト用広告IDを使用しています：
- バナー: `ca-app-pub-3940256099942544/6300978111`
- インタースティシャル: `ca-app-pub-3940256099942544/1033173712`

## 📝 今後の改善点

### 機能追加
- [ ] 学習履歴の保存
- [ ] スコアの推移グラフ
- [ ] 達成バッジシステム
- [ ] ソーシャルシェア機能
- [ ] オフライン学習モード
- [ ] 音声再生機能（参照音声）

### 技術改善
- [ ] Room DatabaseでローカルDB実装
- [ ] WorkManagerでバックグラウンド同期
- [ ] Hiltで依存性注入
- [ ] Unit/UI テストの追加
- [ ] パフォーマンス最適化

### UX改善
- [ ] オンボーディング画面
- [ ] チュートリアル
- [ ] ダークモード対応
- [ ] 多言語対応
- [ ] アクセシビリティ向上

## 🔧 トラブルシューティング

### 録音できない
- マイク権限が許可されているか確認
- デバイスのマイクが正常に動作しているか確認

### サーバーに接続できない
- Pythonサーバーが起動しているか確認
- `RetrofitClient.kt`のBASE_URLが正しいか確認
- ネットワーク接続を確認

### 広告が表示されない
- インターネット接続を確認
- AdMobアカウントが正しく設定されているか確認
- テスト用広告IDを使用しているか確認

## 📄 ライセンス

MIT License

## 👥 開発者

Bisaya Speak AI Development Team

---

**Note**: このアプリはビサヤ語学習を支援するためのツールです。実際のネイティブスピーカーとの会話練習も併せて行うことをお勧めします。
