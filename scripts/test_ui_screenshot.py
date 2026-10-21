#!/usr/bin/env python3
"""
Automation test script for Minecraft Global Search Mod:
1. Detects running Minecraft window (or launches client via ./gradlew :fabric:runClient if requested).
2. Brings Minecraft to foreground.
3. Opens Global Search UI (pressing 'K' or executing '/globalsearch').
4. Cycles through the 4 modern themes (Netherite RPG, Obsidian Glass, Raycast Clean, Ender Twilight).
5. Captures high-res screenshots of each theme and saves them into the screenshots/ directory.
6. Also triggers in-game F2 screenshot.
"""

import os
import sys
import time
import argparse
import subprocess
import ctypes
from ctypes import wintypes
from datetime import datetime

try:
    from PIL import ImageGrab
except ImportError:
    print("[ERROR] Pillow is required for direct screenshot capture. Run: pip install pillow")
    sys.exit(1)

# Windows API definitions
user32 = ctypes.windll.user32
kernel32 = ctypes.windll.kernel32

SW_RESTORE = 9
KEYEVENTF_KEYUP = 0x0002

# Virtual key codes
VK_RETURN = 0x0D
VK_ESCAPE = 0x1B
VK_K = 0x4B
VK_T = 0x54
VK_F2 = 0x71
VK_F6 = 0x75
VK_SLASH = 0xBF


class RECT(ctypes.Structure):
    _fields_ = [
        ('left', wintypes.LONG),
        ('top', wintypes.LONG),
        ('right', wintypes.LONG),
        ('bottom', wintypes.LONG),
    ]


def send_key(vk_code):
    user32.keybd_event(vk_code, 0, 0, 0)
    time.sleep(0.04)
    user32.keybd_event(vk_code, 0, KEYEVENTF_KEYUP, 0)
    time.sleep(0.06)


def type_string(s):
    for char in s:
        if char == '/':
            send_key(VK_SLASH)
        elif char.isalnum():
            vk = ord(char.upper())
            send_key(vk)
        elif char == ' ':
            send_key(0x20)
        time.sleep(0.03)


def find_minecraft_window():
    found_windows = []

    def enum_cb(hwnd, lparam):
        if user32.IsWindowVisible(hwnd):
            length = user32.GetWindowTextLengthW(hwnd)
            if length > 0:
                buff = ctypes.create_unicode_buffer(length + 1)
                user32.GetWindowTextW(hwnd, buff, length + 1)
                title = buff.value
                # Match Minecraft window title or GLFW class
                if 'minecraft' in title.lower():
                    found_windows.append((hwnd, title))
        return True

    CB = ctypes.WINFUNCTYPE(ctypes.c_bool, wintypes.HWND, wintypes.LPARAM)
    user32.EnumWindows(CB(enum_cb), 0)
    return found_windows


def focus_window(hwnd):
    # Bring window to foreground even if background process
    current_thread = kernel32.GetCurrentThreadId()
    target_thread = user32.GetWindowThreadProcessId(hwnd, None)
    user32.AttachThreadInput(current_thread, target_thread, True)

    user32.ShowWindow(hwnd, SW_RESTORE)
    user32.SetForegroundWindow(hwnd)
    user32.BringWindowToTop(hwnd)

    user32.AttachThreadInput(current_thread, target_thread, False)
    time.sleep(0.5)


def capture_window_screenshot(hwnd, output_path):
    rect = RECT()
    user32.GetWindowRect(hwnd, ctypes.byref(rect))

    width = rect.right - rect.left
    height = rect.bottom - rect.top
    if width <= 0 or height <= 0:
        print(f"[WARN] Invalid window bounds: {width}x{height}")
        return False

    bbox = (rect.left, rect.top, rect.right, rect.bottom)
    screenshot = ImageGrab.grab(bbox=bbox, all_screens=True)
    screenshot.save(output_path, "PNG")
    print(f"[SUCCESS] Saved screenshot: {output_path} ({width}x{height})")
    return True


def start_client_and_wait(timeout_seconds=120):
    print("[INFO] Launching Minecraft client via './gradlew :fabric:runClient'...")
    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    cmd = ["./gradlew", ":fabric:runClient"]
    if sys.platform == "win32":
        # Launch using bash or shell
        process = subprocess.Popen(
            cmd,
            cwd=project_root,
            shell=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL
        )
    else:
        process = subprocess.Popen(cmd, cwd=project_root)

    print(f"[INFO] Client process launched (PID {process.pid}). Waiting for Minecraft window...")
    start_time = time.time()
    while time.time() - start_time < timeout_seconds:
        windows = find_minecraft_window()
        if windows:
            print(f"[INFO] Found Minecraft window: '{windows[0][1]}' (HWND: {windows[0][0]})")
            return windows[0][0]
        time.sleep(2.0)

    print("[ERROR] Timeout waiting for Minecraft window.")
    return None


def main():
    parser = argparse.ArgumentParser(description="Test script to open Global Search Screen and capture screenshots")
    parser.add_argument("--start", action="store_true", help="Start client if not already running")
    parser.add_argument("--all-themes", action="store_true", default=True, help="Cycle through all 4 themes and capture each")
    parser.add_argument("--command", action="store_true", help="Open UI via '/globalsearch' chat command instead of key 'K'")
    args = parser.parse_args()

    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    screenshots_dir = os.path.join(project_root, "screenshots")
    os.makedirs(screenshots_dir, exist_ok=True)

    # 1. Locate or start Minecraft
    windows = find_minecraft_window()
    hwnd = None
    if windows:
        hwnd, title = windows[0]
        print(f"[INFO] Found running Minecraft window: '{title}' (HWND {hwnd})")
    elif args.start:
        hwnd = start_client_and_wait()
    else:
        print("[WARN] No Minecraft window detected. If client is still starting, please wait or use '--start'.")
        sys.exit(1)

    if not hwnd:
        sys.exit(1)

    # 2. Focus window
    print("[INFO] Bringing Minecraft window to foreground...")
    focus_window(hwnd)
    time.sleep(0.6)

    # 3. Open Global Search screen
    if args.command:
        print("[INFO] Opening search screen via chat command '/globalsearch'...")
        send_key(VK_SLASH)
        time.sleep(0.1)
        type_string("globalsearch")
        time.sleep(0.1)
        send_key(VK_RETURN)
    else:
        print("[INFO] Opening search screen via keybind 'K'...")
        send_key(VK_K)

    time.sleep(1.2)  # Wait for UI layout & animation to render

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    # 4. Themes to capture
    themes = ["netherite_rpg", "obsidian_glass", "raycast_clean", "ender_twilight"]
    captured_files = []

    if args.all_themes:
        for idx, theme_name in enumerate(themes):
            print(f"[INFO] Capturing theme [{idx+1}/4]: {theme_name.replace('_', ' ').title()}...")
            filename = f"global_search_{theme_name}_{timestamp}.png"
            filepath = os.path.join(screenshots_dir, filename)

            # Trigger in-game F2 as well
            send_key(VK_F2)
            time.sleep(0.1)

            # Direct window screenshot
            if capture_window_screenshot(hwnd, filepath):
                captured_files.append(filepath)

            # Switch to next theme with F6
            if idx < len(themes) - 1:
                print("[INFO] Pressing F6 to switch to next theme...")
                send_key(VK_F6)
                time.sleep(0.8)
    else:
        filename = f"global_search_ui_{timestamp}.png"
        filepath = os.path.join(screenshots_dir, filename)
        send_key(VK_F2)
        if capture_window_screenshot(hwnd, filepath):
            captured_files.append(filepath)

    print("\n" + "=" * 60)
    print("[SUMMARY] Screenshot automation complete!")
    print(f"[SUMMARY] Captured {len(captured_files)} screenshot(s) in: {screenshots_dir}")
    for f in captured_files:
        print(f"  -> {f}")
    print("=" * 60)


if __name__ == "__main__":
    main()
