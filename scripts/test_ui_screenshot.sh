#!/usr/bin/env bash
# Global Search UI Automation Screenshot Script
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "=== Running Global Search UI Test & Screenshot Script ==="
python "${PROJECT_ROOT}/scripts/test_ui_screenshot.py" "$@"
