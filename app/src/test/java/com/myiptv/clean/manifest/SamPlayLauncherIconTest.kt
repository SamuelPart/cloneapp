package com.myiptv.clean.manifest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.ceil
import kotlin.math.roundToInt

class SamPlayLauncherIconTest {
    private val mainDirectory = listOf(File("src/main"), File("app/src/main"))
        .firstOrNull(File::isDirectory) ?: error("No se encuentra el código de la aplicación")
    private val resources = File(mainDirectory, "res")
    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    private data class Density(val qualifier: String, val iconSize: Int, val foregroundSize: Int)

    private val densities = listOf(
        Density("mdpi", 48, 108),
        Density("hdpi", 72, 162),
        Density("xhdpi", 96, 216),
        Density("xxhdpi", 144, 324),
        Density("xxxhdpi", 192, 432),
    )

    private fun xml(path: String): Element = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(File(mainDirectory, path)).documentElement

    private class DecodedImage(val width: Int, val height: Int, private val pixels: IntArray) {
        fun getRGB(x: Int, y: Int): Int = pixels[y * width + x]
    }

    private fun image(path: String): DecodedImage {
        // Android's Kotlin test compilation excludes java.desktop, but local unit tests run on the
        // Gradle JDK. Load ImageIO at runtime to keep its color/alpha checks without Android imports.
        val imageIo = try {
            Class.forName("javax.imageio.ImageIO")
        } catch (error: ClassNotFoundException) {
            throw IllegalStateException("Las pruebas de iconos requieren un JDK con el módulo java.desktop", error)
        }
        val bufferedImage = Class.forName("java.awt.image.BufferedImage")
        val bitmap = imageIo.getMethod("read", File::class.java).invoke(null, File(resources, path))
            ?: error("No se puede leer el PNG: $path")
        val width = bufferedImage.getMethod("getWidth").invoke(bitmap) as Int
        val height = bufferedImage.getMethod("getHeight").invoke(bitmap) as Int
        val pixels = IntArray(width * height)
        val integer = Int::class.javaPrimitiveType
        bufferedImage.getMethod("getRGB", integer, integer, integer, integer, IntArray::class.java, integer, integer)
            .invoke(bitmap, 0, 0, width, height, pixels, 0, width)
        return DecodedImage(width, height, pixels)
    }

    private fun brightness(pixel: Int): Int = maxOf(
        (pixel shr 16) and 0xff,
        (pixel shr 8) and 0xff,
        pixel and 0xff,
    )

    @Test fun manifestUsesTheNewMobileIconsAndTvBanner() {
        val application = xml("AndroidManifest.xml")
            .getElementsByTagName("application").item(0) as Element
        assertEquals("@mipmap/ic_launcher", application.getAttributeNS(androidNamespace, "icon"))
        assertEquals("@mipmap/ic_launcher_round", application.getAttributeNS(androidNamespace, "roundIcon"))
        assertEquals("@drawable/samplay_tv_banner", application.getAttributeNS(androidNamespace, "banner"))
    }

    @Test fun adaptiveAndRoundIconsUseBlackAndTheSameLionForeground() {
        for (name in listOf("ic_launcher", "ic_launcher_round")) {
            val adaptive = xml("res/mipmap-anydpi-v26/$name.xml")
            assertEquals("adaptive-icon", adaptive.tagName)
            val background = adaptive.getElementsByTagName("background").item(0) as Element
            val foreground = adaptive.getElementsByTagName("foreground").item(0) as Element
            assertEquals("@color/black", background.getAttributeNS(androidNamespace, "drawable"))
            assertEquals("@mipmap/ic_launcher_foreground", foreground.getAttributeNS(androidNamespace, "drawable"))
        }
        val colors = xml("res/values/colors.xml").getElementsByTagName("color")
        val black = (0 until colors.length)
            .map { colors.item(it) as Element }
            .single { it.getAttribute("name") == "black" }
        assertEquals("#FF000000", black.textContent.trim().uppercase())
    }

    @Test fun launcherAssetsExistAtEveryDensityAndFitTheAdaptiveSafeZone() {
        for ((qualifier, iconSize, foregroundSize) in densities) {
            val directory = "mipmap-$qualifier"
            val icon = image("$directory/ic_launcher.png")
            val round = image("$directory/ic_launcher_round.png")
            val foreground = image("$directory/ic_launcher_foreground.png")

            assertEquals(iconSize, icon.width)
            assertEquals(iconSize, icon.height)
            assertEquals(iconSize, round.width)
            assertEquals(iconSize, round.height)
            assertEquals(foregroundSize, foreground.width)
            assertEquals(foregroundSize, foreground.height)
            assertEquals("Fondo negro ($qualifier)", 0xff000000.toInt(), icon.getRGB(0, 0))
            assertEquals("Esquina transparente del icono redondo ($qualifier)", 0, round.getRGB(0, 0) ushr 24)
            assertEquals("Fondo negro del icono redondo ($qualifier)", 0xff000000.toInt(), round.getRGB(iconSize / 2, 1))
            assertEquals("Fondo negro adaptable ($qualifier)", 0xff000000.toInt(), foreground.getRGB(0, 0))

            // The face remains light and the two eyes dark even at the smallest launcher size.
            assertTrue("Cara glass legible ($qualifier)", brightness(icon.getRGB(iconSize / 2, iconSize / 2)) > 120)
            val eyeY = iconSize / 2
            val leftEyeX = (iconSize * 0.38).roundToInt()
            val rightEyeX = (iconSize * 0.62).roundToInt()
            assertTrue("Ojo izquierdo oscuro ($qualifier)", brightness(icon.getRGB(leftEyeX, eyeY)) < 100)
            assertTrue("Ojo derecho oscuro ($qualifier)", brightness(icon.getRGB(rightEyeX, eyeY)) < 100)

            // Android's adaptive icon safe region is the central 66 dp of the 108 dp layer.
            val safeStart = ceil(foregroundSize * 21.0 / 108.0).toInt()
            val safeEnd = foregroundSize - safeStart
            var visiblePixels = 0
            for (y in 0 until foregroundSize) {
                for (x in 0 until foregroundSize) {
                    if (brightness(foreground.getRGB(x, y)) > 20) {
                        visiblePixels++
                        assertTrue("Mascota fuera de la zona segura ($qualifier): $x,$y",
                            x in safeStart until safeEnd && y in safeStart until safeEnd)
                    }
                }
            }
            assertTrue("La mascota no debe quedar vacía ($qualifier)", visiblePixels > foregroundSize * foregroundSize / 20)
        }
    }

    @Test fun animatedWelcomeMascotHasTransparentBackground() {
        val picture = image("drawable-nodpi/samplay_lion_animation_base.png")
        assertEquals(768, picture.width)
        assertEquals(768, picture.height)
        assertEquals("La mascota no debe cubrir el fondo de la pantalla", 0, picture.getRGB(0, 0) ushr 24)
        assertTrue("La mascota debe permanecer visible", brightness(picture.getRGB(384, 384)) > 120)
    }

    @Test fun tvBannerHasBlackBackgroundAndVisibleWhiteArtwork() {
        val banner = image("drawable-xhdpi/samplay_tv_banner.png")
        assertEquals(320, banner.width)
        assertEquals(180, banner.height)
        assertEquals(0xff000000.toInt(), banner.getRGB(0, 0))
        var whitePixels = 0
        for (y in 0 until banner.height) {
            for (x in 0 until banner.width) {
                if (brightness(banner.getRGB(x, y)) > 200) whitePixels++
            }
        }
        assertTrue("El banner debe mostrar la mascota y el nombre", whitePixels > 1500)
    }
}
