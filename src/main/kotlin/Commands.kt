import commands.VersionCommand
import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.log
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.InteractionCommand

object Commands {
    suspend fun register() {
        VersionCommand().register()
    }

    suspend fun handle(command: InteractionCommand, response: DeferredPublicMessageInteractionResponseBehavior) {
        when (command.rootName) {
            "version" -> VersionCommand.handle(command, response)
            else -> {
                log("Command /${command.rootName} does not have a handler. Deleting...", LogType.WARNING)
                delete(command.rootId)

                response.respond {
                    // little do they know the command doesn't exist anymore
                    content = "Umm... try again :3"
                }
            }
        }
    }

    private suspend fun delete(command: Snowflake) {
        kord.rest.interaction.deleteGuildApplicationCommand(kord.selfId, guildId, command)
    }
}