#!/usr/bin/env bash
# ==========================================================
# Script khoi chay nhanh DHOPM Stream Visualizer
# Truong Dai Hoc Da Lat - Khoa CNTT
# Sinh vien: Nguyen Thanh Tam - MSSV: 2312741
# ==========================================================

cd "$(dirname "$0")"

# Tu dong tim Maven neu chua co trong PATH
if ! command -v mvn &> /dev/null; then
    if [ -f "/Users/nthtam/.maven/apache-maven-3.9.6/bin/mvn" ]; then
        export PATH="/Users/nthtam/.maven/apache-maven-3.9.6/bin:$PATH"
    fi
fi

# Thiet lap JAVA_HOME cho JDK 25 tuong thich bytecode 69.0
export JAVA_HOME="/Users/nthtam/.jdk/jdk-25.0.2/jdk-25.0.2+10/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"


case "$1" in
    test)
        echo "🧪 Dang chay 18 Unit Tests xac thuc Lab 1..."
        mvn test
        ;;
    verify|runner)
        echo "📊 Dang chay Lab 1 Verification Runner (in bang doi chieu)..."
        mvn compile exec:java -Dexec.mainClass="vn.edu.dlu.dhopm.core.Lab1VerificationRunner"
        ;;
    *)
        echo "⚡ Dang khoi chay giao dien DHOPM Stream Visualizer (JavaFX)..."
        mvn javafx:run
        ;;
esac
