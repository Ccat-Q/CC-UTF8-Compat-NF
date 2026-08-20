package dev.kondrashka.ccutf8compat.mixins.common.cc_tweaked;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.TermMethods;
import dan200.computercraft.core.terminal.Terminal;

import dev.kondrashka.ccutf8compat.config.CcUtf8CompatConfig;
import dev.kondrashka.ccutf8compat.access.CcUtf8TextBufferAccess;

/**
 * Decodes Lua UTF-8 byte strings before writing them to CC:Tweaked terminals.
 *
 * Strings may mix valid UTF-8 sequences (Chinese, accents, ...) with raw bytes
 * from CC:Tweaked's legacy 256-character set (pixel bars, block glyphs, ...).
 * Such mixed strings must still decode to one display unit per valid sequence
 * and one display unit per raw byte.
 */

@Mixin(value = TermMethods.class, remap = false)
public abstract class TermMethodsMixin {

    @Shadow
    public abstract Terminal getTerminal() throws LuaException;

    @Unique
    private static int ccUtf8$unitWidth(byte[] bytes, int i) {
        var b = bytes[i] & 0xFF;

        int width;
        if (b < 0x80) {
            width = 1;
        } else if (b >= 0xC2 && b <= 0xDF) {
            width = 2;
        } else if (b >= 0xE0 && b <= 0xEF) {
            width = 3;
        } else if (b >= 0xF0 && b <= 0xF4) {
            width = 4;
        } else {
            return 1;
        }

        if (i + width > bytes.length) {
            return 1;
        }

        for (var k = 1; k < width; k++) {
            var cb = bytes[i + k] & 0xFF;
            if (cb < 0x80 || cb > 0xBF) {
                return 1;
            }
        }

        return width;
    }

    // decode a Lua byte string into display units: valid UTF-8 sequences become
    // their codepoints, every other byte stays as its own single-unit codepoint
    @Unique
    private static int[] ccUtf8$decodeUnits(byte[] bytes) {
        var codepoints = new int[bytes.length];
        var n = 0;
        var i = 0;

        while (i < bytes.length) {
            var width = ccUtf8$unitWidth(bytes, i);

            if (width > 1) {
                var b = bytes[i] & 0xFF;
                var mask = width == 2 ? 0x1F : width == 3 ? 0x0F : 0x07;
                var value = b & mask;

                for (var k = 1; k < width; k++) {
                    value = (value << 6) | (bytes[i + k] & 0x3F);
                }

                codepoints[n] = value;
            } else {
                codepoints[n] = bytes[i] & 0xFF;
            }

            i += width;
            n++;
        }

        return n == codepoints.length ? codepoints : Arrays.copyOf(codepoints, n);
    }

    // byte position of each display unit in the original Lua byte string
    @Unique
    private static int[] ccUtf8$getUnitOffsets(byte[] bytes) {
        var offsets = new int[bytes.length];
        var n = 0;
        var i = 0;

        while (i < bytes.length) {
            offsets[n] = i;
            i += ccUtf8$unitWidth(bytes, i);
            n++;
        }

        return n == offsets.length ? offsets : Arrays.copyOf(offsets, n);
    }

    // convert a Lua byte string to a Java string, decoding valid UTF-8 runs and
    // preserving every other byte as its own character
    @Unique
    private static String ccUtf8$decodeMixed(String text) {
        var bytes = new byte[text.length()];

        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);

            if (c > 255) {
                // already a Java Unicode string, not a Lua byte string
                return text;
            }

            bytes[i] = (byte) c;
        }

        var codepoints = ccUtf8$decodeUnits(bytes);
        return new String(codepoints, 0, codepoints.length);
    }

    @Inject(method = "write", at = @At("HEAD"), cancellable = true, remap = false)
    private void ccUtf8$writeUtf8(Coerced<String> textA, CallbackInfo ci) throws LuaException {
        if (!CcUtf8CompatConfig.ENABLE_CC_UTF8_COMPAT.get()) {
            return;
        }

        var text = ccUtf8$decodeMixed(textA.value());
        var width = text.codePointCount(0, text.length());
        var terminal = getTerminal();

        synchronized (terminal) {
            terminal.write(text);
            terminal.setCursorPos(terminal.getCursorX() + width, terminal.getCursorY());
        }

        ci.cancel();
    }

    @Unique
    private static byte[] ccUtf8$copyBytes(ByteBuffer buffer) {
        var copy = buffer.slice();
        var bytes = new byte[copy.remaining()];

        copy.get(bytes);

        return bytes;
    }

    @Unique
    private static char ccUtf8$getColour(ByteBuffer buffer, int byteIndex, int charIndex, int byteLength, int charLength) {
        var position = buffer.position();

        if (buffer.remaining() == charLength) {
            return (char) (buffer.get(position + charIndex) & 0xFF);
        }

        return (char) (buffer.get(position + byteIndex) & 0xFF);
    }

    @Inject(method = "blit", at = @At("HEAD"), cancellable = true, remap = false)
    private void ccUtf8$blitUtf8(ByteBuffer text, ByteBuffer textColour, ByteBuffer backgroundColour, CallbackInfo ci) throws LuaException {
        if (!CcUtf8CompatConfig.ENABLE_CC_UTF8_COMPAT.get()) {
            return;
        }

        var textBytes = ccUtf8$copyBytes(text);
        var codepoints = ccUtf8$decodeUnits(textBytes);
        var offsets = ccUtf8$getUnitOffsets(textBytes);

        var textColourLength = textColour.remaining();
        var backgroundColourLength = backgroundColour.remaining();

        var validTextColourLength = textColourLength == textBytes.length || textColourLength == codepoints.length;
        var validBackgroundColourLength = backgroundColourLength == textBytes.length || backgroundColourLength == codepoints.length;

        if (!validTextColourLength || !validBackgroundColourLength) {
            throw new LuaException("Arguments must be the same length");
        }

        var terminal = getTerminal();

        synchronized (terminal) {
            var cursorX = terminal.getCursorX();
            var cursorY = terminal.getCursorY();

            if (cursorY >= 0 && cursorY < terminal.getHeight()) {
                var textLine = terminal.getLine(cursorY);
                var textColourLine = terminal.getTextColourLine(cursorY);
                var backgroundColourLine = terminal.getBackgroundColourLine(cursorY);
                var textAccess = (CcUtf8TextBufferAccess) (Object) textLine;

                for (var i = 0; i < codepoints.length; i++) {
                    var x = cursorX + i;

                    if (x < 0 || x >= terminal.getWidth()) {
                        continue;
                    }

                    var byteIndex = offsets[i];

                    textAccess.ccUtf8$setCodePoint(x, codepoints[i]);
                    textColourLine.setChar(x, ccUtf8$getColour(textColour, byteIndex, i, textBytes.length, codepoints.length));
                    backgroundColourLine.setChar(x, ccUtf8$getColour(backgroundColour, byteIndex, i, textBytes.length, codepoints.length));
                }
            }

            terminal.setCursorPos(cursorX + codepoints.length, cursorY);
            terminal.setChanged();
        }

        ci.cancel();
    }
}
