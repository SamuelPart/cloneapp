package com.myiptv.clean.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SamplayCoinIconTest {
    @Test fun silverCoinHasTransparentCornersAndBluePaw() {
        val mainDirectory = listOf(File("src/main"), File("app/src/main"))
            .firstOrNull(File::isDirectory) ?: error("No se encuentra el código de la aplicación")
        val file = File(mainDirectory, "res/drawable-nodpi/samplay_coin_paw.png")
        assertTrue("La imagen de la moneda debe existir", file.isFile)

        // Local unit tests run on a JDK, while Android's Kotlin compilation excludes java.desktop.
        val imageIo = Class.forName("javax.imageio.ImageIO")
        val bufferedImage = Class.forName("java.awt.image.BufferedImage")
        val bitmap = imageIo.getMethod("read", File::class.java).invoke(null, file)
            ?: error("No se puede leer el PNG de la moneda")
        val width = bufferedImage.getMethod("getWidth").invoke(bitmap) as Int
        val height = bufferedImage.getMethod("getHeight").invoke(bitmap) as Int
        assertEquals(512, width)
        assertEquals(width, height)
        val rgbMethod = bufferedImage.getMethod(
            "getRGB", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
        )
        fun pixel(x: Int, y: Int) = rgbMethod.invoke(bitmap, x, y) as Int
        fun red(color: Int) = (color shr 16) and 0xff
        fun green(color: Int) = (color shr 8) and 0xff
        fun blue(color: Int) = color and 0xff

        for ((x, y) in listOf(0 to 0, width - 1 to 0, 0 to height - 1, width - 1 to height - 1)) {
            assertEquals("La moneda no debe cubrir el fondo", 0, pixel(x, y) ushr 24)
        }
        val paw = pixel(width / 2, height / 2)
        assertEquals("La huella debe ser visible", 255, paw ushr 24)
        assertTrue("La huella debe ser azul", blue(paw) > green(paw) && green(paw) > red(paw))
        val silver = pixel(width / 2, height / 6)
        assertEquals("La moneda debe ser visible", 255, silver ushr 24)
        assertTrue("El cuerpo plateado debe ser claro", minOf(red(silver), green(silver), blue(silver)) > 120)
        assertTrue("El cuerpo plateado debe ser neutro", blue(silver) - red(silver) < 30)
    }
}
