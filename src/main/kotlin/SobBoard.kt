import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.embed

val sobbedMessages: MutableMap<Message, String> = mutableMapOf()

object SobBoard {

    suspend fun addMessage(message: Message) {
        val descriptionString = StringBuilder()
        val footer = message.reactions
            .filter {
                sobEmojis.contains(it.emoji.name)
            }
            .joinToString(" ") { reaction ->
                "${reaction.count} ${reaction.emoji.name}"
            }

        if (message.embeds.isEmpty()) {
            descriptionString.append(message.content)
        } else {
            descriptionString.append(message.embeds[0].description)
        }
        for (attachment in message.attachments) {
            if (!attachment.isImage) descriptionString.appendLine("${clickableButton("video", attachment.url)}\n")
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
                    text = footer
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
        val descriptionString = StringBuilder()

        val footer = originalMessage.reactions
            .filter {
                sobEmojis.contains(it.emoji.name)
            }
            .joinToString(" ") { reaction ->
                "${reaction.count} ${reaction.emoji.name}"
            }

        if (originalMessage.embeds.isEmpty()) {
            descriptionString.append(originalMessage.content)
        } else {
            descriptionString.append(originalMessage.embeds[0].description)
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
                    text = footer
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
