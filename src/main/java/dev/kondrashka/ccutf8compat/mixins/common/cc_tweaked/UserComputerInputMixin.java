package dev.kondrashka.ccutf8compat.mixins.common.cc_tweaked;

import java.nio.ByteBuffer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dan200.computercraft.core.input.ComputerInput;
import dan200.computercraft.core.input.UserComputerInput;

import dev.kondrashka.ccutf8compat.access.CcUtf8ClientInputAccess;
import dev.kondrashka.ccutf8compat.config.CcUtf8CompatConfig;

/**
 * Bypasses CC:Tweaked's legacy character and clipboard validation for codepoints above 255.
 * <p>
 * On the client this forwards Unicode input through the {@link CcUtf8ClientInputAccess} network
 * path, and on both sides it lets UTF-8 encoded clipboard bytes through {@link UserComputerInput}'s
 * {@code isValidClipboard} gate.
 */

@Mixin(value = UserComputerInput.class, remap = false)
public class UserComputerInputMixin {

    @Shadow
    @Final
    private ComputerInput delegate;

    @Inject(method = "codepointTyped", at = @At("HEAD"), cancellable = true, remap = false)
    private void ccUtf8$codepointTyped(int codepoint, CallbackInfo ci) {
        if (!CcUtf8CompatConfig.ENABLE_CC_UTF8_COMPAT.get()) {
            return;
        }

        if (codepoint >= 0 && codepoint <= 255) {
            return;
        }

        if (delegate instanceof CcUtf8ClientInputAccess input) {
            input.ccUtf8$charTypedCodepoint(codepoint);
            ci.cancel();
        }
    }

    @Inject(method = "paste(Ljava/nio/ByteBuffer;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void ccUtf8$pasteUtf8(ByteBuffer contents, CallbackInfo ci) {
        if (!CcUtf8CompatConfig.ENABLE_CC_UTF8_COMPAT.get()) {
            return;
        }

        if (contents.remaining() > 0) {
            delegate.paste(contents);
        }

        ci.cancel();
    }
}
