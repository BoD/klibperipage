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

@file:OptIn(ExperimentalUuidApi::class)

package org.jraf.klibperipage.internal

import com.juul.kable.Bluetooth
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import kotlinx.coroutines.flow.first
import org.jraf.klibnanolog.logd
import org.jraf.klibperipage.Peripage
import org.jraf.klibperipage.internal.util.resilientFlow
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

internal class PeripageImpl<P : Peripage.Printer>(
  private val printerProvider: (
    peripheral: Peripheral,
    transmitCharacteristic: Characteristic,
  ) -> P,
) : Peripage<P> {
  override suspend fun connect(onConnect: suspend Peripage.PrinterConnection<P>.() -> Unit) {
    logd("Scanning for PeriPage device")
    val advertisement = resilientFlow(30.seconds) { Scanner().advertisements }
      .first { it.name?.contains("PeriPage", ignoreCase = true) == true }
    logd("Found device: ${advertisement.identifier}")
    logd("Connecting")

    val peripheral = Peripheral(advertisement)
    peripheral.connect()
    logd("Connected to peripheral: ${peripheral.identifier}")

    val services = peripheral.services.first { it != null }!!
    val uartService = services.first { it.serviceUuid == Bluetooth.BaseUuid + 0xFF00 }
    val transmitCharacteristic = uartService.characteristics
      .first { it.characteristicUuid == Bluetooth.BaseUuid + 0xFF02 }

    val printer = PrinterImpl(peripheral = peripheral, transmitCharacteristic = transmitCharacteristic)
    printer.initialize()
    onConnect(PrinterConnectionImpl(printer = printerProvider(peripheral, transmitCharacteristic)))

    logd("Closing connection to peripheral ${peripheral.identifier}")
    peripheral.close()
  }
}
