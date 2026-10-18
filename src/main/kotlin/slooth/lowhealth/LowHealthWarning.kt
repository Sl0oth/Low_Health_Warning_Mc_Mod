package slooth.lowhealth

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object LowHealthWarning : ModInitializer {
    private val logger = LoggerFactory.getLogger("lowhelthwarning")

    override fun onInitialize() {
        logger.info("Low Health Warning initialized.")
    }
}
