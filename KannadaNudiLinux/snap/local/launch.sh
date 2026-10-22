#!/bin/sh
# Electron needs no SUID sandbox under strict confinement, and X11 (XWayland)
# because DISABLE_WAYLAND hides the Wayland socket from the app.
exec "$SNAP/kannadanudilinux" --no-sandbox --ozone-platform=x11 "$@"
