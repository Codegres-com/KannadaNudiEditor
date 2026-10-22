using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Text;
using KannadaNudiWeb.Services;
using Tmds.DBus.Protocol;

namespace KannadaNudiGlobalKeyboard;

// Global (Direct Type) Kannada keyboard for Linux.
//
// Linux has no equivalent of the Windows low-level keyboard hook: Wayland does not let apps read
// or inject keystrokes system-wide, and snap confinement blocks uinput. Every desktop app does
// however receive its keystrokes through the input method framework (IBus on Ubuntu / GNOME), so
// this helper registers itself on the user's ibus-daemon as an input method engine and makes
// itself the active engine. Keystrokes are then turned into Kannada with the same
// KannadaKeyboardEngine as the editor, in every application (browser, LibreOffice, gedit, ...).
// F9 toggles it on/off, exactly like the Windows helper.
//
// The pending syllable is shown as preedit text (underlined) instead of backspacing over
// committed text, so it also works in apps that do not report surrounding text.
//
// Arguments: --enabled         start with Kannada typing on
//            --layout <Nudi|Baraha>
//
// Protocol with the host over stdin/stdout, one line per message:
//   in : toggle | enable | disable | layout <Nudi|Baraha> | host-focus <on|off> | exit
//   out: ready | state <on|off> <Nudi|Baraha> | error <message>
// While the host (editor) window has focus keys pass through untouched: it transliterates itself.
// The helper exits when stdin closes, so it never outlives the host.
internal static class Program
{
    private const string IBusService = "org.freedesktop.IBus";
    private const string IBusPath = "/org/freedesktop/IBus";
    private const string IBusInterface = "org.freedesktop.IBus";
    private const string FactoryPath = "/org/freedesktop/IBus/Factory";
    private const string EnginePathPrefix = "/org/freedesktop/IBus/Engine/KannadaNudi";
    private const string EngineName = "kannadanudi";
    private const string FallbackEngine = "xkb:us::eng";

    // IBus key state bits (same values as GDK modifier masks)
    private const uint ControlMask = 1 << 2;
    private const uint Mod1Mask = 1 << 3; // Alt
    private const uint Mod4Mask = 1 << 6; // Super
    private const uint SuperMask = 1 << 26;
    private const uint MetaMask = 1 << 28;
    private const uint ReleaseMask = 1 << 30;

    private const uint KeyF9 = 0xffc6;
    private const uint KeyBackSpace = 0xff08;

    private const uint PreeditCommit = 1; // IBUS_ENGINE_PREEDIT_COMMIT: commit preedit on focus out
    private const int MaxPreedit = 8;     // longer than any replacement the engine can ask for

    private static readonly KannadaKeyboardEngine Engine = new();
    private static readonly object Gate = new();
    private static readonly object OutputLock = new();

    private static DBusConnection? _bus;
    private static string? _activeEnginePath;
    private static string _previousEngine = FallbackEngine;
    private static int _engineCount;
    private static long _lastTakeOver;

    private static bool _enabled;
    private static bool _isGlobalEngine;
    private static bool _hostFocused;
    private static readonly StringBuilder Preedit = new();

    private static async Task<int> Main(string[] args)
    {
        var layout = KeyboardLayout.Nudi;
        for (int i = 0; i < args.Length; i++)
        {
            switch (args[i])
            {
                case "--enabled":
                    _enabled = true;
                    break;
                case "--layout" when i + 1 < args.Length:
                    Enum.TryParse(args[++i], true, out layout);
                    break;
            }
        }
        Engine.SetLayout(layout);

        string? address = FindIBusAddress();
        if (address == null)
        {
            Emit("error ibus-not-running");
            return 2;
        }

        string step = "connect";
        try
        {
            _bus = new DBusConnection(new IBusConnectionOptions(address));
            await _bus.ConnectAsync();
            _bus.AddMethodHandler(new FactoryHandler());
            step = "register-component";
            await RegisterComponentAsync();
            step = "get-global-engine";
            _previousEngine = await GetGlobalEngineAsync() ?? FallbackEngine;
            step = "watch-engine";
            await _bus.WatchSignalAsync(null, IBusPath, IBusInterface, "GlobalEngineChanged",
                (Message m, object? _) => m.GetBodyReader().ReadString(),
                (Notification<string> n) => { if (n.HasValue) OnGlobalEngineChanged(n.Value); },
                ObserverFlags.None, false, null);
            step = "set-global-engine";
            await TakeOverAsync();
        }
        catch (Exception ex)
        {
            Emit($"error ibus {step} ({address}) {ex.GetType().Name}: {ex.Message}".Replace('\n', ' '));
            return 1;
        }

        Emit("ready");
        EmitState();

        await ReadCommandsAsync();

        await RestorePreviousEngineAsync();
        _bus.Dispose();
        return 0;
    }

    // ---- ibus-daemon address -------------------------------------------------------------

    // ibus-daemon writes its D-Bus address to ~/.config/ibus/bus/<machine-id>-<host>-<display>.
    // Inside a snap $HOME is redirected, so use the real home directory.
    private static string? FindIBusAddress()
    {
        string? env = Environment.GetEnvironmentVariable("IBUS_ADDRESS");
        if (!string.IsNullOrEmpty(env)) return env;

        string home = Environment.GetEnvironmentVariable("SNAP_REAL_HOME")
            ?? Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        string busDir = Path.Combine(home, ".config", "ibus", "bus");
        if (!Directory.Exists(busDir)) return null;

        foreach (var file in new DirectoryInfo(busDir).GetFiles().OrderByDescending(f => f.LastWriteTimeUtc))
        {
            try
            {
                string? address = null;
                int pid = 0;
                foreach (string line in File.ReadAllLines(file.FullName))
                {
                    if (line.StartsWith("IBUS_ADDRESS=")) address = line["IBUS_ADDRESS=".Length..].Trim();
                    else if (line.StartsWith("IBUS_DAEMON_PID=")) int.TryParse(line["IBUS_DAEMON_PID=".Length..], out pid);
                }
                if (string.IsNullOrEmpty(address)) continue;
                // Skip files left behind by an ibus-daemon that is no longer running. Inside a snap
                // other processes' /proc entries are hidden, so rely on the newest file there.
                if (pid > 0 && !IsSnap() && !Directory.Exists($"/proc/{pid}")) continue;
                return address;
            }
            catch (IOException)
            {
            }
            catch (UnauthorizedAccessException)
            {
            }
        }
        return null;
    }

    private static bool IsSnap() => Environment.GetEnvironmentVariable("SNAP") != null;

    // ibus-daemon is a GLib D-Bus server: it authenticates clients by the SCM_CREDENTIALS attached
    // to the first byte. Tmds sends that byte without credentials, so the kernel reports the peer
    // as "nobody" and the daemon hangs up ("Connection closed by peer"). With SO_PASSCRED set on our
    // own socket the kernel attaches our real credentials to everything we send.
    private sealed class IBusConnectionOptions(string address) : DBusConnectionOptions
    {
        // Raw Linux values: SetRawSocketOption takes the native level, and .NET's
        // SocketOptionLevel.Socket is the Windows value (0xFFFF), which Linux rejects.
        private const int SOL_SOCKET = 1;
        private const int SO_PASSCRED = 16;

        protected override async ValueTask<SetupResult> SetupAsync(CancellationToken cancellationToken)
        {
            var socket = new Socket(AddressFamily.Unix, SocketType.Stream, ProtocolType.Unspecified);
            try
            {
                socket.SetRawSocketOption(SOL_SOCKET, SO_PASSCRED, BitConverter.GetBytes(1));
                await socket.ConnectAsync(ParseUnixEndPoint(address), cancellationToken);
                return new SetupResult
                {
                    ConnectionStream = new NetworkStream(socket, ownsSocket: true),
                    UserId = geteuid().ToString(),
                    TeardownToken = socket,
                };
            }
            catch
            {
                socket.Dispose();
                throw;
            }
        }

        protected override void Teardown(object? token) => (token as IDisposable)?.Dispose();

        // "unix:path=/home/u/.cache/ibus/dbus-XXXX,guid=..." or "unix:abstract=/tmp/dbus-XXXX,guid=..."
        private static UnixDomainSocketEndPoint ParseUnixEndPoint(string address)
        {
            foreach (string entry in address.Split(';'))
            {
                if (!entry.StartsWith("unix:")) continue;
                foreach (string pair in entry["unix:".Length..].Split(','))
                {
                    int eq = pair.IndexOf('=');
                    if (eq < 0) continue;
                    string key = pair[..eq];
                    string value = Uri.UnescapeDataString(pair[(eq + 1)..]);
                    if (key == "path") return new UnixDomainSocketEndPoint(value);
                    if (key == "abstract") return new UnixDomainSocketEndPoint("\0" + value);
                }
            }
            throw new ArgumentException($"Unsupported IBus address: {address}");
        }

        [DllImport("libc")]
        private static extern uint geteuid();
    }

    // ---- ibus-daemon calls ---------------------------------------------------------------

    // org.freedesktop.IBus.RegisterComponent(v component): an IBusComponent holding one
    // IBusEngineDesc. Registered at runtime, so nothing has to be installed on the host.
    private static Task RegisterComponentAsync() => _bus!.CallMethodAsync(CreateRegisterComponentMessage());

    private static MessageBuffer CreateRegisterComponentMessage()
    {
        using var writer = _bus!.GetMessageWriter();
        writer.WriteMethodCallHeader(IBusService, IBusPath, IBusInterface, "RegisterComponent", "v", MessageFlags.None);

        writer.WriteSignature("(sa{sv}ssssssssavav)");
        writer.WriteStructureStart();
        writer.WriteString("IBusComponent");
        WriteEmptyAttachments(writer);
        writer.WriteString("org.freedesktop.IBus.KannadaNudi"); // name
        writer.WriteString("Kannada Nudi Global Keyboard");    // description
        writer.WriteString("1.0");                             // version
        writer.WriteString("ISC");                             // license
        writer.WriteString("Codegres <contact@codegres.com>"); // author
        writer.WriteString("https://nudiweb.com/");            // homepage
        writer.WriteString("");                                // exec (already running)
        writer.WriteString("");                                // textdomain

        ArrayStart observed = writer.WriteArrayStart(DBusType.Variant);
        writer.WriteArrayEnd(observed);

        ArrayStart engines = writer.WriteArrayStart(DBusType.Variant);
        writer.WriteSignature("(sa{sv}ssssssssusssssssss)");
        writer.WriteStructureStart();
        writer.WriteString("IBusEngineDesc");
        WriteEmptyAttachments(writer);
        writer.WriteString(EngineName);                         // name
        writer.WriteString("Kannada (Nudi)");                   // longname
        writer.WriteString("Kannada Nudi / Baraha keyboard. F9 switches Kannada / English."); // description
        writer.WriteString("kn");                               // language
        writer.WriteString("ISC");                              // license
        writer.WriteString("Codegres");                         // author
        writer.WriteString("");                                 // icon
        writer.WriteString("default");                          // layout: keep the current keyboard layout
        writer.WriteUInt32(0);                                  // rank
        writer.WriteString("");                                 // hotkeys
        writer.WriteString("ಕ");                                // symbol
        writer.WriteString("");                                 // setup
        writer.WriteString("");                                 // layout_variant
        writer.WriteString("");                                 // layout_option
        writer.WriteString("1.0");                              // version
        writer.WriteString("");                                 // textdomain
        writer.WriteString("");                                 // icon_prop_key
        writer.WriteArrayEnd(engines);

        return writer.CreateMessage();
    }

    private static async Task<string?> GetGlobalEngineAsync()
    {
        try
        {
            return await _bus!.CallMethodAsync(CreateGetGlobalEngineMessage(), (Message m, object? _) =>
            {
                // v -> IBusEngineDesc variant -> (s typename, a{sv}, s name, ...)
                VariantValue value = m.GetBodyReader().ReadVariantValue();
                while (value.Type == VariantValueType.Variant) value = value.GetVariantValue();
                return value.GetItem(2).GetString();
            }, null);
        }
        catch (DBusErrorReplyException)
        {
            return null; // no global engine yet
        }
    }

    private static MessageBuffer CreateGetGlobalEngineMessage()
    {
        using var writer = _bus!.GetMessageWriter();
        writer.WriteMethodCallHeader(IBusService, IBusPath, "org.freedesktop.DBus.Properties", "Get", "ss", MessageFlags.None);
        writer.WriteString(IBusInterface);
        writer.WriteString("GlobalEngine");
        return writer.CreateMessage();
    }

    private static Task SetGlobalEngineAsync(string name) => _bus!.CallMethodAsync(CreateSetGlobalEngineMessage(name));

    private static MessageBuffer CreateSetGlobalEngineMessage(string name)
    {
        using var writer = _bus!.GetMessageWriter();
        writer.WriteMethodCallHeader(IBusService, IBusPath, IBusInterface, "SetGlobalEngine", "s", MessageFlags.None);
        writer.WriteString(name);
        return writer.CreateMessage();
    }

    private static async Task TakeOverAsync()
    {
        await SetGlobalEngineAsync(EngineName);
        lock (Gate) _isGlobalEngine = true;
    }

    private static async Task RestorePreviousEngineAsync()
    {
        if (!_isGlobalEngine) return;
        try
        {
            await SetGlobalEngineAsync(_previousEngine);
        }
        catch (Exception)
        {
            // ibus-daemon went away
        }
    }

    // GNOME switches the global engine when the user picks another input source (Super+Space)
    // or when the session reloads its keyboard settings (e.g. after unlocking the screen).
    private static void OnGlobalEngineChanged(string name)
    {
        bool wasGlobal;
        lock (Gate)
        {
            wasGlobal = _isGlobalEngine;
            _isGlobalEngine = name == EngineName;
            if (!_isGlobalEngine) _previousEngine = name;
        }
        if (name == EngineName) return;

        // A plain keyboard layout (xkb:...) is exactly what our engine does while Kannada is off,
        // so stay the active engine and keep F9 working everywhere. Another real input method
        // (e.g. Japanese) is a deliberate choice: respect it until Global mode is switched on again.
        if (wasGlobal && name.StartsWith("xkb:") && Environment.TickCount64 - _lastTakeOver > 2000)
        {
            _lastTakeOver = Environment.TickCount64;
            _ = TakeOverSafeAsync();
        }
        else
        {
            lock (Gate) _enabled = false;
            EmitState();
        }
    }

    private static async Task TakeOverSafeAsync()
    {
        try
        {
            await TakeOverAsync();
        }
        catch (Exception ex)
        {
            Emit($"error set-engine {ex.Message}".Replace('\n', ' '));
        }
    }

    // ---- host commands -------------------------------------------------------------------

    private static async Task ReadCommandsAsync()
    {
        string? line;
        while ((line = await Console.In.ReadLineAsync()) != null)
        {
            string[] parts = line.Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries);
            if (parts.Length == 0) continue;

            switch (parts[0])
            {
                case "toggle":
                    await SetEnabledAsync(!_enabled);
                    break;
                case "enable":
                    await SetEnabledAsync(true);
                    break;
                case "disable":
                    await SetEnabledAsync(false);
                    break;
                case "layout" when parts.Length > 1 && Enum.TryParse(parts[1], true, out KeyboardLayout layout):
                    lock (Gate)
                    {
                        DropPreedit();
                        Engine.SetLayout(layout);
                    }
                    EmitState();
                    break;
                case "host-focus" when parts.Length > 1:
                    lock (Gate)
                    {
                        // Focus already left the other app, which committed its preedit itself.
                        _hostFocused = parts[1] == "on";
                        DropPreedit();
                    }
                    break;
                case "exit":
                    return;
            }
        }
    }

    private static async Task SetEnabledAsync(bool enabled)
    {
        if (enabled && !_isGlobalEngine)
        {
            await TakeOverSafeAsync();
        }
        lock (Gate)
        {
            _enabled = enabled;
            DropPreedit(); // toggled from the editor or tray: focus is no longer in the typing app
        }
        EmitState();
    }

    private static void EmitState() =>
        Emit($"state {(_enabled && _isGlobalEngine ? "on" : "off")} {Engine.CurrentLayout}");

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

    // ---- key handling (called by ibus-daemon on the engine object) ------------------------

    // Returns true when the key was consumed.
    private static bool ProcessKeyEvent(uint keyval, uint state)
    {
        bool changed = false;
        bool handled;
        lock (Gate)
        {
            handled = HandleKey(keyval, state, ref changed);
        }
        if (changed) EmitState();
        return handled;
    }

    private static bool HandleKey(uint keyval, uint state, ref bool changed)
    {
        if ((state & ReleaseMask) != 0) return false;

        bool modifier = (state & (ControlMask | Mod1Mask | Mod4Mask | SuperMask | MetaMask)) != 0;

        if (keyval == KeyF9 && !modifier)
        {
            _enabled = !_enabled;
            CommitPreedit();
            Engine.ClearBuffer();
            changed = true;
            return true;
        }

        if (!_enabled || _hostFocused) return false; // the editor has its own Kannada input

        if (modifier)
        {
            CommitPreedit(); // shortcut such as Ctrl+S
            Engine.ClearBuffer();
            return false;
        }

        if (keyval == KeyBackSpace)
        {
            // false => the keystroke only undoes an invisible key (e.g. Nudi's silent 'a')
            if (!Engine.HandleBackspace()) return true;
            if (Preedit.Length == 0) return false; // delete committed text as usual
            Preedit.Length--;
            UpdatePreedit();
            return true;
        }

        // The Nudi / Baraha bindings are defined on US QWERTY characters; IBus already gives us
        // the character for the current keyboard layout, Shift and Caps Lock applied.
        if (keyval < 0x20 || keyval > 0x7e)
        {
            CommitPreedit(); // Enter, Tab, arrows, Home, Delete, numpad, ...
            Engine.ClearBuffer();
            return false;
        }

        string key = ((char)keyval).ToString();
        var (text, backspaceCount) = Engine.GetTransliteration(key);
        if (backspaceCount == 0 && text == key)
        {
            CommitPreedit(); // unmapped (space, digits, ...): let the original key through
            return false;
        }

        if (backspaceCount > Preedit.Length)
        {
            // Never happens with the current bindings (MaxPreedit is larger than any replacement),
            // but stay consistent: the syllable can no longer be edited, start a new one.
            CommitPreedit();
            Preedit.Append(text);
        }
        else
        {
            Preedit.Length -= backspaceCount;
            Preedit.Append(text);
        }

        // Keep only the part that the next keystroke can still change in the preedit.
        if (Preedit.Length > MaxPreedit)
        {
            int keep = TailStart(Preedit.ToString(), MaxPreedit / 2);
            CommitText(Preedit.ToString(0, keep));
            Preedit.Remove(0, keep);
        }

        UpdatePreedit();
        return true;
    }

    // Start index of the last `count` characters, never splitting a surrogate pair.
    private static int TailStart(string s, int count)
    {
        int start = Math.Max(0, s.Length - count);
        if (start > 0 && char.IsLowSurrogate(s[start])) start--;
        return start;
    }

    private static void CommitPreedit()
    {
        if (Preedit.Length == 0) return;
        string text = Preedit.ToString();
        Preedit.Clear();
        UpdatePreedit();
        CommitText(text);
    }

    // The preedit was already committed by the client (focus out / reset with PreeditCommit).
    private static void DropPreedit()
    {
        Preedit.Clear();
        Engine.ClearBuffer();
    }

    // ---- engine signals ------------------------------------------------------------------

    private static void CommitText(string text)
    {
        if (_activeEnginePath == null || text.Length == 0) return;
        using var writer = _bus!.GetMessageWriter();
        writer.WriteSignalHeader(null, _activeEnginePath, "org.freedesktop.IBus.Engine", "CommitText", "v");
        WriteIBusText(writer, text, underline: false);
        _bus.TrySendMessage(writer.CreateMessage());
    }

    private static void UpdatePreedit()
    {
        if (_activeEnginePath == null) return;
        string text = Preedit.ToString();
        using var writer = _bus!.GetMessageWriter();
        writer.WriteSignalHeader(null, _activeEnginePath, "org.freedesktop.IBus.Engine", "UpdatePreeditText", "vubu");
        WriteIBusText(writer, text, underline: true);
        writer.WriteUInt32((uint)text.Length);        // cursor at the end
        writer.WriteBool(text.Length > 0);           // visible
        writer.WriteUInt32(PreeditCommit);
        _bus.TrySendMessage(writer.CreateMessage());
    }

    // Variant holding IBusText: ("IBusText", a{sv}, s text, v IBusAttrList)
    private static void WriteIBusText(MessageWriter writer, string text, bool underline)
    {
        writer.WriteSignature("(sa{sv}sv)");
        writer.WriteStructureStart();
        writer.WriteString("IBusText");
        WriteEmptyAttachments(writer);
        writer.WriteString(text);

        // IBusAttrList: ("IBusAttrList", a{sv}, av attributes)
        writer.WriteSignature("(sa{sv}av)");
        writer.WriteStructureStart();
        writer.WriteString("IBusAttrList");
        WriteEmptyAttachments(writer);
        ArrayStart attrs = writer.WriteArrayStart(DBusType.Variant);
        if (underline && text.Length > 0)
        {
            // IBusAttribute: ("IBusAttribute", a{sv}, u type=UNDERLINE, u value=SINGLE, u start, u end)
            writer.WriteSignature("(sa{sv}uuuu)");
            writer.WriteStructureStart();
            writer.WriteString("IBusAttribute");
            WriteEmptyAttachments(writer);
            writer.WriteUInt32(1);
            writer.WriteUInt32(1);
            writer.WriteUInt32(0);
            writer.WriteUInt32((uint)text.Length);
        }
        writer.WriteArrayEnd(attrs);
    }

    private static void WriteEmptyAttachments(MessageWriter writer)
    {
        ArrayStart dict = writer.WriteDictionaryStart();
        writer.WriteDictionaryEnd(dict);
    }

    // ---- D-Bus objects exported to ibus-daemon -------------------------------------------

    // org.freedesktop.IBus.Factory: ibus-daemon asks it for an engine instance.
    private sealed class FactoryHandler : IPathMethodHandler
    {
        public string Path => FactoryPath;
        public bool HandlesChildPaths => false;

        public ValueTask HandleMethodAsync(MethodContext context)
        {
            Message request = context.Request;
            if (request.MemberAsString == "CreateEngine")
            {
                string name = request.GetBodyReader().ReadString();
                if (name != EngineName)
                {
                    context.ReplyError("org.freedesktop.IBus.Error", $"Unknown engine {name}");
                    return default;
                }
                string path = $"{EnginePathPrefix}/{Interlocked.Increment(ref _engineCount)}";
                context.DBusConnection.AddMethodHandler(new EngineHandler(path));
                using var writer = context.CreateReplyWriter("o");
                writer.WriteObjectPath(path);
                context.Reply(writer.CreateMessage());
            }
            else if (request.MemberAsString == "Destroy")
            {
                ReplyEmpty(context);
            }
            else
            {
                context.ReplyUnknownMethodError();
            }
            return default;
        }
    }

    // org.freedesktop.IBus.Engine: one instance per CreateEngine call; only the enabled one is active.
    private sealed class EngineHandler(string path) : IPathMethodHandler
    {
        public string Path => path;
        public bool HandlesChildPaths => false;

        public ValueTask HandleMethodAsync(MethodContext context)
        {
            Message request = context.Request;
            string member = request.MemberAsString ?? "";

            if (context.IsPropertiesInterfaceRequest)
            {
                HandleProperties(context, member);
                return default;
            }

            switch (member)
            {
                case "ProcessKeyEvent":
                {
                    var reader = request.GetBodyReader();
                    uint keyval = reader.ReadUInt32();
                    reader.ReadUInt32(); // keycode
                    uint state = reader.ReadUInt32();
                    lock (Gate) _activeEnginePath = path;
                    bool handled = ProcessKeyEvent(keyval, state);
                    using var writer = context.CreateReplyWriter("b");
                    writer.WriteBool(handled);
                    context.Reply(writer.CreateMessage());
                    return default;
                }
                case "Enable":
                case "FocusIn":
                case "FocusInId":
                    lock (Gate)
                    {
                        _activeEnginePath = path;
                        DropPreedit();
                    }
                    break;
                case "FocusOut":
                case "FocusOutId":
                case "Reset":
                    lock (Gate)
                    {
                        if (_activeEnginePath == path) DropPreedit();
                    }
                    break;
                case "Disable":
                    lock (Gate)
                    {
                        if (_activeEnginePath == path) DropPreedit();
                    }
                    break;
                case "Destroy":
                    lock (Gate)
                    {
                        if (_activeEnginePath == path) _activeEnginePath = null;
                    }
                    ReplyEmpty(context);
                    context.DBusConnection.RemoveMethodHandler(path);
                    return default;
            }

            // SetCapabilities, SetCursorLocation, SetSurroundingText, PropertyActivate, ...: nothing to do.
            ReplyEmpty(context);
            return default;
        }

        private static void HandleProperties(MethodContext context, string member)
        {
            if (member == "Get")
            {
                // FocusId / ActiveSurroundingText: we need neither.
                using var writer = context.CreateReplyWriter("v");
                writer.WriteVariantBool(false);
                context.Reply(writer.CreateMessage());
            }
            else if (member == "GetAll")
            {
                using var writer = context.CreateReplyWriter("a{sv}");
                WriteEmptyAttachments(writer);
                context.Reply(writer.CreateMessage());
            }
            else
            {
                ReplyEmpty(context); // Set ContentType, ...
            }
        }
    }

    private static void ReplyEmpty(MethodContext context)
    {
        if (context.NoReplyExpected) return;
        using var writer = context.CreateReplyWriter(null);
        context.Reply(writer.CreateMessage());
    }
}
