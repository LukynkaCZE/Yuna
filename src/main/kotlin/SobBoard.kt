import cz.lukynka.prettylog.LogType
import cz.lukynka.prettylog.log
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.embed

val sobbedMessages: MutableMap<Message, String> = mutableMapOf()

object SobBoard {

    suspend fun addMessage(message: Message) {
        val footer = StringBuilder()
        val descriptionString = StringBuilder()
        message.reactions.forEach { reaction ->
            if (sobEmojis.contains(reaction.emoji.name)) footer.append("${reaction.count} ${reaction.emoji.name}")
        }

        if (message.messageReference?.message != null) {
            val referencedMessage = message.messageReference!!.message!!.asMessage()
            descriptionString.appendLine("> Reply to ${referencedMessage.author?.username}")
            descriptionString.appendLine("> [${referencedMessage.content}](${getMessageLink(referencedMessage)})")
        }

        if (message.embeds.isEmpty()) {
            descriptionString.append(message.content)
        } else {
            var descriptor = message.content
            for (embed in message.embeds) {
                descriptor = descriptor.replace(embed.url!!, "${clickableButton("gif", embed.video!!.url!!)}", true)
            }
            descriptionString.appendLine(descriptor)
        }
        for (attachment in message.attachments) {
            if (!attachment.isImage) descriptionString.appendLine(clickableButton("video", attachment.url))
        }

        val boardMessage = sobChannel.createMessage {
            content = getMessageLink(message)
            embed {
                url = "https://example.com/"
                description = descriptionString.toString()
                author {
                    name = message.author?.username
                    icon = message.author?.avatar?.cdnUrl?.toUrl()
                }
                footer {
                    text = footer.toString()

                }
            }
            for (attachment in message.attachments) {
                if (attachment.isImage) {
                    embed {
                        url = "https://example.com/"
                        image = attachment.url
                    }
                }
            }
        }
        sobbedMessages[boardMessage] = getMessageLink(message)
    }

    suspend fun getMessages() {
        sobChannel.messages.collect { boardMessage ->
            if (boardMessage.author?.isBot == false || boardMessage.content.isEmpty()) return@collect
            val originalMessage = getMessageFromLink(boardMessage.content)
            sobbedMessages[boardMessage] = getMessageLink(originalMessage)
        }
        updateBoard()
    }

    private suspend fun updateBoard() {
        sobbedMessages.forEach { (_, message) ->
            val originalMessage: Message = getMessageFromLink(message)
            updateMessageFromMessage(originalMessage)
        }
    }

    suspend fun updateMessageFromMessage(originalMessage: Message) {
        val boardMessage = sobbedMessages.entries.find { it.value == getMessageLink(originalMessage) }?.key
        val footer = StringBuilder()
        val descriptionString = StringBuilder()
        originalMessage.reactions.forEach { reaction ->
            if (sobEmojis.contains(reaction.emoji.name)) footer.append("${reaction.count} ${reaction.emoji.name}")
        }

        if (originalMessage.messageReference?.message != null) {
            val referencedMessage = originalMessage.messageReference!!.message!!.asMessage()
            descriptionString.appendLine("> Reply to ${referencedMessage.author?.username}")
            descriptionString.appendLine("> [${referencedMessage.content}](${getMessageLink(referencedMessage)})\n")
        }

        if (originalMessage.embeds.isEmpty()) {
            descriptionString.append(originalMessage.content)
        } else {
            var descriptor = originalMessage.content
            for (embed in originalMessage.embeds) {
                descriptor = descriptor.replace(embed.url!!, "${clickableButton("gif", embed.video!!.url!!)}", true)
            }
            descriptionString.appendLine(descriptor)
        }
        for (attachment in originalMessage.attachments) {
            if (!attachment.isImage) descriptionString.appendLine(clickableButton("video", attachment.url))
        }

        boardMessage?.edit {
            embed {
                url = "https://example.com/"
                description = descriptionString.toString()
                author {
                    name = originalMessage.author?.username
                    icon = originalMessage.author?.avatar?.cdnUrl?.toUrl()
                }
                footer {
                    text = footer.toString()
                }
            }
            for (attachment in originalMessage.attachments) {
                if (attachment.isImage) {
                    embed {
                        url = "https://example.com/"
                        image = attachment.url
                    }
                }
            }
        }

    }
}