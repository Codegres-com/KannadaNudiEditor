window.quillInterop = {
    quill: null,
    dotNetRef: null,
    lastKeyHandledTime: 0,
    lastProcessedKey: null,
    lastProcessedTime: 0,
    lastProcessedSource: null,

    init: function (elementId, dotNetReference) {
        if (!window.Quill) {
            console.error("Quill JS not found");
            return;
        }

        console.log("Initializing Quill on", elementId);
        this.dotNetRef = dotNetReference;

        // Register custom fonts
        var Font = Quill.import('formats/font');
        Font.whitelist = ['nudiparijatha', 'nudi-01-e', 'nudi-01-k', 'nudi-02-e', 'nudi-05-e', 'nudi-10-e'];
        Quill.register(Font, true);

        // Register Quill Better Table
        var modulesConfig = {
            toolbar: [
                [{ 'font': Font.whitelist }, { 'size': [] }],
                ['bold', 'italic', 'underline', 'strike'],
                [{ 'color': [] }, { 'background': [] }],
                [{ 'script': 'sub' }, { 'script': 'super' }],
                [{ 'header': 1 }, { 'header': 2 }, 'blockquote', 'code-block'],
                [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
                [{ 'list': 'ordered' }, { 'list': 'bullet' }, { 'indent': '-1' }, { 'indent': '+1' }],
                [{ 'direction': 'rtl' }, { 'align': [] }],
                ['link', 'image', 'video', 'formula'],
                ['clean']
            ]
        };

        // Check for window.quillBetterTable (camelCase) as that's what the library exports
        // Also check PascalCase as a fallback
        var BetterTable = window.quillBetterTable || window.QuillBetterTable;

        if (BetterTable) {
            // Handle ESM/UMD mismatch where class might be in .default
            if (typeof BetterTable !== 'function' && BetterTable.default) {
                BetterTable = BetterTable.default;
            }

            if (typeof BetterTable === 'function') {
                Quill.register({
                    'modules/better-table': BetterTable
                }, true);

                modulesConfig['better-table'] = {
                    operationMenu: {
                        items: {
                            unmergeCells: {
                                text: 'Unmerge Cells'
                            }
                        }
                    }
                };

                if (BetterTable.keyboardBindings) {
                    modulesConfig.keyboard = {
                        bindings: BetterTable.keyboardBindings
                    };
                }
            } else {
                console.error("quillBetterTable found but not a valid constructor", BetterTable);
            }
        } else {
            console.warn("quillBetterTable not found. Make sure the script is loaded.");
        }

        this.quill = new Quill(elementId, {
            theme: 'snow',
            modules: modulesConfig
        });

        // Disable predictive text features to prevent mobile keyboard interference
        if (this.quill.root) {
            this.quill.root.setAttribute('autocomplete', 'off');
            this.quill.root.setAttribute('autocorrect', 'off');
            this.quill.root.setAttribute('autocapitalize', 'off');
            this.quill.root.setAttribute('spellcheck', 'false');
        }

        this.quill.on('selection-change', function(range, oldRange, source) {
            // Prevent buffer clearing if we just handled a key/input event recently
            if (Date.now() - window.quillInterop.lastKeyHandledTime < 500) {
                console.log("Ignoring selection change due to recent input");
                return;
            }

            if (source === 'user' && dotNetReference) {
                dotNetReference.invokeMethodAsync('OnSelectionChanged');
            }
        });

        // Mobile Input Support via text-change (Fallback)
        this.quill.on('text-change', (delta, oldDelta, source) => {
            if (source === 'user' && window.isKannadaMode) {
                if (!this.dotNetRef) return;

                let index = 0;
                let handled = false;

                // Iterate ops to find the operation and its position
                for (let i = 0; i < delta.ops.length; i++) {
                    const op = delta.ops[i];
                    if (op.retain) {
                        index += op.retain;
                    } else if (op.insert && typeof op.insert === 'string') {
                        // Handle single character insertions (excluding newlines)
                        if (op.insert.length === 1 && op.insert !== '\n') {

                            // Deduplication Check: If handled by beforeinput or keydown recently, skip
                            if ((window.quillInterop.lastProcessedSource === 'beforeinput' || window.quillInterop.lastProcessedSource === 'keydown') &&
                                Date.now() - window.quillInterop.lastProcessedTime < 200 &&
                                window.quillInterop.lastProcessedKey === op.insert) {
                                console.log("Skipping duplicate input from text-change:", op.insert);
                                handled = true;
                                break;
                            }

                            // Update timestamp to prevent selection-change from clearing buffer
                            this.lastKeyHandledTime = Date.now();

                            // Revert the user's insertion immediately by deleting it
                            this.quill.deleteText(index, op.insert.length, 'silent');

                            // Send the character to C# for transliteration
                            this.dotNetRef.invokeMethodAsync('ProcessKannadaKey', op.insert);

                            // Update state
                            window.quillInterop.lastProcessedKey = op.insert;
                            window.quillInterop.lastProcessedTime = Date.now();
                            window.quillInterop.lastProcessedSource = 'text-change';

                            handled = true;
                            break;
                        }
                        index += op.insert.length;
                    } else if (op.delete) {
                         // Check if this deletion was already handled
                         const now = Date.now();
                         if (now - this.lastKeyHandledTime > 200) {
                             // Update timestamp
                             this.lastKeyHandledTime = Date.now();

                             if (op.delete > 1) {
                                 // Bulk deletion: Sync state by clearing buffer
                                 this.dotNetRef.invokeMethodAsync('OnSelectionChanged');
                             } else {
                                 // Single char deletion: Try to process backspace logic
                                 this.dotNetRef.invokeMethodAsync('ProcessBackspace');
                             }
                             handled = true;
                         }
                         break;
                    }
                }
            }
        });
    },

    insertText: function (text) {
        if (this.quill) {
            const range = this.quill.getSelection(true);
            console.log("Inserting text:", text);

            if (range) {
                if (range.length > 0) {
                    this.quill.deleteText(range.index, range.length);
                }
                this.quill.insertText(range.index, text, 'api');
                this.quill.setSelection(range.index + text.length, 0, 'api');
            } else {
                this.quill.focus();
                const len = this.quill.getLength();
                this.quill.insertText(len - 1, text, 'api');
                this.quill.setSelection(len - 1 + text.length, 0, 'api');
            }
        } else {
            console.error("Quill instance null during insertText");
        }
    },

    backspace: function (count) {
        if (this.quill) {
            const range = this.quill.getSelection(true);
            if (range && range.index >= count) {
                console.log("Backspacing count:", count);
                this.quill.deleteText(range.index - count, count, 'api');
            }
        }
    },

    getHtml: function () {
        if (this.quill) {
            return this.quill.root.innerHTML;
        }
        return "";
    },

    getText: function() {
        if (this.quill) {
            return this.quill.getText();
        }
        return "";
    },

    setHtml: function (html) {
        if (this.quill) {
            this.quill.clipboard.dangerouslyPasteHTML(html);
        }
    },

    saveAsDocx: async function(filename, htmlContent) {
        if (!window.docshift) {
            console.error("DocShift library not loaded");
            return;
        }
        try {
            const blob = await window.docshift.toDocx(htmlContent);
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        } catch (error) {
            console.error("Error generating DocX:", error);
        }
    },

    downloadFile: function(filename, content, contentType) {
        contentType = contentType || 'text/html';
        const element = document.createElement('a');
        const file = new Blob([content], {type: contentType});
        element.href = URL.createObjectURL(file);
        element.download = filename;
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    },

    // Input Interception Logic
    registerKeyInterceptor: function (dotNetReference) {
        const editorDiv = document.getElementById('quill-editor');

        if(!editorDiv) {
            console.error("Quill editor div not found during registration");
            return;
        }

        console.log("Registering key interceptors on #quill-editor");

        // Use beforeinput for modern input interception (Mobile/Desktop)
        editorDiv.addEventListener('beforeinput', (e) => {
             if (window.isKannadaMode) {
                // Update time for ANY input activity to prevent buffer clearing
                window.quillInterop.lastKeyHandledTime = Date.now();

                // Handle text insertion
                if (e.inputType === 'insertText' && e.data && e.data.length === 1) {
                    console.log("beforeinput intercepted:", e.data);

                    e.preventDefault();
                    // Stop propagation to prevent Quill from seeing it if possible,
                    // though preventDefault is usually enough for data.
                    // e.stopPropagation();

                    dotNetReference.invokeMethodAsync('ProcessKannadaKey', e.data);

                    // Update state
                    window.quillInterop.lastProcessedKey = e.data;
                    window.quillInterop.lastProcessedTime = Date.now();
                    window.quillInterop.lastProcessedSource = 'beforeinput';
                }

                // Handle Backspace (deleteContentBackward)
                else if (e.inputType === 'deleteContentBackward') {
                    console.log("beforeinput Backspace intercepted");
                    e.preventDefault();

                    dotNetReference.invokeMethodAsync('ProcessBackspace');

                    window.quillInterop.lastProcessedTime = Date.now();
                    window.quillInterop.lastProcessedSource = 'beforeinput';
                }
             }
        }, true); // Capture phase

        // Keep keydown for special keys or older browsers, but de-prioritize text input
        editorDiv.addEventListener('keydown', (e) => {
            if (window.isKannadaMode) {
                window.quillInterop.lastKeyHandledTime = Date.now();

                // Handle Backspace via keydown if beforeinput didn't catch it recently
                if (e.key === 'Backspace') {
                    if (Date.now() - window.quillInterop.lastProcessedTime < 50 && window.quillInterop.lastProcessedSource === 'beforeinput') {
                         console.log("Ignored keydown Backspace (handled by beforeinput)");
                         return;
                    }
                    console.log("Processing Backspace via keydown fallback");
                    dotNetReference.invokeMethodAsync('ProcessBackspace');
                    return;
                }

                // Handle single chars - FALLBACK ONLY
                // If beforeinput is supported, it fires *before* the input.
                // keydown fires before beforeinput.
                // We don't want to handle it here if beforeinput is going to handle it.
                // But we can't easily know if beforeinput *will* fire.
                // However, almost all modern browsers support beforeinput.
                // We'll trust beforeinput for characters and use text-change as the ultimate fallback.
                // So we REMOVE the character handling from keydown to avoid double-firing or conflicts.
                // Exception: Control keys
                if (e.ctrlKey || e.altKey || e.metaKey) {
                    return; // Let default happen for shortcuts
                }
            }
        }, true);
    },

    setKannadaMode: function (val) {
        console.log("Setting Kannada Mode:", val);
        window.isKannadaMode = val;
    },

    undo: function() {
        if (this.quill) this.quill.history.undo();
    },

    redo: function() {
        if (this.quill) this.quill.history.redo();
    },

    copyText: async function() {
        if (!this.quill) return;
        this.quill.focus();
        try {
            // Try execCommand first to preserve rich text formatting
            const successful = document.execCommand('copy');
            if (successful) {
                console.log('Rich text copied via execCommand');
                return;
            }
        } catch (err) {
            console.warn('execCommand copy failed', err);
        }

        // Fallback to plain text via Clipboard API
        const range = this.quill.getSelection();
        if (range && range.length > 0) {
            const text = this.quill.getText(range.index, range.length);
            try {
                await navigator.clipboard.writeText(text);
                console.log('Text copied to clipboard (Plain)');
            } catch (err) {
                console.error('Failed to copy: ', err);
            }
        }
    },

    pasteText: async function() {
        if (!this.quill) return;
        this.quill.focus();

        // Try Async Clipboard API for HTML (Rich Text)
        try {
            const items = await navigator.clipboard.read();
            for (const item of items) {
                if (item.types.includes('text/html')) {
                    const blob = await item.getType('text/html');
                    const html = await blob.text();

                    const range = this.quill.getSelection(true);
                    if (range) {
                        this.quill.deleteText(range.index, range.length);
                        this.quill.clipboard.dangerouslyPasteHTML(range.index, html);
                    } else {
                        const len = this.quill.getLength();
                        this.quill.clipboard.dangerouslyPasteHTML(len - 1, html);
                    }
                    return; // Success
                }
            }
        } catch (err) {
            console.warn('Clipboard.read() failed or permission denied, falling back to text', err);
        }

        // Fallback to plain text
        try {
            const text = await navigator.clipboard.readText();
            const range = this.quill.getSelection(true);
            if (range) {
                this.quill.deleteText(range.index, range.length);
                this.quill.insertText(range.index, text);
            } else {
                const len = this.quill.getLength();
                this.quill.insertText(len - 1, text);
            }
        } catch (err) {
            console.error('Failed to paste: ', err);
        }
    },

    insertTable: function(rows, cols) {
        if (!this.quill) return;
        const module = this.quill.getModule('better-table');
        if (module) {
            module.insertTable(rows, cols);
        } else {
             console.error("Better Table module not found or not initialized");
        }
    },

    toggleHighlight: function() {
        if (!this.quill) return;
        const range = this.quill.getSelection(true);
        if (range) {
            const format = this.quill.getFormat(range);
            if (format.background === '#ffff00') {
                this.quill.format('background', false);
            } else {
                this.quill.format('background', '#ffff00');
            }
        }
    },

    docToHtml: async function(fileBytes) {
        if (!window.mammoth) {
            console.error("Mammoth library not loaded");
            return "";
        }
        try {
            // fileBytes comes as a Uint8Array from Blazor
            const result = await window.mammoth.convertToHtml({ arrayBuffer: fileBytes.buffer });
            return result.value;
        } catch (error) {
            console.error("Error converting DocX:", error);
            throw error;
        }
    }
};