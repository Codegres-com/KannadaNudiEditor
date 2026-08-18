#!/usr/bin/env bash
set -e

# Start dbus session if not active
if [ -z "$DBUS_SESSION_BUS_ADDRESS" ]; then
    eval $(dbus-launch --sh-syntax)
fi

# Unlock gnome-keyring
export $(echo -n "hegde" | gnome-keyring-daemon --unlock --components=secrets 2>/dev/null) || true

echo "DBUS & Keyring initialized."
/snap/bin/snapcraft "$@"
