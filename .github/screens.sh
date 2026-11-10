#!/usr/bin/env bash
# Runs on the CI emulator: installs the app, taps through it, and saves
# screenshots plus a screen recording into ./shots
set -x
OUT=shots
mkdir -p "$OUT"

shot()  { adb exec-out screencap -p > "$OUT/$1.png"; }
tap()   { adb shell input tap "$1" "$2"; }
burst() { for i in 1 2 3 4; do shot "$1_$i"; done; }
dump()  { adb shell uiautomator dump /sdcard/ui.xml; adb pull /sdcard/ui.xml "$OUT/$1.xml"; }
where() { python3 .github/find_ui.py "$OUT/$1.xml" text "$2"; }

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size
adb shell wm density

# Tidy status bar: fixed clock, Wi-Fi only, no SIM
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 85 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

# The CI emulator is slow right after boot: hide "isn't responding" pop-ups and let it settle
adb shell settings put global hide_error_dialogs 1
sleep 45

adb shell am start -W -n com.wellcontrol.app/.MainActivity
sleep 12
for i in 1 2 3 4; do
  dump home
  WAIT=$(where home Wait)
  [ -z "$WAIT" ] && break
  tap $WAIT
  sleep 8
done
shot 01_home
mapfile -t SW < <(python3 .github/find_ui.py "$OUT/home.xml" switches)
echo "switches: ${SW[*]}"
SETUP=$(where home Setup)
STOP=$(where home "Stop all motors")
echo "setup: $SETUP  stop: $STOP"

adb shell screenrecord --size 720x1600 --bit-rate 6000000 --time-limit 120 /sdcard/demo.mp4 &
REC=$!
sleep 2

if [ -n "${SW[0]}" ]; then
  tap ${SW[0]} & T=$!
  burst 02_well1_turning_on
  wait $T
  sleep 5
  shot 03_well1_running
fi

if [ -n "${SW[2]}" ]; then
  tap ${SW[2]}
  sleep 6
  shot 04_two_running
fi

adb shell input swipe 540 1700 540 700 400
sleep 2
shot 05_scrolled
adb shell input swipe 540 600 540 2100 300
sleep 2

if [ -n "${SW[0]}" ]; then
  tap ${SW[0]} & T=$!
  burst 06_well1_turning_off
  wait $T
  sleep 5
  shot 07_well1_off
fi

if [ -n "$STOP" ]; then
  tap $STOP & T=$!
  burst 08_stop_all
  wait $T
  sleep 6
fi

if [ -n "$SETUP" ]; then
  tap $SETUP
  sleep 3
  shot 09_setup
  dump setup
  ROW=$(where setup "Well 1")
  if [ -n "$ROW" ]; then
    tap $ROW
    sleep 3
    shot 10_edit_device
  fi
fi

adb shell pkill -2 screenrecord
wait $REC
sleep 3
adb pull /sdcard/demo.mp4 "$OUT/demo.mp4"
ls -la "$OUT"
exit 0
