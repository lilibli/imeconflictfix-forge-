package com.bigshua.imeconflictfix;

import com.mojang.logging.LogUtils;
import com.sun.jna.CallbackReference;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * IME Conflict Fix - MC 1.21.10 / Forge 60.1.0
 *
 * Imm32 方案，参考 IMBlocker。
 * 全屏下自绘 IME 组合文字 overlay。
 */
@Mod(ImeConflictFix.MODID)
public final class ImeConflictFix {
    public static final String MODID = "imeconflictfix";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ImeConflictFix() {
        LOGGER.info("IME Conflict Fix 已加载!");
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static final class ClientEvents {

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            ImeController.init();
            Screen s = event.getScreen();
            if (s == null) return;
            if (s instanceof ChatScreen) {
                if (isCommand(s)) {
                    ImeController.setState(false);
                } else {
                    ImeController.setState(true);
                }
            } else if (s instanceof SignEditScreen || s instanceof BookEditScreen || s instanceof AnvilScreen) {
                ImeController.setState(true);
            }
        }

        @SubscribeEvent
        public static void onScreenClose(ScreenEvent.Closing event) {
            Screen s = event.getScreen();
            if (s instanceof ChatScreen || s instanceof SignEditScreen ||
                s instanceof BookEditScreen || s instanceof AnvilScreen) {
                ImeController.setState(null);
            }
        }

        @SubscribeEvent
        public static boolean onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
            Screen s = Minecraft.getInstance().screen;
            if (s == null) return false;
            if (!(s instanceof ChatScreen || s instanceof SignEditScreen ||
                  s instanceof BookEditScreen || s instanceof AnvilScreen)) return false;
            if (isCommand(s)) return false;
            int k = event.getKeyCode();
            if (k == GLFW.GLFW_KEY_W || k == GLFW.GLFW_KEY_A ||
                k == GLFW.GLFW_KEY_S || k == GLFW.GLFW_KEY_D ||
                k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_E ||
                k == GLFW.GLFW_KEY_TAB || k == GLFW.GLFW_KEY_LEFT_SHIFT || k == GLFW.GLFW_KEY_RIGHT_SHIFT)
                return true;
            return false;
        }

        /** 在屏幕上渲染 IME 组合文字和候选列表 */
        @SubscribeEvent
        public static void onRender(ScreenEvent.Render.Post event) {
            Minecraft mc = Minecraft.getInstance();
            GuiGraphics g = event.getGuiGraphics();
            int x = 10;
            int lineH = mc.font.lineHeight + 2;
            int baseY = mc.getWindow().getGuiScaledHeight() - 20;

            // 候选列表（在组合文字下方）
            String[] cands = ImeController.candidates;
            if (cands != null && cands.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cands.length && cands[i] != null; i++) {
                    if (i > 0) sb.append(' ');
                    sb.append(i + 1).append('.').append(cands[i]);
                }
                String cStr = sb.toString();
                int tw = mc.font.width(cStr);
                baseY -= lineH;
                g.fill(x - 2, baseY - 2, x + tw + 2, baseY + lineH, 0xCC000000);
                g.drawString(mc.font, cStr, x, baseY, 0xFFFFFFFF);
            }

            // 组合文字（拼音）
            String comp = ImeController.compositionText;
            if (comp != null && !comp.isEmpty()) {
                int tw = mc.font.width(comp);
                baseY -= lineH;
                g.fill(x - 2, baseY - 2, x + tw + 2, baseY + lineH, 0xAA000000);
                g.drawString(mc.font, comp, x, baseY, 0xFFAAAAAA);
            }
        }

        private static boolean isCommand(Screen s) {
            if (!(s instanceof ChatScreen)) return false;
            for (var c : s.children())
                if (c instanceof EditBox b && b.getValue().startsWith("/")) return true;
            return false;
        }
    }

    /** Imm32 输入法控制器 + 组合文字读取 */
    static class ImeController {
        private static native WinNT.HANDLE ImmGetContext(WinDef.HWND hwnd);
        private static native WinNT.HANDLE ImmAssociateContext(WinDef.HWND hwnd, WinNT.HANDLE himc);
        private static native boolean ImmReleaseContext(WinDef.HWND hwnd, WinNT.HANDLE himc);
        private static native WinNT.HANDLE ImmCreateContext();
        private static native boolean ImmDestroyContext(WinNT.HANDLE himc);
        private static native boolean ImmSetConversionStatus(WinNT.HANDLE himc, int fdwConversion, int fdwSentence);
        private static native boolean ImmSetCompositionWindow(WinNT.HANDLE himc, COMPOSITIONFORM cfr);
        private static native int ImmGetCompositionStringW(WinNT.HANDLE himc, int dwIndex, Pointer lpBuf, int dwBufLen);
        private static native int ImmGetCandidateListW(WinNT.HANDLE himc, int deIndex, Pointer lpCandList, int dwBufLen);

        // IME 消息常量
        private static final int WM_IME_SETCONTEXT      = 0x0281;
        private static final int WM_IME_STARTCOMPOSITION = 0x010D;
        private static final int WM_IME_COMPOSITION      = 0x010F;
        private static final int WM_IME_ENDCOMPOSITION   = 0x010E;
        private static final long ISC_SHOWUICANDIDATEWINDOW = 1L;
        private static final int GCS_COMPSTR = 0x0008;
        private static final int GCS_RESULTSTR = 0x0800;
        private static final int GWL_WNDPROC = -4;
        private static final int WM_IME_NOTIFY = 0x0282;
        private static final int IMN_CHANGECANDIDATE = 0x0003;
        private static final int IMN_OPENCANDIDATE = 0x0004;
        private static final int IMN_CLOSECANDIDATE = 0x0005;

        private static WinDef.HWND hwnd;
        private static boolean ready;
        private static WinUser.WindowProc wndProc;
        private static BaseTSD.LONG_PTR origProc;

        /** 当前 IME 组合文字（例如拼音 "nih"），渲染线程读取 */
        static volatile String compositionText;
        /** 候选字列表，渲染线程读取 */
        static volatile String[] candidates;

        static {
            try { Native.register("imm32"); LOGGER.info("[IME] imm32.dll 已加载"); }
            catch (Throwable t) { LOGGER.error("[IME] imm32 加载: {}", t.toString()); }
        }

        static void init() {
            if (ready) return;
            ready = true;
            try {
                hwnd = User32.INSTANCE.GetActiveWindow();
                LOGGER.info("[IME] HWND: {}", hwnd);
                if (hwnd != null) {
                    WinNT.HANDLE himc = ImmGetContext(hwnd);
                    if (himc != null) {
                        ImmReleaseContext(hwnd, himc);
                        himc = ImmAssociateContext(hwnd, null);
                        if (himc != null) ImmDestroyContext(himc);
                    }
                    hookWindowProc();
                }
            } catch (Throwable t) { LOGGER.error("[IME] 初始化失败: {}", t.toString()); }
        }

        private static void hookWindowProc() {
            try {
                User32 U = User32.INSTANCE;
                origProc = U.GetWindowLongPtr(hwnd, GWL_WNDPROC);
                if (origProc == null || origProc.longValue() == 0) return;
                wndProc = new WinUser.WindowProc() {
                    public WinDef.LRESULT callback(WinDef.HWND h, int msg, WinDef.WPARAM wp, WinDef.LPARAM lp) {
                        switch (msg) {
                            case WM_IME_SETCONTEXT:
                                if (wp.longValue() != 0)
                                    lp.setValue(lp.longValue() & ~ISC_SHOWUICANDIDATEWINDOW);
                                break;
                            case WM_IME_STARTCOMPOSITION:
                                updateComposition();
                                break;
                            case WM_IME_COMPOSITION: {
                                int flags = lp.intValue();
                                if ((flags & GCS_COMPSTR) != 0) {
                                    updateComposition();
                                    updateCandidates();
                                }
                                // 只有组合串无结果时才拦截（防止文字进入游戏但保留自绘）
                                if ((flags & GCS_RESULTSTR) == 0)
                                    return new WinDef.LRESULT(0);
                                break;
                            }
                            case WM_IME_ENDCOMPOSITION:
                                compositionText = null;
                                candidates = null;
                                break;
                            case WM_IME_NOTIFY:
                                switch (wp.intValue()) {
                                    case IMN_CHANGECANDIDATE:
                                    case IMN_OPENCANDIDATE:
                                        updateCandidates();
                                        break;
                                    case IMN_CLOSECANDIDATE:
                                        candidates = null;
                                        break;
                                }
                                break;
                        }
                        return U.CallWindowProc(new Pointer(origProc.longValue()), h, msg, wp, lp);
                    }
                };
                Pointer p = CallbackReference.getFunctionPointer(wndProc);
                U.SetWindowLongPtr(hwnd, GWL_WNDPROC, p);
                LOGGER.info("[IME] WindowProc 钩子已安装");
            } catch (Throwable t) { LOGGER.error("[IME] 钩子失败: {}", t.toString()); }
        }

        /** 读取 IME 组合字符串（拼音等） */
        private static void updateComposition() {
            try {
                WinNT.HANDLE himc = ImmGetContext(hwnd);
                if (himc == null) return;
                int bytes = ImmGetCompositionStringW(himc, GCS_COMPSTR, null, 0);
                if (bytes > 0) {
                    Memory buf = new Memory(bytes);
                    ImmGetCompositionStringW(himc, GCS_COMPSTR, buf, bytes);
                    char[] chars = new char[bytes / 2];
                    for (int i = 0; i < chars.length; i++)
                        chars[i] = (char) buf.getShort(i * 2L);
                    compositionText = new String(chars);
                } else {
                    compositionText = null;
                }
                ImmReleaseContext(hwnd, himc);
            } catch (Throwable t) { compositionText = null; }
        }

        /** 读取候选字列表 */
        private static void updateCandidates() {
            try {
                WinNT.HANDLE himc = ImmGetContext(hwnd);
                if (himc == null) return;
                int size = ImmGetCandidateListW(himc, 0, null, 0);
                if (size <= 0) { candidates = null; ImmReleaseContext(hwnd, himc); return; }
                Memory buf = new Memory(size);
                ImmGetCandidateListW(himc, 0, buf, size);
                int count = buf.getInt(8);
                int start = buf.getInt(16);
                if (count <= 0) { candidates = null; ImmReleaseContext(hwnd, himc); return; }
                String[] list = new String[Math.min(count - start, 9)];
                for (int i = 0; i < list.length; i++) {
                    int off = buf.getInt(24 + (start + i) * 4);
                    // 手动读取宽字符串，避免 getWideString 读到多余内存
                    StringBuilder sb = new StringBuilder();
                    int pos = off;
                    while (true) {
                        short ch = buf.getShort(pos);
                        if (ch == 0) break;
                        sb.append((char) ch);
                        pos += 2;
                    }
                    list[i] = sb.toString();
                }
                candidates = list;
                ImmReleaseContext(hwnd, himc);
            } catch (Throwable t) { candidates = null; }
        }

        @Structure.FieldOrder({"dwStyle", "ptCurrentPos", "rcArea"})
        public static class COMPOSITIONFORM extends Structure {
            public int dwStyle;
            public POINT ptCurrentPos;
            public RECT rcArea;
            @Structure.FieldOrder({"x", "y"})
            public static class POINT extends Structure { public int x, y; }
            @Structure.FieldOrder({"left", "top", "right", "bottom"})
            public static class RECT extends Structure { public int left, top, right, bottom; }
        }

        static void setState(Boolean mode) {
            try {
                if (hwnd == null) return;
                WinNT.HANDLE himc = ImmGetContext(hwnd);
                if (mode == null) {
                    compositionText = null;
                    candidates = null;
                    if (himc != null) {
                        ImmReleaseContext(hwnd, himc);
                        himc = ImmAssociateContext(hwnd, null);
                        if (himc != null) ImmDestroyContext(himc);
                    }
                } else {
                    if (himc == null) {
                        WinNT.HANDLE old = ImmAssociateContext(hwnd, ImmCreateContext());
                        if (old != null) ImmDestroyContext(old);
                        himc = ImmGetContext(hwnd);
                    }
                    if (himc != null) {
                        ImmSetConversionStatus(himc, mode ? 1 : 0, 0);
                        if (mode) {
                            try {
                                COMPOSITIONFORM cf = new COMPOSITIONFORM();
                                cf.dwStyle = 2;
                                cf.ptCurrentPos = new COMPOSITIONFORM.POINT();
                                cf.rcArea = new COMPOSITIONFORM.RECT();
                                int sh = Minecraft.getInstance().getWindow().getScreenHeight();
                                cf.ptCurrentPos.x = 10;
                                cf.ptCurrentPos.y = sh - 40;
                                ImmSetCompositionWindow(himc, cf);
                            } catch (Throwable ignored) {}
                        }
                        ImmReleaseContext(hwnd, himc);
                    }
                }
                LOGGER.info("[IME] 状态 → {}", mode == null ? "关闭" : mode ? "中文" : "英文");
            } catch (Throwable t) { LOGGER.error("[IME] 失败: {}", t.toString()); }
        }
    }
}
