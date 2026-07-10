package com.app.muzzutech.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.app.muzzutech.ui.compose.MuzzuTheme
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sqrt

class ImageEditorFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val imagePath = arguments?.getString("imagePath") ?: ""
        return ComposeView(requireContext()).apply {
            setContent {
                MuzzuTheme {
                    ImageEditorScreen(
                        imagePath = imagePath,
                        onSave = { findNavController().popBackStack() },
                        onCancel = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}

data class DrawPath(
    val path: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val mode: DrawMode = DrawMode.PEN
)

enum class DrawMode { PEN, RECT, CIRCLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    imagePath: String,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember {
        try {
            val options = BitmapFactory.Options().apply { inMutable = true }
            BitmapFactory.decodeFile(imagePath, options)
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap == null) {
        SideEffect { onCancel() }
        return
    }

    var drawMode by remember { mutableStateOf(DrawMode.PEN) }
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableFloatStateOf(10f) }
    
    val paths = remember { mutableStateListOf<DrawPath>() }
    val currentPathPoints = remember { mutableStateListOf<Offset>() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mark Issue") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (paths.isNotEmpty()) paths.removeAt(paths.size - 1)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    Button(
                        onClick = {
                            if (canvasSize != Size.Zero) {
                                saveMarkedImage(bitmap, paths, imagePath, canvasSize)
                                onSave()
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                // Color Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.White, Color.Black)
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 3.dp else 1.dp,
                                    color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = drawMode == DrawMode.PEN,
                        onClick = { drawMode = DrawMode.PEN },
                        label = { Text("Pen") },
                        leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) }
                    )
                    FilterChip(
                        selected = drawMode == DrawMode.RECT,
                        onClick = { drawMode = DrawMode.RECT },
                        label = { Text("Rect") },
                        leadingIcon = { Icon(Icons.Default.CropSquare, null, Modifier.size(18.dp)) }
                    )
                    FilterChip(
                        selected = drawMode == DrawMode.CIRCLE,
                        onClick = { drawMode = DrawMode.CIRCLE },
                        label = { Text("Circle") },
                        leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, null, Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = strokeWidth,
                    onValueChange = { strokeWidth = it },
                    valueRange = 2f..60f,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val imageBitmap = bitmap.asImageBitmap()
            
            BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val containerWidth = maxWidth
                val containerHeight = maxHeight
                
                val imageWidth = imageBitmap.width.toFloat()
                val imageHeight = imageBitmap.height.toFloat()
                
                val scale = minOf(
                    containerWidth.value * context.resources.displayMetrics.density / imageWidth,
                    containerHeight.value * context.resources.displayMetrics.density / imageHeight
                )
                
                val displayWidth = (imageWidth * scale / context.resources.displayMetrics.density).dp
                val displayHeight = (imageHeight * scale / context.resources.displayMetrics.density).dp

                Canvas(
                    modifier = Modifier
                        .size(displayWidth, displayHeight)
                        .pointerInput(drawMode, selectedColor, strokeWidth) {
                            detectDragGestures(
                                onDragStart = { offset -> currentPathPoints.add(offset) },
                                onDrag = { change, _ -> currentPathPoints.add(change.position) },
                                onDragEnd = {
                                    if (currentPathPoints.isNotEmpty()) {
                                        paths.add(DrawPath(currentPathPoints.toList(), selectedColor, strokeWidth, drawMode))
                                        currentPathPoints.clear()
                                    }
                                }
                            )
                        }
                ) {
                    canvasSize = size
                    drawImage(
                        image = imageBitmap,
                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                    )

                    paths.forEach { drawCustomPath(it, it.path) }
                    if (currentPathPoints.isNotEmpty()) {
                        drawCustomPath(DrawPath(currentPathPoints.toList(), selectedColor, strokeWidth, drawMode), currentPathPoints.toList())
                    }
                }
            }
        }
    }
}

fun DrawScope.drawCustomPath(drawPath: DrawPath, points: List<Offset>) {
    if (points.isEmpty()) return
    when (drawPath.mode) {
        DrawMode.PEN -> {
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
            drawPath(path, drawPath.color, style = Stroke(drawPath.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        DrawMode.RECT -> {
            val first = points.first()
            val last = points.last()
            drawRect(drawPath.color, Offset(minOf(first.x, last.x), minOf(first.y, last.y)), Size(abs(last.x - first.x), abs(last.y - first.y)), style = Stroke(drawPath.strokeWidth))
        }
        DrawMode.CIRCLE -> {
            val first = points.first()
            val last = points.last()
            val radius = sqrt((last.x - first.x) * (last.x - first.x) + (last.y - first.y) * (last.y - first.y))
            drawCircle(drawPath.color, radius, first, style = Stroke(drawPath.strokeWidth))
        }
    }
}

fun saveMarkedImage(originalBitmap: Bitmap, drawPaths: List<DrawPath>, path: String, canvasSize: Size) {
    val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)
    
    val scaleFactor = originalBitmap.width.toFloat() / canvasSize.width
    
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    drawPaths.forEach { dp ->
        paint.color = dp.color.toArgb()
        paint.strokeWidth = dp.strokeWidth * scaleFactor
        
        when (dp.mode) {
            DrawMode.PEN -> {
                val p = android.graphics.Path()
                if (dp.path.isNotEmpty()) {
                    p.moveTo(dp.path.first().x * scaleFactor, dp.path.first().y * scaleFactor)
                    for (i in 1 until dp.path.size) {
                        p.lineTo(dp.path[i].x * scaleFactor, dp.path[i].y * scaleFactor)
                    }
                    canvas.drawPath(p, paint)
                }
            }
            DrawMode.RECT -> {
                val first = dp.path.first()
                val last = dp.path.last()
                canvas.drawRect(
                    minOf(first.x, last.x) * scaleFactor,
                    minOf(first.y, last.y) * scaleFactor,
                    maxOf(first.x, last.x) * scaleFactor,
                    maxOf(first.y, last.y) * scaleFactor,
                    paint
                )
            }
            DrawMode.CIRCLE -> {
                val first = dp.path.first()
                val last = dp.path.last()
                val radius = sqrt((last.x - first.x) * (last.x - first.x) + (last.y - first.y) * (last.y - first.y))
                canvas.drawCircle(first.x * scaleFactor, first.y * scaleFactor, radius * scaleFactor, paint)
            }
        }
    }
    
    try {
        FileOutputStream(File(path)).use { out ->
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
