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
import com.juul.kable.WriteType
import org.jraf.klibperipage.Peripage
import org.jraf.klibperipage.Peripage.Printer.Companion.A6_ROW_BYTES
import kotlin.jvm.JvmName

private const val MAX_CHUNK_ROWS = 0xFF // protocol caps each preamble at a 1-byte row count
private const val BLE_MAX_WRITE = 100

internal open class PrinterImpl(
  private val peripheral: Peripheral,
  private val transmitCharacteristic: Characteristic,
) : Peripage.Printer {
  private suspend fun transmit(bytes: ByteArray) {
    peripheral.write(
      characteristic = transmitCharacteristic,
      data = bytes,
      writeType = WriteType.WithResponse,
    )
  }

  private suspend fun transmit(bytesStr: String) {
    transmit(bytesStr.hexToByteArray())
  }

  @JvmName("transmitVarargs")
  private suspend fun transmit(vararg bytes: Byte) {
    transmit(bytes)
  }


  internal suspend fun initialize() {
    reset()
    setConcentration()
  }

  private suspend fun reset() {
    transmit("10FFFE01000000000000000000000000")
  }

  private suspend fun setConcentration() {
    transmit("10FF100002")
  }

  override suspend fun printImage(imageBytes: ByteArray) {
    require(imageBytes.size % A6_ROW_BYTES == 0) {
      "imageBytes size (${imageBytes.size}) must be a multiple of $A6_ROW_BYTES (384px / 8, 1 bit per pixel)"
    }
    val totalRows = imageBytes.size / A6_ROW_BYTES
    if (totalRows == 0) return

    var rowIndex = 0
    while (rowIndex < totalRows) {
      val rowsInChunk = minOf(MAX_CHUNK_ROWS, totalRows - rowIndex)

      // Each chunk must be preceded by a reset
      reset()

      // Preamble: 1d 76 30 00 <rowBytes> 00 <chunkRows> 00
      transmit(0x1d, 0x76, 0x30, 0x00, A6_ROW_BYTES.toByte(), 0x00, rowsInChunk.toByte(), 0x00)

      // Pixel rows - stream up to BLE_MAX_WRITE bytes per BLE write
      val pixelStart = rowIndex * A6_ROW_BYTES
      val pixelEnd = pixelStart + rowsInChunk * A6_ROW_BYTES
      var offset = pixelStart
      while (offset < pixelEnd) {
        val end = minOf(offset + BLE_MAX_WRITE, pixelEnd)
        transmit(imageBytes.copyOfRange(offset, end))
        offset = end
      }

      rowIndex += rowsInChunk
    }
  }

  override suspend fun lineFeed(lines: UByte) {
    if (lines == 0.toUByte()) return
    transmit(0x1B, 0x4A, lines.toByte())
  }

  override fun toString(): String {
    return "Printer(peripheral=${peripheral.identifier})"
  }
}
