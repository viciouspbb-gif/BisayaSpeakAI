# Bisaya Speak AI - 最終ローンチガイド

アプリを公開するための最終アクションステップです。

---

## 🎯 ローンチまでの3ステップ

### ステップ1: サーバー起動
### ステップ2: 最終検証
### ステップ3: ストア提出

---

## 📋 ステップ1: Pythonサーバーの起動

### 1.1 環境セットアップ

#### Windows PowerShellを開く
```powershell
# プロジェクトディレクトリに移動
cd C:\Users\katsunori\CascadeProjects\bisaya-pronunciation-server
```

#### 仮想環境の作成と有効化
```powershell
# 仮想環境を作成（初回のみ）
python -m venv venv

# 仮想環境を有効化
.\venv\Scripts\activate

# プロンプトが (venv) で始まることを確認
```

#### 依存パッケージのインストール
```powershell
# requirements.txtから一括インストール
pip install -r requirements.txt

# インストール確認
pip list
```

**期待される出力**:
```
Package           Version
----------------- -------
fastapi           0.104.1
uvicorn           0.24.0
librosa           0.10.1
soundfile         0.12.1
numpy             1.24.3
scipy             1.11.4
fastdtw           0.3.4
...
```

### 1.2 参照音声の生成

```powershell
# 参照音声生成スクリプトを実行
python generate_reference_audio.py
```

**期待される出力**:
```
============================================================
Bisaya Speak AI - Reference Audio Generator
============================================================
Created: reference_audio\maayong_buntag_ref.wav
Created: reference_audio\maayong_hapon_ref.wav
Created: reference_audio\maayong_gabii_ref.wav
Created: reference_audio\salamat_ref.wav
Created: reference_audio\palihug_ref.wav
Created: reference_audio\oo_ref.wav
Created: reference_audio\dili_ref.wav
Created: reference_audio\kumusta_ref.wav
Created: reference_audio\maayo_ref.wav
Created: reference_audio\pangalan_ref.wav

✓ Generated 10 reference audio files
📁 Location: C:\Users\katsunori\CascadeProjects\bisaya-pronunciation-server\reference_audio

⚠️  IMPORTANT: These are dummy audio files for testing.
   Replace them with actual Bisaya native speaker recordings for production use.

✓ Created README: reference_audio\README.md

============================================================
Setup Complete!
============================================================
```

### 1.3 サーバー起動

```powershell
# FastAPIサーバーを起動
python main.py
```

**期待される出力**:
```
INFO:     Will watch for changes in these directories: ['C:\\Users\\katsunori\\CascadeProjects\\bisaya-pronunciation-server']
INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
INFO:     Started reloader process [12345] using StatReload
INFO:     Started server process [12346]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
```

### 1.4 サーバー動作確認

**別のPowerShellウィンドウを開く**:

```powershell
# ヘルスチェック
curl http://localhost:8000/

# 期待されるレスポンス
# {"status":"ok","message":"Bisaya Speak AI is running","version":"1.0.0"}
```

**ブラウザで確認**:
- http://localhost:8000/ - ヘルスチェック
- http://localhost:8000/docs - API仕様書（Swagger UI）
- http://localhost:8000/redoc - 代替ドキュメント

### 1.5 サーバーログの確認

**正常なログ**:
```
INFO:     127.0.0.1:xxxxx - "GET / HTTP/1.1" 200 OK
INFO:     127.0.0.1:xxxxx - "GET /docs HTTP/1.1" 200 OK
```

**サーバーは起動したまま維持** ✅

---

## 🧪 ステップ2: 最終検証の実行

### 2.1 Android Studioでアプリをビルド

#### プロジェクトを開く
1. Android Studioを起動
2. `Open` → `C:\Users\katsunori\CascadeProjects\BisayaSpeakAI`
3. Gradle Syncを待つ

#### サーバーURLの設定確認

**`RetrofitClient.kt`を開く**:
```kotlin
// エミュレータの場合
private const val BASE_URL = "http://10.0.2.2:8000/"

// 実機の場合（要変更）
// private const val BASE_URL = "http://YOUR_PC_IP:8000/"
```

**実機を使用する場合**:
1. PCのIPアドレスを確認
   ```powershell
   ipconfig
   # IPv4 Address を確認（例: 192.168.1.100）
   ```
2. `BASE_URL`を変更
   ```kotlin
   private const val BASE_URL = "http://192.168.1.100:8000/"
   ```

#### ビルドと実行
1. デバイスを選択（エミュレータまたは実機）
2. `Run` ボタン（緑の三角）をクリック
3. アプリが起動するまで待つ

### 2.2 AdMob動作検証

#### テスト手順

**バナー広告の確認**:
1. アプリを起動
2. レベル選択画面で「初級」を選択
3. 練習画面に遷移
4. **画面下部にバナー広告が表示されることを確認** ✅

**確認ポイント**:
- [ ] バナー広告が表示される
- [ ] 広告のサイズが適切（画面幅）
- [ ] 学習コンテンツを妨げない
- [ ] スクロールしても広告は固定

**インタースティシャル広告の確認**:
1. 練習画面で「Maayong buntag」を選択
2. 録音画面に遷移
3. マイク権限を許可
4. マイクボタンをタップして録音開始
5. 2-3秒後、停止ボタンをタップ
6. 「発音を診断」ボタンをタップ
7. **インタースティシャル広告が表示されることを確認** ✅
8. 広告を閉じる（5秒後にスキップ可能）
9. 診断結果画面に遷移することを確認

**確認ポイント**:
- [ ] 診断ボタン押下後、即座に広告が表示
- [ ] 広告表示中、バックグラウンドで診断処理
- [ ] 広告終了後、スムーズに結果画面に遷移
- [ ] 広告がない場合でも診断は継続

**検証結果記録**:
```
日時: _______________
バナー広告表示: 成功 / 失敗
インタースティシャル広告表示: 成功 / 失敗
問題点: _______________
```

### 2.3 発音診断精度検証

#### 初級者テスト（甘い評価）

**テスト手順**:
1. レベル選択で「初級」を選択
2. 「Maayong buntag」を選択
3. 録音して診断
4. 結果を確認

**期待される結果**:
```
スコア: 75-85点
評価: Excellent ✨
フィードバック: "Great job! Your pronunciation is very close to native."
Tips: "Focus on listening to native speakers and repeating after them."
```

**確認ポイント**:
- [ ] スコアゲージが表示される
- [ ] 80点前後で「Excellent」評価
- [ ] ポジティブなフィードバック
- [ ] 初心者向けのTips

#### 中級者テスト（標準評価）

**テスト手順**:
1. 戻るボタンで練習画面に戻る
2. レベル選択で「中級」を選択
3. 「Pila ni?」を選択
4. 録音して診断
5. 結果を確認

**期待される結果**:
```
スコア: 75-85点
評価: Good 👍
フィードバック: "Good pronunciation! Keep practicing to improve further."
Tips: "Pay attention to subtle sound differences and intonation patterns."
```

**確認ポイント**:
- [ ] 同じスコアでも「Good」評価（初級より厳しい）
- [ ] 具体的な改善点の指摘
- [ ] 中級者向けのTips

#### 上級者テスト（厳しい評価）

**テスト手順**:
1. レベル選択で「上級」を選択
2. 「Unsaon nako pag-adto sa Ayala?」を選択
3. 録音して診断
4. 結果を確認

**期待される結果**:
```
スコア: 75-85点
評価: Good 📊
フィードバック: "Good pronunciation! Keep practicing to improve further."
詳細: ピッチ、タイミング、音量の細かい指摘
Tips: "Work on perfecting the nuances and natural flow of speech."
```

**確認ポイント**:
- [ ] 80点でも「Good」評価（Excellentには届かない）
- [ ] 細かい改善点の指摘
- [ ] 高度なTips

#### レベル別比較

**同じ発音品質での評価比較**:
| レベル | スコア | 評価 | 合格/不合格 |
|--------|--------|------|-------------|
| 初級 | 80点 | Excellent | ✅ |
| 中級 | 80点 | Good | ✅ |
| 上級 | 80点 | Good | ✅ |

**確認ポイント**:
- [ ] レベルによって評価が異なる
- [ ] フィードバックの詳細度が異なる
- [ ] パーソナライズが機能している

### 2.4 UI/UX検証

#### 画面遷移
- [ ] レベル選択 → 練習画面
- [ ] 練習画面 → 録音画面
- [ ] 録音画面 → 結果画面
- [ ] 結果画面 → 練習画面（戻る）
- [ ] すべての遷移がスムーズ

#### アニメーション
- [ ] スコアゲージのアニメーション
- [ ] 録音中の脈動エフェクト
- [ ] 画面遷移のトランジション

#### エラーハンドリング
- [ ] マイク権限拒否時の処理
- [ ] サーバー接続エラー時の処理
- [ ] 録音失敗時の処理

### 2.5 最終判定

#### AdMob
- [ ] バナー広告: 100%表示
- [ ] インタースティシャル: 90%以上表示
- [ ] UXを損なわない

#### 発音診断
- [ ] レベル別評価が正しく機能
- [ ] 初級者が適切に励まされる
- [ ] 上級者が適切に挑戦される

#### 総合判定
- [ ] **商用リリース可能** ✅
- [ ] 要修正（詳細: _______________）

---

## 📤 ステップ3: Google Play Storeへの提出

### 3.1 Google Play Consoleアカウント作成

#### アカウント登録
1. https://play.google.com/console にアクセス
2. Googleアカウントでログイン
3. 「デベロッパーアカウントを作成」
4. 登録料: $25（一度のみ）
5. 支払い情報を入力
6. 利用規約に同意

#### デベロッパープロフィール
```
デベロッパー名: _______________
メールアドレス: _______________
ウェブサイト: _______________ (オプション)
```

### 3.2 アプリの作成

#### 新しいアプリを作成
1. 「アプリを作成」をクリック
2. アプリ名: **Bisaya Speak AI**
3. デフォルトの言語: 日本語
4. アプリまたはゲーム: アプリ
5. 無料または有料: 無料
6. 「アプリを作成」をクリック

### 3.3 署名鍵の生成

#### Android Studioで署名鍵を生成

1. **Build → Generate Signed Bundle / APK**
2. **Android App Bundle** を選択
3. **Create new...** をクリック

**署名鍵情報**:
```
Key store path: C:\Users\katsunori\bisaya-speak-ai-keystore.jks
Password: [強力なパスワード]
Key alias: bisaya-speak-ai
Key password: [強力なパスワード]

Certificate:
First and Last Name: [あなたの名前]
Organizational Unit: [組織名]
Organization: [会社名]
City or Locality: [市区町村]
State or Province: [都道府県]
Country Code: JP
```

4. **OK** をクリック
5. **Next** をクリック
6. **release** を選択
7. **Finish** をクリック

**重要**: 署名鍵とパスワードは安全に保管してください！

### 3.4 リリースビルドの作成

#### AABファイルの生成
1. 署名鍵の生成が完了すると、AABファイルが生成される
2. 場所: `app/release/app-release.aab`
3. ファイルサイズを確認（通常10-20MB）

### 3.5 ストアリスティングの設定

#### アプリの詳細

**メインストアの掲載情報**:
```
アプリ名: Bisaya Speak AI - セブアノ語発音学習

短い説明（80文字）:
AIがあなたのビサヤ語発音を100点満点で診断！レベル別学習で確実に上達

完全な説明（4000文字）:
[STORE_LISTING.mdの完全な説明をコピー]
```

#### グラフィック アセット

**必須アセット**:
1. **アプリアイコン** (512 x 512 px)
   - ICON_DESIGN.mdに従って作成
   - PNG形式、32-bit
   - 透過なし

2. **フィーチャーグラフィック** (1024 x 500 px)
   - アプリの主要機能を表示
   - 「AIが発音を100点満点で診断」などのキャッチコピー

3. **スクリーンショット** (最低2枚、推奨8枚)
   - 携帯電話: 1080 x 1920 px
   - 7インチタブレット: 1200 x 1920 px (オプション)
   - 10インチタブレット: 1600 x 2560 px (オプション)

**スクリーンショット内容**:
1. AIスコアゲージ画面
2. レベル選択画面
3. 練習画面
4. 録音画面
5. 詳細フィードバック画面
6. レベル別評価比較
7. 学習カテゴリ一覧
8. 機能一覧

#### 分類

```
アプリカテゴリ: 教育
タグ: 言語学習、教育

コンテンツレーティング:
- 対象年齢: 全年齢対象（Everyone）
- 暴力: なし
- 性的コンテンツ: なし
- 言語: 適切
```

#### 連絡先の詳細

```
ウェブサイト: https://bisayaspeak.ai (オプション)
メール: support@bisayaspeak.ai
電話: _______________ (オプション)

プライバシーポリシー URL: [必須]
https://your-domain.com/privacy-policy
```

### 3.6 プライバシーポリシーの作成

#### 最小限のプライバシーポリシー

```markdown
# プライバシーポリシー

最終更新日: 2025年10月11日

## 収集する情報

Bisaya Speak AIは、以下の情報を収集します：

1. **音声データ**
   - 発音診断のために録音された音声
   - 診断完了後、即座に削除されます
   - サーバーに保存されません

2. **使用統計**
   - アプリの使用状況（匿名）
   - クラッシュレポート
   - 広告表示データ

## 情報の使用目的

- 発音診断の提供
- アプリの改善
- 広告の表示

## データの保存期間

- 音声データ: 診断完了後即座に削除
- 使用統計: 匿名化して保存

## 第三者への提供

- Google AdMob（広告表示）
- Firebase Analytics（使用統計）

## お問い合わせ

support@bisayaspeak.ai
```

**プライバシーポリシーをウェブサイトに公開**:
- GitHub Pagesを使用（無料）
- または、簡易的なウェブホスティング

### 3.7 コンテンツレーティング

#### アンケートに回答
1. 「コンテンツレーティング」セクション
2. 「アンケートを開始」
3. 質問に回答:
   - 暴力: なし
   - 性的コンテンツ: なし
   - 不適切な言語: なし
   - 薬物: なし
   - ギャンブル: なし

4. レーティング結果: **Everyone**

### 3.8 価格と配布

```
価格: 無料
配布国: すべての国（または選択）
```

### 3.9 アプリのコンテンツ

#### データの安全性
```
データの収集:
- 音声データ（一時的）
- 使用統計（匿名）

データの共有:
- 第三者と共有しない
- 広告IDのみ共有

データの削除:
- ユーザーがリクエスト可能
```

#### 広告
```
広告の表示: はい
広告の種類:
- バナー広告
- インタースティシャル広告
広告プロバイダー: Google AdMob
```

### 3.10 リリースの作成

#### 本番トラックにリリース

1. **「リリース → 本番」**
2. **「新しいリリースを作成」**
3. **AABファイルをアップロード**
   - `app-release.aab`をドラッグ&ドロップ
4. **リリース名**: 1.0.0
5. **リリースノート**:
```
初回リリース

【主な機能】
✨ AI発音診断（0-100点）
📚 レベル別学習（初級/中級/上級）
🎯 30以上のビサヤ語フレーズ
💬 詳細なフィードバック
📊 視覚的なスコア表示

【対応言語】
- ビサヤ語（セブアノ語）
- 日本語

【サポート】
support@bisayaspeak.ai
```

6. **段階的公開** (推奨):
   - 10% → 50% → 100%
   - または、100%で即座に公開

7. **確認して公開**

### 3.11 審査待ち

**審査期間**: 通常1-3日

**審査中のステータス**:
- 審査待ち
- 審査中
- 承認済み / 却下

**承認後**:
- Google Play Storeで公開
- ユーザーがダウンロード可能

---

## 📊 公開後のモニタリング

### 初日（T+0）
- [ ] アプリが公開されているか確認
- [ ] ダウンロード可能か確認
- [ ] 初期ダウンロード数確認
- [ ] クラッシュレポート確認
- [ ] 広告表示確認

### 1週間後（T+7）
- [ ] ダウンロード数: 目標100
- [ ] 平均評価: 4.0以上
- [ ] クラッシュ率: 1%以下
- [ ] ユーザーレビュー確認

### 1ヶ月後（T+30）
- [ ] ダウンロード数: 目標1,000
- [ ] 平均評価: 4.5以上
- [ ] DAU: 200人
- [ ] 収益: $250以上

---

## 🎉 ローンチチェックリスト

### サーバー
- [ ] Pythonサーバー起動
- [ ] 参照音声生成
- [ ] ヘルスチェック成功
- [ ] API動作確認

### アプリ
- [ ] ビルド成功
- [ ] 実機でテスト
- [ ] AdMob動作確認
- [ ] 発音診断動作確認
- [ ] レベル別評価確認

### ストア
- [ ] Google Play Consoleアカウント作成
- [ ] 署名鍵生成
- [ ] AABファイル作成
- [ ] ストアリスティング完成
- [ ] プライバシーポリシー公開
- [ ] コンテンツレーティング取得
- [ ] リリース作成
- [ ] 審査提出

### マーケティング
- [ ] SNSアカウント作成
- [ ] ローンチツイート準備
- [ ] プレスリリース準備
- [ ] コミュニティ投稿準備

---

## 🚨 トラブルシューティング

### サーバーが起動しない
**問題**: `ModuleNotFoundError`
**解決**: 
```powershell
pip install -r requirements.txt
```

### アプリがサーバーに接続できない
**問題**: `Connection refused`
**解決**:
1. サーバーが起動しているか確認
2. BASE_URLが正しいか確認
3. ファイアウォールを確認

### 広告が表示されない
**問題**: 広告が表示されない
**解決**:
1. インターネット接続を確認
2. テスト広告IDを使用しているか確認
3. AdMobアカウントが承認されているか確認

### 審査が却下された
**問題**: Google Playの審査却下
**解決**:
1. 却下理由を確認
2. 問題を修正
3. 再提出

---

## 🎯 成功の定義

### 技術的成功
- ✅ サーバー稼働率: 99%以上
- ✅ アプリクラッシュ率: 1%以下
- ✅ API応答時間: 5秒以内
- ✅ 広告表示率: 90%以上

### ビジネス的成功
- ✅ 1週間: 100ダウンロード
- ✅ 1ヶ月: 1,000ダウンロード
- ✅ 3ヶ月: 5,000ダウンロード
- ✅ 平均評価: 4.5星以上
- ✅ 月間収益: $750以上

---

## 🎊 おめでとうございます！

**Bisaya Speak AIのローンチ準備が完了しました！**

**次のステップ**:
1. ✅ サーバーを起動
2. ✅ 最終検証を実施
3. ✅ Google Play Storeに提出
4. 🎉 ローンチ！

**Good luck with your launch!** 🚀✨🇵🇭🇯🇵
