window.speechInterop = {
    recognition: null,
    dotNetRef: null,

    init: function (dotNetReference) {
        this.dotNetRef = dotNetReference;
    },

    isSupported: function() {
        // Disable on Apple devices due to poor support/bugs
        var isApple = /Mac|iPod|iPhone|iPad/.test(navigator.platform) ||
                      /Mac|iPod|iPhone|iPad/.test(navigator.userAgent) ||
                      (navigator.userAgent.includes("Mac") && navigator.maxTouchPoints > 0);

        if (isApple) {
            console.warn("Speech recognition disabled on Apple devices.");
            return false;
        }

        return !!(window.SpeechRecognition ||
                  window.webkitSpeechRecognition ||
                  window.mozSpeechRecognition ||
                  window.msSpeechRecognition);
    },

    start: function (lang) {
        if (!this.isSupported()) {
            console.warn("Speech recognition is disabled or not supported.");
            return false;
        }

        const SpeechRecognition = window.SpeechRecognition ||
                                  window.webkitSpeechRecognition ||
                                  window.mozSpeechRecognition ||
                                  window.msSpeechRecognition;

        if (!SpeechRecognition) {
            var msg = "Web Speech API is not supported in this browser.";
            console.error(msg);
            alert(msg); // Alert is useful if DotNetRef isn't ready or error handling fails
            if (this.dotNetRef) {
                this.dotNetRef.invokeMethodAsync('OnSpeechError', msg);
            }
            return false;
        }

        // If already running, stop it first to be safe
        if (this.recognition) {
            try {
                this.recognition.stop();
            } catch(e) { /* ignore */ }
            this.recognition = null;
        }

        try {
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
