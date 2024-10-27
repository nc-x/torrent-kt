import bencode.decode
import bencode.encode
import bencode.toBytes
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.security.MessageDigest

data class Torrent(
    val announce: String,
    val info: Info,
) {
    private lateinit var metadata: Map<*, *>

    @OptIn(ExperimentalStdlibApi::class)
    val infoHash: String
        get() {
            val info = encode(metadata["info"]!!).toBytes()
            return MessageDigest.getInstance("SHA-1").digest(info)
                .toHexString()
        }

    companion object {
        private val gson = Gson()

        fun from(file: String): Torrent {
            return from(File(file))
        }

        fun from(file: File): Torrent {
            val decoded = decode(file.readBytes()) as Map<*, *>

            val json = gson.toJson(decoded)
            val torrent = gson.fromJson(json, Torrent::class.java) ?: error("invalid torrent")
            torrent.metadata = decoded
            return torrent
        }
    }
}

data class Info(
    val name: String,
    @SerializedName("piece length")
    val pieceLength: Long,
    val pieces: String,
    val length: Long,
) {
    @OptIn(ExperimentalStdlibApi::class)
    val pieceHashes: Sequence<String>
        get() {
            val nPieces = pieces.length / 20
            return sequence {
                for (i in 0..<nPieces) {
                    val piece = pieces.substring(i * 20..<(i + 1) * 20)
                    yield(piece.toBytes().toHexString())
                }
            }
        }
}