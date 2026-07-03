# 検証手順

このプロジェクトの Gradle 検証は、リポジトリ同梱の Gradle Wrapper を使う。

## 前提

- `local.properties` は Git 管理しない。現在の開発環境では次の SDK パスを設定する。
  - `sdk.dir=C\:\\Users\\20001\\AppData\\Local\\Android\\Sdk`
- `gradle.properties` で Android Studio 同梱 JBR 21 を指定している。
  - `org.gradle.java.home=C\:\\Program Files\\Android\\Android Studio\\jbr`
- Codex の通常サンドボックス内では、Gradle 配布物の取得や Android SDK / Gradle キャッシュの参照が権限で失敗することがある。その場合は `require_escalated` で Gradle コマンドを実行する。

## 標準検証

```powershell
.\gradlew.bat clean :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

期待結果:

- `:app:assembleDebug` が成功する。
- `:app:testDebugUnitTest` は JVM ユニットテストが存在しない場合 `NO-SOURCE` で成功扱いになる。
- `:app:assembleDebugAndroidTest` が成功し、instrumented test APK がビルドできる。

## 実機またはエミュレータが必要な検証

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

実機またはエミュレータが接続されていない場合は、次の理由で失敗する。

```text
DeviceException: No connected devices!
```

これは Gradle 環境の失敗ではなく、デバイス未接続による実行不能状態として扱う。

## ビルドだけ確認したい場合

```powershell
.\gradlew.bat :app:assembleDebug
```

このコマンドは、Android SDK と依存キャッシュが利用可能な環境では成功する。
