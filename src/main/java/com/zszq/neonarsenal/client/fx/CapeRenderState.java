package com.zszq.neonarsenal.client.fx;

/**
 * Render-thread flag used to keep Better Combat's animation library from taking over the cape.
 *
 * <p>The Player Animator Library (used by Better Combat) attaches the vanilla cape to the animated
 * and bent torso while an animation is active, which makes the cape stick to the back instead of
 * rendering like vanilla. While {@link #suppressPalCape} is set (only during {@code CapeLayer.render}
 * when FancyPlayerAnimation is absent), the library's {@code AnimationProcessor#isActive()} reports
 * false so its cape transform is skipped and the cape renders exactly like vanilla.</p>
 */
public final class CapeRenderState {

    public static boolean suppressPalCape;

    private CapeRenderState() {
    }

    /** Starts the vanilla-cape window (called at the head of every CapeLayer render mixin). */
    public static void beginCapeRender() {
        suppressPalCape = !FpaBend.available();
    }

    /** Ends the vanilla-cape window. */
    public static void endCapeRender() {
        suppressPalCape = false;
    }
}
