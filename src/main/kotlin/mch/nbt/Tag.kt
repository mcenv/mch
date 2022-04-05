package mch.nbt

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

sealed class Tag(val type: TagType)
object EndTag : Tag(TagType.END)
data class ByteTag(val data: Byte) : Tag(TagType.BYTE)
data class ShortTag(val data: Short) : Tag(TagType.SHORT)
data class IntTag(val data: Int) : Tag(TagType.INT)
data class LongTag(val data: Long) : Tag(TagType.LONG)
data class FloatTag(val data: Float) : Tag(TagType.FLOAT)
data class DoubleTag(val data: Double) : Tag(TagType.DOUBLE)
data class ByteArrayTag(val data: List<Byte>) : Tag(TagType.BYTE_ARRAY), List<Byte> by data
data class StringTag(val data: String) : Tag(TagType.STRING)
data class ListTag(val tags: List<Tag>) : Tag(TagType.LIST), List<Tag> by tags
data class CompoundTag(val tags: Map<String, Tag>) : Tag(TagType.COMPOUND), Map<String, Tag> by tags
data class IntArrayTag(val data: List<Int>) : Tag(TagType.INT_ARRAY), List<Int> by data
data class LongArrayTag(val data: List<Long>) : Tag(TagType.LONG_ARRAY), List<Long> by data

enum class TagType {
    END,
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BYTE_ARRAY,
    STRING,
    LIST,
    COMPOUND,
    INT_ARRAY,
    LONG_ARRAY,
}

fun writeRootTag(path: Path, tag: CompoundTag): Unit =
    DataOutputStream(BufferedOutputStream(GZIPOutputStream(Files.newOutputStream(path)))).use { it.writeTag(CompoundTag(mapOf("" to tag))) }

fun DataOutput.writeTag(tag: Tag): Unit = when (tag) {
    is EndTag -> Unit
    is ByteTag -> writeByte(tag.data.toInt())
    is ShortTag -> writeShort(tag.data.toInt())
    is IntTag -> writeInt(tag.data)
    is LongTag -> writeLong(tag.data)
    is FloatTag -> writeFloat(tag.data)
    is DoubleTag -> writeDouble(tag.data)
    is ByteArrayTag -> {
        writeInt(tag.data.size)
        write(tag.data.toByteArray())
    }
    is StringTag -> writeUTF(tag.data)
    is ListTag -> {
        writeTagType(tag.tags.firstOrNull()?.type ?: TagType.END)
        writeInt(tag.tags.size)
        tag.tags.forEach { writeTag(it) }
    }
    is CompoundTag -> {
        tag.tags.forEach { (name, tag) ->
            writeTagType(tag.type)
            writeUTF(name)
            writeTag(tag)
        }
        writeTagType(TagType.END)
    }
    is IntArrayTag -> {
        writeInt(tag.data.size)
        tag.data.forEach { writeInt(it) }
    }
    is LongArrayTag -> {
        writeInt(tag.data.size)
        tag.data.forEach { writeLong(it) }
    }
}

fun DataOutput.writeTagType(type: TagType): Unit = writeByte(type.ordinal)

fun readRootTag(path: Path): CompoundTag {
    val root = DataInputStream(BufferedInputStream(GZIPInputStream(Files.newInputStream(path)))).use { it.readTag() } as CompoundTag
    return root[""]!! as CompoundTag
}

fun DataInput.readTag(): Tag = when (readTagType()) {
    TagType.END -> EndTag
    TagType.BYTE -> ByteTag(readByte())
    TagType.SHORT -> ShortTag(readShort())
    TagType.INT -> IntTag(readInt())
    TagType.LONG -> LongTag(readLong())
    TagType.FLOAT -> FloatTag(readFloat())
    TagType.DOUBLE -> DoubleTag(readDouble())
    TagType.BYTE_ARRAY -> {
        val size = readInt()
        val data = ByteArray(size).also { readFully(it) }
        ByteArrayTag(data.toList())
    }
    TagType.STRING -> StringTag(readUTF())
    TagType.LIST -> {
        readTagType()
        val size = readInt()
        val tags = (0 until size).map { readTag() }
        ListTag(tags)
    }
    TagType.COMPOUND -> {
        val tags = mutableMapOf<String, Tag>()
        while (TagType.END != readTagType()) {
            val name = readUTF()
            val tag = readTag()
            tags[name] = tag
        }
        CompoundTag(tags)
    }
    TagType.INT_ARRAY -> {
        val size = readInt()
        val data = (0 until size).map { readInt() }
        IntArrayTag(data)
    }
    TagType.LONG_ARRAY -> {
        val size = readInt()
        val data = (0 until size).map { readLong() }
        LongArrayTag(data)
    }
}

fun DataInput.readTagType(): TagType = TagType.values()[readByte().toInt()]
