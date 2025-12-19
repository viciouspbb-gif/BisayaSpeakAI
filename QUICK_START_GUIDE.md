# Bisaya Speak AI - クイックスタートガイド

**最終更新**: 2025年10月17日

---

## 🚀 5分でアプリを起動する

### ステップ1: Pythonサーバーを起動

```powershell
# PowerShellを開く
cd C:\Users\katsunori\CascadeProjects\bisaya-pronunciation-server
python main.py
```

✅ サーバーが起動したら、`http://localhost:8000` でアクセス可能

---

### ステップ2: Android Studioを開く

```powershell
# PowerShellで実行（または手動で起動）
Start-Process "C:\Program Files\Android\Android Studio\bin\studio64.exe" `
  -ArgumentList "C:\Users\katsunori\CascadeProjects\BisayaSpeakAI"
```

または：
1. Android Studioを起動
2. **File** → **Open**
3. `C:\Users\katsunori\CascadeProjects\BisayaSpeakAI` を選択

---

### ステップ3: デバイスを準備

#### オプションA: エミュレータ（推奨）
1. Android Studio上部の「Device Manager」をクリック
2. エミュレータを起動（なければ「Create Device」で作成）
3. 推奨: Pixel 5 API 33（Android 13）

#### オプションB: 実機
1. 設定 → 開発者向けオプション → USBデバッグを有効化
2. USBケーブルで接続
3. PowerShellで確認:
   ```powershell
   adb devices
   ```

---

### ステップ4: アプリをビルド＆実行

1. Android Studio上部の緑色「▶ Run 'app'」ボタンをクリック
2. デバイスを選択
3. ビルド完了まで待つ（初回は5-10分）
4. 自動的にインストールされて起動

---

## ✅ 動作確認チェックリスト

### 基本機能
- [ ] アプリが起動する
- [ ] レベル選択画面が表示される
- [ ] レベルをタップして練習画面へ遷移
- [ ] フレーズ一覧が表示される
- [ ] フレーズをタップして録音画面へ遷移

### 録音機能
- [ ] マイク権限を許可
- [ ] マイクボタンで録音開始
- [ ] 録音中のアニメーション表示
- [ ] 停止ボタンで録音終了
- [ ] 「診断する」ボタンが表示される

### AI診断機能（サーバー必須）
- [ ] 「診断する」ボタンをタップ
- [ ] インタースティシャル広告が表示される（オプション）
- [ ] 診断処理中のローディング表示
- [ ] 結果画面が表示される
- [ ] スコア（0-100点）が表示される
- [ ] 評価（Excellent/Good/Fair）が表示される
- [ ] 詳細フィードバックが表示される

### 広告表示（AdMob）
- [ ] 練習画面下部にバナー広告が表示される
- [ ] 診断時にインタースティシャル広告が表示される

---

## 🔧 トラブルシューティング

### ビルドエラーが出た場合

#### 1. Gradleを再同期
```
メニュー → File → Sync Project with Gradle Files
```

#### 2. クリーンビルド
```
メニュー → Build → Clean Project
完了後、もう一度「▶ Run」をクリック
```

#### 3. Gradle Wrapperがない場合
プロジェクトルートで：
```powershell
# Android Studioのターミナルで実行
gradle wrapper --gradle-version 8.13.0
```

---

### サーバーに接続できない場合

#### エミュレータの場合
アプリは `http://10.0.2.2:8000` を使用（エミュレータ用のローカルホスト）

#### 実機の場合
1. PCのローカルIPアドレスを確認:
   ```powershell
   ipconfig
   # IPv4アドレスをメモ（例: 192.168.1.100）
   ```

2. アプリのコードを編集:
   ```kotlin
   // app/src/main/java/com/bisayaspeak/ai/data/api/RetrofitClient.kt
   private const val BASE_URL = "http://192.168.1.100:8000/"
   ```

3. 再ビルド

---

### デバイスが認識されない場合

```powershell
# ADBサーバーを再起動
adb kill-server
adb start-server
adb devices
```

---

### 広告が表示されない場合

#### テスト広告IDを使用
```kotlin
// app/src/main/java/com/bisayaspeak/ai/ads/AdManager.kt
// テスト広告IDに変更
private val bannerAdUnitId = "ca-app-pub-3940256099942544/6300978111" // テスト用
private val interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712" // テスト用
```

---

## 📱 実機でテストする場合の注意点

### 1. 同じWi-Fiネットワークに接続
- PCと実機を同じWi-Fiに接続

### 2. ファイアウォール設定
- Windows Defenderでポート8000を許可

### 3. サーバーURLを変更
- 上記「サーバーに接続できない場合」を参照

---

## 🎯 次のステップ

### 開発中
1. ✅ 基本機能のテスト
2. ⏳ 各レベルでの診断テスト
3. ⏳ エラーハンドリングの確認
4. ⏳ UI/UXの改善

### リリース準備
1. ⏳ スクリーンショット撮影（8枚）
2. ⏳ Google Play Consoleアカウント作成
3. ⏳ リリースビルド作成（AAB）
4. ⏳ ストアリスティング完成
5. ⏳ Google Playに公開

---

## 📚 関連ドキュメント

- **PROJECT_STATUS.md** - プロジェクト完成状況
- **BUILD_INSTRUCTIONS.md** - 詳細ビルド手順
- **TESTING_GUIDE.md** - テストガイド
- **RELEASE_CHECKLIST.md** - リリースチェックリスト
- **NEXT_STEPS.md** - 次回作業手順

---

## 🆘 ヘルプ

### よくある質問

**Q: ビルドに時間がかかる**  
A: 初回ビルドは5-10分かかります。2回目以降は1-2分です。

**Q: エミュレータが遅い**  
A: Android Studioの設定でハードウェアアクセラレーションを有効化してください。

**Q: 診断が失敗する**  
A: Pythonサーバーが起動しているか確認してください。

**Q: 広告が表示されない**  
A: テスト広告IDを使用しているか確認してください。

---

**このガイドに従えば、5分でアプリを起動できます！** 🚀

**問題が発生した場合は、エラーメッセージをコピーして次回のセッションで対応します。**

---

**作成日**: 2025年10月17日  
**プロジェクト**: Bisaya Speak AI
