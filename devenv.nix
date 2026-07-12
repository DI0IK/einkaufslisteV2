{ pkgs, ... }:

{
  android = {
    enable = true;
    platforms.version = [ "35" "36" ];
    buildTools.version = [ "35.0.0" "36.0.0" ];
    systemImageTypes = [ "google_apis_playstore" ];
    abis = [ "x86_64" ];
    emulator.enable = true;
  };

  languages = {
    java = {
      enable = true;
      jdk.package = pkgs.openjdk17;
      gradle.enable = true;
    };
    go.enable = true;
    javascript.enable = true;
    javascript.npm.enable = true;
  };

  packages = [
    pkgs.kotlin
    pkgs.golangci-lint
    pkgs.sqlc
    pkgs.goose
    pkgs.buf
    pkgs.antigravity-fhs
  ];

  enterShell = ''
    echo "☕ Einkaufsliste Development Environment Ready"
    echo "Java Version:   $(java -version 2>&1 | head -n 1)"
    echo "Go Version:     $(go version)"
    echo "ANDROID_HOME:   $ANDROID_HOME"
  '';
}
