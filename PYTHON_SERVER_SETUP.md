# Pythonサーバーのセットアップ手順

## 📋 現在の状況

**Pythonがインストールされていません。**

---

## 🐍 ステップ1: Pythonのインストール

### 1-1. Pythonをダウンロード

1. **公式サイトにアクセス**
   ```
   https://www.python.org/downloads/
   ```

2. **「Download Python 3.12.x」をクリック**
   - 最新の安定版をダウンロード

### 1-2. インストール実行

1. **ダウンロードしたファイルを実行**
   - `python-3.12.x-amd64.exe` をダブルクリック

2. **重要: 「Add Python to PATH」にチェック**
   ```
   ☑ Add python.exe to PATH
   ```
   **これを忘れると後で面倒です！**

3. **「Install Now」をクリック**
   - インストール完了まで待つ（2-3分）

4. **「Close」をクリック**

### 1-3. インストール確認

**PowerShellを再起動してから実行:**

```powershell
python --version
```

**期待される出力:**
```
Python 3.12.x
```

---

## 📦 ステップ2: サーバーディレクトリに移動

```powershell
cd C:\Users\katsunori\CascadeProjects\bisaya-pronunciation-server
```

---

## 🔧 ステップ3: 仮想環境の作成（推奨）

### 3-1. 仮想環境を作成

```powershell
python -m venv venv
```

### 3-2. 仮想環境を有効化

```powershell
.\venv\Scripts\activate
```

**成功すると、プロンプトが `(venv)` で始まります:**
```
(venv) PS C:\Users\katsunori\CascadeProjects\bisaya-pronunciation-server>
```

---

## 📥 ステップ4: 依存パッケージのインストール

### 4-1. requirements.txtの確認

```powershell
cat requirements.txt
```

### 4-2. パッケージをインストール

```powershell
pip install -r requirements.txt
```

**インストールには5-10分かかります。**

**期待される出力:**
```
Collecting fastapi==0.104.1
Collecting uvicorn==0.24.0
Collecting librosa==0.10.1
...
Successfully installed fastapi-0.104.1 uvicorn-0.24.0 ...
```

### 4-3. インストール確認

```powershell
pip list
```

**主要パッケージが表示されればOK:**
```
fastapi           0.104.1
uvicorn           0.24.0
librosa           0.10.1
soundfile         0.12.1
numpy             1.24.3
scipy             1.11.4
fastdtw           0.3.4
```

---

## 🎵 ステップ5: 参照音声の生成

### 5-1. 生成スクリプトを実行

```powershell
python generate_reference_audio.py
```

**期待される出力:**
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
```

### 5-2. 生成確認

```powershell
ls reference_audio
```

**10個のWAVファイルが表示されればOK**

---

## 🚀 ステップ6: サーバー起動

### 6-1. サーバーを起動

```powershell
python main.py
```

**期待される出力:**
```
INFO:     Will watch for changes in these directories: ['C:\\Users\\katsunori\\CascadeProjects\\bisaya-pronunciation-server']
INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
INFO:     Started reloader process [12345] using StatReload
INFO:     Started server process [12346]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
```

**このウィンドウは開いたままにしてください！**

---

## ✅ ステップ7: サーバー動作確認

### 7-1. 別のPowerShellウィンドウを開く

### 7-2. ヘルスチェック

```powershell
curl http://localhost:8000/
```

**期待されるレスポンス:**
```json
{"status":"ok","message":"Bisaya Speak AI is running","version":"1.0.0"}
```

### 7-3. ブラウザで確認

以下のURLをブラウザで開く:
```
http://localhost:8000/
http://localhost:8000/docs
```

**Swagger UIが表示されればOK！**

---

## 🔥 ステップ8: ファイアウォール設定（重要）

スマホからPCのサーバーにアクセスするため、ファイアウォールで8000番ポートを許可する必要があります。

### 8-1. Windowsファイアウォール設定

1. **Windowsセキュリティを開く**
   ```
   スタートメニュー → 設定 → プライバシーとセキュリティ → Windowsセキュリティ
   ```

2. **ファイアウォールとネットワーク保護**
   ```
   ファイアウォールとネットワーク保護 → 詳細設定
   ```

3. **受信の規則を追加**
   ```
   左側: 受信の規則
   右側: 新しい規則
   
   規則の種類: ポート
   プロトコル: TCP
   ポート: 8000
   操作: 接続を許可する
   プロファイル: すべてチェック
   名前: Bisaya Speak AI Server
   ```

### 8-2. 簡易コマンド（管理者権限が必要）

**PowerShellを管理者として実行してから:**

```powershell
New-NetFirewallRule -DisplayName "Bisaya Speak AI Server" -Direction Inbound -LocalPort 8000 -Protocol TCP -Action Allow
```

---

## 📱 ステップ9: スマホからテスト

### 9-1. スマホとPCが同じWi-Fiに接続されているか確認

### 9-2. スマホのブラウザで確認

```
http://192.168.0.5:8000/
```

**JSONレスポンスが表示されればOK！**

### 9-3. アプリで発音診断テスト

1. アプリを起動
2. 初級 → Maayong buntag
3. 録音ボタンをタップ
4. 2-3秒話す
5. 停止ボタンをタップ
6. 「発音を診断」ボタンをタップ
7. **スコアが表示されればOK！** 🎉

---

## 🐛 トラブルシューティング

### エラー: "ModuleNotFoundError"

**原因:** パッケージがインストールされていない

**解決策:**
```powershell
pip install -r requirements.txt
```

### エラー: "Address already in use"

**原因:** 8000番ポートが既に使用されている

**解決策:**
```powershell
# 既存のプロセスを確認
netstat -ano | findstr :8000

# プロセスを終了（PIDを確認してから）
taskkill /PID <PID> /F
```

### エラー: "Permission denied"

**原因:** ファイアウォールがブロックしている

**解決策:**
- ステップ8のファイアウォール設定を確認
- または一時的にファイアウォールを無効化してテスト

### スマホから接続できない

**確認事項:**
1. PCとスマホが同じWi-Fiに接続されているか
2. PCのIPアドレスが正しいか（`ipconfig`で確認）
3. ファイアウォールで8000番ポートが許可されているか
4. サーバーが起動しているか

**テスト方法:**
```powershell
# PCから確認
curl http://localhost:8000/

# スマホのブラウザから確認
http://192.168.0.5:8000/
```

### アプリで「サーバーに接続できません」エラー

**確認事項:**
1. サーバーが起動しているか
2. `RetrofitClient.kt`のURLが正しいか
   ```kotlin
   private const val BASE_URL = "http://192.168.0.5:8000/"
   ```
3. スマホとPCが同じWi-Fiか
4. ファイアウォール設定

---

## 📊 期待される動作

### 正常な発音診断フロー

1. **録音**
   - ユーザーがビサヤ語を話す
   - 音声ファイルが作成される

2. **診断ボタンをタップ**
   - インタースティシャル広告が表示される
   - バックグラウンドでサーバーにリクエスト

3. **サーバー処理**
   - 音声ファイルを受信
   - MFCC特徴量を抽出
   - DTWで類似度を計算
   - スコアとフィードバックを生成

4. **結果表示**
   - スコアゲージがアニメーション
   - 評価（Excellent/Good/Fair）
   - 詳細フィードバック
   - 改善のヒント

### スコア例

```
初級ユーザー、80点:
- スコア: 80
- 評価: Excellent ✨
- フィードバック: "Great job! Your pronunciation is very close to native."

中級ユーザー、80点:
- スコア: 80
- 評価: Good 👍
- フィードバック: "Good pronunciation! Keep practicing to improve further."

上級ユーザー、80点:
- スコア: 80
- 評価: Good 📊
- フィードバック: "Good pronunciation! Work on perfecting the nuances."
```

---

## ✅ チェックリスト

### Python環境
- [ ] Pythonインストール完了
- [ ] `python --version` で確認
- [ ] PowerShell再起動済み

### サーバーセットアップ
- [ ] 仮想環境作成
- [ ] 仮想環境有効化（`(venv)`表示）
- [ ] 依存パッケージインストール
- [ ] `pip list` で確認

### 参照音声
- [ ] `generate_reference_audio.py` 実行
- [ ] 10個のWAVファイル生成確認

### サーバー起動
- [ ] `python main.py` 実行
- [ ] "Uvicorn running on http://0.0.0.0:8000" 表示
- [ ] `curl http://localhost:8000/` で確認
- [ ] ブラウザで `/docs` 確認

### ファイアウォール
- [ ] 8000番ポート許可設定
- [ ] スマホのブラウザから確認

### アプリテスト
- [ ] アプリで録音
- [ ] 発音診断実行
- [ ] スコア表示確認
- [ ] フィードバック表示確認

---

## 🎯 次のステップ

サーバーが正常に動作したら:

1. **いろいろなフレーズでテスト**
   - 初級・中級・上級のフレーズ
   - レベル別の評価の違いを確認

2. **パフォーマンス確認**
   - 診断にかかる時間（目標: 5秒以内）
   - サーバーのログを確認

3. **次の機能追加**
   - サブスクリプション機能
   - AI会話機能

---

**このガイドに従ってセットアップを進めてください！** 🚀

何か問題が発生したら、エラーメッセージを教えてください。
