package com.zszq.neonarsenal.compat;

/**
 * Common state written by the client falling-slash controller and read by the Better Combat hooks.
 *
 * <p>Every flag below is only ever set on the client; the server only reacts to the
 * {@link FallSlamWindupPayload} by toggling Better Combat's attack channel.</p>
 */
public final class FallSlamState {

    /**
     * True from the first held tick of a long fall until the slam animation has fully faded out.
     *
     * <p>While set, Better Combat attack animations, swing sounds and slash trails are filtered and
     * the held-attack path is blocked.</p>
     */
    public static boolean waitingForSlam;

    /** True while attacks must be blocked (held fall, windup, committed slam and landing lock). */
    public static boolean blockAttacks;

    /** True during the final-frame 0.3 s fade and its matching 0.3 s attack lock. */
    public static boolean attackLock;

    /** Allows a short tap during a long fall to perform one normal attack on release. */
    public static boolean allowNormalAttack;

    /** Allows exactly the one manually started slam animation through the client animation filter. */
    public static boolean allowAnimation;

    private FallSlamState() {
    }
}
