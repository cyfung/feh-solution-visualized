import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.klock.toTimeString
import com.soywiz.korge.Korge
import com.soywiz.korge.font.readBitmapFontWithMipmaps
import com.soywiz.korge.html.Html
import com.soywiz.korge.input.mouse
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.ui.UITextButton
import com.soywiz.korge.ui.uiComboBox
import com.soywiz.korge.ui.uiScrollableArea
import com.soywiz.korge.ui.uiTextButton
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.FixedSizeContainer
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.View
import com.soywiz.korge.view.centered
import com.soywiz.korge.view.container
import com.soywiz.korge.view.fixedSizeContainer
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.text
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launch
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.plus
import com.soywiz.korma.interpolation.Easing
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.utils.io.core.use
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.serialization.protobuf.ProtoBuf
import me.kumatheta.feh.message.Action
import me.kumatheta.feh.message.AttackType
import me.kumatheta.feh.message.BattleMapPosition
import me.kumatheta.feh.message.MoveSet
import me.kumatheta.feh.message.MoveType
import me.kumatheta.feh.message.SetupInfo
import me.kumatheta.feh.message.Terrain
import me.kumatheta.feh.message.UnitAdded
import kotlin.math.absoluteValue

private val DefaultTextColor = Colors["#0a2533"]

private const val JobUrl = "http://localhost:8080/job"

suspend fun main() = Korge(title = "FEH Solution Visualizer", width = 1350, height = 900,
        bgcolor = Colors["#3e3e3e"]) {
    val center = Point(width / 2, height / 2)
    image(resourcesVfs["book4.png"].readBitmap()) {
        centered
        position(center)
        colorMul = Colors["#2b2b2b"]
    }

    val resources = Resources.load()
    val playArea = createPlayArea(
            topLeft = Point(20, 150)
    )
    val backgroundContainer = playArea.container()

    val font = resourcesVfs["telegrama.fnt"].readBitmapFontWithMipmaps()
    val startButton = simpleButton(font, 20.0, 30.0, "PLAY")
    val stopButton = simpleButton(font, startButton.x + 110, startButton.y, "STOP")
    val restartButton = simpleButton(font, stopButton.x + 110, stopButton.y, "RESTART")
    uiComboBox(
            width = 100,
            height = 40,
            items = listOf(1.5, 1, 1.2, 2, 0.7),
            verticalScroll = false,
            textFont = Html.FontFace.Bitmap(font)
    ) {
        forEachChildren {
            setTextLook(it)
        }
        visible = false
    }
    val textFont = resourcesVfs["roboto28.fnt"].readBitmapFontWithMipmaps()

    val stepAreaX = 20 + playArea.width + 30
    val stepsArea = fixedSizeContainer(width = 720, height = height - 60) {
        position(stepAreaX, 30)
    }
    val infoArea = container {
        position(20, 80)
    }

    val squares = (0..5).asSequence().flatMap { xIndex ->
        (7 downTo 0).asSequence().map { yIndex ->
            Position(xIndex, yIndex)
        }
    }.associateWith { Point(it.x * 90, 720 - 90 - it.y * 90) }

    val unitContainers = squares.asSequence().associate {
        it.key to playArea.container {
            position(it.value)
        }
    }

    val activeContainer = playArea.container()

    val jobRef = korAtomic<Job?>(null)
    val startNewJob = { newJob: CompletableJob, forceRestart: Boolean ->
        startNewJob(
                squares = squares,
                unitContainers = unitContainers,
                font = font,
                activeContainer = activeContainer,
                backgroundContainer = backgroundContainer,
                resources = resources,
                stepsArea = stepsArea,
                infoArea = infoArea,
                jobRef = jobRef,
                newJob = newJob,
                startButton = startButton,
                forceRestart = forceRestart,
                textFont = textFont
        )
    }

    startButton.mouse {
        onClick {
            val currentJob = jobRef.value
            if (currentJob == null) {
                val newJob = Job()
                val success = jobRef.compareAndSet(expect = null, update = newJob)
                if (success) {
                    startButton.text = "PAUSE"
                    startNewJob(newJob, false)
                }
            } else {
                currentJob.cancel()
            }
        }
    }

    restartButton.mouse {
        onClick {
            val newJob = Job()
            val currentJob = jobRef.getAndSet(newJob)
            currentJob?.cancel()
            startButton.text = "PAUSE"
            startNewJob(newJob, true)
        }
    }


    stopButton.mouse {
        onClick {
            launch {
                jobRef.value?.cancel()
                HttpClient().use {
                    it.delete<ByteArray>(JobUrl)
                }
            }
        }
    }
}

private fun setTextLook(view: View) {
    when (view) {
        is UITextButton -> {
            view.textColor = DefaultTextColor
            view.shadowVisible = false
            view.textSize = 22
        }
        is Container -> {
            view.forEachChildren {
                setTextLook(it)
            }
        }
    }
}

private fun Stage.startNewJob(
        squares: Map<Position, Point>,
        unitContainers: Map<Position, Container>,
        font: BitmapFont, activeContainer: Container,
        backgroundContainer: Container, resources: Resources,
        stepsArea: FixedSizeContainer,
        infoArea: Container,
        jobRef: KorAtomicRef<Job?>,
        newJob: CompletableJob,
        startButton: UITextButton,
        forceRestart: Boolean,
        textFont: BitmapFont): Job {
    val job: Job = launch {
        try {
            play(
                    squares = squares,
                    unitContainers = unitContainers,
                    font = font,
                    activeContainer = activeContainer,
                    backgroundContainer = backgroundContainer,
                    resources = resources,
                    stepsArea = stepsArea,
                    infoArea = infoArea,
                    forceRestart = forceRestart,
                    textFont = textFont
            )
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
    job.invokeOnCompletion {
        if (jobRef.compareAndSet(expect = newJob, update = null)) {
            startButton.text = "PLAY"
        }

    }
    newJob.invokeOnCompletion {
        job.cancel()
    }
    return job
}

private fun Stage.simpleButton(
        font: BitmapFont,
        x: Double,
        y: Double,
        buttonText: String
): UITextButton {
    return uiTextButton {
        textColor = Colors["#0a2533"]
        text = buttonText
        textFont = Html.FontFace.Bitmap(font)
        textSize = 22
        shadowSize = 22
        shadowVisible = false
        height = 40.0
        width = 100.0
        position(x, y)
    }
}

private fun <T> KorAtomicRef<T>.getAndSet(value: T): T {
    while (true) {
        val oldValue = this.value
        if (compareAndSet(oldValue, value)) {
            return oldValue
        }
    }
}

val protoBuf = ProtoBuf()

private suspend fun play(
        squares: Map<Position, Point>,
        unitContainers: Map<Position, Container>,
        font: BitmapFont,
        activeContainer: Container,
        backgroundContainer: Container,
        resources: Resources,
        stepsArea: Container,
        infoArea: Container,
        forceRestart: Boolean,
        textFont: BitmapFont
) {
    HttpClient().use { client ->
        val response = client.request<ByteArray>(JobUrl) {
            method = if (forceRestart) HttpMethod.Put else HttpMethod.Get
        }
        val setupInfo = protoBuf.load(SetupInfo.serializer(), response)
        if (setupInfo.battleMap.sizeX != 6 || setupInfo.battleMap.sizeY != 8) {
            throw IllegalStateException("wrong battle map")
        }

        val fehArt = FehArtCache(localVfs("/FEH Art Collection"))
        val buildUnitView: suspend (UnitAdded) -> UnitView = { unitAdded: UnitAdded ->
            builtUnitView(unitAdded, font, resources.moveTypeBitmaps, resources.attackTypeBitmaps,
                    fehArt.get(unitAdded.imageName.ifEmpty { unitAdded.name }))
        }

        backgroundContainer.removeChildren()
        backgroundContainer.setupPlayArea(
                battleMapPositions = setupInfo.battleMap.battleMapPositions,
                resources = resources,
                squares = squares
        )
        val obstacleContainer = backgroundContainer.container()

        while (true) {
            obstacleContainer.removeChildren()
            activeContainer.removeChildren()
            unitContainers.values.forEach { it.removeChildren() }
            val obstacles = obstacleContainer.setupObstacles(setupInfo.battleMap.battleMapPositions,
                    resources.obstacle1, resources.obstacle2, squares)

            val moveSet = try {
                protoBuf.load(MoveSet.serializer(), client.get("$JobUrl/moveSet"))
            } catch (e: ClientRequestException) {
                return
            }
            infoArea.removeChildren()
            val text = "Attempts ${moveSet.totalTries}   Nodes ${moveSet.numberOfNodes}   " +
                       "Elapsed ${moveSet.elapsed.seconds.toTimeString(2)}"
            createTextWithShadow(infoArea, text, textFont, Point(0, 0), 28.0)
            val text2 = "Kill ${moveSet.enemyDied}   Death ${moveSet.playerDied}   Score ${moveSet.score}   Best ${moveSet.bestScore}  "
            val text2View = createTextWithShadow(infoArea, text2, textFont, Point(0, 30), 28.0)
            if (moveSet.ended) {
                createTextWithShadow(
                        infoArea = infoArea,
                        text = "End",
                        font = textFont,
                        point = Point(text2View.width, 30),
                        textSize = 28.0,
                        color = Colors.RED
                )
            }

            val units = mutableMapOf<Int, UnitView>()
            // add new units
            moveSet.moves.forEach {
                it.unitsAdded.forEach { unitAdded ->
                    units[unitAdded.unitId] ?: run {
                        val unitView = buildUnitView(unitAdded)
                        units[unitAdded.unitId] = unitView
                        unitView
                    }
                }
            }

            stepsArea.removeChildren()
            writeSteps(moveSet, units, stepsArea, textFont, 28.0)

            delay(200.milliseconds)
            playBack(moveSet, units, unitContainers, activeContainer, obstacles, 200.milliseconds)
            if (moveSet.ended) {
                obstacleContainer.removeChildren()
                activeContainer.removeChildren()
                unitContainers.values.forEach { it.removeChildren() }
                val newObstacles = obstacleContainer.setupObstacles(setupInfo.battleMap.battleMapPositions,
                        resources.obstacle1, resources.obstacle2, squares)
                delay(500.milliseconds)
                playBack(moveSet, units, unitContainers, activeContainer, newObstacles,
                        400.milliseconds)
                return
            }
            delay(500.milliseconds)
        }

    }
}

private suspend fun playBack(moveSet: MoveSet,
        units: Map<Int, UnitView>,
        unitContainers: Map<Position, Container>,
        activeContainer: Container,
        obstacles: Map<Position, ObstacleView>,
        playBackTime: TimeSpan) {
    moveSet.moves.forEach { updateInfo ->
        val action = updateInfo.action
        if (action.unitId != -1) {
            val unitView = units[action.unitId] ?: throw IllegalStateException()
            val parent = unitView.parent ?: throw IllegalStateException()
            if (unitView.boardX != action.moveX || unitView.boardY != action.moveY) {
                val targetContainer = unitContainers[Position(action.moveX, action.moveY)]
                                      ?: throw IllegalStateException()
                activeContainer.run {
                    unitView.position(parent.x, parent.y)
                    addChild(unitView)
                    tween(
                            unitView::x[targetContainer.x],
                            unitView::y[targetContainer.y],
                            time = playBackTime,
                            easing = Easing.EASE_IN_OUT
                    )
                }
                unitView.position(0, 0)
                targetContainer.addChild(unitView)
                unitView.boardX = action.moveX
                unitView.boardY = action.moveY
            }
            if (action.obstacleX != -1) {
                val obstacle = obstacles[Position(action.obstacleX, action.obstacleY)]
                               ?: error("obstacle not found")
                obstacle.breakObstacle()
            }
        }
        val positionChanged = updateInfo.unitUpdates.asSequence().mapNotNull {
            val unitView = units[it.unitId] ?: throw IllegalStateException()
            if (it.currentHp == 0) {
                unitView.parent!!.removeChild(unitView)
            } else if (it.currentHp != unitView.hp) {
                unitView.hp = it.currentHp
            }
            if (it.active != unitView.active) {
                unitView.active = it.active
            }
            if (it.x != unitView.boardX || it.y != unitView.boardY) {
                val parent = unitView.parent ?: return@mapNotNull null
                val targetContainer = unitContainers[Position(it.x, it.y)]
                                      ?: throw IllegalStateException()
                unitView.position(parent.x, parent.y)
                activeContainer.addChild(unitView)
                val pair = sequenceOf(unitView::x[targetContainer.x],
                        unitView::y[targetContainer.y]) to {
                    unitView.position(0, 0)
                    targetContainer.addChild(unitView)
                    unitView.boardX = action.moveX
                    unitView.boardY = action.moveY
                }
                pair
            } else {
                null
            }
        }.toList()
        if (positionChanged.isNotEmpty()) {
            activeContainer.tween(*positionChanged.asSequence().flatMap {
                it.first
            }.toList().toTypedArray(),
                    time = playBackTime,
                    easing = Easing.EASE_IN_OUT)
            positionChanged.asSequence().map {
                it.second
            }.forEach {
                it()
            }
        }
        val newViews = updateInfo.unitsAdded.map {
            val unitView = units[it.unitId] ?: throw IllegalStateException()
            unitView.resetUnit()
            unitView.boardX = it.startX
            unitView.boardY = it.startY
            val container = unitContainers[Position(unitView.boardX, unitView.boardY)]
                            ?: throw IllegalStateException()
            container.addChild(unitView)
            container
        }
        if (newViews.isNotEmpty()) {
            activeContainer.parent.tween(*newViews.map {
                it::colorMul[Colors.TRANSPARENT_BLACK, Colors.WHITE]
            }.toList().toTypedArray(),
                    time = playBackTime,
                    easing = Easing.EASE_IN_OUT)
        }
        delay(50.milliseconds)
    }
}

private fun createTextWithShadow(
        infoArea: Container,
        text: String,
        font: BitmapFont,
        point: IPoint,
        textSize: Double,
        color: RGBA = Colors.LIGHTGRAY
): Text {
    infoArea.text(text, textSize = textSize, color = Colors.BLACK, font = font) {
        position(point + Point(2, 2))
    }
    return infoArea.text(text, textSize = textSize, color = color, font = font) {
        position(point)
    }
}

private fun writeSteps(
        moveSet: MoveSet,
        units: MutableMap<Int, UnitView>,
        textArea: Container,
        font: BitmapFont,
        textSize: Double
) {
    val positionMap = mutableMapOf<Int, Pair<Int, Int>>()
    var y = 0.0
    var maxX = 0.0
    val uiScrollableArea = textArea.uiScrollableArea(width = textArea.width,
            height = textArea.height) {
        moveSet.moves.forEach {
            val action = it.action
            if (action.unitId != -1) {
                val unitView = units[action.unitId] ?: throw IllegalStateException()
                val target = if (action.targetUnitId != -1) {
                    units[action.targetUnitId] ?: throw IllegalStateException()
                } else {
                    null
                }
                val (unitX, unitY) = positionMap[action.unitId] ?: throw IllegalStateException()
                var x = 0.0
                action.toMessage(unitView, unitX, unitY, target) { message: String, color: RGBA ->
                    text(message, textSize = textSize, color = Colors.BLACK, font = font) {
                        position(x + 1, y + 1)
                    }
                    val text = text(message, textSize = textSize, color = color, font = font) {
                        position(x, y)
                    }
                    x += text.width
                }
                maxX = maxOf(maxX, x)
                y += textSize
                positionMap[action.unitId] = Pair(action.moveX, action.moveY)
            }
            it.unitUpdates.forEach { unitUpdate ->
                positionMap[unitUpdate.unitId] = Pair(unitUpdate.x, unitUpdate.y)
            }
            it.unitsAdded.forEach { unitAdded ->
                positionMap[unitAdded.unitId] = Pair(unitAdded.startX, unitAdded.startY)
            }
        }
    }
    val height = if (maxX <= uiScrollableArea.width) {
        uiScrollableArea.horizontalScroll = false
        uiScrollableArea.height
    } else {
        uiScrollableArea.contentWidth = maxX
        uiScrollableArea.viewportHeight
    }

    if (y <= height) {
        uiScrollableArea.verticalScroll = false
    } else {
        uiScrollableArea.contentHeight = y
        if (maxX > uiScrollableArea.viewportWidth) {
            uiScrollableArea.horizontalScroll = true
            uiScrollableArea.contentWidth = maxX
        }
    }
}

private inline fun Action.toMessage(
        unitView: UnitView,
        unitX: Int,
        unitY: Int,
        target: UnitView?,
        append: (message: String, color: RGBA) -> Unit
) {
    val unitColor: RGBA = if (unitView.playerControl) {
        COLOR_PLAYER
    } else {
        COLOR_ENEMY
    }
    val changeX = moveX - unitX
    val changeY = moveY - unitY
    val distance = changeX.absoluteValue + changeY.absoluteValue
    append(unitView.unitName, unitColor)
    val moveMessage = when {
                          distance == 0 -> " STANDS STILL"
                          distance <= 3 -> when {
                              changeX == 0 -> if (changeY < 0) {
                                  " MOVES ${-changeY} space Down"
                              } else {
                                  " MOVES $changeY space Up"
                              }
                              changeY == 0 -> if (changeX < 0) {
                                  " MOVES ${-changeX} space Left"
                              } else {
                                  " MOVES $changeX space Right"
                              }
                              changeX == 1 -> when (changeY) {
                                  1 -> " MOVES Up Right"
                                  -1 -> " MOVES Down Right"
                                  else -> null
                              }
                              changeX == -1 -> when (changeY) {
                                  1 -> " MOVES Up Left"
                                  -1 -> " MOVES Down Left"
                                  else -> null
                              }
                              else -> null
                          }
                          else -> null
                      } ?: when {
                          changeX == 0 -> if (changeY < 0) {
                              " MOVES ${-changeY} space Down"
                          } else {
                              " MOVES $changeY space Up"
                          }
                          changeY == 0 -> if (changeX < 0) {
                              " MOVES ${-changeX} space Left"
                          } else {
                              " MOVES $changeX space Right"
                          }
                          else -> {
                              val upDown = if (changeY > 0) "Up" else "Down"
                              val leftRight = if (changeX > 0) "Right" else "Left"
                              " MOVES $upDown ${changeY.absoluteValue} $leftRight ${changeX.absoluteValue}"
                          }
                      }
    if (target != null) {
        val targetColor: RGBA = if (target.playerControl) {
            COLOR_PLAYER
        } else {
            COLOR_ENEMY
        }
        append("$moveMessage and ", Colors.LIGHTGREY)
        if (target.playerControl != unitView.playerControl) {
            append("ATTACKS ", Colors.ORANGERED)
            append(target.unitName, targetColor)
        } else {
            append("ASSISTS ", Colors.LIGHTGREEN)
            append(target.unitName, targetColor)
        }
    } else if (obstacleX != -1) {
        val diffX = obstacleX - moveX
        val diffY = obstacleY - moveY
        val direction = when {
            diffX == 1 && diffY == 1 -> "on UPPER RIGHT"
            (diffX == 1 || diffX == 2) && diffY == 0 -> "on the RIGHT"
            diffX == 1 && diffY == -1 -> "on LOWER RIGHT"
            diffX == 0 && (diffY == 1 || diffY == 2) -> "ABOVE"
            diffX == 0 && (diffY == -1 || diffY == -2) -> "BELOW"
            diffX == -1 && diffY == 1 -> "on UPPER LEFT"
            (diffX == -1 || diffX == -2) && diffY == 0 -> "on the LEFT"
            diffX == -1 && diffY == -1 -> "on LOWER LEFT"
            else -> error("unknown position ($diffX, $diffY)")
        }

        append("$moveMessage and Break Obstacle $direction", Colors.LIGHTGREY)
    } else {
        append(moveMessage, Colors.LIGHTGREY)
    }
}


private fun builtUnitView(
        unitAdded: UnitAdded,
        font: BitmapFont,
        moveTypeBitmaps: Map<MoveType, BitmapSlice<Bitmap>>,
        attackTypeBitmaps: Map<AttackType, BitmapSlice<Bitmap>>,
        bitmapSlice: BitmapSlice<Bitmap>
): UnitView {
    return unitView(
            unitAdded = unitAdded,
            bitmapSlice = bitmapSlice,
            font = font,
            moveTypeBitmap = moveTypeBitmaps[unitAdded.moveType] ?: throw IllegalStateException(),
            attackTypeBitmap = attackTypeBitmaps[unitAdded.attackType]
                               ?: throw IllegalStateException()
    )
}

private suspend fun Stage.createPlayArea(
        topLeft: Point
): FixedSizeContainer {
    return fixedSizeContainer(540, 720, true) {
        position(topLeft)
        image(resourcesVfs["base_map.png"].readBitmap()) {
            position(0, 0)
        }
    }
}

private fun Container.setupObstacles(
        battleMapPositions: List<BattleMapPosition>,
        obstacle1: BitmapSlice<Bitmap>,
        obstacle2: BitmapSlice<Bitmap>,
        squares: Map<Position, Point>
): Map<Position, ObstacleView> {
    return battleMapPositions.filter {
        it.obstacle > 0
    }.associate {
        val position = Position(it.x, it.y)
        val point = squares[position] ?: throw IllegalStateException()
        position to obstacleView(it.obstacle, obstacle1, obstacle2) {
            position(point)
        }
    }
}

private fun Container.setupPlayArea(
        battleMapPositions: List<BattleMapPosition>,
        resources: Resources,
        squares: Map<Position, Point>
) {
    battleMapPositions.forEach {
        val position = Position(it.x, it.y)
        val point = squares[position] ?: throw IllegalStateException()
        container {
            position(point)
            val terrain = it.terrain
            if (terrain.isDefenseTile) {
                when (terrain.type) {
                    Terrain.Type.TRENCH -> image(resources.defenseTileTrench)
                    Terrain.Type.WALL, Terrain.Type.FOREST, Terrain.Type.REGULAR -> image(
                            resources.defenseTile)
                    Terrain.Type.FLIER_ONLY -> throw IllegalStateException(
                            "defense tile together with flier only")
                }
            } else {
                when (terrain.type) {
                    Terrain.Type.TRENCH -> image(resources.trench)
                    Terrain.Type.FLIER_ONLY -> image(resources.flyingTerrain)
                    Terrain.Type.WALL, Terrain.Type.FOREST, Terrain.Type.REGULAR -> Unit
                }
            }
            if (terrain.type == Terrain.Type.FOREST) {
                image(resources.tree) {
                    position(0, -13)
                }
            } else if (terrain.type == Terrain.Type.WALL) {
                image(resources.wall) {
                    scale = 0.5
                }
            }
        }
    }
}