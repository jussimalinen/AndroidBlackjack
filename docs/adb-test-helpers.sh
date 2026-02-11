#!/bin/bash
# ADB test helper functions for Blackjack app

dump_ui() {
    adb shell uiautomator dump /sdcard/ui.xml 2>/dev/null
    adb shell cat /sdcard/ui.xml 2>/dev/null
}

# Find a node by text and return its bounds
find_bounds() {
    local text="$1"
    local xml
    xml=$(dump_ui)
    echo "$xml" | grep -oP "text=\"${text}\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" | grep -oP 'bounds="\[\d+,\d+\]\[\d+,\d+\]"' | head -1
}

# Tap element by its exact text
tap_text() {
    local text="$1"
    local xml
    xml=$(dump_ui)
    local bounds
    bounds=$(echo "$xml" | grep -oP "text=\"${text}\"[^>]*bounds=\"\[\d+,\d+\]\[\d+,\d+\]\"" | grep -oP '\[\d+,\d+\]\[\d+,\d+\]' | head -1)
    if [ -z "$bounds" ]; then
        echo "NOTFOUND: '$text'"
        return 1
    fi
    local l t r b
    l=$(echo "$bounds" | grep -oP '\d+' | sed -n 1p)
    t=$(echo "$bounds" | grep -oP '\d+' | sed -n 2p)
    r=$(echo "$bounds" | grep -oP '\d+' | sed -n 3p)
    b=$(echo "$bounds" | grep -oP '\d+' | sed -n 4p)
    local cx=$(( (l + r) / 2 ))
    local cy=$(( (t + b) / 2 ))
    echo "TAP: '$text' at ($cx, $cy)"
    adb shell input tap "$cx" "$cy"
    sleep 0.5
}

# Check if text exists in current UI
has_text() {
    local text="$1"
    local xml
    xml=$(dump_ui)
    echo "$xml" | grep -q "text=\"${text}\""
}

# Get all visible text nodes
visible_texts() {
    local xml
    xml=$(dump_ui)
    echo "$xml" | grep -oP 'text="[^"]*"' | sed 's/text="//;s/"//' | sort -u
}
