// F9 hotkey + bridge to the Windows desktop app's Global (Direct Type) keyboard.
//
// In the Windows app (KannadaNudiWindows) a native helper hooks the keyboard system-wide and
// handles F9 itself, so Kannada can be typed in any application. `window.nudiDesktop` is
// exposed by that app's preload script. In a normal browser only this page can be controlled,
// so F9 toggles the editor's KAN / ENG input mode instead.
window.nudiGlobalKeyboard = {
    _bridge: function () {
        return window.nudiDesktop && window.nudiDesktop.globalKeyboard;
    },

    // Returns the desktop Global mode state ({ available, enabled, layout }), or null in a browser.
    init: async function (dotNetRef) {
        document.addEventListener('keydown', (e) => {
            if (e.key !== 'F9' || e.ctrlKey || e.altKey || e.metaKey || e.repeat) return;
            e.preventDefault();
            e.stopPropagation();
            dotNetRef.invokeMethodAsync('OnF9Pressed');
        }, true);

        const bridge = this._bridge();
        if (!bridge) return null;

        bridge.onStateChanged((state) => dotNetRef.invokeMethodAsync('OnGlobalKeyboardState', state));
        return await bridge.getState();
    },

    toggle: function () {
        const bridge = this._bridge();
        if (bridge) bridge.toggle();
    },

    setLayout: function (layout) {
        const bridge = this._bridge();
        if (bridge) bridge.setLayout(layout);
    }
};
