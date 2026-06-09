package com.example.minesweeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinesweeperTheme {
                MinesweeperApp()
            }
        }
    }
}

private enum class GameStatus {
    Ready, Running, Won, Lost
}

private data class Difficulty(
    val label: String,
    val rows: Int,
    val cols: Int,
    val mines: Int
)

private data class Cell(
    val hasMine: Boolean = false,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
    val adjacentMines: Int = 0
)

private val difficulties = listOf(
    Difficulty("简单 9x9", rows = 9, cols = 9, mines = 10),
    Difficulty("中等 16x16", rows = 16, cols = 16, mines = 40),
    Difficulty("困难 16x30", rows = 16, cols = 30, mines = 99)
)

@Composable
private fun MinesweeperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = MaterialTheme.typography,
        content = content
    )
}

@Composable
private fun MinesweeperApp() {
    var difficulty by remember { mutableStateOf(difficulties.first()) }
    var board by remember { mutableStateOf(emptyBoard(difficulty)) }
    var gameStatus by remember { mutableStateOf(GameStatus.Ready) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    fun reset(newDifficulty: Difficulty = difficulty) {
        difficulty = newDifficulty
        board = emptyBoard(newDifficulty)
        gameStatus = GameStatus.Ready
        elapsedSeconds = 0
    }

    LaunchedEffect(gameStatus) {
        while (gameStatus == GameStatus.Running) {
            delay(1_000)
            elapsedSeconds += 1
        }
    }

    val flaggedCount = board.flatten().count { it.flagged }
    val minesLeft = difficulty.mines - flaggedCount

    Scaffold { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "扫雷",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                DifficultySelector(
                    selected = difficulty,
                    onSelected = { reset(it) }
                )

                Spacer(Modifier.height(12.dp))

                StatusPanel(
                    status = gameStatus,
                    minesLeft = minesLeft,
                    elapsedSeconds = elapsedSeconds,
                    onNewGame = { reset() }
                )

                Spacer(Modifier.height(12.dp))

                BoardView(
                    modifier = Modifier.weight(1f, fill = true),
                    board = board,
                    onReveal = { row, col ->
                        if (gameStatus == GameStatus.Won || gameStatus == GameStatus.Lost) return@BoardView

                        var currentBoard = board
                        if (gameStatus == GameStatus.Ready) {
                            currentBoard = generateBoard(difficulty, safeRow = row, safeCol = col)
                        }

                        val result = revealCell(currentBoard, row, col)
                        board = result.board
                        gameStatus = when {
                            result.exploded -> GameStatus.Lost
                            hasWon(result.board) -> GameStatus.Won
                            else -> GameStatus.Running
                        }
                    },
                    onToggleFlag = { row, col ->
                        if (gameStatus != GameStatus.Running) return@BoardView
                        board = toggleFlag(board, row, col)
                    }
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "点击翻开；长按插旗。首个点击位置一定安全。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DifficultySelector(
    selected: Difficulty,
    onSelected: (Difficulty) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center
    ) {
        difficulties.forEach { difficulty ->
            FilterChip(
                selected = selected == difficulty,
                onClick = { onSelected(difficulty) },
                label = { Text(difficulty.label) }
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun StatusPanel(
    status: GameStatus,
    minesLeft: Int,
    elapsedSeconds: Int,
    onNewGame: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "剩余雷数：$minesLeft", fontWeight = FontWeight.SemiBold)
                Text(text = "用时：${formatSeconds(elapsedSeconds)}")
                Text(text = statusText(status))
            }
            Button(onClick = onNewGame, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                Text("新游戏")
            }
        }
    }
}

@Composable
private fun BoardView(
    modifier: Modifier = Modifier,
    board: List<List<Cell>>,
    onReveal: (Int, Int) -> Unit,
    onToggleFlag: (Int, Int) -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            board.forEachIndexed { rowIndex, row ->
                Row {
                    row.forEachIndexed { colIndex, cell ->
                        MineCell(
                            cell = cell,
                            onReveal = { onReveal(rowIndex, colIndex) },
                            onToggleFlag = { onToggleFlag(rowIndex, colIndex) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MineCell(
    cell: Cell,
    onReveal: () -> Unit,
    onToggleFlag: () -> Unit
) {
    val cellBackground = when {
        cell.revealed && cell.hasMine -> Color(0xFFFFDAD6)
        cell.revealed -> MaterialTheme.colorScheme.surfaceVariant
        cell.flagged -> Color(0xFFFFF1B8)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val label = when {
        cell.revealed && cell.hasMine -> "💣"
        cell.flagged -> "🚩"
        cell.revealed && cell.adjacentMines > 0 -> cell.adjacentMines.toString()
        else -> ""
    }

    val labelColor = when (cell.adjacentMines) {
        1 -> Color(0xFF1565C0)
        2 -> Color(0xFF2E7D32)
        3 -> Color(0xFFC62828)
        4 -> Color(0xFF4527A0)
        5 -> Color(0xFF6D4C41)
        6 -> Color(0xFF00838F)
        7 -> Color(0xFF424242)
        8 -> Color(0xFF616161)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .padding(1.dp)
            .background(cellBackground, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = onReveal,
                onLongClick = onToggleFlag
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = if (label == "💣" || label == "🚩") 18.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

private data class RevealResult(
    val board: List<List<Cell>>,
    val exploded: Boolean
)

private fun emptyBoard(difficulty: Difficulty): List<List<Cell>> =
    List(difficulty.rows) { List(difficulty.cols) { Cell() } }

private fun generateBoard(difficulty: Difficulty, safeRow: Int, safeCol: Int): List<List<Cell>> {
    val safeZone = buildSet {
        for (row in (safeRow - 1)..(safeRow + 1)) {
            for (col in (safeCol - 1)..(safeCol + 1)) {
                if (row in 0 until difficulty.rows && col in 0 until difficulty.cols) {
                    add(row to col)
                }
            }
        }
    }

    val allCells = buildList {
        for (row in 0 until difficulty.rows) {
            for (col in 0 until difficulty.cols) {
                add(row to col)
            }
        }
    }

    val candidates = allCells.filterNot { it in safeZone }
        .ifEmpty { allCells.filterNot { it == (safeRow to safeCol) } }

    val mines = candidates
        .shuffled(Random(System.currentTimeMillis()))
        .take(difficulty.mines)
        .toSet()

    return List(difficulty.rows) { row ->
        List(difficulty.cols) { col ->
            val hasMine = row to col in mines
            Cell(
                hasMine = hasMine,
                adjacentMines = if (hasMine) 0 else countAdjacentMines(mines, row, col, difficulty.rows, difficulty.cols)
            )
        }
    }
}

private fun countAdjacentMines(
    mines: Set<Pair<Int, Int>>,
    row: Int,
    col: Int,
    rows: Int,
    cols: Int
): Int {
    var count = 0
    for (r in (row - 1)..(row + 1)) {
        for (c in (col - 1)..(col + 1)) {
            if (r == row && c == col) continue
            if (r in 0 until rows && c in 0 until cols && r to c in mines) count += 1
        }
    }
    return count
}

private fun revealCell(board: List<List<Cell>>, row: Int, col: Int): RevealResult {
    val mutableBoard = board.map { it.toMutableList() }.toMutableList()
    val start = mutableBoard[row][col]

    if (start.revealed || start.flagged) return RevealResult(board, exploded = false)

    if (start.hasMine) {
        for (r in mutableBoard.indices) {
            for (c in mutableBoard[r].indices) {
                if (mutableBoard[r][c].hasMine) {
                    mutableBoard[r][c] = mutableBoard[r][c].copy(revealed = true)
                }
            }
        }
        return RevealResult(mutableBoard.map { it.toList() }, exploded = true)
    }

    val queue = ArrayDeque<Pair<Int, Int>>()
    queue.add(row to col)

    while (queue.isNotEmpty()) {
        val (currentRow, currentCol) = queue.removeFirst()
        val current = mutableBoard[currentRow][currentCol]
        if (current.revealed || current.flagged || current.hasMine) continue

        mutableBoard[currentRow][currentCol] = current.copy(revealed = true)

        if (current.adjacentMines == 0) {
            for (r in (currentRow - 1)..(currentRow + 1)) {
                for (c in (currentCol - 1)..(currentCol + 1)) {
                    if (r == currentRow && c == currentCol) continue
                    if (r in mutableBoard.indices && c in mutableBoard[r].indices) {
                        val neighbor = mutableBoard[r][c]
                        if (!neighbor.revealed && !neighbor.flagged && !neighbor.hasMine) {
                            queue.add(r to c)
                        }
                    }
                }
            }
        }
    }

    return RevealResult(mutableBoard.map { it.toList() }, exploded = false)
}

private fun toggleFlag(board: List<List<Cell>>, row: Int, col: Int): List<List<Cell>> =
    board.mapIndexed { r, cells ->
        cells.mapIndexed { c, cell ->
            if (r == row && c == col && !cell.revealed) cell.copy(flagged = !cell.flagged) else cell
        }
    }

private fun hasWon(board: List<List<Cell>>): Boolean =
    board.flatten().all { it.hasMine || it.revealed }

private fun statusText(status: GameStatus): String = when (status) {
    GameStatus.Ready -> "状态：准备开始"
    GameStatus.Running -> "状态：游戏中"
    GameStatus.Won -> "状态：你赢了 🎉"
    GameStatus.Lost -> "状态：踩雷了 💥"
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
