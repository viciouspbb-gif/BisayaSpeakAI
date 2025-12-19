# 次回の作業手順

## 現在の状況

✅ **完了したこと**
- Android Studioでプロジェクトを開いた
- 必要なリソースファイルを作成（themes.xml, backup_rules.xml, data_extraction_rules.xml）
- 実験的API警告を修正（PracticeScreen.ktに@OptInを追加）

⏳ **次にやること**
- アプリのビルドとインストール
- 基本機能の動作確認
- （オプション）Pythonサーバーのセットアップ

---

## 次回起動時の手順

### 1. Android Studioを開く

```powershell
# PowerShellで実行
Start-Process "C:\Program Files\Android\Android Studio\bin\studio64.exe" -ArgumentList "C:\Users\katsunori\CascadeProjects\BisayaSpeakAI"
```

または、Android Studioを起動して：
- **File** → **Open**
- `C:\Users\katsunori\CascadeProjects\BisayaSpeakAI` を選択

### 2. デバイスを準備

#### オプションA: エミュレータを使用
1. Android Studio上部の「Device Manager」をクリック
2. エミュレータを起動（なければ「Create Device」で作成）

#### オプションB: 実機を使用
1. USBデバッグを有効にする
2. USBケーブルで接続
3. `adb devices` で接続確認

### 3. アプリをビルド＆実行

1. 上部ツールバーの緑色「▶ Run 'app'」ボタンをクリック
2. デバイスを選択
3. ビルドが完了するまで待つ（初回は5-10分）
4. 自動的にインストールされて起動

### 4. 動作確認

アプリが起動したら：
- ✅ レベル選択画面が表示される
- ✅ レベルをタップして練習画面へ
- ✅ フレーズをタップして録音画面へ
- ✅ マイクボタンで録音できる
- ❌ 発音診断（サーバーが必要）

---

## トラブルシューティング

### ビルドエラーが出た場合

1. **Gradleを再同期**
   - メニュー → File → Sync Project with Gradle Files

2. **クリーンビルド**
   - メニュー → Build → Clean Project
   - 完了後、もう一度「▶ Run」をクリック

3. **エラーメッセージを確認**
   - 画面下部の「Build」タブ（Alt + 6）
   - 赤いエラーメッセージをコピーして対応

### デバイスが認識されない場合

```powershell
# デバイス確認
adb devices

# ADBサーバーを再起動
adb kill-server
adb start-server
adb devices
```

---

## Pythonサーバーのセットアップ（オプション）

発音診断機能を使いたい場合のみ必要です。

### 1. Pythonをインストール

1. https://www.python.org/downloads/ からダウンロード
2. インストール時に **「Add Python to PATH」にチェック**
3. インストール完了後、PowerShellを再起動

### 2. 必要なパッケージをインストール

```powershell
cd C:\Users\katsunori\CascadeProjects\bisaya-pronunciation-server
pip install -r requirements.txt
```

### 3. サーバーを起動

```powershell
cd C:\Users\katsunori\CascadeProjects\bisaya-pronunciation-server
python main.py
```

サーバーが起動したら：
- http://localhost:8000/ でヘルスチェック
- アプリから発音診断が使えるようになります

---

## プロジェクトの場所

```
C:\Users\katsunori\CascadeProjects\
├── BisayaSpeakAI\              # Androidアプリ
│   ├── app\
│   ├── build.gradle.kts
│   └── README.md
└── bisaya-pronunciation-server\ # Pythonサーバー
    ├── main.py
    ├── requirements.txt
    └── README.md
```

---

## 参考ドキュメント

- **README.md**: アプリの概要と機能説明
- **PROJECT_SUMMARY.md**: プロジェクト全体のサマリー
- **TESTING_GUIDE.md**: テスト手順
- **BUILD_INSTRUCTIONS.md**: ビルド詳細手順

---

## 質問や問題が発生したら

1. エラーメッセージをコピー
2. どの手順で発生したかメモ
3. 次回のセッションで対応します

---

**次回は「▶ Run」ボタンをクリックするところから始めてください！**

お疲れ様でした！ 🎉
