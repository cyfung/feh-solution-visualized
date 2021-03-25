import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import me.kumatheta.feh.message.AttackType
import me.kumatheta.feh.message.MoveType

class Resources private constructor(
        val tree: Bitmap,
        val defenseTile: Bitmap,
        val defenseTileTrench: Bitmap,
        val flyingTerrain: Bitmap,
        val trench: Bitmap,
        val wall: BitmapSlice<Bitmap>,
        val obstacle1: BitmapSlice<Bitmap>,
        val obstacle2: BitmapSlice<Bitmap>,
        val moveTypeBitmaps: Map<MoveType, BitmapSlice<Bitmap>>,
        val attackTypeBitmaps: Map<AttackType, BitmapSlice<Bitmap>>
) {

    companion object {
        suspend fun load(): Resources {
            val tree = resourcesVfs["tree.png"].readBitmap()
            val defenseTile = resourcesVfs["defense_tile.png"].readBitmap()
            val defenseTileTrench = resourcesVfs["defense_tile_trench.png"].readBitmap()
            val flyingTerrain = resourcesVfs["flying_terrain.png"].readBitmap()
            val trench = resourcesVfs["trench.png"].readBitmap()
            val walls = resourcesVfs["walls.png"].readBitmap()
            val wall = walls.sliceWithSize(1, 1, 180, 180)
            val obstacle2 = walls.sliceWithSize(183, 1, 180, 180)
            val obstacle1 = walls.sliceWithSize(365, 1, 180, 180)
            val statusBitmap = resourcesVfs["Status.png"].readBitmap()
            val moveTypeBitmaps = mapOf(
                    MoveType.INFANTRY to statusBitmap.sliceWithSize(1842, 2, 51, 50),
                    MoveType.ARMORED to statusBitmap.sliceWithSize(1842, 58, 51, 50),
                    MoveType.CAVALRY to statusBitmap.sliceWithSize(1842, 114, 51, 50),
                    MoveType.FLIER to statusBitmap.sliceWithSize(1842, 170, 51, 50)
            )
            val attackTypeBitmaps = AttackType.values().asSequence().associateWith {
                statusBitmap.sliceWithSize(it.ordinal * 56 + 2, 354, 52, 52)
            }
            return Resources(
                    tree = tree,
                    defenseTile = defenseTile,
                    defenseTileTrench = defenseTileTrench,
                    flyingTerrain = flyingTerrain,
                    trench = trench,
                    wall = wall,
                    obstacle1 = obstacle1,
                    obstacle2 = obstacle2,
                    moveTypeBitmaps = moveTypeBitmaps,
                    attackTypeBitmaps = attackTypeBitmaps
            )
        }
    }
}