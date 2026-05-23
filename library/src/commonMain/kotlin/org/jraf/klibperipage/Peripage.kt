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

package org.jraf.klibperipage

import kotlinx.io.files.Path

interface Peripage<out P : Peripage.Printer> {
  interface Printer {
    companion object {
      const val A6_ROW_WIDTH = 384

      // 384 px / 8 bits = 48 bytes per row
      const val A6_ROW_BYTES = A6_ROW_WIDTH / 8
    }

    suspend fun printImage(imageBytes: ByteArray)
    suspend fun lineFeed(lines: UByte)
  }

  interface CanPrintImageFilePrinter : Printer {
    suspend fun printImage(imagePath: Path)
  }

  interface PrinterConnection<out P : Printer> {
    val printer: P
  }

  suspend fun connect(onConnect: suspend PrinterConnection<P>.() -> Unit)
}
