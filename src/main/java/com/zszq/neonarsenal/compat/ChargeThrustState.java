package com.zszq.neonarsenal.compat;

/**
 * Client-side flags for the mounted charge thrust, read by the Better Combat attack gates.
 *
 * <p>Kept separate from {@link FallSlamState} so the two long-press skills never fight over the same
 * flags; the gates combine both.</p>
 */
public final class ChargeThrustState {

    /** True from the first held tick until the charge animation has fully faded out. */
    public static boolean blockAttacks;

    /** Allows exactly the one manually started charge-thrust animation through the client filter. */
    public static boolean allowAnimation;

    /** Allows the short-tap normal attack through the attack gates. */
    public static boolean allowNormalAttack;

    private ChargeThrustState() {
    }
}
