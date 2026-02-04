window.speechInterop = {
    recognition: null,
    dotNetRef: null,

    init: function (dotNetReference, triggerId, langSelectId) {
        this.dotNetRef = dotNetReference;
        var trigger = document.getElementById(triggerId);
        var langSelect = document.getElementById(langSelectId);

        if (trigger && !trigger.hasAttribute('data-speech-initialized')) {
            trigger.setAttribute('data-speech-initialized', 'true');
            trigger.addEventListener('click', () => {
                if (this.recognition) {
                    this.stop();
                } else {
                    var lang = langSelect ? langSelect.value : 'kn-IN';
                    this.startInternal(lang);
                }
            });
        }
    },

    start: function (dotNetReference, lang) {
        // Fallback for direct invocation if needed, but primary use is via init/click
        this.dotNetRef = dotNetReference;
        this.startInternal(lang);
    },

    startInternal: function(lang) {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) {
            console.error("Web Speech API not supported.");
            if (this.dotNetRef) {
                this.dotNetRef.invokeMethodAsync('OnSpeechError', "Web Speech API not supported.");
            }
            return false;
        }

        this.recognition = new SpeechRecognition();
        this.recognition.continuous = true;
        this.recognition.interimResults = false;
        this.recognition.lang = lang || 'kn-IN';

        this.recognition.onresult = (event) => {
            let finalTranscript = '';
            for (let i = event.resultIndex; i < event.results.length; ++i) {
                if (event.results[i].isFinal) {
                    finalTranscript += event.results[i][0].transcript;
                }
            }
            if (finalTranscript.length > 0 && this.dotNetRef) {
                this.dotNetRef.invokeMethodAsync('OnSpeechResult', finalTranscript);
            }
        };

        this.recognition.onerror = (event) => {
            console.error("Speech recognition error", event.error);
            if (this.dotNetRef) {
                this.dotNetRef.invokeMethodAsync('OnSpeechError', event.error);
            }
        };

        this.recognition.onstart = () => {
            if (this.dotNetRef) {
                this.dotNetRef.invokeMethodAsync('OnSpeechStarted');
            }
        };

        this.recognition.onend = () => {
            if (this.dotNetRef) {
                this.dotNetRef.invokeMethodAsync('OnSpeechEnded');
            }
            this.recognition = null;
        };

        try {
            this.recognition.start();
            return true;
        } catch (e) {
            console.error("Error starting recognition:", e);
             if (this.dotNetRef) {
                this.dotNetRef.invokeMethodAsync('OnSpeechError', e.message);
            }
            return false;
        }
    },

    stop: function () {
        if (this.recognition) {
            this.recognition.stop();
            // onend will clear the reference
        }
    }
};
