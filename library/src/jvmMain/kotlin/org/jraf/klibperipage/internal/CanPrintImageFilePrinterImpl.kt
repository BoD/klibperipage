/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2026-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jraf.klibperipage.internal

import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import kotlinx.io.files.Path
import org.jraf.klibperipage.Peripage
import org.jraf.klibperipage.Peripage.Printer
import java.io.File
import javax.imageio.ImageIO

internal class CanPrintImageFilePrinterImpl(
  peripheral: Peripheral,
  transmitCharacteristic: Characteristic,
) : PrinterImpl(
  peripheral = peripheral,
  transmitCharacteristic = transmitCharacteristic,
), Peripage.CanPrintImageFilePrinter {
  private fun imageToBytes(imageFile: Path): ByteArray {
    val img = ImageIO.read(File(imageFile.toString())) ?: error("Could not decode image: $imageFile")
    require(img.width == Printer.A6_ROW_WIDTH) { "Image width must be ${Printer.A6_ROW_WIDTH} px, was ${img.width}" }

    val out = ByteArray(Printer.A6_ROW_BYTES * img.height)
    for (y in 0 until img.height) {
      for (x in 0 until Printer.A6_ROW_WIDTH) {
        // Threshold on luminance so we don't depend on color model / palette / inversion.
        val rgb = img.getRGB(x, y)
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        val isDark = r + g + b < 3 * 128
        if (isDark) {
          val byteIndex = y * Printer.A6_ROW_BYTES + (x / 8)
          out[byteIndex] = (out[byteIndex].toInt() or (0x80 ushr (x % 8))).toByte()
        }
      }
    }
    return out
  }

  override suspend fun printImage(imagePath: Path) {
    printImage(imageToBytes(imagePath))
  }
}
