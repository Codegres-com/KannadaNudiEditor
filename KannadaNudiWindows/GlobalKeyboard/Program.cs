using System.Collections.Concurrent;
using System.Runtime.InteropServices;
using KannadaNudiWeb.Services;

namespace KannadaNudiGlobalKeyboard;

// Global (Direct Type) Kannada keyboard.
//
// A low-level keyboard hook turns physical keystrokes into Kannada using the same
// KannadaKeyboardEngine as the editor, and types the result into whatever window has
// focus (browser, Word, Excel, Notepad, ...) via SendInput. F9 toggles it on/off.
//
// Arguments: --host-pid <pid>  windows of this process are skipped (the editor transliterates itself)
//            --enabled         start with Kannada typing on
//            --layout <Nudi|Baraha>
//
// Protocol with the host over stdin/stdout, one line per message:
//   in : toggle | enable | disable | layout <Nudi|Baraha> | exit
//   out: ready | state <on|off> <Nudi|Baraha> | error <message>
// The helper exits when stdin closes, so it never outlives the host.
internal static class Program
{
    private const int WH_KEYBOARD_LL = 13;
    private const int WH_MOUSE_LL = 14;
    private const int WM_KEYDOWN = 0x0100;
    private const int WM_SYSKEYDOWN = 0x0104;
    private const int WM_LBUTTONDOWN = 0x0201;
    private const int WM_RBUTTONDOWN = 0x0204;
    private const int WM_MBUTTONDOWN = 0x0207;
    private const uint WM_APP_COMMAND = 0x8001;
    private const uint PM_NOREMOVE = 0x0000;

    private const uint INPUT_KEYBOARD = 1;
    private const uint KEYEVENTF_KEYUP = 0x0002;
    private const uint KEYEVENTF_UNICODE = 0x0004;

    private const int VK_BACK = 0x08;
    private const int VK_SHIFT = 0x10;
    private const int VK_CONTROL = 0x11;
    private const int VK_MENU = 0x12;
    private const int VK_CAPITAL = 0x14;
    private const int VK_SPACE = 0x20;
    private const int VK_LWIN = 0x5B;
    private const int VK_RWIN = 0x5C;
    private const int VK_F9 = 0x78;

    // Tags our own SendInput events so the hook lets them through untouched.
    private static readonly IntPtr InjectedMarker = new(0x4B4E5544); // "KNUD"

    private static readonly KannadaKeyboardEngine Engine = new();
    private static readonly ConcurrentQueue<string> Commands = new();
    private static readonly LowLevelProc KeyboardProc = KeyboardHookCallback; // keep delegates alive
    private static readonly LowLevelProc MouseProc = MouseHookCallback;
    private static readonly object OutputLock = new();

    private static bool _enabled;
    private static uint _hostPid;
    private static uint _mainThreadId;
    private static IntPtr _lastForeground;
    private static bool _lastForegroundIsHost;

    [STAThread]
    private static int Main(string[] args)
    {
        var layout = KeyboardLayout.Nudi;
        for (int i = 0; i < args.Length; i++)
        {
            switch (args[i])
            {
                case "--host-pid" when i + 1 < args.Length:
                    uint.TryParse(args[++i], out _hostPid);
                    break;
                case "--enabled":
                    _enabled = true;
                    break;
                case "--layout" when i + 1 < args.Length:
                    Enum.TryParse(args[++i], true, out layout);
                    break;
            }
        }
        Engine.SetLayout(layout);

        using var mutex = new Mutex(true, @"Local\KannadaNudiGlobalKeyboard", out bool isOnlyInstance);
        if (!isOnlyInstance)
        {
            Emit("error already-running");
            return 2;
        }

        _mainThreadId = GetCurrentThreadId();
        PeekMessage(out _, IntPtr.Zero, 0, 0, PM_NOREMOVE); // create the message queue before the stdin thread posts to it

        IntPtr module = GetModuleHandle(null);
        IntPtr keyboardHook = SetWindowsHookEx(WH_KEYBOARD_LL, KeyboardProc, module, 0);
        if (keyboardHook == IntPtr.Zero)
        {
            Emit($"error hook-failed {Marshal.GetLastWin32Error()}");
            return 1;
        }
        IntPtr mouseHook = SetWindowsHookEx(WH_MOUSE_LL, MouseProc, module, 0);

        Emit("ready");
        EmitState();

        var stdinThread = new Thread(ReadCommands) { IsBackground = true, Name = "stdin" };
        stdinThread.Start();

        while (GetMessage(out MSG msg, IntPtr.Zero, 0, 0) > 0)
        {
            if (msg.message == WM_APP_COMMAND)
            {
                DrainCommands();
                continue;
            }
            TranslateMessage(ref msg);
            DispatchMessage(ref msg);
        }

        UnhookWindowsHookEx(keyboardHook);
        if (mouseHook != IntPtr.Zero) UnhookWindowsHookEx(mouseHook);
        return 0;
    }

    private static void ReadCommands()
    {
        try
        {
            string? line;
            while ((line = Console.In.ReadLine()) != null)
            {
                Commands.Enqueue(line.Trim());
                PostThreadMessage(_mainThreadId, WM_APP_COMMAND, IntPtr.Zero, IntPtr.Zero);
            }
        }
        catch (IOException)
        {
        }
        Commands.Enqueue("exit");
        PostThreadMessage(_mainThreadId, WM_APP_COMMAND, IntPtr.Zero, IntPtr.Zero);
    }

    // Runs on the hook thread, so engine state is never touched concurrently.
    private static void DrainCommands()
    {
        while (Commands.TryDequeue(out string? command))
        {
            string[] parts = command.Split(' ', StringSplitOptions.RemoveEmptyEntries);
            if (parts.Length == 0) continue;

            switch (parts[0])
            {
                case "toggle":
                    SetEnabled(!_enabled);
                    break;
                case "enable":
                    SetEnabled(true);
                    break;
                case "disable":
                    SetEnabled(false);
                    break;
                case "layout" when parts.Length > 1 && Enum.TryParse(parts[1], true, out KeyboardLayout layout):
                    Engine.SetLayout(layout);
                    EmitState();
                    break;
                case "exit":
                    PostQuitMessage(0);
                    return;
            }
        }
    }

    private static void SetEnabled(bool enabled)
    {
        _enabled = enabled;
        Engine.ClearBuffer();
        EmitState();
    }

    private static void EmitState() =>
        Emit($"state {(_enabled ? "on" : "off")} {Engine.CurrentLayout}");

    private static void Emit(string line)
    {
        lock (OutputLock)
        {
            try
            {
                Console.Out.WriteLine(line);
                Console.Out.Flush();
            }
            catch (IOException)
            {
                // Host went away; the stdin reader will shut us down.
            }
        }
    }

    private static unsafe IntPtr KeyboardHookCallback(int nCode, IntPtr wParam, IntPtr lParam)
    {
        if (nCode >= 0)
        {
            var info = (KBDLLHOOKSTRUCT*)lParam;
            if (info->dwExtraInfo != InjectedMarker && HandleKey((int)wParam, (int)info->vkCode))
            {
                return 1; // swallow the physical key
            }
        }
        return CallNextHookEx(IntPtr.Zero, nCode, wParam, lParam);
    }

    private static IntPtr MouseHookCallback(int nCode, IntPtr wParam, IntPtr lParam)
    {
        // A click usually moves the caret, so the pending syllable no longer applies.
        if (nCode >= 0 && _enabled)
        {
            int msg = (int)wParam;
            if (msg == WM_LBUTTONDOWN || msg == WM_RBUTTONDOWN || msg == WM_MBUTTONDOWN)
            {
                Engine.ClearBuffer();
            }
        }
        return CallNextHookEx(IntPtr.Zero, nCode, wParam, lParam);
    }

    // Returns true when the key was consumed.
    private static bool HandleKey(int message, int vk)
    {
        if (message != WM_KEYDOWN && message != WM_SYSKEYDOWN) return false;

        bool ctrl = IsPressed(VK_CONTROL);
        bool alt = IsPressed(VK_MENU);
        bool win = IsPressed(VK_LWIN) || IsPressed(VK_RWIN);

        if (vk == VK_F9 && !ctrl && !alt && !win)
        {
            SetEnabled(!_enabled);
            return true;
        }

        if (!_enabled || IsModifierKey(vk)) return false;

        if (IsHostForeground()) return false; // the editor has its own Kannada input

        if (ctrl || alt || win)
        {
            Engine.ClearBuffer(); // shortcut such as Ctrl+S
            return false;
        }

        if (vk == VK_BACK)
        {
            // false => the keystroke only undoes an invisible key (e.g. Nudi's silent 'a')
            return !Engine.HandleBackspace();
        }

        bool shift = IsPressed(VK_SHIFT);
        bool capsLock = (GetKeyState(VK_CAPITAL) & 1) != 0;
        char? ch = MapUsKey(vk, shift, capsLock);
        if (ch == null)
        {
            Engine.ClearBuffer(); // Enter, Tab, arrows, Home, Delete, numpad, ...
            return false;
        }

        string key = ch.Value.ToString();
        var (text, backspaceCount) = Engine.GetTransliteration(key);
        if (backspaceCount == 0 && text == key)
        {
            return false; // unmapped (space, digits, ...): let the original key through
        }

        SendOutput(backspaceCount, text);
        return true;
    }

    private static bool IsHostForeground()
    {
        IntPtr foreground = GetForegroundWindow();
        if (foreground != _lastForeground)
        {
            _lastForeground = foreground;
            Engine.ClearBuffer();
            GetWindowThreadProcessId(foreground, out uint pid);
            _lastForegroundIsHost = _hostPid != 0 && pid == _hostPid;
        }
        return _lastForegroundIsHost;
    }

    private static void SendOutput(int backspaceCount, string text)
    {
        var inputs = new INPUT[(backspaceCount + text.Length) * 2];
        int n = 0;
        for (int i = 0; i < backspaceCount; i++)
        {
            inputs[n++] = KeyInput(VK_BACK, 0, 0);
            inputs[n++] = KeyInput(VK_BACK, 0, KEYEVENTF_KEYUP);
        }
        foreach (char c in text)
        {
            inputs[n++] = KeyInput(0, c, KEYEVENTF_UNICODE);
            inputs[n++] = KeyInput(0, c, KEYEVENTF_UNICODE | KEYEVENTF_KEYUP);
        }
        if (n > 0)
        {
            SendInput((uint)n, inputs, Marshal.SizeOf<INPUT>());
        }
    }

    private static INPUT KeyInput(ushort vk, ushort scan, uint flags) => new()
    {
        type = INPUT_KEYBOARD,
        U = new InputUnion
        {
            ki = new KEYBDINPUT { wVk = vk, wScan = scan, dwFlags = flags, dwExtraInfo = InjectedMarker },
        },
    };

    // The Nudi / Baraha bindings are defined on US QWERTY characters, so map virtual keys
    // to the US character regardless of the active Windows input language.
    private static char? MapUsKey(int vk, bool shift, bool capsLock)
    {
        if (vk >= 'A' && vk <= 'Z')
        {
            return shift ^ capsLock ? (char)vk : char.ToLowerInvariant((char)vk);
        }
        if (vk >= '0' && vk <= '9')
        {
            return shift ? ")!@#$%^&*("[vk - '0'] : (char)vk;
        }
        return vk switch
        {
            VK_SPACE => ' ',
            0xBA => shift ? ':' : ';',  // VK_OEM_1
            0xBB => shift ? '+' : '=',  // VK_OEM_PLUS
            0xBC => shift ? '<' : ',',  // VK_OEM_COMMA
            0xBD => shift ? '_' : '-',  // VK_OEM_MINUS
            0xBE => shift ? '>' : '.',  // VK_OEM_PERIOD
            0xBF => shift ? '?' : '/',  // VK_OEM_2
            0xC0 => shift ? '~' : '`',  // VK_OEM_3
            0xDB => shift ? '{' : '[',  // VK_OEM_4
            0xDC => shift ? '|' : '\\', // VK_OEM_5
            0xDD => shift ? '}' : ']',  // VK_OEM_6
            0xDE => shift ? '"' : '\'', // VK_OEM_7
            _ => null,
        };
    }

    private static bool IsModifierKey(int vk) =>
        vk is VK_SHIFT or VK_CONTROL or VK_MENU or VK_CAPITAL or VK_LWIN or VK_RWIN
            or 0xA0 or 0xA1 or 0xA2 or 0xA3 or 0xA4 or 0xA5; // L/R Shift, Ctrl, Alt

    private static bool IsPressed(int vk) => (GetAsyncKeyState(vk) & 0x8000) != 0;

    private delegate IntPtr LowLevelProc(int nCode, IntPtr wParam, IntPtr lParam);

    [StructLayout(LayoutKind.Sequential)]
    private struct KBDLLHOOKSTRUCT
    {
        public uint vkCode;
        public uint scanCode;
        public uint flags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct INPUT
    {
        public uint type;
        public InputUnion U;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)] public MOUSEINPUT mi;
        [FieldOffset(0)] public KEYBDINPUT ki;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MOUSEINPUT
    {
        public int dx;
        public int dy;
        public uint mouseData;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MSG
    {
        public IntPtr hwnd;
        public uint message;
        public IntPtr wParam;
        public IntPtr lParam;
        public uint time;
        public int ptX;
        public int ptY;
    }

    [DllImport("user32.dll", SetLastError = true)]
    private static extern IntPtr SetWindowsHookEx(int idHook, LowLevelProc lpfn, IntPtr hMod, uint dwThreadId);

    [DllImport("user32.dll")]
    private static extern bool UnhookWindowsHookEx(IntPtr hhk);

    [DllImport("user32.dll")]
    private static extern IntPtr CallNextHookEx(IntPtr hhk, int nCode, IntPtr wParam, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern int GetMessage(out MSG lpMsg, IntPtr hWnd, uint wMsgFilterMin, uint wMsgFilterMax);

    [DllImport("user32.dll")]
    private static extern bool PeekMessage(out MSG lpMsg, IntPtr hWnd, uint wMsgFilterMin, uint wMsgFilterMax, uint wRemoveMsg);

    [DllImport("user32.dll")]
    private static extern bool TranslateMessage(ref MSG lpMsg);

    [DllImport("user32.dll")]
    private static extern IntPtr DispatchMessage(ref MSG lpMsg);

    [DllImport("user32.dll")]
    private static extern bool PostThreadMessage(uint idThread, uint msg, IntPtr wParam, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern void PostQuitMessage(int nExitCode);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

    [DllImport("user32.dll")]
    private static extern short GetAsyncKeyState(int vKey);

    [DllImport("user32.dll")]
    private static extern short GetKeyState(int nVirtKey);

    [DllImport("user32.dll")]
    private static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

    [DllImport("kernel32.dll")]
    private static extern uint GetCurrentThreadId();

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode)]
    private static extern IntPtr GetModuleHandle(string? lpModuleName);
}
