package slooth.lowhealth;

import slooth.lowhealth.WarningConfig;

public final class WarningState {
    private boolean active;
    private int soundCooldown;

    public static boolean shouldWarn(WarningConfig config, float health, float absorption, boolean alive, boolean spectator, boolean creative) {
        if (!config.warningEnabled) return false;
        if (!alive) return false;
        if (!(health > 0.0f)) return false;
        if (spectator) return false;
        if (creative) {
            if (!config.warnInCreative) return false;
        }
        float f = config.includeAbsorption ? absorption : 0.0f;
        if (!(health + f <= (float)config.warningThreshold)) return false;
        return true;
    }

    public boolean tick(boolean shouldWarn, boolean soundEnabled, int intervalSeconds) {
        if (!shouldWarn) {
            this.reset();
            return false;
        }
        this.active = true;
        if (!soundEnabled) {
            this.soundCooldown = 0;
            return false;
        }
        if (this.soundCooldown > 0) {
            --this.soundCooldown;
        }
        if (this.soundCooldown == 0) {
            this.soundCooldown = Math.max(1, intervalSeconds) * 20;
            return true;
        }
        return false;
    }

    public boolean isActive() {
        return this.active;
    }

    public void reset() {
        this.active = false;
        this.soundCooldown = 0;
    }
}

