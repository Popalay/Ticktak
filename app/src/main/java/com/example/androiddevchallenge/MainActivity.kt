/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androiddevchallenge.ui.theme.MyTheme
import com.example.androiddevchallenge.ui.theme.darkGray
import com.example.androiddevchallenge.ui.theme.red
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.Rect as NativeRect

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                MyApp()
            }
        }
    }
}

// Start building your app here!
@Composable
fun MyApp() {
    val animatedDegree = remember { Animatable(1F) }
    var isStarted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun onButtonClicked() {
        isStarted = isStarted.not()
        scope.launch {
            if (isStarted) {
                animatedDegree.animateTo(
                    1F,
                    animationSpec = tween(
                        durationMillis = animatedDegree.value.degreesToMillis(),
                        easing = LinearEasing
                    )
                )
            } else {
                animatedDegree.stop()
            }
        }
    }

    fun onScrolled(delta: Float) {
        isStarted = false
        scope.launch {
            val newDegrees = -(animatedDegree.value + delta * 0.5F).absoluteValue % 360
            animatedDegree.snapTo(newDegrees)
        }
    }

    Surface(color = MaterialTheme.colors.background) {
        Column {
            WatchFace(
                sweepDegrees = animatedDegree.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75F)
                    .scrollable(
                        orientation = Orientation.Horizontal,
                        state = rememberScrollableState { delta ->
                            onScrolled(delta)
                            delta
                        },
                    )
            )
            Box(modifier = Modifier.fillMaxSize()) {
                TextButton(
                    onClick = ::onButtonClicked,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(if (isStarted) "Stop" else "Start")
                }
            }
        }
    }
}

@Composable
fun WatchFace(sweepDegrees: Float, modifier: Modifier = Modifier) {
    val paint = remember {
        Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val bounds = remember { NativeRect() }
    Canvas(modifier = modifier, onDraw = {
        val smallRadius = size.width / 4
        val bigRadius = smallRadius - 1.dp.toPx()
        val textRadius = smallRadius + 24.dp.toPx()

        val smallWidth = 4.dp.toPx()
        val bigWidth = 12.dp.toPx()

        val smallDash = 1.dp.toPx()
        val bigDash = 2.dp.toPx()

        val smallInterval = (2 * PI.toFloat() * smallRadius - 60 * smallDash) / 60
        val bigInterval = (2 * PI.toFloat() * bigRadius - 12 * bigDash) / 12

        drawCircle(
            color = darkGray,
            radius = smallRadius,
            style = Stroke(
                width = smallWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(smallDash, smallInterval))
            )
        )

        drawCircle(
            color = darkGray,
            radius = bigRadius,
            style = Stroke(
                width = bigWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(bigDash, bigInterval))
            )
        )

        drawCircle(
            color = darkGray,
            radius = smallRadius / 5
        )

        drawCircle(
            color = red,
            radius = smallRadius / 12
        )

        val path = Path().also {
            it.moveTo(center.x, center.y)
            it.arcTo(
                rect = Rect(
                    center = center,
                    radius = smallRadius + smallWidth / 2
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = sweepDegrees,
                forceMoveTo = false
            )
            it.close()
        }

        drawPath(
            color = red,
            path = path,
            alpha = 0.8F
        )

        drawCircle(
            color = darkGray,
            radius = smallRadius / 20
        )

        drawIntoCanvas { canvas ->
            (0..11).forEach {
                val labelDegrees = it * 30F
                val textSize = 18.sp.toPx() * sweepDegrees.degreesToLabelAmplifier(labelDegrees)
                val text = ((60 - it * 5) % 60).toString()
                paint.apply { this.textSize = textSize }
                paint.getTextBounds(text, 0, text.length, bounds)
                val coordinates = coordinatesOnCircle(textRadius, center, labelDegrees)
                val x = coordinates.x - bounds.width() / 2
                val y = coordinates.y + bounds.height() / 2
                canvas.nativeCanvas.drawText(text, x, y, paint)
            }
        }
    })
}

private fun coordinatesOnCircle(
    radius: Float,
    center: Offset = Offset.Zero,
    degree: Float
) = Offset(
    x = radius * sin(Math.toRadians(-degree + 180.0)).toFloat() + center.x,
    y = radius * cos(Math.toRadians(-degree + 180.0)).toFloat() + center.y
)

private fun Float.degreesToMillis() = (this * 1 / 6 * 1000).absoluteValue.toInt()

private fun Float.degreesToLabelAmplifier(labelDegrees: Float): Float {
    val actualDegree = (360 + this) % 360
    return if ((labelDegrees - actualDegree).absoluteValue <= 5) 1.2F else 1F
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun LightPreview() {
    MyTheme {
        MyApp()
    }
}
