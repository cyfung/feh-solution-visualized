import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.VfsFile

class FehArtCache(private val baseFolder: VfsFile) {
    private val cache = mutableMapOf<String, BitmapSlice<Bitmap>>()

    suspend fun get(name: String): BitmapSlice<Bitmap> {
        val cachedValue = cache[name]
        return if (cachedValue == null) {
            val folder = baseFolder[name]
            val value = if (folder.isDirectory()) {
                folder["Face_FC.png"].readBitmap().slice()
            } else {
                baseFolder[",GENERIC UNITS"][name]["BtlFace_BU.png"].readBitmap().sliceWithSize(88,
                        0, 300, 300)
            }
            cache[name] = value
            value
        } else {
            cachedValue
        }
    }
}