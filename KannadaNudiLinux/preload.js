const { contextBridge, ipcRenderer } = require('electron');

// Exposed to the Blazor editor (see KannadaNudiWeb/wwwroot/js/globalKeyboard.js).
contextBridge.exposeInMainWorld('nudiDesktop', {
  globalKeyboard: {
    getState: () => ipcRenderer.invoke('global-keyboard:get-state'),
    toggle: () => ipcRenderer.send('global-keyboard:toggle'),
    setLayout: (layout) => ipcRenderer.send('global-keyboard:set-layout', layout),
    onStateChanged: (callback) => {
      ipcRenderer.on('global-keyboard:state', (_event, state) => callback(state));
    },
  },
});
