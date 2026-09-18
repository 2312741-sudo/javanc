#!/usr/bin/env bash
# ==========================================================
# Script khoi chay DHOPM Stream Visualizer (JavaFX App)
# ==========================================================

export PATH="/Users/nthtam/.maven/apache-maven-3.9.6/bin:$PATH"
cd "$(dirname "$0")"

echo "⚡ Dang khoi chay DHOPM Stream Visualizer..."
mvn javafx:run
