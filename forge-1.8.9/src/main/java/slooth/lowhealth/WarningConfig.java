package slooth.lowhealth;

public final class WarningConfig {
    public boolean warningEnabled = true;
    public int warningThreshold = 8;
    public int displayMode = 0;
    public String warningMessage = "\u00a7cLow Health!";
    public boolean soundEnabled = false;
    public float soundVolume = 0.5f;
    public int soundIntervalSeconds = 3;
    public boolean includeAbsorption = false;
    public boolean warnInCreative = false;

    public WarningConfig normalized() {
        this.warningThreshold = this.warningThreshold < 1 ? 8 : Math.min(this.warningThreshold, 40);
        if (this.displayMode < 0 || this.displayMode > 2) {
            this.displayMode = 0;
        }
        if (this.warningMessage == null || this.warningMessage.trim().isEmpty()) {
            this.warningMessage = "\u00a7cLow Health!";
        }
        this.warningMessage = this.warningMessage.replace('\n', ' ').replace('\r', ' ');
        if (this.warningMessage.length() > 120) {
            this.warningMessage = this.warningMessage.substring(0, 120);
        }
        this.soundVolume = Float.isFinite(this.soundVolume) ? Math.max(0.0f, Math.min(this.soundVolume, 1.0f)) : 0.5f;
        this.soundIntervalSeconds = Math.max(1, Math.min(this.soundIntervalSeconds, 10));
        return this;
    }

    public WarningConfig copy() {
        WarningConfig c = new WarningConfig();
        c.warningEnabled = this.warningEnabled;
        c.warningThreshold = this.warningThreshold;
        c.displayMode = this.displayMode;
        c.warningMessage = this.warningMessage;
        c.soundEnabled = this.soundEnabled;
        c.soundVolume = this.soundVolume;
        c.soundIntervalSeconds = this.soundIntervalSeconds;
        c.includeAbsorption = this.includeAbsorption;
        c.warnInCreative = this.warnInCreative;
        return c;
    }
}

