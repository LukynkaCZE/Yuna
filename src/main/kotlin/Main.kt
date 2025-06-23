import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.LoggerSettings
import cz.lukynka.prettylog.LoggerStyle
import cz.lukynka.prettylog.log
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.embed
import embeds.Projects
import embeds.RolePicker
import embeds.Rules

lateinit var kord: Kord
lateinit var sobChannel: TextChannel
val version = Resources.getVersion()
val guildId = Snowflake(Environment.GUILD_ID)
val memberRole = Snowflake(Environment.MEMBER_ROLE)
val sobEmojis = listOf("🏳️‍🌈", "😭", "🏳️‍⚧️")

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    LoggerSettings.loggerStyle = LoggerStyle.BRACKET_PREFIX
    log("Loading Yuna..", LogType.DEBUG)
    log("Authenticating to Discord..", LogType.NETWORK)
    kord = Kord(Environment.DISCORD_TOKEN)
    sobChannel = kord.getGuild(guildId).getChannel(Snowflake(Environment.SOB_BOARD_CHANNEL)).asChannelOf<TextChannel>()

    Commands.register()
    Projects().register()
    RolePicker().register()
    Rules().register()
    SobBoard.getMessages()

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val response = interaction.deferPublicResponse()
        val command = interaction.command
        log("Received command /${command.rootName}")

        Commands.handle(command, response)
    }

    kord.on<MemberJoinEvent> {
        member.addRole(memberRole)
    }

    kord.on<ReactionAddEvent> {
        val message = getMessage()
        if(!sobEmojis.contains(emoji.name) || message.author?.isBot == true) return@on
        if(sobbedMessages.containsValue(getMessageLink(message))) {
            SobBoard.updateMessageFromMessage(message)
        } else {
            message.reactions.forEach { reaction ->
                if (sobEmojis.contains(emoji.name) && reaction.data.count >= Environment.SOB_BOARD_REQUIREMENT) {
                    SobBoard.addMessage(message)
                }
            }
        }
    }

    kord.on<MessageUpdateEvent> {
        val message = getMessage()
        if(sobbedMessages.containsValue(getMessageLink(message))) SobBoard.updateMessageFromMessage(message)
    }

    kord.on<ReactionRemoveEvent> {
        val message = getMessage()
        if(!sobEmojis.contains(emoji.name) || message.author?.isBot == true) return@on
        if(sobbedMessages.containsValue(getMessageLink(message))) SobBoard.updateMessageFromMessage(message)
    }

    val rolesChannel = kord.getGuild(guildId).getChannel(Snowflake(1249503869608788050)).asChannelOf<TextChannel>()
    kord.on<ButtonInteractionCreateEvent> {

        if(interaction.channelId != rolesChannel.id) return@on

        val guild = kord.getGuild(guildId)
        val role = when(interaction.componentId) {
            "role-tester" -> guild.getRole(Snowflake(1249425620216320124))
            "role-ping-twitch" -> guild.getRole(Snowflake(1249426163881873538))
            "role-ping-updates" -> guild.getRole(Snowflake(1249426247260311583))
            "role-ping-dockyard" -> guild.getRole(Snowflake(1272642832242315306))
            else -> null
        }

        var responseColor = Color(243, 139, 168)
        var responseMessage = "Something went wrong: button with id ${interaction.componentId} does not have role assigned"

        if(role == null) {
            interaction.respondEphemeral {
                embed { title = responseMessage; color = responseColor }
            }
        }

        val member = interaction.user.asMember(guildId)
        val hasRole = member.roleIds.contains(role!!.id)
        if(!hasRole) {
            responseMessage = "Role `${role.name}` has been added to you!"
            responseColor = Color(166, 227, 161)
            member.addRole(role.id)
        } else {
            responseMessage = "Role `${role.name}` has been removed from you!"
            responseColor = Color(250, 179, 135)
            member.removeRole(role.id)
        }

        interaction.respondEphemeral {
            embed { title = responseMessage; color = responseColor }
        }
    }

    kord.login {
        presence { playing("Version ${version.version}") }
        intents += Intent.MessageContent
        log("Yuna running, version ${version.version} (git-${version.gitCommit}@${version.gitBranch})", LogType.SUCCESS)
    }
}

data class YunaVersionInfo(
    val version: String,
    val gitBranch: String,
    val gitCommit: String,
)
