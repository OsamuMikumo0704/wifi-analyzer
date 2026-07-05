# Room WiFi Logger

自宅の部屋ごとに Wi-Fi の電波強度(RSSI)を手動計測し、複数日にわたって記録・蓄積できる個人用 Android アプリです。蓄積したデータは 1 つの CSV ファイルとして端末の Downloads フォルダへ出力し、PC の Excel などで部屋別・日別に分析できます。

> 個人利用(開発者自身の端末 1 台)を前提としたアプリです。Play ストアでの公開は想定していません。

## 主な機能

- **部屋管理**: 計測対象の部屋をフル CRUD(追加・名前変更・削除)で管理。削除時は計測データも連鎖削除されるため確認ダイアログを表示します。
- **自動計測セッション**: 部屋を選んで計測開始ボタンを押すと、接続中 Wi-Fi アクセスポイントの RSSI を 30 秒間・約 3 秒間隔で自動サンプリングし、平均/最小/最大 RSSI とサンプル数を集計します。計測中は進捗表示と画面消灯抑止を行い、いつでもキャンセル可能です(キャンセル時は保存しません)。
- **前提条件チェック**: 位置情報の権限、位置情報サービスの有効化、Wi-Fi 接続状態を確認し、不足があれば計測を開始せず理由を表示します。
- **計測記録**: 計測日時・部屋名・SSID・BSSID・周波数帯(2.4/5GHz)・平均/最小/最大 RSSI・サンプル数・リンク速度・メモを 1 レコードとして保存します。
- **履歴閲覧**: 保存済みの計測結果を新しい順に一覧表示し、部屋で絞り込めます。
- **CSV エクスポート**: 全計測データを BOM 付き UTF-8 の CSV ファイル(`wifi_log_yyyy-MM-dd.csv`)として `Downloads` フォルダへ出力します。日本語の部屋名も文字化けせずに Excel で開けます。

## スクリーンショット

準備中です。

## 動作要件

- Android 13 (API 33) 以上
- 位置情報サービス(GPS)が有効であること(接続中 SSID/BSSID の取得に必要)
- Wi-Fi 接続済みであること(計測開始時)

## インストール方法

1. [Releases](../../releases) から最新の APK(例: `RoomWifiLogger-<version>.apk`)をダウンロードします。
2. Android 端末で「提供元不明のアプリ」のインストールを許可します(設定 > アプリ > 特別なアプリアクセス > 不明なアプリのインストール)。
3. ダウンロードした APK を開いてインストールします。
4. 初回起動時に位置情報の利用権限を許可してください(接続中 Wi-Fi の SSID/BSSID 取得に必要です)。

## ソースからビルドする

```powershell
# デバッグ APK をビルド
.\gradlew.bat :app:assembleDebug

# 生成物: app/build/outputs/apk/debug/app-debug.apk
```

前提として Android SDK(compileSdk 36, minSdk 33)と JDK 17 相当の環境が必要です。`local.properties` に SDK パスを設定してください。

```
sdk.dir=<Android SDK のパス>
```

## 技術構成

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) によるローカル永続化
- `ConnectivityManager.registerNetworkCallback()` による接続中 Wi-Fi 情報の取得(`FLAG_INCLUDE_LOCATION_INFO`)
- Navigation Compose によるシングルアクティビティ構成
- MediaStore 経由の CSV 書き出し(ランタイムストレージ権限不要)

## 開発の経緯

本アプリは [Kiro-style Spec-Driven Development](.kiro/) のワークフローで、要件定義 → 設計 → タスク分解 → 実装のプロセスを経て作成されました。詳細な仕様は [`.kiro/specs/room-wifi-logger`](.kiro/specs/room-wifi-logger) を参照してください。

## 公開手順(メモ)

このリポジトリの `git push` 後、GitHub の Releases から APK を配布します。

1. GitHub 上に空のリポジトリを作成し、リモートを設定して `git push` する。
2. GitHub の「Releases」→「Draft a new release」でタグ(例: `v1.0`)を作成する。
3. ビルド済みの APK(`.\gradlew.bat :app:assembleDebug` で生成される `app/build/outputs/apk/debug/app-debug.apk`)をアセットとして添付する。
4. 上記「インストール方法」の Releases リンクから APK をダウンロードできるようになる。

APK 自体は `.gitignore` で除外しているため、リポジトリ本体には含めず Releases のアセットとして配布する運用とする。

## ライセンス

個人利用目的のプロジェクトです。ライセンスは未設定です。
