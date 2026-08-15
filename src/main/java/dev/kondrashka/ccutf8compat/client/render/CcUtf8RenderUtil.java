package dev.kondrashka.ccutf8compat.client.render;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

import org.joml.Matrix4f;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Draws UTF-8 terminal glyphs so that wide characters (such as CJK, whose glyph advance is
 * roughly twice a terminal cell) are scaled horizontally to fit CC:Tweaked's fixed-width cells
 * instead of overflowing into neighbouring cells.
 * <p>
 * The glyphs are rendered slightly wider than a single cell (see {@link #WIDE_GLYPH_SCALE}) so
 * that CJK strokes stay readable, with the small overflow centred over the cell. Only the
 * horizontal scale is applied, anchored at the glyph's draw origin. MC glyphs are already one
 * cell tall, and their local origin sits on the baseline, so scaling vertically or centering on
 * the cell would shift glyphs out of the terminal.
 */
public final class CcUtf8RenderUtil {

    /**
     * How much wider than a terminal cell wide glyphs are drawn. 1.0 fits exactly one cell;
     * values above 1.0 overflow slightly into the neighbouring cells to keep CJK glyphs legible.
     */
    private static final float WIDE_GLYPH_SCALE = 1.2f;

    private CcUtf8RenderUtil() {
    }

    /**
     * Draw a single terminal cell's glyph in world/screen space (used by renderers that draw
     * through a {@link MultiBufferSource}).
     *
     * @param font   The Minecraft font.
     * @param text   The glyph text to draw.
     * @param cellX  The left edge of the terminal cell (in terminal pixel space).
     * @param cellY  The top edge of the terminal cell (in terminal pixel space).
     * @param colour The ARGB colour of the text.
     * @param parent The parent matrix stack pose.
     * @param buffer The buffer source to draw into.
     * @param light  The packed lightmap coordinates.
     */
    public static void drawGlyph(
            Font font,
            String text,
            float cellX,
            float cellY,
            int colour,
            Matrix4f parent,
            MultiBufferSource buffer,
            int light) {
        var glyphWidth = font.width(text);
        var scale = glyphWidth > FONT_WIDTH ? FONT_WIDTH * WIDE_GLYPH_SCALE / (float) glyphWidth : 1.0f;
        if (scale == 1.0f) {
            font.drawInBatch(
                    text,
                    cellX,
                    cellY,
                    colour,
                    false,
                    parent,
                    buffer,
                    Font.DisplayMode.NORMAL,
                    0,
                    light);
            return;
        }

        // Centre the slightly-wider glyph over the cell: shift the anchor left by half the overflow.
        var anchorX = cellX - (FONT_WIDTH * WIDE_GLYPH_SCALE - FONT_WIDTH) / 2.0f;

        // Scale horizontally about the glyph's draw origin. The vertical axis is left untouched,
        // as MC glyphs are already one cell tall.
        var matrix = new Matrix4f(parent);
        matrix.translate(anchorX, cellY, 0.0f);
        matrix.scale(scale, 1.0f, 1.0f);
        matrix.translate(-anchorX, -cellY, 0.0f);

        font.drawInBatch(
                text,
                anchorX,
                cellY,
                colour,
                false,
                matrix,
                buffer,
                Font.DisplayMode.NORMAL,
                0,
                light);
    }

    /**
     * Draw a single terminal cell's glyph in GUI space (used by widgets drawing with
     * {@link GuiGraphics}).
     *
     * @param graphics The GUI graphics context.
     * @param font     The Minecraft font.
     * @param text     The glyph text to draw.
     * @param cellX    The left edge of the terminal cell (in GUI pixel space).
     * @param cellY    The top edge of the terminal cell (in GUI pixel space).
     * @param colour   The ARGB colour of the text.
     */
    public static void drawGlyph(
            GuiGraphics graphics,
            Font font,
            String text,
            float cellX,
            float cellY,
            int colour) {
        var glyphWidth = font.width(text);
        var scale = glyphWidth > FONT_WIDTH ? FONT_WIDTH * WIDE_GLYPH_SCALE / (float) glyphWidth : 1.0f;
        if (scale == 1.0f) {
            font.drawInBatch(
                    text,
                    cellX,
                    cellY,
                    colour,
                    false,
                    graphics.pose().last().pose(),
                    graphics.bufferSource(),
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880);
            return;
        }

        // Centre the slightly-wider glyph over the cell: shift the anchor left by half the overflow.
        var anchorX = cellX - (FONT_WIDTH * WIDE_GLYPH_SCALE - FONT_WIDTH) / 2.0f;

        // Scale horizontally about the glyph's draw origin (anchorX, cellY). The draw origin is
        // passed through to drawInBatch so that the glyph's local vertex coordinates are offset
        // by (anchorX, cellY) before the scale is applied.
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(anchorX, cellY, 0.0f);
        pose.scale(scale, 1.0f, 1.0f);
        pose.translate(-anchorX, -cellY, 0.0f);

        font.drawInBatch(
                text,
                anchorX,
                cellY,
                colour,
                false,
                pose.last().pose(),
                graphics.bufferSource(),
                Font.DisplayMode.NORMAL,
                0,
                15728880);

        pose.popPose();
    }
}
