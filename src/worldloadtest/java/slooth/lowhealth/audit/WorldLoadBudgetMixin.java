package slooth.lowhealth.audit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Test fixture only: allow slow CI spawn generation without retrying failed assertions. */
@Mixin(targets = "net.fabricmc.fabric.impl.client.gametest.util.ClientGameTestImpl", remap = false)
public abstract class WorldLoadBudgetMixin {
    @ModifyConstant(method = "waitForWorldLoad", constant = @Constant(intValue = 1200), require = 1)
    private static int worldLoadBudget(int original) {
        return 6000;
    }
}
